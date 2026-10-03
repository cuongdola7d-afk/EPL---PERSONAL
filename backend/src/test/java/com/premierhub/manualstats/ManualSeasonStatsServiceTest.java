package com.premierhub.manualstats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:manual-season-stats-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class ManualSeasonStatsServiceTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ManualSeasonStatsService service;

    @BeforeEach
    void seedCompleteGameweek() {
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        for (int club = 100; club < 120; club++) {
            jdbc.update("INSERT INTO clubs (id, name) VALUES (?, ?)", club, "Club " + club);
            for (int slot = 1; slot <= 20; slot++) {
                int player = club * 100 + slot;
                jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", player, "Player " + player);
                jdbc.update("""
                        INSERT INTO player_season_stats
                        (league_id, season_year, player_id, club_id, position)
                        VALUES (39, 2026, ?, ?, 'MIDFIELDER')
                        """, player, club);
                jdbc.update("""
                        INSERT INTO manual_player_memberships
                        (league_id, season_year, player_id, club_id, start_date, end_date)
                        VALUES (39, 2026, ?, ?, DATE '2026-08-01', NULL)
                        """, player, club);
            }
        }
        jdbc.update("INSERT INTO players (id, name) VALUES (999999, 'Unused player')");
        jdbc.update("""
                INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                VALUES (39, 2026, 999999, 100, 'FORWARD')
                """);
        jdbc.update("INSERT INTO player_season_stats VALUES (39, 2024, 10001, 100, 'MIDFIELDER', 1, 90, 1, 0)");

        for (int match = 0; match < 10; match++) {
            int fixture = 1000 + match;
            int home = 100 + match * 2;
            int away = home + 1;
            jdbc.update("""
                    INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                                          gameweek, match_date, status, provider_status,
                                          home_goals, away_goals, payload_hash, synced_at)
                    VALUES (?, 39, 2026, ?, ?, 1, DATE '2026-08-15', 'FINISHED', 'FT',
                            1, 0, 'hash', CURRENT_TIMESTAMP)
                    """, fixture, home, away);
            for (int club : new int[] {home, away}) {
                for (int slot = 1; slot <= 20; slot++) {
                    int player = club * 100 + slot;
                    String status = player == 10003 ? "DID_NOT_PLAY" : "PLAYED";
                    Integer minutes = null;
                    Integer goals = null;
                    Integer assists = null;
                    if (status.equals("PLAYED")) {
                        minutes = 90;
                        goals = player == 10001 ? 1 : 0;
                        assists = 0;
                    }
                    if (player == 10002) {
                        minutes = null;
                        goals = null;
                    }
                    jdbc.update("""
                            INSERT INTO manual_fixture_player_stats
                            (fixture_id, player_id, league_id, season_year, club_id,
                             participation_status, minutes, goals, assists)
                            VALUES (?, ?, 39, 2026, ?, ?, ?, ?, ?)
                            """, fixture, player, club, status, minutes, goals, assists);
                }
            }
        }
    }

    @Test
    void rebuildsOnly2026TotalsAndRerunChangesNothing() {
        ManualSeasonStatsService.Result first = service.rebuildThroughGameweek(1);
        assertEquals(10, first.fixtures());
        assertEquals(400, first.matchRows());
        assertEquals(401, first.seasonRows());
        assertEquals(401, first.updatedRows());
        assertEquals(1, first.missingMinutes());
        assertEquals(1, first.missingGoals());
        assertEquals(0, first.missingAssists());

        assertEquals(1, number(10001, "appearances"));
        assertEquals(90, number(10001, "minutes"));
        assertEquals(1, number(10001, "goals"));
        assertNull(number(10002, "minutes"));
        assertNull(number(10002, "goals"));
        for (int player : new int[] {10003, 999999}) {
            assertEquals(0, number(player, "appearances"));
            assertEquals(0, number(player, "minutes"));
            assertEquals(0, number(player, "goals"));
            assertEquals(0, number(player, "assists"));
        }
        assertEquals(0, service.rebuildThroughGameweek(1).updatedRows());
        assertEquals(0, service.rebuildLatestComplete().updatedRows());
        assertEquals(1, jdbc.queryForObject("""
                SELECT goals FROM player_season_stats
                WHERE league_id=39 AND season_year=2024 AND player_id=10001
                """, Integer.class));
    }

    @Test
    void incompleteFixtureStopsBeforeAnySeasonRowIsWritten() {
        jdbc.update("DELETE FROM manual_fixture_player_stats WHERE fixture_id=1000 AND player_id=10001");
        assertThrows(IllegalStateException.class, () -> service.rebuildThroughGameweek(1));
        assertNull(number(10003, "appearances"));
        assertEquals(0, service.rebuildLatestComplete().updatedRows());
    }

    private Integer number(int player, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM player_season_stats "
                + "WHERE league_id=39 AND season_year=2026 AND player_id=?", Integer.class, player);
    }
}
