package com.premierhub.minigame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static com.premierhub.minigame.PlayerGuessData.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerGuessPersistenceTest {
    @TempDir Path directory;

    @Test void databaseShutdownAndReopenKeepsPracticeProgressSnapshotAndDailyCycle() {
        String url = "jdbc:h2:file:" + directory.resolve("game").toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        var source = new DriverManagerDataSource(url, "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"), new ClassPathResource("auth-schema.sql"),
                new ClassPathResource("player-guess-schema.sql")).execute(source);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO accounts VALUES (101,'persist@example.test','Test persistence',NULL,'USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO seasons VALUES (39,2026)");
        jdbc.update("INSERT INTO clubs VALUES (1,'Test club',NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,1)");
        for (int id = 1; id <= 3; id++) {
            jdbc.update("INSERT INTO players VALUES (?,?)", id, "Persistence test player " + id);
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,?,1)", id);
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,1,'2026-07-01',NULL)", id);
            jdbc.update("INSERT INTO player_season_profiles VALUES (39,2026,?,1,'Vietnam','2000-01-01',180,'RIGHT',9,80)", id);
            jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,'ST')", id);
        }
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        var clock = Clock.fixed(Instant.parse("2026-10-08T05:00:00Z"), ZoneOffset.UTC);
        var service = new PlayerGuessService(new PlayerGuessRepository(jdbc), clock);
        var started = transactions.execute(status -> service.start(101, Mode.PRACTICE, UUID.randomUUID().toString(), null, 0).game());
        assertNotNull(started);
        var progress = transactions.execute(status -> service.change(101, started.gameId(), UUID.randomUUID().toString(), 0, null).game());
        String cycle = jdbc.queryForObject("SELECT cycle_json FROM player_guess_selector", String.class);
        jdbc.execute("SHUTDOWN");

        var reopenedSource = new DriverManagerDataSource(url, "sa", "");
        var reopenedJdbc = new JdbcTemplate(reopenedSource);
        var reopenedService = new PlayerGuessService(new PlayerGuessRepository(reopenedJdbc), clock);
        var reopenedTransactions = new TransactionTemplate(new DataSourceTransactionManager(reopenedSource));
        assertEquals(progress, reopenedTransactions.execute(status -> reopenedService.current(101, Mode.PRACTICE).game()));
        assertEquals(cycle, reopenedJdbc.queryForObject("SELECT cycle_json FROM player_guess_selector", String.class));
        assertEquals(2, reopenedJdbc.queryForObject("SELECT COUNT(*) FROM player_guess_questions", Integer.class));
        jdbc.execute("SHUTDOWN");
    }
}
