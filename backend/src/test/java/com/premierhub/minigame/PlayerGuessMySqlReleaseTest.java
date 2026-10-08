package com.premierhub.minigame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.premierhub.minigame.PlayerGuessData.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, empty disposable MySQL only. Never uses application/prod datasource variables. */
@EnabledIfEnvironmentVariable(named = "PRISMAXI_MINIGAME_MYSQL_TEST", matches = "true")
class PlayerGuessMySqlReleaseTest {
    private static final String URL = "jdbc:mysql://127.0.0.1:33028/minigame_release_check"
            + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&sslMode=DISABLED&allowPublicKeyRetrieval=true";
    private static final Path MIGRATION = Path.of("sql/2026-10-08-player-guess-mysql.sql");
    private static final Instant NOON = Instant.parse("2026-10-08T05:00:00Z");
    private static final Instant MIDNIGHT = Instant.parse("2026-10-08T17:00:00Z");

    @Test void reviewedMigrationAndRealRosterWorkOnDisposableMySqlWithoutChangingOtherFeatures() throws Exception {
        var source = new DriverManagerDataSource(URL, "root", "");
        var jdbc = new JdbcTemplate(source);
        assertEquals("minigame_release_check", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()", Integer.class),
                "Refuse a nonempty database; use a freshly initialized test server at loopback port 33028");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(source);
        for (String file : List.of("2026-10-04-accounts-mysql.sql", "2026-10-05-fantasy-gameweeks-mysql.sql",
                "2026-10-05-fantasy-entries-mysql.sql", "2026-10-05-fantasy-results-mysql.sql"))
            apply(source, Path.of("sql", file));
        jdbc.update("INSERT INTO accounts VALUES (101,'release-a@example.test','Release test A',NULL,'USER','2026-10-08'),"
                + "(102,'release-b@example.test','Release test B',NULL,'USER','2026-10-08')");
        jdbc.update("INSERT INTO account_identities VALUES ('google','synthetic-release-subject',101,'2026-10-08')");
        jdbc.update("INSERT INTO seasons VALUES (39,2024)");
        jdbc.update("INSERT INTO clubs VALUES (42,'Historical fixture club',NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2024,42)");
        jdbc.update("INSERT INTO players VALUES (42,'Historical fixture player')");
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,goals) VALUES (39,2024,42,42,11)");
        jdbc.update("INSERT INTO fantasy_gameweeks VALUES (2026,6,'2026-10-09','2026-10-08',1,'2026-10-09','OPEN',NULL,1,'2026-10-08')");
        jdbc.update("INSERT INTO fantasy_entries (account_id,season,gameweek,version) VALUES (101,2026,6,1)");
        var baselineBeforeMigration = snapshot(jdbc, false);
        apply(source, MIGRATION);
        assertEquals(baselineBeforeMigration, snapshot(jdbc, false));
        assertEquals(0, count(jdbc, "player_guess_questions"));
        assertEquals(0, count(jdbc, "player_guess_games"));
        var schema = schemas(jdbc);
        assertEquals(7, schema.size());
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        // Explicit test-only invocation imports reviewed CSVs into the disposable DB, never production.
        var imported = new PlayerGuessLocalDataCommand(jdbc, transactions, URL).importReviewedData(Path.of("data"));
        assertEquals(534, imported.rosterPlayers());
        assertEquals(364, imported.eligibleAnswers());
        var protectedRows = snapshot(jdbc, false);
        var repository = new PlayerGuessRepository(jdbc);
        var clock = new MutableClock(NOON);
        var service = new PlayerGuessService(repository, clock);
        var daily = transactions.execute(status -> service.start(101, Mode.DAILY, key(), null, 0).game());
        assertNotNull(daily);
        assertEquals(100, daily.currentScore());
        assertEquals(3, daily.revealedHintCount());
        assertEquals("ovr", daily.nextHintKey());
        assertNull(daily.answer());
        int answer = answer(repository, daily);
        var candidate = repository.candidates(daily.questionDate()).stream().filter(value -> value.playerId()==answer).findFirst().orElseThrow();
        assertTrue(candidate.eligible(daily.questionDate()));
        assertTrue(candidate.fc27Overall()>=75);
        assertEquals(1, candidate.membershipCount());
        assertEquals("2026-10-08 17:00:00", jdbc.queryForObject("SELECT DATE_FORMAT(expires_at,'%Y-%m-%d %H:%i:%s') FROM player_guess_questions WHERE id=?",
                String.class, repository.game(101, daily.gameId(), false).orElseThrow().questionId()));
        String action = key();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transactions.execute(status -> service.change(101, daily.gameId(), action, daily.version(), null)));
            var second = executor.submit(() -> transactions.execute(status -> service.change(101, daily.gameId(), action, daily.version(), null)));
            var a = first.get(10, TimeUnit.SECONDS);
            var b = second.get(10, TimeUnit.SECONDS);
            assertEquals(90, a.game().currentScore());
            assertEquals(a.game(), b.game());
            assertNotEquals(a.effect().replayed(), b.effect().replayed());
        }
        // New service/datasource simulates backend restart; progress/selector are in MySQL.
        var reopened = new DriverManagerDataSource(URL, "root", "");
        var reopenedTransactions = new TransactionTemplate(new DataSourceTransactionManager(reopened));
        var resumedService = new PlayerGuessService(new PlayerGuessRepository(new JdbcTemplate(reopened)), clock);
        var progress = reopenedTransactions.execute(status -> resumedService.current(101, Mode.DAILY).game());
        assertEquals(90, progress.currentScore());
        assertEquals(4, progress.revealedHintCount());
        assertEquals("VERSION_CONFLICT", transactions.execute(status -> service.change(101, daily.gameId(), key(), 0, answer)).code());
        var won = transactions.execute(status -> service.change(101, daily.gameId(), key(), progress.version(), answer).game());
        assertEquals(PlayerGuessRules.Status.WON, won.status());
        assertEquals(90, won.finalScore());
        var secondDaily = transactions.execute(status -> service.start(102, Mode.DAILY, key(), null, 0).game());
        assertEquals(answer, answer(repository, secondDaily));
        var secondProgress = transactions.execute(status -> service.change(102, secondDaily.gameId(), key(), 0, null).game());
        transactions.execute(status -> service.change(102, secondDaily.gameId(), key(), secondProgress.version(), answer));
        var standings = transactions.execute(status -> service.leaderboard(0, 20).players());
        assertEquals(List.of(1L,1L), standings.stream().map(Ranked::rank).toList());
        assertEquals(List.of(90L,90L), standings.stream().map(Ranked::totalPoints).toList());
        var practice = transactions.execute(status -> service.start(101, Mode.PRACTICE, key(), null, 0).game());
        assertNotEquals(answer, answer(repository, practice));
        transactions.execute(status -> service.change(101, practice.gameId(), key(), 0, answer(repository, practice)));
        assertEquals(2, count(jdbc, "player_guess_daily_results"));
        assertEquals(standings, transactions.execute(status -> service.leaderboard(0, 20).players()));
        var gameRows = snapshot(jdbc, true);
        apply(source, MIGRATION);
        apply(source, MIGRATION);
        assertEquals(gameRows, snapshot(jdbc, true), "Rerun must not reset snapshots, cycle, versions, scores or ledgers");
        assertEquals(schema, schemas(jdbc));
        assertEquals(protectedRows, snapshot(jdbc, false));
        // MySQL constraints, not merely service validation.
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE player_guess_games SET current_score=-1 WHERE id=?", daily.gameId()));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE player_guess_games SET account_id=999999 WHERE id=?", daily.gameId()));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE player_guess_games SET season=2024 WHERE id=?", daily.gameId()));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE player_guess_selector SET cycle_json='invalid' WHERE season=2026"));
        var dailyQuestion = repository.question(repository.game(101, daily.gameId(), false).orElseThrow().questionId());
        assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> repository.insertQuestion(new Question(key(), Mode.DAILY, dailyQuestion.date(),
                        dailyQuestion.expiresAt(), dailyQuestion.snapshot())));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> jdbc.update("INSERT INTO player_guess_daily_results VALUES (?,101,2026,?,40)", practice.gameId(), daily.questionDate()));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("DELETE FROM accounts WHERE id=101"));
        assertEquals(gameRows, snapshot(jdbc, true));
        // Any late failure rolls back the score and result together.
        clock.now = MIDNIGHT.minusSeconds(1);
        String unfinished = transactions.execute(status -> service.start(101, Mode.PRACTICE, key(), practice.gameId(), 1).game().gameId());
        var beforeRollback = snapshot(jdbc, true);
        assertThrows(IllegalStateException.class, () -> transactions.execute(status -> {
            service.change(101, unfinished, key(), 0, null);
            throw new IllegalStateException("Synthetic post-write failure");
        }));
        assertEquals(beforeRollback, snapshot(jdbc, true));
        clock.now = MIDNIGHT;
        var next = transactions.execute(status -> service.start(101, Mode.DAILY, key(), null, 0).game());
        assertEquals("2026-10-09", next.questionDate().toString());
        assertEquals(100, next.currentScore());
        assertNotEquals(answer, answer(repository, next));
        clock.now = MIDNIGHT.plusSeconds(86400);
        var expired = transactions.execute(status -> service.read(101, next.gameId()));
        assertEquals(PlayerGuessRules.Status.EXPIRED, expired.status());
        assertEquals(0, expired.finalScore());
        assertEquals(3, count(jdbc, "player_guess_daily_results"));
        long totalPoints = transactions.execute(status -> service.leaderboard(0, 20).players().getFirst().totalPoints());
        assertEquals(90L, totalPoints);
        assertEquals(protectedRows, snapshot(jdbc, false), "Accounts, Fantasy and football (including 2024) must remain identical");
        new ResourceDatabasePopulator(new FileSystemResource("sql/2026-10-08-player-guess-preflight.sql")).execute(source);
    }

    private static int answer(PlayerGuessRepository repository, GameView game) {
        return repository.question(repository.game(game.accountId(), game.gameId(), false).orElseThrow().questionId()).snapshot().playerId();
    }
    private static String key() { return UUID.randomUUID().toString(); }
    private static int count(JdbcTemplate jdbc, String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private static void apply(DriverManagerDataSource source, Path file) {
        new ResourceDatabasePopulator(new FileSystemResource(file)).execute(source);
    }
    private static Map<String,String> snapshot(JdbcTemplate jdbc, boolean minigame) {
        var result = new LinkedHashMap<String,String>();
        var json = JsonMapper.builder().build();
        for (String table : jdbc.queryForList("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME", String.class)) {
            if (table.startsWith("player_guess_")!=minigame) continue;
            var rows = jdbc.queryForList("SELECT * FROM `" + table + "`");
            result.put(table, json.writeValueAsString(rows.stream().map(json::writeValueAsString).sorted().toList()));
        }
        return result;
    }
    private static Map<String,String> schemas(JdbcTemplate jdbc) {
        var result = new LinkedHashMap<String,String>();
        for (String table : jdbc.queryForList("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'player\\_guess\\_%' ORDER BY TABLE_NAME", String.class))
            result.put(table, jdbc.queryForMap("SHOW CREATE TABLE `" + table + "`").get("Create Table").toString().replaceAll(" AUTO_INCREMENT=\\d+", ""));
        return result;
    }
    private static final class MutableClock extends Clock {
        private Instant now;
        private MutableClock(Instant now) { this.now=now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
