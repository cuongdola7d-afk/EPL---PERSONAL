package com.premierhub.minigame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.premierhub.minigame.PlayerGuessData.Mode.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerGuessLocalDataCommandTest {
    static final Path DATA = Path.of("data");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    JdbcTemplate jdbc;
    TransactionTemplate transactions;
    PlayerGuessLocalDataCommand loader;
    SingleConnectionDataSource source;
    @TempDir Path temporary;

    @BeforeEach void setup() {
        source = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"), new ClassPathResource("auth-schema.sql"),
                new ClassPathResource("player-guess-schema.sql")).execute(source);
        jdbc = new JdbcTemplate(source);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        loader = new PlayerGuessLocalDataCommand(jdbc, transactions, "jdbc:h2:mem:test");
        jdbc.update("INSERT INTO seasons VALUES (39,2024)");
        jdbc.update("INSERT INTO clubs VALUES (42,'Historical club',NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2024,42)");
        jdbc.update("INSERT INTO players VALUES (42,'Historical player')");
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,goals) VALUES (39,2024,42,42,11)");
        jdbc.update("INSERT INTO accounts VALUES (101,'local-data@example.test','Local data test',NULL,'USER',CURRENT_TIMESTAMP)");
    }

    @AfterEach void closeDatabase() { source.destroy(); }

    @Test void reviewedSnapshotIsPlayableAndPreservesMissingDataAndHistoricalRows() throws Exception {
        var result = loader.importReviewedData(DATA);
        assertEquals(534, result.rosterPlayers());
        assertEquals(20, count("season_clubs", "season_year=2026"));
        assertEquals(534, count("player_season_profiles", "season_year=2026"));
        assertEquals(532, count("player_specific_positions", "season_year=2026"));
        assertEquals(3, count("player_season_profiles", "season_year=2026 AND fc27_overall IS NULL"));
        assertEquals(1, count("player_season_profiles", "season_year=2026 AND height_cm IS NULL"));
        assertEquals(72, jdbc.queryForObject("SELECT fc27_overall FROM player_season_profiles WHERE player_id=2000001025", Integer.class));
        var candidates = new PlayerGuessRepository(jdbc).candidates(TODAY);
        assertEquals(534, candidates.size());
        assertEquals(result.eligibleAnswers(), candidates.stream().filter(candidate -> candidate.eligible(TODAY)).count());
        assertTrue(result.eligibleAnswers() > 100);
        assertTrue(candidates.stream().filter(candidate -> candidate.eligible(TODAY))
                .noneMatch(candidate -> candidate.playerId() == 2000001025 || candidate.playerId() == 2000030229));
        assertEquals(11, jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2024 AND player_id=42", Integer.class));
        assertEquals(1, count("seasons", "season_year=2024"));
        assertEquals(1, count("accounts", "id=101"));
        assertEquals(0, count("player_guess_games", "1=1"));
        assertEquals(0, count("player_guess_questions", "1=1"));

        var repository = new PlayerGuessRepository(jdbc);
        var service = new PlayerGuessService(repository, Clock.fixed(Instant.parse("2026-10-08T05:00:00Z"), ZoneOffset.UTC));
        var daily = transactions.execute(status -> service.start(101, DAILY, key(), null, 0).game());
        var practice = transactions.execute(status -> service.start(101, PRACTICE, key(), null, 0).game());
        assertNotNull(daily); assertNotNull(practice);
        int dailyAnswer = repository.question(repository.game(101, daily.gameId(), false).orElseThrow().questionId()).snapshot().playerId();
        int practiceAnswer = repository.question(repository.game(101, practice.gameId(), false).orElseThrow().questionId()).snapshot().playerId();
        assertNotEquals(dailyAnswer, practiceAnswer);
        assertNull(daily.answer()); assertNull(practice.answer());
        assertEquals(3, daily.hints().stream().filter(PlayerGuessData.Hint::revealed).count());
        var hinted = transactions.execute(status -> service.change(101, daily.gameId(), key(), daily.version(), null).game());
        var won = transactions.execute(status -> service.change(101, daily.gameId(), key(), hinted.version(), dailyAnswer).game());
        assertEquals(90, won.finalScore());
        assertEquals(1, count("player_guess_daily_results", "1=1"));

        String cycle = jdbc.queryForObject("SELECT cycle_json FROM player_guess_selector", String.class);
        var repeated = loader.importReviewedData(DATA);
        assertEquals(0, repeated.inserted());
        assertEquals(cycle, jdbc.queryForObject("SELECT cycle_json FROM player_guess_selector", String.class));
        assertEquals(won, transactions.execute(status -> service.current(101, DAILY).game()));
        assertEquals(practice, transactions.execute(status -> service.current(101, PRACTICE).game()));
    }

    @Test void conflictingIdentityRollsBackAllNewSeasonData() {
        jdbc.update("INSERT INTO players VALUES (2000001001,'Conflicting name')");
        assertThrows(IllegalStateException.class, () -> loader.importReviewedData(DATA));
        assertEquals(1, count("clubs", "1=1"));
        assertEquals(0, count("seasons", "season_year=2026"));
        assertEquals(0, count("player_season_profiles", "1=1"));
        assertEquals("Conflicting name", jdbc.queryForObject("SELECT name FROM players WHERE id=2000001001", String.class));
    }

    @Test void conflictingPositionsAfterRosterAndProfileWritesRollBackTheWholeImport() {
        seedPartialPlayer();
        jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,2000001001,'ST')");
        jdbc.update("INSERT INTO player_eligible_positions VALUES (39,2026,2000001001,'ST')");
        assertThrows(IllegalArgumentException.class, () -> loader.importReviewedData(DATA));
        assertEquals(2, count("players", "1=1"));
        assertEquals(1, count("season_clubs", "season_year=2026"));
        assertEquals(0, count("manual_player_memberships", "1=1"));
        assertEquals(0, count("player_season_profiles", "1=1"));
        assertEquals("ST", jdbc.queryForObject("SELECT primary_position FROM player_specific_positions WHERE player_id=2000001001", String.class));
    }

    @Test void savedMembershipIsNeverExtendedOrReplaced() {
        seedPartialPlayer();
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,2000001001,1000000057,'2026-09-22','2026-10-01')");
        assertThrows(IllegalStateException.class, () -> loader.importReviewedData(DATA));
        assertEquals(LocalDate.of(2026, 10, 1), jdbc.queryForObject("SELECT end_date FROM manual_player_memberships", java.sql.Date.class).toLocalDate());
        assertEquals(2, count("players", "1=1"));
        assertEquals(0, count("player_season_profiles", "1=1"));
    }

    @Test void incompleteSnapshotIsRejectedBeforeAnyWrites() throws Exception {
        Path roster = Files.createDirectories(temporary.resolve("roster-2026-09-30"));
        for (int batch = 1; batch <= 4; batch++) {
            String name = "batch-%02d.csv".formatted(batch);
            Files.copy(DATA.resolve("roster-2026-09-30").resolve(name), roster.resolve(name));
        }
        Path profiles = Files.createDirectories(temporary.resolve("player-data-production-2026-10-04"));
        for (String name : List.of("profiles-final.csv", "positions-final.csv", "club-coverage.csv")) {
            Files.copy(DATA.resolve("player-data-production-2026-10-04").resolve(name), profiles.resolve(name));
        }
        Path file = profiles.resolve("profiles-final.csv");
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Files.write(file, lines.subList(0, lines.size() - 1), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> loader.importReviewedData(temporary));
        assertEquals(1, count("clubs", "1=1"));
        assertEquals(0, count("seasons", "season_year=2026"));
    }

    @Test void applicationEntryRejectsMemoryMysqlOtherFilesAndUnexpectedJdbcOptions() {
        PlayerGuessLocalDataCommand.requireDedicatedLocalDatabase("jdbc:h2:file:./target/minigame-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE");
        PlayerGuessLocalDataCommand.requireDedicatedLocalDatabase("jdbc:h2:file:" + Path.of("target/minigame-local").toAbsolutePath().normalize());
        for (String url : List.of("jdbc:mysql://example.test/production", "jdbc:h2:mem:test", "jdbc:h2:tcp://localhost/test",
                "jdbc:h2:file:./premierhub-local", "jdbc:h2:file:./target/other-db", "jdbc:h2:file:./target/../other-db",
                "jdbc:h2:file:./target/minigame-local;INIT=RUNSCRIPT FROM 'unexpected.sql'")) {
            assertThrows(IllegalStateException.class, () -> PlayerGuessLocalDataCommand.requireDedicatedLocalDatabase(url));
        }
        assertThrows(IllegalStateException.class, () -> loader.run(null));
        assertEquals(0, count("seasons", "season_year=2026"));
    }

    private void seedPartialPlayer() {
        jdbc.update("INSERT INTO seasons VALUES (39,2026)");
        jdbc.update("INSERT INTO clubs VALUES (1000000057,'Arsenal FC',NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,1000000057)");
        jdbc.update("INSERT INTO players VALUES (2000001001,'David Raya')");
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,2000001001,1000000057,'GOALKEEPER')");
    }

    private int count(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class);
    }
    private static String key() { return UUID.randomUUID().toString(); }
}
