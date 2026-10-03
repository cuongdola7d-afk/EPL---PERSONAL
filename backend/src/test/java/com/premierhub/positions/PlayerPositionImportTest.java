package com.premierhub.positions;

import com.premierhub.service.FixtureEvidenceService;
import com.premierhub.service.FootballQueries;
import com.premierhub.service.MatchScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPositionImportTest {
    private JdbcTemplate jdbc;
    private PlayerPositionImporter importer;
    private FootballQueries queries;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:position-" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        importer = new PlayerPositionImporter(jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        queries = new FootballQueries(jdbc, new MatchScoringService(),
                new FixtureEvidenceService(jdbc, new ObjectMapper()));
        jdbc.update("INSERT INTO seasons VALUES (39, 2024), (39, 2026)");
        jdbc.update("INSERT INTO clubs (id, name) VALUES (1, 'Test FC')");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, 1)");
        for (int id : List.of(101, 102, 103)) {
            jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", id, "Player " + id);
            jdbc.update("""
                    INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                    VALUES (39, 2026, ?, 1, ?)
                    """, id, id == 103 ? "MIDFIELDER" : "FORWARD");
            jdbc.update("""
                    INSERT INTO manual_player_memberships
                    (league_id, season_year, player_id, club_id, start_date)
                    VALUES (39, 2026, ?, 1, '2026-07-01')
                    """, id);
        }
        jdbc.update("""
                INSERT INTO player_season_stats
                (league_id, season_year, player_id, club_id, position)
                VALUES (39, 2024, 101, 1, 'FORWARD')
                """);
    }

    @Test
    void returnsPerPlayerEligibilityIncludingRmAcrossBroadGroupAndMissingStatus() throws Exception {
        Path file = csv("101,2026,,,RW,RW|RM\n102,2026,,,RW,RW\n");
        assertEquals(new PlayerPositionImporter.Result(2, 2, 0), importer.importFile(file));
        assertEquals(new PlayerPositionImporter.Result(2, 0, 0), importer.importFile(file));
        var roster = queries.players(2026, null, null, LocalDate.of(2026, 10, 2));
        var rwRm = roster.stream().filter(p -> p.id() == 101).findFirst().orElseThrow();
        var rwOnly = roster.stream().filter(p -> p.id() == 102).findFirst().orElseThrow();
        var missing = roster.stream().filter(p -> p.id() == 103).findFirst().orElseThrow();
        assertEquals("FORWARD", rwRm.position());
        assertEquals("RW", rwRm.primaryPosition());
        assertEquals(List.of("RW", "RM"), rwRm.eligiblePositions());
        assertTrue(rwRm.eligiblePositions().contains("RM"));
        assertFalse(rwOnly.eligiblePositions().contains("RM"));
        assertEquals("MISSING", missing.positionStatus());
        assertNull(missing.primaryPosition());
        assertEquals(List.of(), missing.eligiblePositions());
        assertEquals("NOT_APPLICABLE", queries.player(101, 2024).orElseThrow().positionStatus());
        assertEquals("FORWARD", queries.player(101, 2024).orElseThrow().position());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM player_specific_positions WHERE season_year=2024", Integer.class));
    }

    @Test
    void explicitExpectedValuesAllowUpdateButStaleBatchConflictsAtomically() throws Exception {
        importer.importFile(csv("101,2026,,,RW,RW|RM\n"));
        Path changed = csv("101,2026,RW,RW|RM,RW,RW\n");
        assertEquals(new PlayerPositionImporter.Result(1, 0, 1), importer.importFile(changed));
        assertEquals(new PlayerPositionImporter.Result(1, 0, 0), importer.importFile(changed));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(
                csv("102,2026,,,RW,RW\n101,2026,RW,RW|RM,RW,RW|ST\n")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM player_specific_positions", Integer.class));
        assertEquals(List.of("RW"), queries.player(101, 2026).orElseThrow().eligiblePositions());
    }

    @Test
    void rejectsInvalidCodesMissingPrimaryDuplicateIdsAndOtherSeasons() {
        for (String row : List.of(
                "101,2026,,,RW,RW|RW",
                "101,2026,,,RW,RM",
                "101,2026,,,RW,RW|ST|INVALID",
                "101,2026,,,CDM,CDM",
                "101,2024,,,RW,RW",
                "999,2026,,,RW,RW")) {
            assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(row + "\n")), row);
        }
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(
                "101,2026,,,RW,RW\n101,2026,,,RM,RM\n")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM player_specific_positions", Integer.class));
    }

    private static Path csv(String rows) throws Exception {
        Path file = Files.createTempFile("player-positions-", ".csv");
        Files.writeString(file, PlayerPositionCsvReader.HEADER + "\n" + rows);
        file.toFile().deleteOnExit();
        return file;
    }
}
