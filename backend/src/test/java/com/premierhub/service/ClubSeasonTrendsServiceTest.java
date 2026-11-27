package com.premierhub.service;

import com.premierhub.web.ClubController;
import com.premierhub.web.dto.ClubSeasonTrendsResponse;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.AssisterEntry;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.RankPoint;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.ScorerEntry;
import com.premierhub.web.error.GlobalExceptionHandler;
import com.premierhub.web.error.InvalidFilterException;
import com.premierhub.web.error.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClubSeasonTrendsServiceTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private ClubSeasonTrendsService service;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        service = new ClubSeasonTrendsService(jdbc);
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
    }

    @AfterEach
    void close() {
        database.shutdown();
    }

    @Test
    void teamRatingIgnoresDidNotPlayAndNullRatingsAndMissingExtraReturnsNullXg() {
        club(1, "Alpha FC");
        club(2, "Beta FC");
        player(10, "P1");
        player(11, "P2");
        player(12, "P3");
        player(13, "P4");
        player(20, "Opp1");

        // GW1: away match with ratings (7.10, 7.20, null, DNP 9.90) -> avg = 7.15; extra stats on both sides
        fixture(100, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 2, 1, 1, 2);
        stat(100, 10, 1, "PLAYED", "7.10", 1, 0);
        stat(100, 11, 1, "PLAYED", "7.20", 1, 1);
        stat(100, 12, 1, "PLAYED", null, 0, 0);
        stat(100, 13, 1, "DID_NOT_PLAY", "9.90", 5, 5);
        stat(100, 20, 2, "PLAYED", "6.80", 1, 0);
        extra(100, 10, 1, "0.85");
        extra(100, 11, 1, "0.42");
        extra(100, 13, 1, "3.00"); // DID_NOT_PLAY extra must be ignored
        extra(100, 20, 2, "1.11");

        // GW2: home match with NO ratings and NO extra rows -> teamRating=null, xgFor=null, xgAgainst=null
        fixture(101, 2, "2026-08-22", "2026-08-22T16:30:00Z", "FINISHED", 1, 2, 3, 0);
        stat(101, 10, 1, "PLAYED", null, 2, 0);
        stat(101, 11, 1, "PLAYED", null, 1, 1);

        ClubSeasonTrendsResponse result = service.trends(1, 2026);
        assertEquals(2, result.matches().size());

        var m1 = result.matches().get(0);
        assertEquals(100, m1.fixtureId());
        assertFalse(m1.home());
        assertEquals(2, m1.opponentClubId());
        assertEquals("Beta FC", m1.opponentName());
        assertEquals(2, m1.goalsFor());
        assertEquals(1, m1.goalsAgainst());
        assertEquals(Instant.parse("2026-08-15T14:00:00Z"), m1.kickoffUtc());
        assertEquals(7.15, m1.teamRating());
        assertEquals(1.27, m1.xgFor());
        assertEquals(1.11, m1.xgAgainst());

        var m2 = result.matches().get(1);
        assertEquals(101, m2.fixtureId());
        assertTrue(m2.home());
        assertEquals(2, m2.opponentClubId());
        assertEquals(3, m2.goalsFor());
        assertEquals(0, m2.goalsAgainst());
        assertNull(m2.teamRating());
        assertNull(m2.xgFor());
        assertNull(m2.xgAgainst());
    }

    @Test
    void homeAndAwayPerspectiveAndKickoffChronologicalOrder() {
        club(1, "Liverpool FC");
        club(2, "Arsenal FC");
        club(3, "Chelsea FC");

        player(10, "Liv1");
        player(20, "Ars1");
        player(30, "Che1");

        // Later fixture inserted first to verify sorting by kickoffUtc
        fixture(202, 2, "2026-08-29", "2026-08-29T11:30:00Z", "FINISHED", 1, 3, 3, 1);
        stat(202, 10, 1, "PLAYED", "7.50", 3, 0);
        stat(202, 30, 3, "PLAYED", "6.20", 1, 0);
        extra(202, 10, 1, "2.15");
        extra(202, 30, 3, "0.75");

        fixture(201, 1, "2026-08-23", "2026-08-23T15:30:00Z", "FINISHED", 2, 1, 0, 2);
        stat(201, 10, 1, "PLAYED", "7.80", 2, 0);
        stat(201, 20, 2, "PLAYED", "6.40", 0, 0);
        extra(201, 10, 1, "1.60");
        extra(201, 20, 2, "0.90");

        var result = service.trends(1, 2026);
        assertEquals(List.of(201, 202), result.matches().stream().map(ClubSeasonTrendsResponse.MatchTrend::fixtureId).toList());

        var away = result.matches().get(0);
        assertFalse(away.home());
        assertEquals("Arsenal FC", away.opponentName());
        assertEquals(2, away.goalsFor());
        assertEquals(0, away.goalsAgainst());
        assertEquals(1.60, away.xgFor());
        assertEquals(0.90, away.xgAgainst());

        var home = result.matches().get(1);
        assertTrue(home.home());
        assertEquals("Chelsea FC", home.opponentName());
        assertEquals(3, home.goalsFor());
        assertEquals(1, home.goalsAgainst());
        assertEquals(2.15, home.xgFor());
        assertEquals(0.75, home.xgAgainst());
    }

    @Test
    void rankHistoryBreaksTiesByGoalDifferenceThenGoalsForThenNameAndIgnoresStandingsTable() {
        club(1, "Bravo FC");
        club(2, "Alpha FC");
        club(3, "Charlie FC");
        club(4, "Delta FC");
        club(5, "Echo FC");
        club(6, "Foxtrot FC");

        // Deliberately poison standings table to verify service does NOT read standings
        jdbc.update("INSERT INTO standings VALUES (39, 2026, 1, 20, 1, 0, 0, 1, 0, 99, -99, 0)");

        // GW1:
        // Echo beats Foxtrot 4-1 -> Echo 3 pts, GD +3, GF 4 -> #1
        // Charlie beats Delta 3-1 -> Charlie 3 pts, GD +2, GF 3 -> #2
        // Alpha beats Bravo? Wait: let's have 3 teams tied on 3 pts & GD +1 with different GF and names:
        // Let's make 6 clubs -> 3 matches:
        // Match A: Delta (4) 2-1 Foxtrot (6)  -> Delta: 3 pts, GD +1, GF 2
        // Match B: Charlie (3) 3-2 Echo (5)   -> Charlie: 3 pts, GD +1, GF 3 (beats Delta on GF!)
        // Match C: Alpha (2) 2-1 Bravo (1)    -> Alpha: 3 pts, GD +1, GF 2 (ties Delta on pts/GD/GF, beats Delta alphabetically A < D!)
        fixture(301, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 4, 6, 2, 1);
        fixture(302, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 3, 5, 3, 2);
        fixture(303, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 2, 1, 2, 1);

        // GW1 order:
        // 1. Charlie (3 pts, +1, 3 GF)
        // 2. Alpha   (3 pts, +1, 2 GF, "Alpha FC")
        // 3. Delta   (3 pts, +1, 2 GF, "Delta FC")
        // 4. Echo    (0 pts, -1, 2 GF)
        // 5. Bravo   (0 pts, -1, 1 GF, "Bravo FC")
        // 6. Foxtrot (0 pts, -1, 1 GF, "Foxtrot FC")
        assertEquals(List.of(new RankPoint(1, 1)), service.trends(3, 2026).rankHistory());
        assertEquals(List.of(new RankPoint(1, 2)), service.trends(2, 2026).rankHistory());
        assertEquals(List.of(new RankPoint(1, 3)), service.trends(4, 2026).rankHistory());
        assertEquals(List.of(new RankPoint(1, 4)), service.trends(5, 2026).rankHistory());
        assertEquals(List.of(new RankPoint(1, 5)), service.trends(1, 2026).rankHistory());
        assertEquals(List.of(new RankPoint(1, 6)), service.trends(6, 2026).rankHistory());

        // GW2: Bravo beats Charlie 3-0 -> Bravo now 3 pts, GD +2, GF 4 (jumps to #1 on GD!)
        fixture(304, 2, "2026-08-22", "2026-08-22T14:00:00Z", "FINISHED", 1, 3, 3, 0);
        assertEquals(List.of(new RankPoint(1, 5), new RankPoint(2, 1)), service.trends(1, 2026).rankHistory());
    }

    @Test
    void postponedFixtureIsIgnoredUntilFinishedAndRescheduledFixtureCountsAtOriginalGameweek() {
        club(1, "Alpha FC");
        club(2, "Beta FC");
        club(3, "Gamma FC");
        club(4, "Delta FC");

        // GW1: Alpha vs Beta POSTPONED; Gamma beats Delta 1-0
        fixture(401, 1, "2026-08-15", "2026-08-15T14:00:00Z", "POSTPONED", 1, 2, null, null);
        fixture(402, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 3, 4, 1, 0);

        // GW2: Alpha beats Delta 2-0; Gamma draws Beta 0-0
        fixture(403, 2, "2026-08-22", "2026-08-22T14:00:00Z", "FINISHED", 1, 4, 2, 0);
        fixture(404, 2, "2026-08-22", "2026-08-22T14:00:00Z", "FINISHED", 3, 2, 0, 0);

        // While GW1 match is postponed:
        // After GW1: Gamma 3pts (#1), Alpha 0pts 0GD (#2), Beta 0pts 0GD (#3), Delta 0pts -1GD (#4)
        // After GW2: Gamma 4pts (+1), Alpha 3pts (+2), Beta 1pt (0), Delta 0pts (-3) -> Alpha is #2
        var beforeCatchup = service.trends(1, 2026);
        assertEquals(1, beforeCatchup.matches().size());
        assertEquals(List.of(new RankPoint(1, 2), new RankPoint(2, 2)), beforeCatchup.rankHistory());

        // When postponed GW1 match is later played (in Sep) and marked FINISHED at original gameweek=1:
        // Alpha beats Beta 3-0 -> Alpha jumps to #1 in GW1 (3 pts, +3) AND #1 in GW2 (6 pts, +5)
        jdbc.update("UPDATE fixtures SET status='FINISHED', match_date=DATE '2026-09-10', home_goals=3, away_goals=0 WHERE id=401");
        jdbc.update("UPDATE football_data_fixtures SET kickoff_utc='2026-09-10T19:00:00Z' WHERE fixture_id=401");

        var afterCatchup = service.trends(1, 2026);
        assertEquals(2, afterCatchup.matches().size());
        // Matches sorted by kickoffUtc: GW2 (Aug 22) then rescheduled GW1 (Sep 10)
        assertEquals(List.of(403, 401), afterCatchup.matches().stream().map(ClubSeasonTrendsResponse.MatchTrend::fixtureId).toList());
        assertEquals(List.of(new RankPoint(1, 1), new RankPoint(2, 1)), afterCatchup.rankHistory());
    }

    @Test
    void scorersAssistersOwnGoalsAndTransferredPlayerStillCreditedToOldClub() {
        club(1, "Old Club FC");
        club(2, "New Club FC");

        player(10, "Zed Scorer");
        player(11, "Aaron Scorer");
        player(12, "Departed Star");
        player(13, "Zero Output");

        // Departed Star played GW1 for Old Club (club 1), then transferred to New Club (club 2)
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,12,1)");
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,12,2)");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,12,1,DATE '2026-08-01',DATE '2026-08-20')");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,12,2,DATE '2026-08-20',NULL)");

        // GW1: Old Club wins 4-1 (Zed 1g, Aaron 1g 2a, Departed 1g 1a -> 3 player goals + 1 own goal = 4)
        fixture(501, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 1, 2, 4, 1);
        stat(501, 10, 1, "PLAYED", "7.50", 1, 1);
        stat(501, 11, 1, "PLAYED", "7.60", 1, 2);
        stat(501, 12, 1, "PLAYED", "7.40", 1, 1);
        stat(501, 13, 1, "PLAYED", "6.50", 0, 0);

        // GW2: Departed Star now scores 2 for New Club (club 2) against Old Club -> must NOT count for Old Club
        fixture(502, 2, "2026-08-25", "2026-08-25T14:00:00Z", "FINISHED", 2, 1, 2, 0);
        stat(502, 12, 2, "PLAYED", "8.20", 2, 0);

        var oldClub = service.trends(1, 2026);
        assertEquals(1, oldClub.ownGoalsFor());
        // All three scorers have 1 goal -> sorted alphabetically: Aaron, Departed, Zed
        assertEquals(List.of(
                new ScorerEntry(11, "Aaron Scorer", 1),
                new ScorerEntry(12, "Departed Star", 1),
                new ScorerEntry(10, "Zed Scorer", 1)
        ), oldClub.scorers());
        // Assisters: Aaron 2, then Departed 1 & Zed 1 alphabetically; Zero Output excluded
        assertEquals(List.of(
                new AssisterEntry(11, "Aaron Scorer", 2),
                new AssisterEntry(12, "Departed Star", 1),
                new AssisterEntry(10, "Zed Scorer", 1)
        ), oldClub.assisters());

        // New Club credits Departed Star's 2 goals (GW1 had 1 goal with no player stats -> ownGoalsFor = 1)
        var newClub = service.trends(2, 2026);
        assertEquals(List.of(new ScorerEntry(12, "Departed Star", 2)), newClub.scorers());
        assertEquals(1, newClub.ownGoalsFor());
    }

    @Test
    void negativeOwnGoalsClampsToZero() {
        club(1, "Alpha FC");
        club(2, "Beta FC");
        player(10, "Overcounted");

        // Match ended 1-0, but player stat has 2 goals -> ownGoalsFor clamps to 0 (no negative)
        fixture(601, 1, "2026-08-15", "2026-08-15T14:00:00Z", "FINISHED", 1, 2, 1, 0);
        stat(601, 10, 1, "PLAYED", "8.00", 2, 0);

        var result = service.trends(1, 2026);
        assertEquals(0, result.ownGoalsFor());
    }

    @Test
    void rejectsUnsupportedSeasonAndReturns404ForMissingClub() throws Exception {
        club(1, "Alpha FC");

        assertThrows(InvalidFilterException.class, () -> service.trends(1, 2024));
        assertThrows(InvalidFilterException.class, () -> service.trends(1, 2025));
        assertThrows(ResourceNotFoundException.class, () -> service.trends(999, 2026));

        var queries = new FootballQueries(jdbc, new MatchScoringService(),
                new FixtureEvidenceService(jdbc, new ObjectMapper()));
        var statsService = new ClubStatisticsService(jdbc);
        var overviewService = new ClubOverviewService(jdbc, statsService);
        var mvc = MockMvcBuilders.standaloneSetup(new ClubController(queries, statsService, overviewService, service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/clubs/999/season-trends").param("season", "2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private void club(int id, String name) {
        jdbc.update("INSERT INTO clubs (id, name, city) VALUES (?, ?, 'City')", id, name);
        jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) VALUES (39, 2026, ?)", id);
    }

    private void player(int id, String name) {
        jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", id, name);
    }

    private void fixture(int id, int gw, String date, String kickoffUtc, String status,
                         int home, int away, Integer homeGoals, Integer awayGoals) {
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id, gameweek,
                    match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (?, 39, 2026, ?, ?, ?, CAST(? AS DATE), ?, 'FT', ?, ?, ?, CURRENT_TIMESTAMP)
                """, id, home, away, gw, date, status, homeGoals, awayGoals, "0".repeat(64));
        if (kickoffUtc != null) {
            jdbc.update("""
                    INSERT INTO football_data_fixtures (provider_id, fixture_id, kickoff_utc, provider_status)
                    VALUES (?, ?, ?, ?)
                    """, id + 10000, id, kickoffUtc, status);
        }
    }

    private void stat(int fixtureId, int playerId, int clubId, String participation,
                      String rating, Integer goals, Integer assists) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats (fixture_id, player_id, league_id, season_year, club_id,
                    participation_status, rating, goals, assists)
                VALUES (?, ?, 39, 2026, ?, ?, CAST(? AS DECIMAL(4,2)), ?, ?)
                """, fixtureId, playerId, clubId, participation, rating, goals, assists);
    }

    private void extra(int fixtureId, int playerId, int clubId, String expectedGoals) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_extra_stats (fixture_id, player_id, league_id, season_year, club_id,
                    expected_goals, source, fetched_at)
                VALUES (?, ?, 39, 2026, ?, CAST(? AS DECIMAL(5,2)), 'FPL', CURRENT_TIMESTAMP)
                """, fixtureId, playerId, clubId, expectedGoals);
    }
}
