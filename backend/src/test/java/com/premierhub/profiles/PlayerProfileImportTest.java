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
    private static final Path BRIGHTON = Path.of("data/brighton-profiles-2026-10-02/players.csv");
    private static final Path BRENTFORD = Path.of("data/brentford-profiles-2026-10-02/players.csv");
    private static final Path LEEDS = Path.of("data/leeds-profiles-2026-10-02/players.csv");
    private static final Path EVERTON = Path.of("data/everton-profiles-2026-10-02/players.csv");
    private static final Path NEWCASTLE = Path.of("data/newcastle-united-profiles-2026-10-02/players.csv");
    private static final Path HULL = Path.of("data/hull-city-profiles-2026-10-02/players.csv");
    private static final Path IPSWICH = Path.of("data/ipswich-town-profiles-2026-10-02/players.csv");
    private static final Path FOREST = Path.of("data/nottingham-forest-profiles-2026-10-02/players.csv");
    private static final Path SUNDERLAND = Path.of("data/sunderland-profiles-2026-10-02/players.csv");
    private static final Path PALACE = Path.of("data/crystal-palace-profiles-2026-10-02/players.csv");
    private static final Path BOURNEMOUTH = Path.of("data/bournemouth-profiles-2026-10-02/players.csv");
    private static final Path VILLA = Path.of("data/aston-villa-profiles-2026-10-02/players.csv");
    private static final Path COVENTRY = Path.of("data/coventry-city-profiles-2026-10-02/players.csv");
    private static final Path FULHAM = Path.of("data/fulham-profiles-2026-10-02/players.csv");
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
                1000000061, 1000000073, 1000000397, 1000000402,
                1000000341, 1000000062, 1000000067, 1000000322,
                1000000349, 1000000351, 1000000071, 1000000354,
                1000001044, 1000000058, 1000001076, 1000000063)) {
            jdbc.update("INSERT INTO clubs (id, name) VALUES (?, ?)", clubId, "Test club " + clubId);
            jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) VALUES (39, 2026, ?)", clubId);
        }
        PlayerProfileCsvReader reader = new PlayerProfileCsvReader();
        for (Path file : List.of(UNITED, ARSENAL, LIVERPOOL, MAN_CITY, CHELSEA, TOTTENHAM,
                BRIGHTON, BRENTFORD, LEEDS, EVERTON, NEWCASTLE, HULL,
                IPSWICH, FOREST, SUNDERLAND, PALACE,
                BOURNEMOUTH, VILLA, COVENTRY, FULHAM)) {
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
    void importsBournemouthVillaCoventryAndFulhamBatchesIdempotently() throws Exception {
        assertEquals(26, importer.importFile(BOURNEMOUTH, AS_OF).inserted());
        assertEquals(27, importer.importFile(VILLA, AS_OF).inserted());
        assertEquals(30, importer.importFile(COVENTRY, AS_OF).inserted());
        assertEquals(26, importer.importFile(FULHAM, AS_OF).inserted());
        assertEquals(0, importer.importFile(BOURNEMOUTH, AS_OF).inserted());
        assertEquals(0, importer.importFile(VILLA, AS_OF).inserted());
        assertEquals(0, importer.importFile(COVENTRY, AS_OF).inserted());
        assertEquals(0, importer.importFile(FULHAM, AS_OF).inserted());
        assertEquals(109, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(105, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(4, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
        assertEquals(2, count("SELECT COUNT(*) FROM player_season_profiles WHERE height_cm IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE preferred_foot IS NULL"));
        assertEquals(2, count("SELECT COUNT(*) FROM player_season_profiles WHERE shirt_number IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE player_id=2000030180 AND height_cm IS NULL AND preferred_foot IS NULL AND shirt_number IS NULL AND fc27_overall IS NULL"));
    }

    @Test
    void importsIpswichForestSunderlandAndPalaceBatchesIdempotently() throws Exception {
        assertEquals(26, importer.importFile(IPSWICH, AS_OF).inserted());
        assertEquals(24, importer.importFile(FOREST, AS_OF).inserted());
        assertEquals(22, importer.importFile(SUNDERLAND, AS_OF).inserted());
        assertEquals(24, importer.importFile(PALACE, AS_OF).inserted());
        assertEquals(0, importer.importFile(IPSWICH, AS_OF).inserted());
        assertEquals(0, importer.importFile(FOREST, AS_OF).inserted());
        assertEquals(0, importer.importFile(SUNDERLAND, AS_OF).inserted());
        assertEquals(0, importer.importFile(PALACE, AS_OF).inserted());
        assertEquals(96, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(96, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(0, count("SELECT COUNT(*) FROM player_season_profiles WHERE nationality IS NULL OR birth_date IS NULL OR height_cm IS NULL OR preferred_foot IS NULL OR shirt_number IS NULL"));
        assertEquals(62, jdbc.queryForObject("SELECT fc27_overall FROM player_season_profiles WHERE player_id=2000030255", Integer.class));
        assertEquals(71, jdbc.queryForObject("SELECT fc27_overall FROM player_season_profiles WHERE player_id=2000030245", Integer.class));
    }

    @Test
    void importsNewcastleAndHullBatchesIdempotently() throws Exception {
        assertEquals(28, importer.importFile(NEWCASTLE, AS_OF).inserted());
        assertEquals(34, importer.importFile(HULL, AS_OF).inserted());
        assertEquals(0, importer.importFile(NEWCASTLE, AS_OF).inserted());
        assertEquals(0, importer.importFile(HULL, AS_OF).inserted());
        assertEquals(62, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(57, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(5, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
        assertEquals(3, count("SELECT COUNT(*) FROM player_season_profiles WHERE height_cm IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE preferred_foot IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE player_id=2000030242 AND height_cm IS NULL AND preferred_foot IS NULL AND fc27_overall IS NULL"));
    }

    @Test
    void importsLeedsAndEvertonBatchesIdempotently() throws Exception {
        assertEquals(22, importer.importFile(LEEDS, AS_OF).inserted());
        assertEquals(20, importer.importFile(EVERTON, AS_OF).inserted());
        assertEquals(0, importer.importFile(LEEDS, AS_OF).inserted());
        assertEquals(0, importer.importFile(EVERTON, AS_OF).inserted());
        assertEquals(42, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(41, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE height_cm IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE player_id=2000020081 AND height_cm IS NULL"));
    }

    @Test
    void importsBrightonAndBrentfordBatchesIdempotently() throws Exception {
        assertEquals(30, importer.importFile(BRIGHTON, AS_OF).inserted());
        assertEquals(28, importer.importFile(BRENTFORD, AS_OF).inserted());
        assertEquals(0, importer.importFile(BRIGHTON, AS_OF).inserted());
        assertEquals(0, importer.importFile(BRENTFORD, AS_OF).inserted());
        assertEquals(58, count("SELECT COUNT(*) FROM player_season_profiles"));
        assertEquals(55, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NOT NULL"));
        assertEquals(3, count("SELECT COUNT(*) FROM player_season_profiles WHERE fc27_overall IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE height_cm IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM player_season_profiles WHERE player_id=2000030176 AND height_cm IS NULL AND fc27_overall IS NULL"));
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
