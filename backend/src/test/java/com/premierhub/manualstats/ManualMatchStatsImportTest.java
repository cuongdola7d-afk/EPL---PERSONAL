package com.premierhub.manualstats;

import com.premierhub.roster.ManualRosterImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:manual-match-stats-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class ManualMatchStatsImportTest {
    private static final Path ROSTER = Path.of("src/test/resources/manual-players-2026-example.csv");
    private static final Path EXAMPLE = Path.of("src/test/resources/manual-match-stats-2026-example.csv");

    @Autowired JdbcTemplate jdbc;
    @Autowired ManualRosterImporter rosters;
    @Autowired ManualMatchStatsImporter importer;
    @Autowired MockMvc mvc;
    @TempDir Path temp;

    private void seed() throws Exception {
        jdbc.update("INSERT INTO seasons VALUES (39, 2024), (39, 2026)");
        jdbc.update("INSERT INTO clubs VALUES (42, 'Legacy Club', NULL), "
                + "(1000000057, 'Arsenal FC', NULL), (1000000061, 'Chelsea FC', NULL), "
                + "(1000000070, 'Example FC', NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2024, 42), "
                + "(39, 2026, 1000000057), (39, 2026, 1000000061), (39, 2026, 1000000070)");
        jdbc.update("INSERT INTO players VALUES (101, 'Legacy Player')");
        jdbc.update("INSERT INTO player_season_stats VALUES (39, 2024, 101, 42, 'FORWARD', 1, 90, 1, 0)");
        rosters.importFile(ROSTER);
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals,
                  payload_hash, synced_at) VALUES
                  (900, 39, 2024, 42, 42, 1, '2024-08-16', 'FINISHED', 'FT', 1, 0, 'test', CURRENT_TIMESTAMP),
                  (901, 39, 2026, 1000000057, 1000000061, 1, '2026-09-14',
                   'FINISHED', 'FINISHED', 1, 0, 'test', CURRENT_TIMESTAMP),
                  (902, 39, 2026, 1000000057, 1000000061, 2, '2026-09-16',
                   'SCHEDULED', 'TIMED', NULL, NULL, 'test', CURRENT_TIMESTAMP),
                  (904, 39, 2026, 1000000057, 1000000070, 3, '2026-09-20',
                   'FINISHED', 'FINISHED', 2, 1, 'test', CURRENT_TIMESTAMP)
                """);
    }

    private Path csv(String... lines) throws Exception {
        Path file = Files.createTempFile(temp, "match-stats-", ".csv");
        Files.writeString(file, ManualMatchStatsCsvReader.HEADER + "\n"
                + String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return file;
    }

    private String row(int fixture, int player, String status, String rating,
                       String minutes, String goals) {
        return "2026," + fixture + "," + player + "," + status + "," + rating + ","
                + minutes + "," + goals + ",0,0,0";
    }

    @Test
    void importsTwiceWithoutDuplicatesAndHistoryReadsRealManualColumns() throws Exception {
        seed();
        assertEquals(2, importer.importFile(EXAMPLE).inserted());
        assertEquals(0, importer.importFile(EXAMPLE).inserted());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fixture_player_stats", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats WHERE season_year=2024", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2024", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT fantasy_points FROM manual_fixture_player_stats "
                + "WHERE player_id=2000000002", BigDecimal.class).compareTo(BigDecimal.ZERO));
        assertEquals(0, jdbc.queryForObject("SELECT fantasy_points FROM manual_fixture_player_stats "
                + "WHERE player_id=2000000001", BigDecimal.class).compareTo(new BigDecimal("7.25")));
        mvc.perform(get("/api/players/2000000001/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].match.id").value(901))
                .andExpect(jsonPath("$[0].evidenceStatus").value("MANUAL_VERIFIED"))
                .andExpect(jsonPath("$[0].stats.rating").value("7.25"))
                .andExpect(jsonPath("$[0].stats.minutes").value(90))
                .andExpect(jsonPath("$[0].stats.goals").value(1))
                .andExpect(jsonPath("$[0].stats.assists").value(0))
                .andExpect(jsonPath("$[0].stats.score").value(nullValue()))
                .andExpect(jsonPath("$[1].stats").value(nullValue()));
        mvc.perform(get("/api/players/2000000002/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stats.minutes").value(0))
                .andExpect(jsonPath("$[0].stats.rating").value(nullValue()))
                .andExpect(jsonPath("$[0].stats.goals").value(nullValue()));
        mvc.perform(get("/api/players/101/matches").param("season", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stats").value(nullValue()));
    }

    @Test
    void oneBatchCanImportMultipleFinishedFixturesAndRerunWithoutDuplicates() throws Exception {
        seed();
        Path batch = csv(row(901, 2000000001, "PLAYED", "7.20", "90", "1"),
                row(904, 2000000001, "PLAYED", "6.40", "75", "0"));

        assertEquals(2, importer.importFile(batch).inserted());
        assertEquals(0, importer.importFile(batch).inserted());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats WHERE fixture_id=901", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats WHERE fixture_id=904", Integer.class));
    }

    @Test
    void invalidFixtureOrMembershipLeavesWholeFileUntouched() throws Exception {
        seed();
        Path scheduled = csv(row(901, 2000000001, "PLAYED", "7.20", "90", "1"),
                row(902, 2000000002, "PLAYED", "6.50", "45", "0"));
        String error = assertThrows(IllegalArgumentException.class, () -> importer.importFile(scheduled))
                .getMessage();
        assertTrue(error.contains("CSV line 3"));
        assertTrue(error.contains("fixture_id=902"));
        assertTrue(error.contains("not FINISHED"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> importer.importFile(csv(row(900, 2000000001, "PLAYED", "7.20", "90", "1"))))
                .getMessage().contains("not a Premier League 2026/27 fixture"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> importer.importFile(csv(row(904, 2000000002, "PLAYED", "7.20", "90", "1"))))
                .getMessage().contains("membership"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
    }

    @Test
    void conflictingSavedRowRejectsWholeNewBatchWithoutOverwrite() throws Exception {
        seed();
        importer.importFile(EXAMPLE);
        Path changed = csv(row(904, 2000000001, "PLAYED", "6.10", "90", "0"),
                row(901, 2000000001, "PLAYED", "8.00", "90", "1"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> importer.importFile(changed))
                .getMessage().contains("conflicting saved"));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT rating FROM manual_fixture_player_stats "
                + "WHERE fixture_id=901 AND player_id=2000000001", BigDecimal.class)
                .compareTo(new BigDecimal("7.25")));
    }

    @Test
    void unknownPlayedRatingRemainsNullAndProviderRowCannotBeOverwritten() throws Exception {
        seed();
        Path partial = csv(row(904, 2000000001, "PLAYED", "", "", ""));
        assertEquals(1, importer.importFile(partial).inserted());
        assertEquals(null, jdbc.queryForObject("SELECT fantasy_points FROM manual_fixture_player_stats "
                + "WHERE fixture_id=904", BigDecimal.class));
        mvc.perform(get("/api/players/2000000001/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].stats.rating").value(nullValue()))
                .andExpect(jsonPath("$[2].stats.minutes").value(nullValue()));
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, raw_statistics, synced_at)
                VALUES (901, 2000000001, 1000000057, 'G', '{}', CURRENT_TIMESTAMP)
                """);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> importer.importFile(EXAMPLE))
                .getMessage().contains("provider fixture_player_stats"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
    }
}
