package com.premierhub.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:player-season-profile-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class PlayerSeasonProfileIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        seedData();
    }

    private void seedData() {
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("""
                INSERT INTO clubs VALUES
                  (9001, 'Test United', 'Manchester'),
                  (9002, 'Test Albion', 'London')
                """);
        jdbc.update("""
                INSERT INTO season_clubs VALUES
                  (39, 2026, 9001),
                  (39, 2026, 9002)
                """);

        // 5 fixtures in 2026 (GW1-GW5, all FINISHED) + 1 GW6 SCHEDULED
        // Test United goals across GW1-GW5 = 2 + 3 + 1 + 0 + 4 = 10 goals
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES
                  (95001, 39, 2026, 9001, 9002, 1, DATE '2026-08-15', 'FINISHED', 'FINISHED', 2, 0, 'h1', CURRENT_TIMESTAMP),
                  (95002, 39, 2026, 9002, 9001, 2, DATE '2026-08-22', 'FINISHED', 'FINISHED', 1, 3, 'h2', CURRENT_TIMESTAMP),
                  (95003, 39, 2026, 9001, 9002, 3, DATE '2026-08-29', 'FINISHED', 'FINISHED', 1, 1, 'h3', CURRENT_TIMESTAMP),
                  (95004, 39, 2026, 9002, 9001, 4, DATE '2026-09-12', 'FINISHED', 'FINISHED', 2, 0, 'h4', CURRENT_TIMESTAMP),
                  (95005, 39, 2026, 9001, 9002, 5, DATE '2026-09-19', 'FINISHED', 'FINISHED', 4, 0, 'h5', CURRENT_TIMESTAMP),
                  (95006, 39, 2026, 9002, 9001, 6, DATE '2026-09-26', 'FINISHED', 'FINISHED', 1, 2, 'h6', CURRENT_TIMESTAMP),
                  (95007, 39, 2026, 9001, 9002, 7, DATE '2026-10-03', 'SCHEDULED', 'SCHEDULED', NULL, NULL, 'h7', CURRENT_TIMESTAMP)
                """);
        // Total Test United goals across GW1-GW6 = 2 + 3 + 1 + 0 + 4 + 2 = 12 goals

        // Players:
        // 9101: CAM (MIDFIELDER) - ATTACK, 5 played (450m) + 1 DNP (GW4) -> clubFinishedMatches=6, teamGoals=12
        // 9102: CM  (MIDFIELDER) - MIXED,  5 played (450m) - tied with 9101 on goals/90!
        // 9103: CM  (MIDFIELDER) - MIXED,  5 played (450m) - lower goals/90 than 9101
        // 9104: CAM (MIDFIELDER) - ATTACK, 5 played (450m) - missing extra on GW1 (like Ndiaye!)
        // 9105: CAM (MIDFIELDER) - ATTACK, 2 played (180m) - under 450 minutes
        // 9201: CB  (DEFENDER)   - DEFEND, 5 played (450m), 2 clean sheets
        // 9202: LB  (DEFENDER)   - DEFEND, 5 played (450m), 1 clean sheet
        // 9301: GK  (GOALKEEPER) - GK,     5 played (450m), 2 goals conceded (best, lowest)
        // 9302: GK  (GOALKEEPER) - GK,     5 played (450m), 5 goals conceded (tied mid)
        // 9303: GK  (GOALKEEPER) - GK,     5 played (450m), 5 goals conceded (tied mid)
        // 9304: GK  (GOALKEEPER) - GK,     5 played (450m), 10 goals conceded (worst, highest)
        addPlayer(9101, "Bruno Test", 9001, "MIDFIELDER", "CAM");
        addPlayer(9102, "Peer Tie Mid", 9002, "MIDFIELDER", "CM");
        addPlayer(9103, "Peer Low Mid", 9002, "MIDFIELDER", "CM");
        addPlayer(9104, "Missing Extra Mid", 9001, "MIDFIELDER", "CAM");
        addPlayer(9105, "Low Minutes Mid", 9001, "MIDFIELDER", "CAM");
        addPlayer(9201, "Center Back Test", 9001, "DEFENDER", "CB");
        addPlayer(9202, "Peer Defender", 9002, "DEFENDER", "LB");
        addPlayer(9301, "Best Keeper", 9001, "GOALKEEPER", "GK");
        addPlayer(9302, "Mid Keeper A", 9002, "GOALKEEPER", "GK");
        addPlayer(9303, "Mid Keeper B", 9002, "GOALKEEPER", "GK");
        addPlayer(9304, "High Concede Keeper", 9002, "GOALKEEPER", "GK");

        int[] playedGw = {95001, 95002, 95003, 95005, 95006};
        // 9101 (Bruno Test): plays 5 matches (450m), DNP GW4 (95004)
        // Totals: 5 goals, 3 assists, 1 YC, 0 RC, ratings: 7.0, 9.0, 6.0, 8.0, 7.5 -> avg 7.50
        // Extra totals: xG=3.50, xA=2.00, tackles=10, cbi=5, recoveries=20, bps=125, bonus=4, cs=2
        int[] brunoGoals = {1, 2, 0, 1, 1};
        int[] brunoAssists = {0, 1, 1, 1, 0};
        int[] brunoYc = {0, 0, 1, 0, 0};
        String[] brunoRatings = {"7.00", "9.00", "6.00", "8.00", "7.50"};
        String[] brunoXg = {"0.50", "1.20", "0.30", "0.80", "0.70"};
        String[] brunoXa = {"0.20", "0.60", "0.40", "0.50", "0.30"};
        int[] brunoCs = {1, 0, 0, 1, 0};
        int[] brunoBonus = {1, 3, 0, 0, 0};
        for (int i = 0; i < playedGw.length; i++) {
            addManualStat(playedGw[i], 9101, 9001, "PLAYED", brunoRatings[i], 90, brunoGoals[i], brunoAssists[i], brunoYc[i], 0);
            addExtraStat(playedGw[i], 9101, 9001, brunoCs[i], 0, 0, brunoBonus[i], 25, 1, 4, 2, 7, brunoXg[i], brunoXa[i]);
        }
        addManualStat(95004, 9101, 9001, "DID_NOT_PLAY", null, 0, 0, 0, 0, 0);

        // 9102 (Peer Tie Mid): 5 goals (ties 9101), 1 assist, xG=2.50, xA=1.00, tackles=15, cbi=10, recoveries=25, bps=100
        for (int f : playedGw) {
            addManualStat(f, 9102, 9002, "PLAYED", "7.00", 90, 1, f == 95001 ? 1 : 0, 0, 0);
            addExtraStat(f, 9102, 9002, 0, 1, 0, 0, 20, 2, 5, 3, 10, "0.50", "0.20");
        }

        // 9103 (Peer Low Mid): 0 goals, 0 assists, xG=0.50, xA=0.50, tackles=5, cbi=5, recoveries=10, bps=50
        for (int f : playedGw) {
            addManualStat(f, 9103, 9002, "PLAYED", "6.20", 90, 0, 0, 0, 0);
            addExtraStat(f, 9103, 9002, 0, 1, 0, 0, 10, 1, 2, 1, 4, "0.10", "0.10");
        }

        // 9104 (Missing Extra Mid): 5 played (450m), 10 goals, 5 assists, extra missing on GW1 (95001)!
        for (int f : playedGw) {
            addManualStat(f, 9104, 9001, "PLAYED", "7.40", 90, 2, 1, 0, 0);
            if (f != 95001) {
                addExtraStat(f, 9104, 9001, 0, 0, 0, 1, 22, 1, 3, 2, 6, "0.60", "0.30");
            }
        }

        // 9105 (Low Minutes Mid): only 2 played (180m < 450)
        for (int f : new int[]{95001, 95002}) {
            addManualStat(f, 9105, 9001, "PLAYED", "6.80", 90, 1, 0, 0, 0);
            addExtraStat(f, 9105, 9001, 1, 0, 0, 0, 18, 1, 2, 1, 4, "0.40", "0.10");
        }

        // 9201 (CB) & 9202 (LB)
        for (int i = 0; i < playedGw.length; i++) {
            int f = playedGw[i];
            addManualStat(f, 9201, 9001, "PLAYED", "7.30", 90, 0, i == 0 ? 1 : 0, 0, 0);
            addExtraStat(f, 9201, 9001, i < 2 ? 1 : 0, 1, 0, 1, 24, 6, 5, 3, 14, "0.10", "0.10");

            addManualStat(f, 9202, 9002, "PLAYED", "6.70", 90, 0, 0, 0, 0);
            addExtraStat(f, 9202, 9002, i == 0 ? 1 : 0, 2, 0, 0, 15, 3, 3, 1, 7, "0.05", "0.02");
        }

        // 4 Goalkeepers (9301..9304) to test lower-is-better goalsConcededPer90 + ties!
        // Conceded over 5 matches: 9301=0 (0/90), 9302=5 (1/90), 9303=5 (1/90), 9304=10 (2/90)
        for (int i = 0; i < playedGw.length; i++) {
            int f = playedGw[i];
            addManualStat(f, 9301, 9001, "PLAYED", "7.60", 90, 0, 0, 0, 0);
            addExtraStat(f, 9301, 9001, i < 3 ? 1 : 0, 0, 4, 2, 26, 0, 8, 0, 0, "0.00", "0.00");

            addManualStat(f, 9302, 9002, "PLAYED", "6.90", 90, 0, 0, 0, 0);
            addExtraStat(f, 9302, 9002, i < 1 ? 1 : 0, 1, 3, 0, 18, 0, 6, 0, 0, "0.00", "0.00");

            addManualStat(f, 9303, 9002, "PLAYED", "6.80", 90, 0, 0, 0, 0);
            addExtraStat(f, 9303, 9002, i < 1 ? 1 : 0, 1, 2, 0, 16, 0, 5, 0, 0, "0.00", "0.00");

            addManualStat(f, 9304, 9002, "PLAYED", "6.10", 90, 0, 0, 0, 0);
            addExtraStat(f, 9304, 9002, 0, 2, 1, 0, 10, 0, 4, 0, 0, "0.00", "0.00");
        }
    }

    private void addPlayer(int id, String name, int clubId, String position, String primaryPosition) {
        jdbc.update("INSERT INTO players VALUES (?, ?)", id, name);
        jdbc.update("""
                INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                VALUES (39, 2026, ?, ?, ?)
                """, id, clubId, position);
        jdbc.update("""
                INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (39, 2026, ?, ?, DATE '2026-08-01', NULL)
                """, id, clubId);
        if (primaryPosition != null) {
            jdbc.update("""
                    INSERT INTO player_specific_positions (league_id, season_year, player_id, primary_position)
                    VALUES (39, 2026, ?, ?)
                    """, id, primaryPosition);
        }
    }

    private void addManualStat(int fixtureId, int playerId, int clubId, String status, String rating,
                               int minutes, int goals, int assists, int yc, int rc) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (?, ?, 39, 2026, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, fixtureId, playerId, clubId, status,
                rating == null ? null : new BigDecimal(rating),
                rating == null ? null : new BigDecimal(rating),
                minutes, goals, assists, yc, rc);
    }

    private void addExtraStat(int fixtureId, int playerId, int clubId, int cs, int gc, int saves,
                              int bonus, int bps, int cbi, int recoveries, int tackles, int defCon,
                              String xg, String xa) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_extra_stats
                (fixture_id, player_id, league_id, season_year, club_id, starts, clean_sheets, goals_conceded,
                 own_goals, penalties_saved, penalties_missed, saves, bonus, bps,
                 clearances_blocks_interceptions, recoveries, tackles, defensive_contribution,
                 expected_goals, expected_assists, expected_goal_involvements, expected_goals_conceded,
                 influence, creativity, threat, ict_index, source, fetched_at)
                VALUES (?, ?, 39, 2026, ?, 1, ?, ?, 0, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0.00, 0.00,
                        10.0, 10.0, 10.0, 3.0, 'FPL', CURRENT_TIMESTAMP)
                """, fixtureId, playerId, clubId, cs, gc, saves, bonus, bps, cbi, recoveries, tackles, defCon,
                new BigDecimal(xg), new BigDecimal(xa));
    }

    @Test
    void playerMatchesAttachesExtraFor2026AndOmitsWhenMissingAndLeaves2024Unchanged() throws Exception {
        // 9104 has missing extra on GW1 (95001) and present extra on GW2 (95002)
        mockMvc.perform(get("/api/players/9104/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].match.id").value(95001))
                .andExpect(jsonPath("$[0].stats.goals").value(2))
                .andExpect(jsonPath("$[0].stats.extra").doesNotExist())
                .andExpect(jsonPath("$[1].match.id").value(95002))
                .andExpect(jsonPath("$[1].stats.extra.expectedGoals").value(0.6))
                .andExpect(jsonPath("$[1].stats.extra.expectedAssists").value(0.3))
                .andExpect(jsonPath("$[1].stats.extra.tackles").value(2))
                .andExpect(jsonPath("$[1].stats.extra.recoveries").value(3))
                .andExpect(jsonPath("$[1].stats.extra.bps").value(22));

        // Seed a minimal 2024 player & fixture to verify 2024 is untouched
        jdbc.update("MERGE INTO seasons (league_id, season_year) KEY(league_id, season_year) VALUES (39, 2024)");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2024, 9001), (39, 2024, 9002)");
        jdbc.update("INSERT INTO players VALUES (9401, 'Legacy Player')");
        jdbc.update("""
                INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position, appearances, minutes, goals, assists)
                VALUES (39, 2024, 9401, 9001, 'FORWARD', 1, 90, 1, 0)
                """);
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (94501, 39, 2024, 9001, 9002, 1, DATE '2024-08-17', 'FINISHED', 'FINISHED', 2, 1, 'leg1', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists, yellow_cards, red_cards,
                 rating, shots_on, passes_key, tackles, saves, raw_statistics, synced_at)
                VALUES (94501, 9401, 9001, 'F', 90, 1, 0, 0, 0, '7.8', 2, 1, 0, 0, '{}', CURRENT_TIMESTAMP)
                """);

        mockMvc.perform(get("/api/players/9401/matches").param("season", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].match.id").value(94501))
                .andExpect(jsonPath("$[0].stats.goals").value(1))
                .andExpect(jsonPath("$[0].stats.extra").doesNotExist());

        mockMvc.perform(get("/api/players/9401/season-profile").param("season", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleGroup").value("ATTACK"))
                .andExpect(jsonPath("$.totals.appearances").value(1))
                .andExpect(jsonPath("$.totals.clubFinishedMatches").value(1))
                .andExpect(jsonPath("$.totals.minutes").value(90))
                .andExpect(jsonPath("$.totals.goals").value(1))
                .andExpect(jsonPath("$.totals.expectedGoals").value(nullValue()))
                .andExpect(jsonPath("$.teamContribution").value(nullValue()))
                .andExpect(jsonPath("$.cleanSheets").value(nullValue()))
                .andExpect(jsonPath("$.percentiles").value(nullValue()));
    }

    @Test
    void seasonProfileComputesTotalsTeamContributionAndPercentilesWithTies() throws Exception {
        // 9101 (Bruno Test): ATTACK in MIDFIELDER pool (peers >=450m: 9101, 9102, 9103, 9104)
        // Goals/90 pool (4 players): 9103=0.0, 9101=1.0, 9102=1.0 (tie), 9104=2.0
        // For 9101: 3 others [0.0, 1.0, 2.0] -> 1 strictly lower -> 33%
        // xG/90 pool (3 players with complete extra: 9101=0.70, 9102=0.50, 9103=0.10; 9104 excluded!)
        // For 9101: 2 others [0.50, 0.10] -> 2 strictly lower -> 100%, poolSize=3
        mockMvc.perform(get("/api/players/9101/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleGroup").value("ATTACK"))
                .andExpect(jsonPath("$.comparisonGroup").value("M"))
                .andExpect(jsonPath("$.comparisonGroupLabel").value("tiền vệ"))
                .andExpect(jsonPath("$.totals.appearances").value(5))
                .andExpect(jsonPath("$.totals.clubFinishedMatches").value(6))
                .andExpect(jsonPath("$.totals.minutes").value(450))
                .andExpect(jsonPath("$.totals.goals").value(5))
                .andExpect(jsonPath("$.totals.assists").value(3))
                .andExpect(jsonPath("$.totals.yellowCards").value(1))
                .andExpect(jsonPath("$.totals.redCards").value(0))
                .andExpect(jsonPath("$.totals.averageRating").value(7.5))
                .andExpect(jsonPath("$.totals.ratedMatches").value(5))
                .andExpect(jsonPath("$.totals.expectedGoals").value(3.5))
                .andExpect(jsonPath("$.totals.expectedAssists").value(2.0))
                .andExpect(jsonPath("$.totals.tackles").value(10))
                .andExpect(jsonPath("$.totals.clearancesBlocksInterceptions").value(5))
                .andExpect(jsonPath("$.totals.recoveries").value(20))
                .andExpect(jsonPath("$.totals.bps").value(125))
                .andExpect(jsonPath("$.totals.bonus").value(4))
                .andExpect(jsonPath("$.teamContribution.teamGoals").value(12))
                .andExpect(jsonPath("$.teamContribution.goals").value(5))
                .andExpect(jsonPath("$.teamContribution.assists").value(3))
                .andExpect(jsonPath("$.cleanSheets").value(nullValue()))
                .andExpect(jsonPath("$.percentiles[*].key",
                        contains("goalsPer90", "xgPer90", "assistsPer90", "xaPer90",
                                "tacklesPer90", "defendingPer90", "bpsPer90")))
                .andExpect(jsonPath("$.percentiles[0].percentile").value(33))
                .andExpect(jsonPath("$.percentiles[0].poolSize").value(4))
                .andExpect(jsonPath("$.percentiles[1].value").value(0.7))
                .andExpect(jsonPath("$.percentiles[1].percentile").value(100))
                .andExpect(jsonPath("$.percentiles[1].poolSize").value(3));
    }

    @Test
    void missingExtraRowNullsAllExtendedTotalsAndLeavesBasicTotalsIntact() throws Exception {
        mockMvc.perform(get("/api/players/9104/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.appearances").value(5))
                .andExpect(jsonPath("$.totals.goals").value(10))
                .andExpect(jsonPath("$.totals.expectedGoals").value(nullValue()))
                .andExpect(jsonPath("$.totals.expectedAssists").value(nullValue()))
                .andExpect(jsonPath("$.totals.tackles").value(nullValue()))
                .andExpect(jsonPath("$.totals.clearancesBlocksInterceptions").value(nullValue()))
                .andExpect(jsonPath("$.totals.recoveries").value(nullValue()))
                .andExpect(jsonPath("$.totals.bps").value(nullValue()))
                .andExpect(jsonPath("$.totals.bonus").value(nullValue()))
                // Basic percentile (goalsPer90) still computed; extra percentile (xgPer90) is null
                .andExpect(jsonPath("$.percentiles[0].key").value("goalsPer90"))
                .andExpect(jsonPath("$.percentiles[0].percentile").value(100))
                .andExpect(jsonPath("$.percentiles[1].key").value("xgPer90"))
                .andExpect(jsonPath("$.percentiles[1].value").value(nullValue()))
                .andExpect(jsonPath("$.percentiles[1].percentile").value(nullValue()));
    }

    @Test
    void playerUnder450MinutesHasNullPercentiles() throws Exception {
        mockMvc.perform(get("/api/players/9105/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.minutes").value(180))
                .andExpect(jsonPath("$.percentiles").value(nullValue()));
    }

    @Test
    void mixedDefendAndGkRoleGroupsExposeRightBlocksAndLowerIsBetterPercentiles() throws Exception {
        // MIXED (CM)
        mockMvc.perform(get("/api/players/9102/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleGroup").value("MIXED"))
                .andExpect(jsonPath("$.teamContribution").isMap())
                .andExpect(jsonPath("$.cleanSheets").value(nullValue()));

        // DEFEND (CB)
        mockMvc.perform(get("/api/players/9201/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleGroup").value("DEFEND"))
                .andExpect(jsonPath("$.comparisonGroup").value("D"))
                .andExpect(jsonPath("$.comparisonGroupLabel").value("hậu vệ"))
                .andExpect(jsonPath("$.teamContribution").value(nullValue()))
                .andExpect(jsonPath("$.cleanSheets.cleanSheets").value(2))
                .andExpect(jsonPath("$.cleanSheets.appearances").value(5))
                .andExpect(jsonPath("$.percentiles[*].key",
                        contains("tacklesPer90", "cbiPer90", "recoveriesPer90", "defendingPer90",
                                "assistsPer90", "xaPer90", "bpsPer90")))
                .andExpect(jsonPath("$.percentiles[0].percentile").value(100));

        // GK: 9301 has 0 conceded/90 (lowest -> 100%); 9302 has 1.0 conceded/90 tied with 9303, better than 9304 (1/3 -> 33%)
        mockMvc.perform(get("/api/players/9301/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleGroup").value("GK"))
                .andExpect(jsonPath("$.comparisonGroup").value("G"))
                .andExpect(jsonPath("$.comparisonGroupLabel").value("thủ môn"))
                .andExpect(jsonPath("$.teamContribution").value(nullValue()))
                .andExpect(jsonPath("$.cleanSheets.cleanSheets").value(3))
                .andExpect(jsonPath("$.cleanSheets.appearances").value(5))
                .andExpect(jsonPath("$.percentiles[*].key",
                        contains("savesPer90", "goalsConcededPer90", "cleanSheetRate", "recoveriesPer90", "bpsPer90")))
                .andExpect(jsonPath("$.percentiles[1].key").value("goalsConcededPer90"))
                .andExpect(jsonPath("$.percentiles[1].value").value(0.0))
                .andExpect(jsonPath("$.percentiles[1].percentile").value(100))
                .andExpect(jsonPath("$.percentiles[2].key").value("cleanSheetRate"))
                .andExpect(jsonPath("$.percentiles[2].value").value(0.6))
                .andExpect(jsonPath("$.percentiles[2].percentile").value(100));

        mockMvc.perform(get("/api/players/9302/season-profile").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles[1].key").value("goalsConcededPer90"))
                .andExpect(jsonPath("$.percentiles[1].value").value(1.0))
                .andExpect(jsonPath("$.percentiles[1].percentile").value(33));

        mockMvc.perform(get("/api/players/999999/season-profile").param("season", "2026"))
                .andExpect(status().isNotFound());
    }
}
