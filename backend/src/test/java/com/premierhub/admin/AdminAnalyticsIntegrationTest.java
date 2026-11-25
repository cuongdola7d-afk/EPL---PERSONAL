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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-analytics-int;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
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
}
