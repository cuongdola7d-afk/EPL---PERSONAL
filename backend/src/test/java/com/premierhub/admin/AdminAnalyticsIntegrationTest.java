package com.premierhub.admin;

import com.premierhub.accounts.AccountService;
import com.premierhub.accounts.GoogleAccountService;
import com.premierhub.accounts.SystemAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-analytics-int;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "PREMIERHUB_CORS_ALLOWED_ORIGINS=https://premierhub.vercel.app",
        "PREMIERHUB_GOOGLE_CLIENT_ID=",
        "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "premierhub.auth-proxy.enabled=false",
        "premierhub.fantasy.automation.enabled=false",
        "premierhub.fantasy.automation.dry-run=true"
})
@AutoConfigureMockMvc
class AdminAnalyticsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccountService accountService;
    @Autowired GoogleAccountService googleAccountService;
    @MockitoBean(name = "fantasyGameweekClock") Clock clock;

    private static final Instant NOW = Instant.parse("2026-11-24T17:30:00Z"); // 2026-11-25 00:30 VN

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        jdbc.update("DELETE FROM fantasy_result_publications");
        jdbc.update("DELETE FROM fantasy_deadline_changes");
        jdbc.update("DELETE FROM fantasy_gameweeks");
        jdbc.update("DELETE FROM fixtures WHERE league_id = 39 AND season_year = 2026 AND gameweek = 11");
        jdbc.update("DELETE FROM site_visitors_daily");
        jdbc.update("DELETE FROM site_page_views_daily");
        jdbc.update("DELETE FROM account_activity_days");
        jdbc.update("DELETE FROM account_identities");
        jdbc.update("DELETE FROM accounts WHERE email <> ?", SystemAccount.EMAIL);

        // Ensure system account exists as seeded by auth-schema.sql
        jdbc.update("""
                INSERT INTO accounts (email, display_name, password_hash, role, created_at)
                SELECT ?, ?, NULL, 'ADMIN', CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE email = ?)
                """, SystemAccount.EMAIL, SystemAccount.DISPLAY_NAME, SystemAccount.EMAIL);

        jdbc.update("INSERT INTO accounts (email, display_name, password_hash, role, created_at) "
                + "VALUES ('admin@example.test', 'Admin User', '{noop}secret', 'ADMIN', TIMESTAMP '2026-11-20 10:00:00')");
        jdbc.update("INSERT INTO accounts (email, display_name, password_hash, role, created_at) "
                + "VALUES ('player1@example.test', 'Player One', NULL, 'USER', TIMESTAMP '2026-11-24 10:00:00')");
        Long playerId = jdbc.queryForObject("SELECT id FROM accounts WHERE email = 'player1@example.test'", Long.class);
        jdbc.update("INSERT INTO account_identities (provider, subject_id, account_id, created_at) "
                + "VALUES ('google', 'sub-player-1', ?, CURRENT_TIMESTAMP)", playerId);
    }

    @Test
    void systemAccountCannotLogInByPasswordOrGoogle() throws Exception {
        assertThrows(UsernameNotFoundException.class,
                () -> accountService.loadUserByUsername(SystemAccount.EMAIL));

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SystemAccount.EMAIL + "\",\"password\":\"anything123\"}"))
                .andExpect(status().isUnauthorized());

        RuntimeException googleFailure = assertThrows(RuntimeException.class,
                () -> googleAccountService.login(
                        new GoogleAccountService.Profile("google-sub-system", SystemAccount.EMAIL, "Hệ thống")));
        assertEquals("GoogleAccountException", googleFailure.getClass().getSimpleName());
    }

    @Test
    void adminOverviewEnforcesRoleAndExcludesSystemAccountAndEmails() throws Exception {
        // Guest -> 401
        mvc.perform(get("/api/admin/overview?range=7"))
                .andExpect(status().isUnauthorized());

        // USER -> 403
        mvc.perform(get("/api/admin/overview?range=7")
                        .with(user("player1@example.test").roles("USER")))
                .andExpect(status().isForbidden());

        // Trigger authenticated activity for player1
        mvc.perform(get("/api/auth/me")
                        .with(user("player1@example.test").roles("USER")))
                .andExpect(status().isOk());

        // Public pageview (no CSRF required)
        String rawVisitorId = UUID.randomUUID().toString();
        mvc.perform(post("/api/analytics/pageview")
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + rawVisitorId + "\",\"page\":\"schedule\"}"))
                .andExpect(status().isNoContent());

        // Invalid page rejected
        mvc.perform(post("/api/analytics/pageview")
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + rawVisitorId + "\",\"page\":\"bad-page\"}"))
                .andExpect(status().isBadRequest());

        // Bot ignored
        mvc.perform(post("/api/analytics/pageview")
                        .header("User-Agent", "Googlebot/2.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\",\"page\":\"home\"}"))
                .andExpect(status().isNoContent());

        // Verify raw visitorId is NOT stored in DB
        Integer rawMatches = jdbc.queryForObject(
                "SELECT COUNT(*) FROM site_visitors_daily WHERE visitor_hash = ?", Integer.class, rawVisitorId);
        assertEquals(0, rawMatches);

        // ADMIN -> 200
        String body = mvc.perform(get("/api/admin/overview?range=7")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.rangeDays").value(7))
                .andExpect(jsonPath("$.accounts.total").value(2)) // excludes system@prismaxi.invalid!
                .andExpect(jsonPath("$.accounts.activeToday").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.traffic.dailySeries", hasSize(7)))
                .andExpect(jsonPath("$.traffic.topPages[0].pageKey").value("schedule"))
                .andExpect(jsonPath("$.fantasy.automationEnabled").value(false))
                .andExpect(jsonPath("$.fantasy.automationDryRun").value(true))
                .andExpect(jsonPath("$.recentAccounts", hasSize(2)))
                .andExpect(jsonPath("$.recentAccounts[0].displayName").value("Player One"))
                .andExpect(jsonPath("$.recentAccounts[0].authMethod").value("Google"))
                .andExpect(jsonPath("$.recentAccounts[1].displayName").value("Admin User"))
                .andExpect(jsonPath("$.recentAccounts[1].authMethod").value("Email"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("player1@example.test"), "Overview JSON must never expose user email");
        assertFalse(body.contains("admin@example.test"), "Overview JSON must never expose admin email");
        assertFalse(body.contains(SystemAccount.EMAIL), "Overview JSON must never expose system email");

        // Invalid range -> 400
        mvc.perform(get("/api/admin/overview?range=14")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminOverviewReturns200WhenPublishedGameweekExists() throws Exception {
        Long systemId = jdbc.queryForObject(
                "SELECT id FROM accounts WHERE email = ?", Long.class, SystemAccount.EMAIL);
        assertNotNull(systemId);

        jdbc.update("MERGE INTO seasons (league_id, season_year) KEY (league_id, season_year) VALUES (39, 2026)");
        jdbc.update("MERGE INTO clubs (id, name, city) KEY (id) VALUES (901, 'Club Alpha', 'London')");
        jdbc.update("MERGE INTO clubs (id, name, city) KEY (id) VALUES (902, 'Club Beta', 'Liverpool')");
        String hash = "0".repeat(64);
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id, gameweek,
                                      match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (990001, 39, 2026, 901, 902, 11, DATE '2026-11-21', 'FINISHED', 'FINISHED', 2, 1, ?, TIMESTAMP '2026-11-21 17:00:00'),
                       (990002, 39, 2026, 902, 901, 11, DATE '2026-11-22', 'FINISHED', 'FINISHED', 1, 0, ?, TIMESTAMP '2026-11-22 17:00:00')
                """, hash, hash);
        jdbc.update("""
                INSERT INTO fantasy_gameweeks (season, gameweek, deadline_utc, deadline_published_at,
                                               first_fixture_id, first_kickoff_utc, workflow_status,
                                               results_published_at, revision, updated_at, roster_as_of)
                VALUES (2026, 11, TIMESTAMP '2026-11-21 11:00:00', TIMESTAMP '2026-11-15 10:00:00',
                        990001, TIMESTAMP '2026-11-21 12:30:00', 'PUBLISHED',
                        TIMESTAMP '2026-11-24 12:00:00', 1, TIMESTAMP '2026-11-24 12:00:00', DATE '2026-11-15')
                """);
        jdbc.update("""
                INSERT INTO fantasy_result_publications (season, gameweek, version, source_hash,
                                                         published_at, published_by, action, reason)
                VALUES (2026, 11, 1, ?, TIMESTAMP '2026-11-24 12:00:00', ?, 'PUBLISH', 'Tự động công bố khi đủ 10/10 trận')
                """, hash, systemId);

        mvc.perform(get("/api/admin/overview?range=30")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fantasy.latestPublishedGameweek.gameweek").value(11))
                .andExpect(jsonPath("$.fantasy.latestPublishedGameweek.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.fantasy.latestPublishedGameweek.actionAtUtc").value("2026-11-24T12:00:00Z"))
                .andExpect(jsonPath("$.fantasy.latestPublishedGameweek.bySystem").value(true))
                .andExpect(jsonPath("$.fantasy.latestPublishedGameweek.finishedFixtures").value(2))
                .andExpect(jsonPath("$.fantasy.latestPublishedGameweek.totalFixtures").value(2))
                .andExpect(jsonPath("$.fantasy.history[0].actionType").value("PUBLISH_RESULTS"))
                .andExpect(jsonPath("$.fantasy.history[0].gameweek").value(11))
                .andExpect(jsonPath("$.fantasy.history[0].occurredAtUtc").value("2026-11-24T12:00:00Z"))
                .andExpect(jsonPath("$.fantasy.history[0].actorType").value("SYSTEM"));
    }

    @Test
    void pageviewCorsAllowsConfiguredOriginRecordsGuestSkipsAdminRejectsForeignAndSupportsPreflight() throws Exception {
        String allowedOrigin = "https://premierhub.vercel.app";
        String guestVisitorId = UUID.randomUUID().toString();

        // 1. Valid Origin + guest -> 204 + CORS headers + writes 1 row each to site_visitors_daily & site_page_views_daily
        mvc.perform(post("/api/analytics/pageview")
                        .header("Origin", allowedOrigin)
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + guestVisitorId + "\",\"page\":\"schedule\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_page_views_daily", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT views FROM site_page_views_daily WHERE page_key = 'schedule'", Integer.class));

        // 2. Valid Origin + authenticated ADMIN -> 204 + CORS headers + writes nothing
        mvc.perform(post("/api/analytics/pageview")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .header("Origin", allowedOrigin)
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\",\"page\":\"admin\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_page_views_daily", Integer.class));

        // 3. Foreign Origin -> 403, no CORS allow header, writes nothing
        mvc.perform(post("/api/analytics/pageview")
                        .header("Origin", "https://evil.example")
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"" + UUID.randomUUID() + "\",\"page\":\"home\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_page_views_daily", Integer.class));

        // 4. Preflight OPTIONS from valid Origin -> 200 with full CORS headers
        mvc.perform(options("/api/analytics/pageview")
                        .header("Origin", allowedOrigin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void allPublicFrontendPostEndpointsAllowConfiguredOriginWithoutCors403() throws Exception {
        String allowedOrigin = "https://premierhub.vercel.app";

        // Public POST endpoints called directly by the frontend without CSRF
        String[][] publicPosts = {
                {"/api/analytics/pageview", "{\"visitorId\":\"" + UUID.randomUUID() + "\",\"page\":\"home\"}"},
                {"/api/fantasy/2026/validate", "{\"formation\":\"4-3-3\",\"picks\":{}}"}
        };

        for (String[] endpoint : publicPosts) {
            String path = endpoint[0];
            String payload = endpoint[1];

            mvc.perform(options(path)
                            .header("Origin", allowedOrigin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                    .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));

            int statusCode = mvc.perform(post(path)
                            .header("Origin", allowedOrigin)
                            .header("User-Agent", "Mozilla/5.0")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            assertNotEquals(403, statusCode, "Public POST " + path + " must not be rejected with 403 CORS");
        }
    }
}
