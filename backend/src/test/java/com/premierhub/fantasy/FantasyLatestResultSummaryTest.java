package com.premierhub.fantasy;

import com.premierhub.accounts.SystemAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fantasy-latest-summary;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=",
        "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "premierhub.auth-proxy.enabled=false"
})
@AutoConfigureMockMvc
class FantasyLatestResultSummaryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean(name = "fantasyGameweekClock") Clock clock;

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        jdbc.update("DELETE FROM fantasy_team_results");
        jdbc.update("DELETE FROM fantasy_result_publications");
        jdbc.update("DELETE FROM fantasy_submitted_picks");
        jdbc.update("DELETE FROM fantasy_draft_picks");
        jdbc.update("DELETE FROM fantasy_entries");
        jdbc.update("DELETE FROM fantasy_deadline_changes");
        jdbc.update("DELETE FROM fantasy_gameweeks");
        jdbc.update("DELETE FROM account_activity_days");
        jdbc.update("DELETE FROM account_identities");
        jdbc.update("DELETE FROM accounts");

        jdbc.update("INSERT INTO accounts (id, email, display_name, password_hash, role, created_at) VALUES "
                + "(10, ?, ?, NULL, 'ADMIN', CURRENT_TIMESTAMP),"
                + "(101, 'alice@example.test', 'Alice', '{noop}pw', 'USER', CURRENT_TIMESTAMP),"
                + "(102, 'bob@example.test', 'Bob', '{noop}pw', 'USER', CURRENT_TIMESTAMP),"
                + "(103, 'charlie@example.test', 'Charlie', '{noop}pw', 'USER', CURRENT_TIMESTAMP),"
                + "(104, 'dana@example.test', 'Dana', '{noop}pw', 'USER', CURRENT_TIMESTAMP),"
                + "(105, 'evan@example.test', 'Evan', '{noop}pw', 'USER', CURRENT_TIMESTAMP)",
                SystemAccount.EMAIL, SystemAccount.DISPLAY_NAME);
    }

    private void seedPublishedGw(int gw, Instant publishedAt) {
        String hash = String.valueOf(gw).repeat(64).substring(0, 64);
        jdbc.update("""
                INSERT INTO fantasy_gameweeks (season, gameweek, deadline_utc, deadline_published_at,
                                               first_fixture_id, first_kickoff_utc, workflow_status,
                                               results_published_at, revision, updated_at, roster_as_of)
                VALUES (2026, ?, TIMESTAMP '2026-11-10 11:00:00', TIMESTAMP '2026-11-05 10:00:00',
                        100001, TIMESTAMP '2026-11-10 11:30:00', 'PUBLISHED', ?, 1, ?, DATE '2026-11-05')
                """, gw, GameweekRepository.timestamp(publishedAt), GameweekRepository.timestamp(publishedAt));
        jdbc.update("""
                INSERT INTO fantasy_result_publications (season, gameweek, version, source_hash,
                                                         published_at, published_by, action, reason)
                VALUES (2026, ?, 1, ?, ?, 10, 'PUBLISH', 'Công bố kết quả thử nghiệm')
                """, gw, hash, GameweekRepository.timestamp(publishedAt));
    }

    private void seedTeamResult(long accountId, int gw, String totalPoints, List<FantasyResultService.PlayerScore> players) {
        jdbc.update("""
                INSERT INTO fantasy_entries (account_id, season, gameweek, version, draft_formation, draft_saved_at,
                                             submitted_formation, submitted_at, submitted_version, submitted_total_ovr)
                VALUES (?, 2026, ?, 1, '4-2-1-3', TIMESTAMP '2026-11-08 10:00:00',
                        '4-2-1-3', TIMESTAMP '2026-11-08 10:00:00', 1, 860)
                """, accountId, gw);
        var score = new FantasyResultService.TeamScore("4-2-1-3", new BigDecimal(totalPoints), players);
        jdbc.update("""
                INSERT INTO fantasy_team_results (account_id, season, gameweek, version, submitted_version, total_points, breakdown_json)
                VALUES (?, 2026, ?, 1, 1, ?, ?)
                """, accountId, gw, new BigDecimal(totalPoints), json.writeValueAsString(score));
    }

    private static FantasyResultService.PlayerScore player(String slotKey, int id, String name, String club, String pts) {
        return new FantasyResultService.PlayerScore(slotKey, id, name, club, "ST", new BigDecimal(pts), List.of());
    }

    @Test
    void returnsNullGameweekWhenNoRoundIsPublished() throws Exception {
        mvc.perform(get("/api/fantasy/2026/latest-result-summary"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.gameweek").value(nullValue()))
                .andExpect(jsonPath("$.participants").value(0))
                .andExpect(jsonPath("$.leader").value(nullValue()))
                .andExpect(jsonPath("$.me").value(nullValue()));
    }

    @Test
    void returnsSummaryForGuestPlayerNonPlayerTiesAndExcludesSystemAndEmails() throws Exception {
        Instant pub10 = Instant.parse("2026-11-17T16:00:00Z");
        seedPublishedGw(10, pub10);
        // System account row (if ever present) must NEVER appear in participants/average/leader.
        seedTeamResult(10, 10, "99.90", List.of(player("0-0", 1, "Sys Player", "Arsenal FC", "9.90")));
        // Alice (101) and Bob (102) tie at 71.00; Charlie (103) has 50.50.
        // Average of (71.00 + 71.00 + 50.50) / 3 = 192.50 / 3 = 64.166... -> 64.2
        seedTeamResult(101, 10, "71.00", List.of(
                player("0-2", 202, "Bukayo Saka", "Arsenal FC", "9.50"),
                player("0-0", 201, "Florian Wirtz", "Liverpool FC", "9.50"), // same 9.50, earlier slot 0-0 wins tie!
                player("4-0", 203, "David Raya", "Arsenal FC", "7.00")
        ));
        seedTeamResult(102, 10, "71.00", List.of(player("0-1", 204, "Erling Haaland", "Manchester City FC", "10.00")));
        seedTeamResult(103, 10, "50.50", List.of(player("0-1", 205, "Cole Palmer", "Chelsea FC", "8.20")));

        // 1. Guest
        String guestBody = mvc.perform(get("/api/fantasy/2026/latest-result-summary"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.gameweek").value(10))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.publishedAt").value(pub10.toString()))
                .andExpect(jsonPath("$.participants").value(3))
                .andExpect(jsonPath("$.averagePoints").value(64.2))
                .andExpect(jsonPath("$.leader.displayName").value("Alice")) // lower account_id wins tie like leaderboard
                .andExpect(jsonPath("$.leader.points").value(71.0))
                .andExpect(jsonPath("$.me").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        assertFalse(guestBody.contains("@"), "Must never leak any email address");

        // 2. Logged-in participant (Alice: tied rank 1, topPlayer tie broken by pitch slot order 0-0 -> Florian Wirtz)
        String aliceBody = mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("alice@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.points").value(71.0))
                .andExpect(jsonPath("$.me.rank").value(1))
                .andExpect(jsonPath("$.me.participants").value(3))
                .andExpect(jsonPath("$.me.topPlayer.name").value("Florian Wirtz"))
                .andExpect(jsonPath("$.me.topPlayer.club").value("Liverpool FC"))
                .andExpect(jsonPath("$.me.topPlayer.points").value(9.5))
                .andExpect(jsonPath("$.me.seasonRank").value(1))
                .andExpect(jsonPath("$.me.seasonRankChange").value(nullValue())) // first published GW
                .andReturn().getResponse().getContentAsString();
        assertFalse(aliceBody.contains("alice@example.test"));

        // 3. Logged-in tied participant (Bob also gets rank 1!)
        mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("bob@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.rank").value(1))
                .andExpect(jsonPath("$.me.topPlayer.name").value("Erling Haaland"));

        // 4. Logged-in non-participant (Dana did not play GW10)
        mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("dana@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameweek").value(10))
                .andExpect(jsonPath("$.leader.displayName").value("Alice"))
                .andExpect(jsonPath("$.me").value(nullValue()));
    }

    @Test
    void computesSeasonRankChangeUpDownSameAndFirstRoundNull() throws Exception {
        // GW10 season ranks: Alice=1 (80), Bob=2 (70), Charlie=3 (60). Dana didn't play GW10.
        seedPublishedGw(10, Instant.parse("2026-11-17T16:00:00Z"));
        seedTeamResult(101, 10, "80.00", List.of(player("0-0", 301, "Player A", "Arsenal FC", "8.00")));
        seedTeamResult(102, 10, "70.00", List.of(player("0-0", 302, "Player B", "Chelsea FC", "7.00")));
        seedTeamResult(103, 10, "60.00", List.of(player("0-0", 303, "Player C", "Liverpool FC", "6.00")));

        // GW11 totals -> cumulative season:
        // Bob: 70 + 80 = 150 -> rank 1 (was 2 => UP +1)
        // Alice: 80 + 50 = 130 -> rank 2 (was 1 => DOWN -1)
        // Charlie: 60 + 60 = 120 -> rank 3 (was 3 => SAME 0)
        // Dana: 0 + 75 = 75 -> rank 4 (first GW => null)
        seedPublishedGw(11, Instant.parse("2026-11-24T16:52:52Z"));
        seedTeamResult(101, 11, "50.00", List.of(player("0-0", 301, "Player A", "Arsenal FC", "6.50")));
        seedTeamResult(102, 11, "80.00", List.of(player("0-0", 302, "Player B", "Chelsea FC", "9.20")));
        seedTeamResult(103, 11, "60.00", List.of(player("0-0", 303, "Player C", "Liverpool FC", "7.40")));
        seedTeamResult(104, 11, "75.00", List.of(player("0-0", 304, "Player D", "Everton FC", "8.80")));

        // Bob moved UP 2 -> 1 (+1)
        mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("bob@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameweek").value(11))
                .andExpect(jsonPath("$.me.rank").value(1))
                .andExpect(jsonPath("$.me.seasonRank").value(1))
                .andExpect(jsonPath("$.me.seasonRankChange").value(1));

        // Alice moved DOWN 1 -> 2 (-1)
        mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("alice@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.seasonRank").value(2))
                .andExpect(jsonPath("$.me.seasonRankChange").value(-1));

        // Charlie stayed SAME 3 -> 3 (0)
        mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("charlie@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.seasonRank").value(3))
                .andExpect(jsonPath("$.me.seasonRankChange").value(0));

        // Dana's first published round -> seasonRank=4, seasonRankChange=null
        mvc.perform(get("/api/fantasy/2026/latest-result-summary")
                        .with(user("dana@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.seasonRank").value(4))
                .andExpect(jsonPath("$.me.seasonRankChange").value(nullValue()));
    }
}
