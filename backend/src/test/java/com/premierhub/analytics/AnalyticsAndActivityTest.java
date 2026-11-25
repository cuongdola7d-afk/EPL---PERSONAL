package com.premierhub.analytics;

import com.premierhub.accounts.AccountRepository;
import com.premierhub.accounts.SystemAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsAndActivityTest {
    private SingleConnectionDataSource ds;
    private JdbcTemplate jdbc;
    private AccountRepository accountRepository;
    private MutableClock clock;

    @BeforeEach
    void setUp() throws Exception {
        ds = new SingleConnectionDataSource(
                "jdbc:h2:mem:analytics-unit-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                "sa", "", true);
        jdbc = new JdbcTemplate(ds);
        jdbc.execute(Files.readString(Path.of("src/main/resources/auth-schema.sql")));
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS site_visitors_daily (
                    stat_day DATE NOT NULL,
                    visitor_hash CHAR(64) NOT NULL,
                    first_seen_at TIMESTAMP(6) NOT NULL,
                    views INTEGER NOT NULL,
                    PRIMARY KEY (stat_day, visitor_hash)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS site_page_views_daily (
                    stat_day DATE NOT NULL,
                    page_key VARCHAR(32) NOT NULL,
                    views INTEGER NOT NULL,
                    PRIMARY KEY (stat_day, page_key)
                )
                """);
        accountRepository = new AccountRepository(jdbc);
        clock = new MutableClock(Instant.parse("2026-11-24T17:30:00Z")); // 2026-11-25 00:30 VN
    }

    @Test
    void activityWritesAtMostOnceEvery15MinutesAndExcludesSystemAccount() {
        jdbc.update("INSERT INTO accounts (id, email, display_name, password_hash, role, created_at) "
                + "VALUES (10, 'user@example.test', 'User One', 'hash', 'USER', CURRENT_TIMESTAMP)");
        var service = new AccountActivityService(jdbc, accountRepository, clock);

        assertFalse(service.recordActivity(SystemAccount.EMAIL));
        assertTrue(service.recordActivity("user@example.test"));

        Timestamp firstSeen = jdbc.queryForObject(
                "SELECT last_seen_at FROM account_activity_days WHERE account_id = 10", Timestamp.class);
        assertNotNull(firstSeen);

        // 10 minutes later -> throttled
        clock.advanceSeconds(600);
        assertFalse(service.recordActivity("user@example.test"));
        Timestamp stillSame = jdbc.queryForObject(
                "SELECT last_seen_at FROM account_activity_days WHERE account_id = 10", Timestamp.class);
        assertEquals(firstSeen, stillSame);

        // 15 minutes after initial write -> updates last_seen_at
        clock.advanceSeconds(300);
        assertTrue(service.recordActivity("user@example.test"));
        Timestamp updated = jdbc.queryForObject(
                "SELECT last_seen_at FROM account_activity_days WHERE account_id = 10", Timestamp.class);
        assertTrue(updated.after(firstSeen));

        // Stored day is Vietnam calendar date (2026-11-25 for 2026-11-24T17:30Z)
        LocalDate storedDay = jdbc.queryForObject(
                "SELECT stat_day FROM account_activity_days WHERE account_id = 10", Date.class).toLocalDate();
        assertEquals(LocalDate.of(2026, 11, 25), storedDay);
    }

    @Test
    void pageviewRejectsUnknownPageAndInvalidUuidAndIgnoresBotsAndHashesVisitorId() {
        var service = new AnalyticsPageviewService(jdbc, "test-salt-123", clock);
        String visitorId = UUID.randomUUID().toString();

        assertEquals(AnalyticsPageviewService.Status.INVALID_INPUT,
                service.record(visitorId, "match/1000560649", "Mozilla/5.0", false));
        assertEquals(AnalyticsPageviewService.Status.INVALID_INPUT,
                service.record("not-a-uuid", "schedule", "Mozilla/5.0", false));
        assertEquals(AnalyticsPageviewService.Status.IGNORED_BOT,
                service.record(visitorId, "schedule", "Mozilla/5.0 (compatible; Googlebot/2.1)", false));
        assertEquals(AnalyticsPageviewService.Status.IGNORED_BOT,
                service.record(visitorId, "schedule", "UptimeRobot/2.0", false));
        assertEquals(AnalyticsPageviewService.Status.IGNORED_ADMIN,
                service.record(visitorId, "schedule", "Mozilla/5.0", true));

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM site_page_views_daily", Integer.class));

        assertEquals(AnalyticsPageviewService.Status.RECORDED,
                service.record(visitorId, "schedule", "Mozilla/5.0", false));
        assertEquals(AnalyticsPageviewService.Status.RECORDED,
                service.record(visitorId, "fantasy", "Mozilla/5.0", false));

        String storedHash = jdbc.queryForObject("SELECT visitor_hash FROM site_visitors_daily", String.class);
        assertNotNull(storedHash);
        assertEquals(64, storedHash.length());
        assertNotEquals(visitorId, storedHash);
        assertFalse(storedHash.contains(visitorId));

        assertEquals(2, jdbc.queryForObject("SELECT views FROM site_visitors_daily", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT views FROM site_page_views_daily WHERE page_key='schedule'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT views FROM site_page_views_daily WHERE page_key='fantasy'", Integer.class));
    }

    @Test
    void pageviewEnforcesHourlyRateLimitAndRetentionDeletesOnlyOldVisitors() {
        var service = new AnalyticsPageviewService(jdbc, "test-salt-123", clock);
        String visitorId = UUID.randomUUID().toString();

        for (int i = 0; i < AnalyticsPageviewService.MAX_VIEWS_PER_VISITOR_PER_HOUR; i++) {
            assertEquals(AnalyticsPageviewService.Status.RECORDED,
                    service.record(visitorId, "home", "Mozilla/5.0", false));
        }
        assertEquals(AnalyticsPageviewService.Status.RATE_LIMITED,
                service.record(visitorId, "home", "Mozilla/5.0", false));

        // Seed 401-day-old visitor & pageview rows
        LocalDate oldDay = LocalDate.of(2026, 11, 25).minusDays(401);
        jdbc.update("INSERT INTO site_visitors_daily (stat_day, visitor_hash, first_seen_at, views) VALUES (?, ?, CURRENT_TIMESTAMP, 3)",
                Date.valueOf(oldDay), "a".repeat(64));
        jdbc.update("INSERT INTO site_page_views_daily (stat_day, page_key, views) VALUES (?, 'home', 3)",
                Date.valueOf(oldDay));

        int deleted = service.deleteVisitorsOlderThan(400);
        assertEquals(1, deleted);
        // Recent visitor stays; old pageview stays
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM site_page_views_daily WHERE stat_day = ?", Integer.class, Date.valueOf(oldDay)));
    }

    @Test
    void refusesPageviewInProdOrRenderProfileWhenSaltMissingOrDevDefault() {
        String visitorId = UUID.randomUUID().toString();

        var blankSaltService = new AnalyticsPageviewService(jdbc, "", clock, true);
        assertTrue(blankSaltService.isSaltMissing());
        assertEquals(AnalyticsPageviewService.Status.SALT_MISSING,
                blankSaltService.record(visitorId, "schedule", "Mozilla/5.0", false));

        var devDefaultSaltService = new AnalyticsPageviewService(jdbc, AnalyticsPageviewService.DEV_DEFAULT_SALT, clock, true);
        assertTrue(devDefaultSaltService.isSaltMissing());
        assertEquals(AnalyticsPageviewService.Status.SALT_MISSING,
                devDefaultSaltService.record(visitorId, "schedule", "Mozilla/5.0", false));

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM site_page_views_daily", Integer.class));

        var explicitSaltService = new AnalyticsPageviewService(jdbc, "prod-secret-random-salt-value", clock, true);
        assertFalse(explicitSaltService.isSaltMissing());
        assertEquals(AnalyticsPageviewService.Status.RECORDED,
                explicitSaltService.record(visitorId, "schedule", "Mozilla/5.0", false));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM site_visitors_daily", Integer.class));

        var renderEnv = new org.springframework.mock.env.MockEnvironment();
        renderEnv.setActiveProfiles("render");
        var renderService = new AnalyticsPageviewService(jdbc, "", clock, renderEnv);
        assertTrue(renderService.isSaltMissing());
        assertEquals(AnalyticsPageviewService.Status.SALT_MISSING,
                renderService.record(visitorId, "home", "Mozilla/5.0", false));
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        MutableClock(Instant start) {
            this.current = start;
        }

        void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
