package com.premierhub.service;

import com.premierhub.web.ClubController;
import com.premierhub.web.dto.ClubOverviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClubOverviewServiceTest {
    private static final int CLUB_A = 101;
    private static final int CLUB_B = 102;
    private static final String HASH = "0".repeat(64);

    private JdbcTemplate jdbc;
    private ClubOverviewService service;
    private FootballQueries queries;
    private ClubStatisticsService statsService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:club-overview-" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
        jdbc = new JdbcTemplate(ds);
        statsService = new ClubStatisticsService(jdbc);
        service = new ClubOverviewService(jdbc, statsService);
        queries = new FootballQueries(jdbc, new MatchScoringService(),
                new FixtureEvidenceService(jdbc, new ObjectMapper()));

        jdbc.update("INSERT INTO seasons VALUES (39, 2024), (39, 2026)");
        jdbc.update("INSERT INTO clubs (id, name, city) VALUES (?, 'Arsenal FC', 'London'), (?, 'Rival FC', 'Manchester')",
                CLUB_A, CLUB_B);
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, ?), (39, 2026, ?), (39, 2024, ?)",
                CLUB_A, CLUB_B, CLUB_A);
    }

    private void addPlayer(int id, String name, int clubId, String startDate, String endDate) {
        jdbc.update("MERGE INTO players (id, name) KEY (id) VALUES (?, ?)", id, name);
        jdbc.update("""
                MERGE INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                KEY (league_id, season_year, player_id, club_id)
                VALUES (39, 2026, ?, ?, 'MIDFIELDER')
                """, id, clubId);
        jdbc.update("""
                INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (39, 2026, ?, ?, DATE '%s', %s)
                """.formatted(startDate, endDate == null ? "NULL" : "DATE '" + endDate + "'"), id, clubId);
    }

    private void addFixture(int id, int gw, String date) {
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id, gameweek,
                                      match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (?, 39, 2026, ?, ?, ?, DATE '%s', 'FINISHED', 'FINISHED', 2, 1, ?, CURRENT_TIMESTAMP)
                """.formatted(date), id, CLUB_A, CLUB_B, gw, HASH);
    }

    private void addLineup(int fixtureId, int clubId, String formation) {
        jdbc.update("""
                INSERT INTO fixture_lineups (fixture_id, club_id, league_id, season_year, formation)
                VALUES (?, ?, 39, 2026, ?)
                """, fixtureId, clubId, formation);
    }

    private void addStarter(int fixtureId, int clubId, int playerId, String pos, int row, int slot) {
        jdbc.update("""
                INSERT INTO fixture_lineup_players (fixture_id, club_id, player_id, role, match_position, row_index, slot_index)
                VALUES (?, ?, ?, 'STARTER', ?, ?, ?)
                """, fixtureId, clubId, playerId, pos, row, slot);
    }

    private void addStat(int fixtureId, int playerId, int clubId, int goals, int assists, Double rating) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (?, ?, 39, 2026, ?, 'PLAYED', ?, NULL, 90, ?, ?, 0, 0)
                """, fixtureId, playerId, clubId, rating, goals, assists);
    }

    @Test
    void selectsMostUsedFormationAndUniqueXiWithRecencyAndRatingTieBreakersAndCrossFormationBadgeCounts() {
        // 13 players at CLUB_A
        String[] names = {
                "David Raya", "Riccardo Calafiori", "Gabriel Magalhaes", "Ezri Konsa",
                "Ben White", "Jurrien Timber", "Declan Rice", "Bruno Guimaraes",
                "Eberechi Eze", "Martin Odegaard", "Noni Madueke", "Kai Havertz",
                "Bukayo Saka"
        };
        for (int i = 0; i < names.length; i++) {
            addPlayer(1001 + i, names[i], CLUB_A, "2026-07-01", null);
        }

        // 2 matches with 4-2-3-1 (GW6, GW8) and 2 matches with 4-3-3 (GW7, GW9) -> tie 2-2!
        // Latest match is GW9 (4-3-3) -> wait: let's test 2x 4-2-3-1 (GW6, GW9) vs 2x 4-3-3 (GW7, GW8)
        // so 4-2-3-1 wins formation tie-break by latest match (GW9)!
        addFixture(506, 6, "2026-10-01");
        addFixture(507, 7, "2026-10-08");
        addFixture(508, 8, "2026-10-15");
        addFixture(509, 9, "2026-10-22");

        // GW6: 4-2-3-1 with White (1005) at RB
        addLineup(506, CLUB_A, "4-2-3-1");
        addStarter(506, CLUB_A, 1001, "GK", 0, 0);
        addStarter(506, CLUB_A, 1002, "LB", 1, 0);
        addStarter(506, CLUB_A, 1003, "CB", 1, 1);
        addStarter(506, CLUB_A, 1004, "CB", 1, 2);
        addStarter(506, CLUB_A, 1005, "RB", 1, 3); // White
        addStarter(506, CLUB_A, 1008, "CM", 2, 0); // Bruno
        addStarter(506, CLUB_A, 1007, "CM", 2, 1); // Rice
        addStarter(506, CLUB_A, 1009, "LW", 3, 0); // Eze
        addStarter(506, CLUB_A, 1010, "CAM", 3, 1); // Odegaard
        addStarter(506, CLUB_A, 1011, "RW", 3, 2); // Madueke
        addStarter(506, CLUB_A, 1012, "ST", 4, 0); // Havertz

        // GW7: 4-3-3 with White (1005) at RB, Havertz (1012) at ST (row 3, slot 1), Saka (1013) at RW
        addLineup(507, CLUB_A, "4-3-3");
        addStarter(507, CLUB_A, 1001, "GK", 0, 0);
        addStarter(507, CLUB_A, 1002, "LB", 1, 0);
        addStarter(507, CLUB_A, 1003, "CB", 1, 1);
        addStarter(507, CLUB_A, 1004, "CB", 1, 2);
        addStarter(507, CLUB_A, 1005, "RB", 1, 3); // White (2 starts, last GW7)
        addStarter(507, CLUB_A, 1008, "CM", 2, 0); // Bruno
        addStarter(507, CLUB_A, 1010, "CM", 2, 1); // Odegaard as CM in 4-3-3
        addStarter(507, CLUB_A, 1007, "CM", 2, 2); // Rice
        addStarter(507, CLUB_A, 1009, "LW", 3, 0); // Eze
        addStarter(507, CLUB_A, 1012, "ST", 3, 1); // Havertz ST across formation!
        addStarter(507, CLUB_A, 1013, "RW", 3, 2); // Saka

        // GW8: 4-3-3 with Timber (1006) at RB, Saka (1013) at RW
        addLineup(508, CLUB_A, "4-3-3");
        addStarter(508, CLUB_A, 1001, "GK", 0, 0);
        addStarter(508, CLUB_A, 1002, "LB", 1, 0);
        addStarter(508, CLUB_A, 1003, "CB", 1, 1);
        addStarter(508, CLUB_A, 1004, "CB", 1, 2);
        addStarter(508, CLUB_A, 1006, "RB", 1, 3); // Timber
        addStarter(508, CLUB_A, 1008, "CM", 2, 0); // Bruno
        addStarter(508, CLUB_A, 1010, "CM", 2, 1); // Odegaard
        addStarter(508, CLUB_A, 1007, "CM", 2, 2); // Rice
        addStarter(508, CLUB_A, 1009, "LW", 3, 0); // Eze
        addStarter(508, CLUB_A, 1012, "ST", 3, 1); // Havertz
        addStarter(508, CLUB_A, 1013, "RW", 3, 2); // Saka (2 starts, last GW8)

        // GW9: 4-2-3-1 with Timber (1006) at RB (ties White 2-2, Timber more recent GW9!),
        // Madueke (1011) at RW (ties Saka 2-2, Madueke more recent GW9!)
        addLineup(509, CLUB_A, "4-2-3-1");
        addStarter(509, CLUB_A, 1001, "GK", 0, 0);
        addStarter(509, CLUB_A, 1002, "LB", 1, 0);
        addStarter(509, CLUB_A, 1003, "CB", 1, 1);
        addStarter(509, CLUB_A, 1004, "CB", 1, 2);
        addStarter(509, CLUB_A, 1006, "RB", 1, 3); // Timber (2 starts, last GW9 > White GW7)
        addStarter(509, CLUB_A, 1008, "CM", 2, 0); // Bruno (4 starts CM)
        addStarter(509, CLUB_A, 1007, "CM", 2, 1); // Rice (4 starts CM)
        addStarter(509, CLUB_A, 1009, "LW", 3, 0); // Eze
        addStarter(509, CLUB_A, 1010, "CAM", 3, 1); // Odegaard (2 starts CAM)
        addStarter(509, CLUB_A, 1011, "RW", 3, 2); // Madueke (2 starts RW, last GW9 > Saka GW8)
        addStarter(509, CLUB_A, 1012, "ST", 4, 0); // Havertz (4 starts ST across both formations!)

        ClubOverviewResponse overview = service.overview(CLUB_A, 2026);
        assertEquals("4-2-3-1", overview.formation());
        assertEquals(4, overview.lineupMatches());
        assertEquals(List.of(
                new ClubOverviewResponse.FormationCount("4-2-3-1", 2),
                new ClubOverviewResponse.FormationCount("4-3-3", 2)), overview.formationCounts());
        assertEquals(11, overview.xi().size());

        // No duplicate player IDs
        Set<Integer> ids = new HashSet<>();
        overview.xi().forEach(p -> assertTrue(ids.add(p.playerId()), "Duplicate player in XI: " + p.playerId()));

        // RB tie-break: Timber (GW9) beats White (GW7)
        var rb = overview.xi().stream().filter(p -> p.position().equals("RB")).findFirst().orElseThrow();
        assertEquals(1006, rb.playerId());
        assertEquals(2, rb.startsAtPosition());
        assertEquals("Timber", rb.shortName());
        assertEquals("van den Berg", ClubOverviewService.shortenPitchName("Sepp van den Berg"));
        assertEquals("van Dijk", ClubOverviewService.shortenPitchName("Virgil van Dijk"));

        // ST cross-formation badge count = 4 (2 in 4-2-3-1 + 2 in 4-3-3)
        var st = overview.xi().stream().filter(p -> p.position().equals("ST")).findFirst().orElseThrow();
        assertEquals(1012, st.playerId());
        assertEquals(4, st.startsAtPosition());

        // Odegaard gets CAM (2), not duplicated in CM
        var cam = overview.xi().stream().filter(p -> p.position().equals("CAM")).findFirst().orElseThrow();
        assertEquals(1010, cam.playerId());
        assertEquals(2, cam.startsAtPosition());
    }

    @Test
    void breaksSameMatchTieByAverageRatingAndHandlesNoLineupData() {
        // Season 2024 has no lineups -> formation null, xi empty
        ClubOverviewResponse legacy = service.overview(CLUB_A, 2024);
        assertNull(legacy.formation());
        assertEquals(0, legacy.lineupMatches());
        assertTrue(legacy.xi().isEmpty());
        assertTrue(legacy.formationCounts().isEmpty());

        // 2 players tie on starts AND latest match (impossible in same slot unless 2 CBs, or via same match date):
        // Use 2 fixtures on the SAME match_date & same gameweek (edge case) or verify rating tie-breaker!
        for (int i = 1; i <= 12; i++) {
            addPlayer(2000 + i, "Player " + i, CLUB_A, "2026-07-01", null);
        }
        addFixture(601, 6, "2026-10-01");
        addFixture(602, 6, "2026-10-01");
        // Both players start ST once on 2026-10-01 in different fixtures, fixture 602 is later by id ->
        // To test rating tie-breaker directly when starts AND latestMatch are equal, test two CBs competing for 1 CB slot?
        // Actually in 1 fixture with 2 CBs (1003 and 1004) competing when only 1 CB slot? No, 4-2-3-1 has 2 CBs.
        // Instead, put both 2011 and 2012 as CB in a 4-2-3-1 (fixture 601) and a 3-back? Even simpler:
        // In 1 match of 4-2-3-1, 2011 and 2012 both start as CB (same matchKey, same 1 start!).
        // In 2 matches of 4-3-3? Wait: if 3 players start as CB in the SAME match (3-4-3, 1 match),
        // and primary formation is 4-2-3-1 (2 matches with 2 other CBs? No, 1 match of 3-4-3 with 3 CBs vs 2 matches of 4-2-3-1):
        // Better: 1 match of 3-4-3 (GW6) where 2002, 2003, 2004 all start as CB (each 1 start, same matchKey 601!),
        // and 2 matches of 4-2-3-1 (GW7, GW8) where 2012 starts as CB in GW7 & GW8, and in GW7 2003 is CB?
        // Wait: in 1 match of 3-4-3 (601), players 2002, 2003, 2004 all have 1 CB start on fixture 601.
        // In 2 matches of 4-2-3-1 (602, 603), player 2012 has 2 CB starts, and NO other CB (e.g. 2005 is LB, etc.)?
        // No, 4-2-3-1 has 2 CB slots -> slot 1 goes to 2012 (2 starts), slot 2 has a 3-way tie between 2002, 2003, 2004
        // (all 1 start, all same fixture 601!) -> highest average rating MUST win!
        addLineup(601, CLUB_A, "3-4-3");
        addStarter(601, CLUB_A, 2001, "GK", 0, 0);
        addStarter(601, CLUB_A, 2002, "CB", 1, 0); // rating 6.5
        addStarter(601, CLUB_A, 2003, "CB", 1, 1); // rating 8.4 -> MUST WIN CB tie-break!
        addStarter(601, CLUB_A, 2004, "CB", 1, 2); // rating 7.1
        addStarter(601, CLUB_A, 2005, "LM", 2, 0);
        addStarter(601, CLUB_A, 2006, "CM", 2, 1);
        addStarter(601, CLUB_A, 2007, "CM", 2, 2);
        addStarter(601, CLUB_A, 2008, "RM", 2, 3);
        addStarter(601, CLUB_A, 2009, "LW", 3, 0);
        addStarter(601, CLUB_A, 2010, "ST", 3, 1);
        addStarter(601, CLUB_A, 2011, "RW", 3, 2);

        addStat(601, 2002, CLUB_A, 0, 0, 6.5);
        addStat(601, 2003, CLUB_A, 0, 0, 8.4);
        addStat(601, 2004, CLUB_A, 0, 0, 7.1);

        addFixture(603, 7, "2026-10-08");
        addLineup(602, CLUB_A, "4-2-3-1");
        addLineup(603, CLUB_A, "4-2-3-1");
        for (int fx : List.of(602, 603)) {
            addStarter(fx, CLUB_A, 2001, "GK", 0, 0);
            addStarter(fx, CLUB_A, 2005, "LB", 1, 0);
            addStarter(fx, CLUB_A, 2012, "CB", 1, 1);
            addStarter(fx, CLUB_A, 2006, "RB", 1, 3);
            addStarter(fx, CLUB_A, 2007, "CM", 2, 0);
            addStarter(fx, CLUB_A, 2008, "CM", 2, 1);
            addStarter(fx, CLUB_A, 2009, "LW", 3, 0);
            addStarter(fx, CLUB_A, 2010, "CAM", 3, 1);
            addStarter(fx, CLUB_A, 2011, "RW", 3, 2);
            addStarter(fx, CLUB_A, 2002, "ST", 4, 0); // 2002 takes ST so 2003 (8.4) vs 2004 (7.1) fight for 2nd CB!
        }

        ClubOverviewResponse overview = service.overview(CLUB_A, 2026);
        assertEquals("4-2-3-1", overview.formation());
        List<Integer> cbIds = overview.xi().stream()
                .filter(p -> p.position().equals("CB"))
                .map(ClubOverviewResponse.XiPlayer::playerId)
                .toList();
        assertTrue(cbIds.contains(2012));
        assertTrue(cbIds.contains(2003), "Player 2003 (rating 8.4) should beat 2004 (rating 7.1) on same-match tie");
        assertFalse(cbIds.contains(2004));
    }

    @Test
    void computesTiedLeadersTransferIsolationRatingThresholdAndControllerEndpoint() throws Exception {
        // Players at CLUB_A
        addPlayer(3001, "Alpha Striker", CLUB_A, "2026-07-01", null);
        addPlayer(3002, "Beta Striker", CLUB_A, "2026-07-01", null);
        addPlayer(3003, "Gamma Striker", CLUB_A, "2026-07-01", null);
        // Transferred player: played at CLUB_B in GW1-GW2 (scored 5 goals!), moved to CLUB_A on 2026-09-01 (scored 1 goal)
        addPlayer(3004, "Transferred Mid", CLUB_B, "2026-07-01", "2026-09-01");
        addPlayer(3004, "Transferred Mid", CLUB_A, "2026-09-01", null);
        // Zero goals/assists, only 4 rated games (< 5 threshold -> must NOT appear in topRatings!)
        addPlayer(3005, "Four Games Wonder", CLUB_A, "2026-07-01", null);

        for (int gw = 1; gw <= 6; gw++) {
            addFixture(700 + gw, gw, "2026-08-%02d".formatted(gw));
        }
        addFixture(707, 7, "2026-09-10");

        // Transferred Mid: 5 goals at CLUB_B in GW1 -> MUST NOT count for CLUB_A!
        addStat(701, 3004, CLUB_B, 5, 4, 9.5);
        // 1 goal at CLUB_A in GW7 -> counts as 1 goal for CLUB_A
        addStat(707, 3004, CLUB_A, 1, 0, 7.0);

        // Alpha (5 apps): 3 goals, 2 assists, avg rating 7.50
        for (int gw = 1; gw <= 5; gw++) {
            addStat(700 + gw, 3001, CLUB_A, gw <= 3 ? 1 : 0, gw <= 2 ? 1 : 0, 7.5);
        }
        // Beta (6 apps): 3 goals (tied with Alpha & Gamma, more apps -> after Gamma & Alpha), avg rating 7.50
        for (int gw = 1; gw <= 6; gw++) {
            addStat(700 + gw, 3002, CLUB_A, gw <= 3 ? 1 : 0, 0, 7.5);
        }
        // Gamma (3 apps): 3 goals (fewest apps -> first among 3 tied at rank 1!), 0 assists, only 3 rated (< 5)
        for (int gw = 1; gw <= 3; gw++) {
            addStat(700 + gw, 3003, CLUB_A, 1, 0, 8.8);
        }
        // Four Games Wonder: 4 rated matches with 9.9 rating (< 5 threshold -> excluded!)
        for (int gw = 1; gw <= 4; gw++) {
            addStat(700 + gw, 3005, CLUB_A, 0, 0, 9.9);
        }

        ClubOverviewResponse overview = service.overview(CLUB_A, 2026);

        // Top scorers: 3-way tie at rank 1, ordered by fewest appearances: Gamma (3 apps), Alpha (5 apps), Beta (6 apps)
        // Transferred Mid (5 goals at CLUB_B, only 1 at CLUB_A) must NOT overtake them!
        assertEquals(3, overview.topScorers().size());
        assertEquals(List.of(1, 1, 1), overview.topScorers().stream().map(ClubOverviewResponse.LeaderEntry::rank).toList());
        assertEquals(List.of(3003, 3001, 3002), overview.topScorers().stream().map(ClubOverviewResponse.LeaderEntry::playerId).toList());
        assertEquals(List.of(3, 3, 3), overview.topScorers().stream().map(ClubOverviewResponse.LeaderEntry::value).toList());

        // Top assists: only Alpha has > 0 assists (zeroes excluded!)
        assertEquals(1, overview.topAssists().size());
        assertEquals(3001, overview.topAssists().getFirst().playerId());
        assertEquals(2, overview.topAssists().getFirst().value());

        // Top ratings (>= 5 rated appearances): Alpha (5 apps, 7.50) and Beta (6 apps, 7.50), both rank 1.
        // Gamma (3 rated) and Four Games Wonder (4 rated, 9.9) excluded!
        assertEquals(2, overview.topRatings().size());
        assertEquals(List.of(1, 1), overview.topRatings().stream().map(ClubOverviewResponse.RatingLeaderEntry::rank).toList());
        assertEquals(List.of(3001, 3002), overview.topRatings().stream().map(ClubOverviewResponse.RatingLeaderEntry::playerId).toList());

        // Verify public controller endpoint
        var mvc = MockMvcBuilders.standaloneSetup(new ClubController(queries, statsService, service)).build();
        mvc.perform(get("/api/clubs/" + CLUB_A + "/overview").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubId").value(CLUB_A))
                .andExpect(jsonPath("$.topScorers", hasSize(3)))
                .andExpect(jsonPath("$.topScorers[0].rank").value(1))
                .andExpect(jsonPath("$.topRatings", hasSize(2)));
    }
}
