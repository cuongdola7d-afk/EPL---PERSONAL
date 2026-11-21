package com.premierhub.fpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:manual-extra-stats-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class ManualExtraStatsImportTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ManualExtraStatsImporter importer;
    @TempDir Path temp;

    private void seed() {
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO clubs VALUES (1000000057, 'Arsenal FC', NULL), (1000000061, 'Chelsea FC', NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, 1000000057), (39, 2026, 1000000061)");
        jdbc.update("INSERT INTO players VALUES (2000001001, 'Alex Alpha')");
        jdbc.update("""
                INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                VALUES (39, 2026, 2000001001, 1000000057, 'FORWARD')
                """);
        jdbc.update("""
                INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (39, 2026, 2000001001, 1000000057, DATE '2026-08-01', NULL)
                """);
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560639, 39, 2026, 1000000057, 1000000061, 10, DATE '2026-11-08',
                        'FINISHED', 'FINISHED', 1, 0, 'gw10', CURRENT_TIMESTAMP),
                       (1000560650, 39, 2026, 1000000057, 1000000061, 11, DATE '2026-11-21',
                        'FINISHED', 'FINISHED', 2, 1, 'gw11', CURRENT_TIMESTAMP)
                """);
        // Seed existing GW10 StatMuse row to confirm GW1-GW10 are untouched
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (1000560639, 2000001001, 39, 2026, 1000000057, 'PLAYED', 7.50, 7.50, 90, 1, 0, 0, 0)
                """);
    }

    private Path writeCsv(String... rows) throws Exception {
        Path file = Files.createTempFile(temp, "extra-stats-", ".csv");
        Files.writeString(file, ManualExtraStatsCsvReader.HEADER + "\n"
                + String.join("\n", rows) + "\n", StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void importsIdempotentlyRejectsConflictsAndProtectsGw1ThroughGw10() throws Exception {
        seed();
        Path valid = writeCsv(
                "2026,1000560650,2000001001,1,0,1,0,0,0,0,3,42,2,5,3,5,0.74,0.36,1.10,0.92,48.6,31.2,55.0,13.5,FPL,2026-11-21T20:00:00Z");

        assertEquals(1, importer.importFile(valid).inserted());
        assertEquals(0, importer.importFile(valid).inserted());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM manual_fixture_player_extra_stats WHERE fixture_id = 1000560650", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT expected_goals FROM manual_fixture_player_extra_stats WHERE fixture_id = 1000560650",
                BigDecimal.class).compareTo(new BigDecimal("0.74")));
        assertEquals(0, jdbc.queryForObject(
                "SELECT ict_index FROM manual_fixture_player_extra_stats WHERE fixture_id = 1000560650",
                BigDecimal.class).compareTo(new BigDecimal("13.5")));

        // Conflict against existing non-NULL value must be rejected without overwriting
        Path conflict = writeCsv(
                "2026,1000560650,2000001001,1,0,1,0,0,0,0,2,42,2,5,3,5,0.74,0.36,1.10,0.92,48.6,31.2,55.0,13.5,FPL,2026-11-21T20:05:00Z");
        IllegalArgumentException conflictErr = assertThrows(IllegalArgumentException.class,
                () -> importer.importFile(conflict));
        assertTrue(conflictErr.getMessage().contains("conflicting saved extra-stats row"));
        assertEquals(3, jdbc.queryForObject(
                "SELECT bonus FROM manual_fixture_player_extra_stats WHERE fixture_id = 1000560650", Integer.class));

        // GW1-GW10 backfill must be rejected and existing GW10 data untouched
        Path gw10Backfill = writeCsv(
                "2026,1000560639,2000001001,1,1,0,0,0,0,0,1,28,1,3,1,2,0.50,0.00,0.50,0.40,20.0,10.0,25.0,5.5,FPL,2026-11-21T20:00:00Z");
        IllegalArgumentException gw10Err = assertThrows(IllegalArgumentException.class,
                () -> importer.importFile(gw10Backfill));
        assertTrue(gw10Err.getMessage().contains("GW1-GW10"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM manual_fixture_player_extra_stats WHERE fixture_id = 1000560639", Integer.class));
        assertEquals(90, jdbc.queryForObject(
                "SELECT minutes FROM manual_fixture_player_stats WHERE fixture_id = 1000560639", Integer.class));
    }
}
