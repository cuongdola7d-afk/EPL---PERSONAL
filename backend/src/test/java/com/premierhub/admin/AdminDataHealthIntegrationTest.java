package com.premierhub.admin;

import com.premierhub.accounts.SystemAccount;
import com.premierhub.web.dto.PlayerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-data-health-int;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=",
        "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "premierhub.auth-proxy.enabled=false",
        "premierhub.fantasy.automation.enabled=false",
        "premierhub.fantasy.automation.dry-run=true"
})
@AutoConfigureMockMvc
class AdminDataHealthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean(name = "fantasyGameweekClock") Clock clock;

    private static final Instant NOW = Instant.parse("2026-11-26T09:12:00Z"); // 16:12 VN
    private static final String HASH = "0".repeat(64);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);

        // Clean season 2026 operational tables in FK-safe order
        jdbc.update("DELETE FROM fpl_provisional_unmapped");
        jdbc.update("DELETE FROM fantasy_unrated_confirmations");
        jdbc.update("DELETE FROM manual_fixture_player_stats");
        jdbc.update("DELETE FROM fantasy_result_publications");
        jdbc.update("DELETE FROM fantasy_deadline_changes");
        jdbc.update("DELETE FROM fantasy_gameweeks");
        jdbc.update("DELETE FROM fixtures WHERE league_id = 39 AND season_year = 2026");
        jdbc.update("DELETE FROM player_eligible_positions WHERE league_id = 39 AND season_year = 2026");
        jdbc.update("DELETE FROM player_specific_positions WHERE league_id = 39 AND season_year = 2026");
        jdbc.update("DELETE FROM player_season_profiles WHERE league_id = 39 AND season_year = 2026");
        jdbc.update("DELETE FROM manual_player_memberships WHERE league_id = 39 AND season_year = 2026");
        jdbc.update("DELETE FROM player_season_stats WHERE league_id = 39 AND season_year = 2026");
        jdbc.update("DELETE FROM players WHERE id >= 900000");
        jdbc.update("DELETE FROM season_clubs WHERE league_id = 39 AND season_year = 2026 AND club_id >= 900");
        jdbc.update("DELETE FROM clubs WHERE id >= 900");

        jdbc.update("MERGE INTO seasons (league_id, season_year) KEY (league_id, season_year) VALUES (39, 2026)");
        jdbc.update("MERGE INTO clubs (id, name, city) KEY (id) VALUES (901, 'Sunderland AFC', 'Sunderland')");
        jdbc.update("MERGE INTO clubs (id, name, city) KEY (id) VALUES (902, 'Hull City AFC', 'Hull')");
        jdbc.update("MERGE INTO clubs (id, name, city) KEY (id) VALUES (903, 'Chelsea FC', 'London')");
        jdbc.update("MERGE INTO season_clubs (league_id, season_year, club_id) KEY (league_id, season_year, club_id) VALUES (39, 2026, 901), (39, 2026, 902), (39, 2026, 903)");

        jdbc.update("DELETE FROM accounts WHERE email IN ('admin-dh@example.test', 'user-dh@example.test')");
        jdbc.update("INSERT INTO accounts (email, display_name, password_hash, role, created_at) "
                + "VALUES ('admin-dh@example.test', 'Admin DH', '{noop}secret', 'ADMIN', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO accounts (email, display_name, password_hash, role, created_at) "
                + "VALUES ('user-dh@example.test', 'User DH', '{noop}secret', 'USER', CURRENT_TIMESTAMP)");
    }

    private void insertPlayer(int playerId, String name, int clubId, String broadPos) {
        jdbc.update("MERGE INTO players (id, name) KEY (id) VALUES (?, ?)", playerId, name);
        jdbc.update("""
                MERGE INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                KEY (league_id, season_year, player_id, club_id)
                VALUES (39, 2026, ?, ?, ?)
                """, playerId, clubId, broadPos);
    }

    private void insertMembership(int playerId, int clubId, String startDate, String endDate) {
        jdbc.update("""
                INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (39, 2026, ?, ?, DATE '%s', %s)
                """.formatted(startDate, endDate == null ? "NULL" : "DATE '" + endDate + "'"),
                playerId, clubId);
    }

    private void insertPosition(int playerId, String primary, String... eligible) {
        jdbc.update("""
                MERGE INTO player_specific_positions (league_id, season_year, player_id, primary_position)
                KEY (league_id, season_year, player_id)
                VALUES (39, 2026, ?, ?)
                """, playerId, primary);
        for (String code : eligible) {
            jdbc.update("""
                    MERGE INTO player_eligible_positions (league_id, season_year, player_id, position_code)
                    KEY (league_id, season_year, player_id, position_code)
                    VALUES (39, 2026, ?, ?)
                    """, playerId, code);
        }
    }

    private void insertOvr(int playerId, int clubId, Integer ovr) {
        jdbc.update("""
                MERGE INTO player_season_profiles (league_id, season_year, player_id, club_id, fc27_overall)
                KEY (league_id, season_year, player_id, club_id)
                VALUES (39, 2026, ?, ?, ?)
                """, playerId, clubId, ovr);
    }

    private void insertFixture(int fixtureId, int gw, int homeId, int awayId, String date, String status) {
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id, gameweek,
                                      match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (?, 39, 2026, ?, ?, ?, DATE '%s', ?, ?, 1, 0, ?, CURRENT_TIMESTAMP)
                """.formatted(date), fixtureId, homeId, awayId, gw, status, status, HASH);
    }

    private void insertManualStat(int fixtureId, int playerId, int clubId, String status,
                                  Integer minutes, Integer goals, Integer assists, Double rating) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (?, ?, 39, 2026, ?, ?, ?, NULL, ?, ?, ?, 0, 0)
                """, fixtureId, playerId, clubId, status, rating, minutes, goals, assists);
    }

    @Test
    void enforcesAdminAccessAndReturnsCleanStateWithoutEmails() throws Exception {
        // Guest -> 401
        mvc.perform(get("/api/admin/data-health"))
                .andExpect(status().isUnauthorized());

        // USER -> 403
        mvc.perform(get("/api/admin/data-health")
                        .with(user("user-dh@example.test").roles("USER")))
                .andExpect(status().isForbidden());

        // Seed 1 clean player with valid membership, position, OVR, and complete GW11 stat
        insertPlayer(900001, "Clean Player", 901, "MIDFIELDER");
        insertMembership(900001, 901, "2026-07-01", null);
        insertPosition(900001, "CM", "CM");
        insertOvr(900001, 901, 78);
        insertFixture(990011, 11, 901, 902, "2026-11-21", "FINISHED");
        insertManualStat(990011, 900001, 901, "PLAYED", 90, 1, 0, 7.6);

        String body = mvc.perform(get("/api/admin/data-health")
                        .with(user("admin-dh@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.season").value(2026))
                .andExpect(jsonPath("$.warnCount").value(0))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.checks", hasSize(5)))
                .andExpect(jsonPath("$.checks[0].key").value("playedWithoutMembership"))
                .andExpect(jsonPath("$.checks[0].count").value(0))
                .andExpect(jsonPath("$.checks[0].status").value("OK"))
                .andExpect(jsonPath("$.checks[1].key").value("rosterNotSelectable"))
                .andExpect(jsonPath("$.checks[1].count").value(0))
                .andExpect(jsonPath("$.checks[1].status").value("OK"))
                .andExpect(jsonPath("$.checks[2].key").value("fplUnmapped"))
                .andExpect(jsonPath("$.checks[2].count").value(0))
                .andExpect(jsonPath("$.checks[2].status").value("OK"))
                .andExpect(jsonPath("$.checks[3].key").value("multipleOpenMemberships"))
                .andExpect(jsonPath("$.checks[3].count").value(0))
                .andExpect(jsonPath("$.checks[3].status").value("OK"))
                .andExpect(jsonPath("$.checks[4].key").value("playedMissingCore"))
                .andExpect(jsonPath("$.checks[4].count").value(0))
                .andExpect(jsonPath("$.checks[4].status").value("OK"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("@"), "Data health response must never expose emails");
        assertFalse(body.contains(SystemAccount.EMAIL));
    }

    @Test
    void detectsAllFiveIssueTypesWithExactRulesAndCleanExclusions() throws Exception {
        // Fixtures: GW5 (old), GW9 (older than last 2 FINISHED), GW11 & GW12 (last 2 FINISHED)
        insertFixture(990005, 5, 901, 902, "2026-09-20", "FINISHED");
        insertFixture(990009, 9, 901, 902, "2026-10-25", "FINISHED");
        insertFixture(990011, 11, 901, 902, "2026-11-21", "FINISHED");
        insertFixture(990012, 12, 901, 902, "2026-11-26", "FINISHED");

        // Open GW13 with roster_as_of = 2026-11-26
        jdbc.update("""
                INSERT INTO fantasy_gameweeks (season, gameweek, deadline_utc, deadline_published_at,
                                               first_fixture_id, first_kickoff_utc, workflow_status,
                                               results_published_at, revision, updated_at, roster_as_of)
                VALUES (2026, 13, TIMESTAMP '2026-12-04 19:30:00', TIMESTAMP '2026-11-25 10:00:00',
                        990012, TIMESTAMP '2026-12-04 20:00:00', 'OPEN',
                        NULL, 1, TIMESTAMP '2026-11-25 10:00:00', DATE '2026-11-26')
                """);

        // 1) playedWithoutMembership:
        // - 900101 played in GW12 with closed membership -> FLAGGED
        insertPlayer(900101, "Romaine Mundle", 901, "FORWARD");
        insertMembership(900101, 901, "2026-07-01", "2026-08-31");
        insertManualStat(990012, 900101, 901, "DID_NOT_PLAY", 0, 0, 0, null);

        // - 900102 played ONLY in GW9 (not in last 2 FINISHED GWs 11-12) with closed membership -> CLEAN
        insertPlayer(900102, "Old Closed Player", 901, "DEFENDER");
        insertMembership(900102, 901, "2026-07-01", "2026-08-31");
        insertManualStat(990009, 900102, 901, "PLAYED", 90, 0, 0, 7.0);

        // - 900103 played in GW12 with open membership -> CLEAN
        insertPlayer(900103, "Active Starter", 901, "MIDFIELDER");
        insertMembership(900103, 901, "2026-07-01", null);
        insertPosition(900103, "CM", "CM");
        insertOvr(900103, 901, 79);
        insertManualStat(990012, 900103, 901, "PLAYED", 90, 0, 1, 7.4);

        // - 900104 Cissé case: DID_NOT_PLAY on 2026-11-22 (GW11), membership closed later on 2026-11-25 -> CLEAN
        insertFixture(990013, 11, 903, 902, "2026-11-22", "FINISHED");
        insertPlayer(900104, "Modou Keba Cisse", 903, "DEFENDER");
        insertMembership(900104, 903, "2026-07-01", "2026-11-25");
        insertManualStat(990013, 900104, 903, "DID_NOT_PLAY", 0, 0, 0, null);

        // 2) rosterNotSelectable:
        // - 900201 active on 2026-11-26, missing position -> FLAGGED (POSITION)
        insertPlayer(900201, "Missing Pos Rookie", 903, "DEFENDER");
        insertMembership(900201, 903, "2026-07-01", null);
        insertOvr(900201, 903, 66);

        // - 900202 active on 2026-11-26, missing OVR -> FLAGGED (OVR)
        insertPlayer(900202, "Missing Ovr Rookie", 903, "FORWARD");
        insertMembership(900202, 903, "2026-07-01", null);
        insertPosition(900202, "ST", "ST");

        // 3) fplUnmapped:
        jdbc.update("""
                INSERT INTO fpl_provisional_unmapped (fixture_id, fpl_element_id, fpl_name, club_id, club_name, minutes, fetched_at)
                VALUES (990012, 777, 'Unknown FPL Kid', 902, 'Hull City AFC', 14, CURRENT_TIMESTAMP)
                """);

        // 4) multipleOpenMemberships:
        // - 900401 has 2 open memberships -> FLAGGED
        insertPlayer(900401, "Two Club Player", 901, "MIDFIELDER");
        insertPlayer(900401, "Two Club Player", 902, "MIDFIELDER");
        insertMembership(900401, 901, "2026-07-01", null);
        insertMembership(900401, 902, "2026-09-01", null);
        insertPosition(900401, "CM", "CM");
        insertOvr(900401, 901, 74);
        insertOvr(900401, 902, 74);

        // - 900402 has 1 closed + 1 open -> CLEAN
        insertPlayer(900402, "Transferred Cleanly", 901, "DEFENDER");
        insertPlayer(900402, "Transferred Cleanly", 902, "DEFENDER");
        insertMembership(900402, 901, "2026-07-01", "2026-08-15");
        insertMembership(900402, 902, "2026-08-15", null);
        insertPosition(900402, "CB", "CB");
        insertOvr(900402, 902, 75);

        // 5) playedMissingCore:
        // - 900501 GW11 PLAYED 45' rating NULL (no unrated confirmation) -> FLAGGED (RATING)
        insertPlayer(900501, "Missing Rating Player", 902, "FORWARD");
        insertMembership(900501, 902, "2026-07-01", null);
        insertPosition(900501, "ST", "ST");
        insertOvr(900501, 902, 72);
        insertManualStat(990011, 900501, 902, "PLAYED", 45, 0, 0, null);

        // - 900502 GW11 PLAYED goals NULL -> FLAGGED (GOALS)
        insertPlayer(900502, "Missing Goals Player", 902, "MIDFIELDER");
        insertMembership(900502, 902, "2026-07-01", null);
        insertPosition(900502, "CM", "CM");
        insertOvr(900502, 902, 73);
        insertManualStat(990011, 900502, 902, "PLAYED", 60, null, 0, 6.8);

        // - 900503 GW11 PLAYED 8' (<= 10) rating NULL -> CLEAN
        insertPlayer(900503, "Short Sub Player", 902, "FORWARD");
        insertMembership(900503, 902, "2026-07-01", null);
        insertPosition(900503, "RW", "RW");
        insertOvr(900503, 902, 70);
        insertManualStat(990011, 900503, 902, "PLAYED", 8, 0, 0, null);

        // - 900504 GW11 PLAYED 20' rating NULL WITH fantasy_unrated_confirmations -> CLEAN
        insertPlayer(900504, "Confirmed Unrated Player", 902, "DEFENDER");
        insertMembership(900504, 902, "2026-07-01", null);
        insertPosition(900504, "LB", "LB");
        insertOvr(900504, 902, 71);
        insertManualStat(990011, 900504, 902, "PLAYED", 20, 0, 0, null);
        jdbc.update("""
                INSERT INTO fantasy_unrated_confirmations (fixture_id, player_id, source_ref, reason, confirmed_at)
                VALUES (990011, 900504, 'user-screenshot', 'SofaScore unrated', CURRENT_TIMESTAMP)
                """);

        // - GW5 PLAYED 90' rating NULL -> CLEAN (before GW6)
        insertManualStat(990005, 900103, 901, "PLAYED", 90, 0, 0, null);

        String body = mvc.perform(get("/api/admin/data-health")
                        .with(user("admin-dh@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openGameweek").value(13))
                .andExpect(jsonPath("$.rosterAsOf").value("2026-11-26"))
                .andExpect(jsonPath("$.warnCount").value(5))
                .andExpect(jsonPath("$.status").value("WARN"))
                // Check 1
                .andExpect(jsonPath("$.checks[0].key").value("playedWithoutMembership"))
                .andExpect(jsonPath("$.checks[0].count").value(1))
                .andExpect(jsonPath("$.checks[0].status").value("WARN"))
                .andExpect(jsonPath("$.checks[0].items", hasSize(1)))
                .andExpect(jsonPath("$.checks[0].items[0].playerName").value("Romaine Mundle"))
                .andExpect(jsonPath("$.checks[0].items[0].clubName").value("Sunderland AFC"))
                .andExpect(jsonPath("$.checks[0].items[0].gameweek").value(12))
                .andExpect(jsonPath("$.checks[0].items[0].participationStatus").value("DID_NOT_PLAY"))
                .andExpect(jsonPath("$.checks[0].items[0].lastClosedEndDate").value("2026-08-31"))
                // Check 2
                .andExpect(jsonPath("$.checks[1].key").value("rosterNotSelectable"))
                .andExpect(jsonPath("$.checks[1].count").value(2))
                .andExpect(jsonPath("$.checks[1].status").value("WARN"))
                .andExpect(jsonPath("$.checks[1].items", hasSize(2)))
                .andExpect(jsonPath("$.checks[1].items[0].playerName").value("Missing Ovr Rookie"))
                .andExpect(jsonPath("$.checks[1].items[0].missing[0]").value("OVR"))
                .andExpect(jsonPath("$.checks[1].items[1].playerName").value("Missing Pos Rookie"))
                .andExpect(jsonPath("$.checks[1].items[1].missing[0]").value("POSITION"))
                // Check 3
                .andExpect(jsonPath("$.checks[2].key").value("fplUnmapped"))
                .andExpect(jsonPath("$.checks[2].count").value(1))
                .andExpect(jsonPath("$.checks[2].status").value("WARN"))
                .andExpect(jsonPath("$.checks[2].items[0].fplName").value("Unknown FPL Kid"))
                .andExpect(jsonPath("$.checks[2].items[0].clubName").value("Hull City AFC"))
                .andExpect(jsonPath("$.checks[2].items[0].gameweek").value(12))
                // Check 4
                .andExpect(jsonPath("$.checks[3].key").value("multipleOpenMemberships"))
                .andExpect(jsonPath("$.checks[3].count").value(1))
                .andExpect(jsonPath("$.checks[3].status").value("WARN"))
                .andExpect(jsonPath("$.checks[3].items[0].playerName").value("Two Club Player"))
                .andExpect(jsonPath("$.checks[3].items[0].openCount").value(2))
                .andExpect(jsonPath("$.checks[3].items[0].clubs", contains("Sunderland AFC", "Hull City AFC")))
                // Check 5
                .andExpect(jsonPath("$.checks[4].key").value("playedMissingCore"))
                .andExpect(jsonPath("$.checks[4].count").value(2))
                .andExpect(jsonPath("$.checks[4].status").value("WARN"))
                .andExpect(jsonPath("$.checks[4].items", hasSize(2)))
                .andExpect(jsonPath("$.checks[4].items[0].playerName").value("Missing Goals Player"))
                .andExpect(jsonPath("$.checks[4].items[0].missing", contains("GOALS")))
                .andExpect(jsonPath("$.checks[4].items[1].playerName").value("Missing Rating Player"))
                .andExpect(jsonPath("$.checks[4].items[1].missing", contains("RATING")))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("@"), "Data health response must never expose emails");
    }

    @Test
    void capsItemsAtTwentyWhileReportingExactCount() throws Exception {
        for (int i = 1; i <= 25; i++) {
            int id = 910000 + i;
            insertPlayer(id, "Rookie %02d".formatted(i), 903, "MIDFIELDER");
            insertMembership(id, 903, "2026-07-01", null);
        }
        mvc.perform(get("/api/admin/data-health")
                        .with(user("admin-dh@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checks[1].key").value("rosterNotSelectable"))
                .andExpect(jsonPath("$.checks[1].count").value(25))
                .andExpect(jsonPath("$.checks[1].items", hasSize(20)));
    }

    @Test
    void missingSelectableFieldsFlagsOutOfRangeOverallUsingFantasyLineupRule() {
        PlayerResponse outOfRange = new PlayerResponse(
                1, "Bad OVR", 901, "Club", "MIDFIELDER", 0, 0,
                100, null, null, null, null, null, "CM", List.of("CM"), "VERIFIED");
        assertEquals(List.of("OVR_RANGE"), AdminDataHealthService.missingSelectableFields(outOfRange));

        PlayerResponse zeroOvr = new PlayerResponse(
                2, "Zero OVR", 901, "Club", "MIDFIELDER", 0, 0,
                0, null, null, null, null, null, "CM", List.of("CM"), "VERIFIED");
        assertEquals(List.of("OVR_RANGE"), AdminDataHealthService.missingSelectableFields(zeroOvr));
    }

    @Test
    void doesNotFlagPlayedWithoutMembershipWhenClosedMembershipCoversMatchDate() throws Exception {
        insertFixture(990021, 11, 903, 902, "2026-11-22", "FINISHED");
        insertPlayer(900601, "Modou Keba Cisse", 903, "DEFENDER");
        insertMembership(900601, 903, "2026-07-01", "2026-11-25");
        insertManualStat(990021, 900601, 903, "DID_NOT_PLAY", 0, 0, 0, null);

        mvc.perform(get("/api/admin/data-health")
                        .with(user("admin-dh@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checks[0].key").value("playedWithoutMembership"))
                .andExpect(jsonPath("$.checks[0].count").value(0))
                .andExpect(jsonPath("$.checks[0].status").value("OK"))
                .andExpect(jsonPath("$.checks[0].items", hasSize(0)));
    }
}
