package com.premierhub.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Privacy-preserving pageview ingestion: stores only SHA-256(visitorId + salt) and fixed page keys.
 * Never persists raw visitorId, IP addresses, full User-Agent strings, emails, or parameterized URLs.
 */
@Service
public class AnalyticsPageviewService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsPageviewService.class);
    static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_VIEWS_PER_VISITOR_PER_HOUR = 120;
    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final int MAX_RATE_ENTRIES = 20_000;

    public static final String DEV_DEFAULT_SALT = "dev-local-prismaxi-analytics-salt";

    public static final Set<String> ALLOWED_PAGES = Set.of(
            "home", "clubs", "club", "players", "player",
            "schedule", "match-detail", "standings", "fantasy", "minigame", "admin"
    );

    private static final Set<String> BOT_TOKENS = Set.of("bot", "crawler", "spider", "uptimerobot");

    private final JdbcTemplate jdbc;
    private final String salt;
    private final boolean saltMissing;
    private final Clock clock;
    private final ConcurrentHashMap<String, RateBucket> rateBuckets = new ConcurrentHashMap<>();

    @Autowired
    public AnalyticsPageviewService(
            JdbcTemplate jdbc,
            @Value("${premierhub.analytics.salt:dev-local-prismaxi-analytics-salt}") String salt,
            @Qualifier("fantasyGameweekClock") Clock clock,
            Environment environment) {
        this(jdbc, salt, clock, environment != null && environment.acceptsProfiles(Profiles.of("prod", "render")));
    }

    AnalyticsPageviewService(JdbcTemplate jdbc, String salt, Clock clock) {
        this(jdbc, salt, clock, false);
    }

    AnalyticsPageviewService(JdbcTemplate jdbc, String salt, Clock clock, boolean requireExplicitSalt) {
        this.jdbc = jdbc;
        this.salt = salt == null ? "" : salt.strip();
        this.clock = clock;
        this.saltMissing = requireExplicitSalt
                && (this.salt.isBlank() || DEV_DEFAULT_SALT.equals(this.salt));
        if (this.saltMissing) {
            log.error("PREMIERHUB_ANALYTICS_SALT is missing or using dev default in prod/render profile; "
                    + "pageview recording is disabled until an explicit salt is configured.");
        }
    }

    public boolean isSaltMissing() {
        return saltMissing;
    }

    public enum Status {
        RECORDED,
        INVALID_INPUT,
        IGNORED_BOT,
        IGNORED_ADMIN,
        RATE_LIMITED,
        SALT_MISSING
    }

    public Status record(String visitorId, String page, String userAgent, boolean isAdmin) {
        if (saltMissing) {
            log.error("Refusing to record pageview: PREMIERHUB_ANALYTICS_SALT is not configured for prod/render profile.");
            return Status.SALT_MISSING;
        }
        if (isAdmin) {
            return Status.IGNORED_ADMIN;
        }
        if (!isValidUuid(visitorId) || !isAllowedPage(page)) {
            return Status.INVALID_INPUT;
        }
        if (isBot(userAgent)) {
            return Status.IGNORED_BOT;
        }

        String normalizedPage = page.strip();
        String visitorHash = hashVisitor(visitorId.strip());
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

        if (!allowRequest(visitorHash, now)) {
            return Status.RATE_LIMITED;
        }

        LocalDate vnDay = now.atZone(VIETNAM).toLocalDate();
        Date sqlDay = Date.valueOf(vnDay);
        Timestamp utcNow = Timestamp.valueOf(LocalDateTime.ofInstant(now, ZoneOffset.UTC));

        upsertVisitorDaily(sqlDay, visitorHash, utcNow);
        upsertPageViewDaily(sqlDay, normalizedPage);
        return Status.RECORDED;
    }

    public int deleteVisitorsOlderThan(int retentionDays) {
        int days = Math.max(1, retentionDays);
        LocalDate cutoff = clock.instant().atZone(VIETNAM).toLocalDate().minusDays(days);
        int deleted = jdbc.update("DELETE FROM site_visitors_daily WHERE stat_day < ?", Date.valueOf(cutoff));
        if (deleted > 0) {
            log.info("Analytics retention: deleted {} site_visitors_daily rows older than {}", deleted, cutoff);
        }
        return deleted;
    }

    String hashVisitor(String visitorId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((visitorId + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static boolean isAllowedPage(String page) {
        return page != null && ALLOWED_PAGES.contains(page.strip());
    }

    static boolean isValidUuid(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.strip();
        if (trimmed.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(trimmed);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        for (String token : BOT_TOKENS) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean allowRequest(String visitorHash, Instant now) {
        if (rateBuckets.size() >= MAX_RATE_ENTRIES) {
            Instant cutoff = now.minus(RATE_WINDOW);
            rateBuckets.entrySet().removeIf(e -> e.getValue().windowStart.isBefore(cutoff));
            if (rateBuckets.size() >= MAX_RATE_ENTRIES) {
                rateBuckets.clear();
            }
        }
        final boolean[] allowed = {false};
        rateBuckets.compute(visitorHash, (key, bucket) -> {
            if (bucket == null || !now.isBefore(bucket.windowStart.plus(RATE_WINDOW))) {
                allowed[0] = true;
                return new RateBucket(now, 1);
            }
            if (bucket.count >= MAX_VIEWS_PER_VISITOR_PER_HOUR) {
                allowed[0] = false;
                return bucket;
            }
            allowed[0] = true;
            return new RateBucket(bucket.windowStart, bucket.count + 1);
        });
        return allowed[0];
    }

    private void upsertVisitorDaily(Date sqlDay, String visitorHash, Timestamp utcNow) {
        int updated = jdbc.update(
                "UPDATE site_visitors_daily SET views = views + 1 WHERE stat_day = ? AND visitor_hash = ?",
                sqlDay, visitorHash);
        if (updated > 0) {
            return;
        }
        try {
            jdbc.update(
                    "INSERT INTO site_visitors_daily (stat_day, visitor_hash, first_seen_at, views) VALUES (?, ?, ?, 1)",
                    sqlDay, visitorHash, utcNow);
        } catch (DuplicateKeyException race) {
            jdbc.update(
                    "UPDATE site_visitors_daily SET views = views + 1 WHERE stat_day = ? AND visitor_hash = ?",
                    sqlDay, visitorHash);
        }
    }

    private void upsertPageViewDaily(Date sqlDay, String pageKey) {
        int updated = jdbc.update(
                "UPDATE site_page_views_daily SET views = views + 1 WHERE stat_day = ? AND page_key = ?",
                sqlDay, pageKey);
        if (updated > 0) {
            return;
        }
        try {
            jdbc.update(
                    "INSERT INTO site_page_views_daily (stat_day, page_key, views) VALUES (?, ?, 1)",
                    sqlDay, pageKey);
        } catch (DuplicateKeyException race) {
            jdbc.update(
                    "UPDATE site_page_views_daily SET views = views + 1 WHERE stat_day = ? AND page_key = ?",
                    sqlDay, pageKey);
        }
    }

    private record RateBucket(Instant windowStart, int count) {
    }
}
