package com.premierhub.profiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileImportTest {
    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 2);
    private static final Path UNITED = Path.of("data/manchester-united-profiles-2026-10-02/players.csv");
    private static final Path ARSENAL = Path.of("data/arsenal-profiles-2026-10-02/players.csv");
    private static final Path LIVERPOOL = Path.of("data/liverpool-profiles-2026-10-02/players.csv");
    private static final Path MAN_CITY = Path.of("data/manchester-city-profiles-2026-10-02/players.csv");
    private static final Path CHELSEA = Path.of("data/chelsea-profiles-2026-10-02/players.csv");
    private static final Path TOTTENHAM = Path.of("data/tottenham-profiles-2026-10-02/players.csv");
    private JdbcTemplate jdbc;
    private PlayerProfileImporter importer;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:profiles-" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        importer = new PlayerProfileImporter(jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        jdbc.update("INSERT INTO seasons (league_id, season_year) VALUES (39, 2026)");
        for (int clubId : List.of(1000000066, 1000000057, 1000000064, 1000000065,
                1000000061, 1000000073)) {
            jdbc.update("INSERT INTO clubs (id, name) VALUES (?, ?)", clubId, "Test club " + clubId);
            jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) VALUES (39, 2026, ?)", clubId);
        }
        PlayerProfileCsvReader reader = new PlayerProfileCsvReader();
        for (Path file : List.of(UNITED, ARSENAL, LIVERPOOL, MAN_CITY, CHELSEA, TOTTENHAM)) {
            for (var row : reader.read(file)) {
                jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", row.playerId(), "Player " + row.playerId());
                jdbc.update("""
                        INSERT INTO player_season_stats (league_id, season_year, player_id, club_id)
                        VALUES (39, 2026, ?, ?)
                        """, row.playerId(), row.clubId());
                jdbc.update("""
                        INSERT INTO manual_player_memberships
                        (league_id, season_year, player_id, club_id, start_date, end_date)
                        VALUES (39, 2026, ?, ?, '2026-07-01', NULL)
                        """, row.playerId(), row.clubId());
            }
        }
    }

    @Test
    void importsLiverpoolAndManchesterCityBatchesIdempotently() throws Exception {
        assertEquals(31, importer.importFile(LIVERPOOL, AS_OF).inserted());
        assertEquals(26, importer.importFile(MAN_CITY, AS_OF).inserted());
        assertEquals(0, importer.importFile(LIVERPOOL, AS_OF).inserted());
        assertEquals(0, importer.importFile(MAN_CITY, AS_OF).inserted());
        assertEquals(57, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(51, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(6, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
    }

    @Test
    void importsChelseaAndTottenhamBatchesIdempotently() throws Exception {
        assertEquals(27, importer.importFile(CHELSEA, AS_OF).inserted());
        assertEquals(29, importer.importFile(TOTTENHAM, AS_OF).inserted());
        assertEquals(0, importer.importFile(CHELSEA, AS_OF).inserted());
        assertEquals(0, importer.importFile(TOTTENHAM, AS_OF).inserted());
        assertEquals(56, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(54, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(2, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
    }

    @Test
    void importsBothReviewedBatchesAndRerunsWithoutChanges() throws Exception {
        assertEquals(30, importer.importFile(UNITED, AS_OF).inserted());
        assertEquals(24, importer.importFile(ARSENAL, AS_OF).inserted());
        assertEquals(0, importer.importFile(UNITED, AS_OF).inserted());
        assertEquals(0, importer.importFile(ARSENAL, AS_OF).inserted());
        assertEquals(54, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(53, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM player_season_profiles
                WHERE player_id=2000030235 AND fc27_overall IS NULL
                """));
        assertEquals(72, jdbc.queryForObject("""
                SELECT fc27_overall FROM player_season_profiles WHERE player_id=2000001025
                """, Integer.class));
        assertEquals(1000000066, jdbc.queryForObject("""
                SELECT club_id FROM player_season_profiles WHERE player_id=2000006019
                """, Integer.class));
    }

    @Test
    void rejectsChangedContentWithoutOverwritingSavedProfiles() throws Exception {
        importer.importFile(UNITED, AS_OF);
        String changed = Files.readString(UNITED).replace("173,LEFT,16,79", "173,LEFT,16,78");
        Path conflict = Files.createTempFile(Path.of("target"), "profile-conflict-", ".csv");
        try {
            Files.writeString(conflict, changed);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> importer.importFile(conflict, AS_OF));
            assertTrue(error.getMessage().contains("conflicting saved profile"));
            assertEquals(30, count("SELECT COUNT(*) FROM player_season_profiles"));
            assertEquals(79, jdbc.queryForObject("""
                    SELECT fc27_overall FROM player_season_profiles WHERE player_id=2000006015
                    """, Integer.class));
        } finally {
            Files.deleteIfExists(conflict);
        }
    }

    @Test
    void rejectsExpiredMembershipBeforeWriting() throws Exception {
        jdbc.update("""
                UPDATE manual_player_memberships SET end_date='2026-10-01' WHERE player_id=2000006015
                """);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> importer.importFile(UNITED, AS_OF));
        assertTrue(error.getMessage().contains("active membership"));
        assertEquals(0, count("SELECT COUNT(*) FROM player_season_profiles"));
    }

    @Test
    void rejectsDuplicateIdsAndZeroOverall() throws Exception {
        String original = Files.readString(UNITED);
        String firstDataRow = original.lines().skip(1).findFirst().orElseThrow();
        PlayerProfileCsvReader reader = new PlayerProfileCsvReader();
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(new StringReader(original + firstDataRow + "\n")))
                .getMessage().contains("Duplicate player_id"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(new StringReader(original.replace("173,LEFT,16,79", "173,LEFT,16,0"))))
                .getMessage().contains("fc27_overall"));
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
