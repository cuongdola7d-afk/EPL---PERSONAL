package com.premierhub.diagnostics;

import com.premierhub.fantasy.FantasyEntryService;
import com.premierhub.service.FootballQueries;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReadLatencyTest {
    private HikariDataSource pool() {
        var pool = new HikariDataSource();
        pool.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        pool.setUsername("sa");
        pool.setMaximumPoolSize(1);
        pool.setMinimumIdle(0);
        return pool;
    }

    @Test void countsRepeatedAndSessionSqlAndPhysicalReuseWithoutLoggingValues() throws Exception {
        try (var pool = pool()) {
            var source = new MeasuredDataSource(pool);
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE TABLE SPRING_SESSION (SESSION_ID VARCHAR(100))");
            jdbc.update("INSERT INTO SPRING_SESSION VALUES (?)", "private-session-value");
            // No scope means no wrappers or query instrumentation.
            try (var connection = source.getConnection()) { assertTrue(connection.unwrap(Connection.class) != null); }
            try (var trace = ReadTrace.open()) {
                assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
                assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
                assertEquals("private-session-value", jdbc.queryForObject("SELECT SESSION_ID FROM SPRING_SESSION WHERE SESSION_ID=?",
                        String.class, "private-session-value"));
                var report = trace.report("/api/players", 200);
                assertEquals(3, report.statements());
                assertEquals(1, report.sessionStatements());
                assertEquals(3, report.acquisitions());
                assertEquals(2, report.reusedConnections());
                assertEquals(2, report.queries().size());
                assertTrue(report.queries().values().stream().anyMatch(q -> q.count() == 2));
                assertTrue(report.connectionMs() >= 0 && report.sqlExecuteMs() > 0 && report.sqlFetchMs() > 0);
                String json = JsonMapper.builder().build().writeValueAsString(report);
                assertFalse(json.contains("private-session-value"));
                assertFalse(json.contains("SESSION_ID"));
                assertFalse(json.contains("SELECT 1"));
            }
            assertNull(ReadTrace.current());
        }
    }

    @Test void transactionsCommitAndRollbackAndSqlExceptionsKeepTheirSemantics() {
        try (var pool = pool()) {
            var source = new MeasuredDataSource(pool);
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE TABLE sample (id INT PRIMARY KEY)");
            var transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
            try (var trace = ReadTrace.open()) {
                transactions.executeWithoutResult(status -> jdbc.update("INSERT INTO sample VALUES (1)"));
                transactions.executeWithoutResult(status -> { jdbc.update("INSERT INTO sample VALUES (2)"); status.setRollbackOnly(); });
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM sample", Integer.class));
                assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.queryForList("SELECT * FROM absent_table"));
                assertEquals(4, trace.report("test", 200).statements());
                assertTrue(trace.report("test", 200).jdbcControlMs() > 0);
                assertDoesNotThrow(() -> {
                    try (var connection = source.getConnection()) {
                        assertSame(pool, source.unwrap(HikariDataSource.class));
                        assertTrue(connection.isWrapperFor(Connection.class));
                        assertThrows(SQLException.class, () -> connection.createStatement().executeQuery("invalid sql"));
                    }
                });
            }
            assertNull(ReadTrace.current());
        }
    }

    @Test void probeRejectsMissingKeysAndPostAndOnlyReturnsMeasurements() throws Exception {
        try (var pool = pool()) {
            var jdbc = new JdbcTemplate(new MeasuredDataSource(pool));
            var football = mock(FootballQueries.class);
            var fantasy = mock(FantasyEntryService.class);
            var probe = new ReadLatencyProbe(jdbc, football, fantasy, new DataSourceTransactionManager(jdbc.getDataSource()));
            var environment = new MockEnvironment().withProperty("premierhub.diagnostics.read-latency.probe-enabled", "true")
                    .withProperty("PREMIERHUB_READ_LATENCY_KEY", "a".repeat(64))
                    .withProperty("PREMIERHUB_AUTH_PROXY_SECRET", "b".repeat(64));
            var filter = new ReadLatencyFilter(environment, probe, JsonMapper.builder().build());
            FilterChain denied = (request, response) -> fail("Operator probe must not enter session/security/business filters");
            var request = new MockHttpServletRequest("GET", ReadLatencyFilter.PROBE_PATH);
            request.setParameter("workload", "select1");
            var unauthorized = new MockHttpServletResponse();
            filter.doFilter(request, unauthorized, denied);
            assertEquals(404, unauthorized.getStatus());
            request.addHeader("X-PrismaXI-Read-Latency-Key", "a".repeat(64));
            request.addHeader("X-PrismaXI-Proxy-Secret", "b".repeat(64));
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, denied);
            assertEquals(200, response.getStatus());
            var body = JsonMapper.builder().build().readTree(response.getContentAsString());
            assertTrue(body.get("available").asBoolean());
            assertEquals(1, body.get("measurement").get("statements").asInt());
            assertFalse(response.getContentAsString().contains("SELECT 1"));
            assertFalse(response.getContentAsString().contains("a".repeat(64)));
            request.setMethod("POST");
            var post = new MockHttpServletResponse();
            filter.doFilter(request, post, denied);
            assertEquals(404, post.getStatus());
            verifyNoInteractions(football, fantasy);
            assertNull(ReadTrace.current());
        }
    }

    @Test void disabledProbeCannotBeAccessedAndRoutesContainNoGameIdentifiers() throws Exception {
        var filter = new ReadLatencyFilter(new MockEnvironment(), null, JsonMapper.builder().build());
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", ReadLatencyFilter.PROBE_PATH), response,
                (request, result) -> fail("Disabled probe must never run"));
        assertEquals(404, response.getStatus());
        assertEquals("/api/players/{id}", ReadLatencyFilter.route("/api/players/123"));
        assertEquals("/api/minigame/2026/player-guess/games/{id}",
                ReadLatencyFilter.route("/api/minigame/2026/player-guess/games/a8ea8957-a692-4031-844c-25e8f1d3ce64"));
        assertNotNull(ReadLatencyFilter.route("/api/minigame/2026/player-guess/practice/current"));
        assertNotNull(ReadLatencyFilter.route("/api/minigame/2026/player-guess/daily/current"));
        assertNull(ReadLatencyFilter.route("/api/auth/google/callback"));
        AtomicBoolean continued = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/players"), new MockHttpServletResponse(),
                (request, result) -> continued.set(ReadTrace.current() != null));
        assertTrue(continued.get());
        assertNull(ReadTrace.current());
    }

    @Test void minigameProjectionOnlyReadsExistingProgressAndNeverInvokesExpiryOrGameService() {
        try (var pool = pool()) {
            var jdbc = new JdbcTemplate(new MeasuredDataSource(pool));
            jdbc.execute("CREATE TABLE accounts (id BIGINT)");
            jdbc.execute("CREATE TABLE player_guess_games (id VARCHAR(50),account_id BIGINT,question_id VARCHAR(50),season INT,created_at TIMESTAMP,current_score INT)");
            jdbc.execute("CREATE TABLE player_guess_questions (id VARCHAR(50),snapshot_json VARCHAR(200))");
            jdbc.execute("CREATE TABLE player_guess_guesses (game_id VARCHAR(50),guess_number INT)");
            jdbc.update("INSERT INTO accounts VALUES (7)");
            jdbc.update("INSERT INTO player_guess_games VALUES ('game',7,'question',2026,CURRENT_TIMESTAMP,90)");
            jdbc.update("INSERT INTO player_guess_questions VALUES ('question','hidden-answer')");
            jdbc.update("INSERT INTO player_guess_guesses VALUES ('game',1)");
            var probe = new ReadLatencyProbe(jdbc, mock(FootballQueries.class), mock(FantasyEntryService.class),
                    new DataSourceTransactionManager(jdbc.getDataSource()));
            try (var trace = ReadTrace.open()) {
                assertTrue(probe.run("minigame"));
                var report = trace.report("probe/minigame", 200);
                assertEquals(5, report.statements());
                assertEquals(1, report.acquisitions(), "The read projection must use one consistent transaction");
                assertTrue(report.queries().values().stream().allMatch(query -> query.kind().equals("SELECT")));
                assertFalse(report.toString().contains("hidden-answer"));
            }
            assertEquals(90, jdbc.queryForObject("SELECT current_score FROM player_guess_games", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM player_guess_games", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM player_guess_questions", Integer.class));
        }
    }

    @Test void preparedBatchesCountEachSessionStatementAndRespectClearBatch() throws Exception {
        try (var pool = pool()) {
            var source = new MeasuredDataSource(pool);
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE TABLE SPRING_SESSION_ATTRIBUTES (id INT)");
            try (var trace = ReadTrace.open(); var connection = source.getConnection();
                 var statement = connection.prepareStatement("INSERT INTO SPRING_SESSION_ATTRIBUTES VALUES (?)")) {
                statement.setInt(1, 99);
                statement.addBatch();
                statement.clearBatch();
                statement.setInt(1, 1);
                statement.addBatch();
                statement.setInt(1, 2);
                statement.addBatch();
                assertEquals(2, statement.executeBatch().length);
                var report = trace.report("test", 200);
                assertEquals(2, report.statements());
                assertEquals(2, report.sessionStatements());
                assertEquals(1, report.executeCalls());
                assertEquals(2, report.queries().values().iterator().next().count());
            }
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES", Integer.class));
        }
    }
}
