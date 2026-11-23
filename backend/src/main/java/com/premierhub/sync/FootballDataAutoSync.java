package com.premierhub.sync;

import com.premierhub.service.FootballQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically triggers {@link FootballDataSync#sync()} to keep season 2026 fixtures,
 * standings, and teams up to date without touching player statistics, lineups, or Fantasy tables.
 */
@Component
public class FootballDataAutoSync {
    private static final Logger log = LoggerFactory.getLogger(FootballDataAutoSync.class);
    private static final int LEAGUE = FootballQueries.LEAGUE_ID;
    private static final int SEASON = FootballDataBatch.SEASON;
    static final int REQUESTS_PER_SYNC = 3;

    private static final Set<String> LIVE_STATUSES = Set.of(
            "LIVE", "IN_PLAY", "PAUSED", "EXTRA_TIME", "PENALTY_SHOOTOUT"
    );
    private static final Set<String> OVERDUE_UNFINISHED_STATUSES = Set.of(
            "LIVE", "IN_PLAY", "PAUSED", "EXTRA_TIME", "PENALTY_SHOOTOUT", "SCHEDULED", "TIMED"
    );

    private final FootballDataSync sync;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final boolean enabled;
    private final Duration matchdayInterval;
    private final Duration normalInterval;
    private final Duration windowBefore;
    private final Duration windowAfter;
    private final long backoffInitialMinutes;
    private final long backoffMaxMinutes;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant lastSuccessAt;
    private volatile Instant lastFailureAt;
    private volatile int consecutiveFailures;

    @Autowired
    public FootballDataAutoSync(
            FootballDataSync sync,
            JdbcTemplate jdbc,
            @Value("${premierhub.football-data.auto-sync.enabled:false}") boolean enabled,
            @Value("${premierhub.football-data.auto-sync.matchday-interval-minutes:10}") long matchdayIntervalMinutes,
            @Value("${premierhub.football-data.auto-sync.normal-interval-minutes:360}") long normalIntervalMinutes,
            @Value("${premierhub.football-data.auto-sync.window-before-minutes:15}") long windowBeforeMinutes,
            @Value("${premierhub.football-data.auto-sync.window-after-minutes:240}") long windowAfterMinutes,
            @Value("${premierhub.football-data.auto-sync.backoff-initial-minutes:10}") long backoffInitialMinutes,
            @Value("${premierhub.football-data.auto-sync.backoff-max-minutes:60}") long backoffMaxMinutes) {
        this(sync, jdbc, Clock.systemUTC(), enabled,
                Duration.ofMinutes(positive(matchdayIntervalMinutes, "matchdayIntervalMinutes")),
                Duration.ofMinutes(positive(normalIntervalMinutes, "normalIntervalMinutes")),
                Duration.ofMinutes(nonNegative(windowBeforeMinutes, "windowBeforeMinutes")),
                Duration.ofMinutes(nonNegative(windowAfterMinutes, "windowAfterMinutes")),
                positive(backoffInitialMinutes, "backoffInitialMinutes"),
                positive(backoffMaxMinutes, "backoffMaxMinutes"));
    }

    FootballDataAutoSync(
            FootballDataSync sync,
            JdbcTemplate jdbc,
            Clock clock,
            boolean enabled,
            Duration matchdayInterval,
            Duration normalInterval,
            Duration windowBefore,
            Duration windowAfter,
            long backoffInitialMinutes,
            long backoffMaxMinutes) {
        this.sync = sync;
        this.jdbc = jdbc;
        this.clock = clock;
        this.enabled = enabled;
        this.matchdayInterval = matchdayInterval;
        this.normalInterval = normalInterval;
        this.windowBefore = windowBefore;
        this.windowAfter = windowAfter;
        this.backoffInitialMinutes = backoffInitialMinutes;
        this.backoffMaxMinutes = Math.max(backoffInitialMinutes, backoffMaxMinutes);
    }

    @Scheduled(
            fixedDelayString = "${premierhub.football-data.auto-sync.check-interval-ms:60000}",
            initialDelayString = "${premierhub.football-data.auto-sync.initial-delay-ms:15000}"
    )
    public void scheduledTick() {
        runIfDue();
    }

    public TickOutcome runIfDue() {
        if (!enabled) {
            return TickOutcome.DISABLED;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("Football-data auto-sync skipped: previous run is still in progress");
            return TickOutcome.SKIPPED_RUNNING;
        }
        try {
            Instant now = clock.instant();
            if (consecutiveFailures > 0 && lastFailureAt != null) {
                Duration backoff = backoffDuration(consecutiveFailures);
                if (now.isBefore(lastFailureAt.plus(backoff))) {
                    return TickOutcome.NOT_DUE;
                }
            } else {
                boolean matchday = isMatchdayWindow(now);
                Duration requiredInterval = matchday ? matchdayInterval : normalInterval;
                if (lastSuccessAt != null && now.isBefore(lastSuccessAt.plus(requiredInterval))) {
                    return TickOutcome.NOT_DUE;
                }
            }

            Instant startedAt = clock.instant();
            try {
                FootballDataSync.Result result = sync.sync();
                Instant finishedAt = clock.instant();
                long durationMs = Math.max(0L, Duration.between(startedAt, finishedAt).toMillis());
                consecutiveFailures = 0;
                lastFailureAt = null;
                lastSuccessAt = finishedAt;
                boolean nextMatchday = isMatchdayWindow(finishedAt);
                Duration nextInterval = nextMatchday ? matchdayInterval : normalInterval;
                log.info("Football-data auto-sync completed: mode={} requests={} fixturesChanged={} "
                                + "fixturesInserted={} fixtures={} standings={} durationMs={} nextIntervalMinutes={}",
                        nextMatchday ? "MATCHDAY" : "NORMAL",
                        REQUESTS_PER_SYNC,
                        result.fixturesChanged(),
                        result.fixturesInserted(),
                        result.fixtures(),
                        result.standings(),
                        durationMs,
                        nextInterval.toMinutes());
                return TickOutcome.SYNCED;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                recordFailure(startedAt, ex);
                return TickOutcome.FAILED;
            } catch (Exception ex) {
                recordFailure(startedAt, ex);
                return TickOutcome.FAILED;
            }
        } finally {
            running.set(false);
        }
    }

    public boolean isMatchdayWindow(Instant now) {
        List<FixtureScheduleRow> rows = jdbc.query("""
                SELECT f.status, d.provider_status, d.kickoff_utc
                FROM fixtures f
                JOIN football_data_fixtures d ON d.fixture_id = f.id
                WHERE f.league_id = ? AND f.season_year = ?
                """, (rs, rowNum) -> new FixtureScheduleRow(
                rs.getString("status"),
                rs.getString("provider_status"),
                rs.getString("kickoff_utc")
        ), LEAGUE, SEASON);
        Instant windowStart = now.minus(windowAfter);
        Instant windowEnd = now.plus(windowBefore);
        for (FixtureScheduleRow row : rows) {
            if (isMatchdayFixture(row, now, windowStart, windowEnd)) {
                return true;
            }
        }
        return false;
    }

    Duration currentInterval(Instant now) {
        if (consecutiveFailures > 0) {
            return backoffDuration(consecutiveFailures);
        }
        return isMatchdayWindow(now) ? matchdayInterval : normalInterval;
    }

    Duration backoffDuration(int failures) {
        if (failures <= 0) {
            return Duration.ZERO;
        }
        int shift = Math.min(failures - 1, 10);
        long minutes = backoffInitialMinutes * (1L << shift);
        return Duration.ofMinutes(Math.min(backoffMaxMinutes, minutes));
    }

    int consecutiveFailures() {
        return consecutiveFailures;
    }

    private void recordFailure(Instant startedAt, Exception ex) {
        Instant failedAt = clock.instant();
        long durationMs = Math.max(0L, Duration.between(startedAt, failedAt).toMillis());
        int failures = ++consecutiveFailures;
        lastFailureAt = failedAt;
        Duration backoff = backoffDuration(failures);
        log.warn("Football-data auto-sync failed (attempt={}, backoffMinutes={}, durationMs={}): {}",
                failures, backoff.toMinutes(), durationMs, safeErrorMessage(ex));
    }

    private static boolean isMatchdayFixture(FixtureScheduleRow row, Instant now, Instant windowStart, Instant windowEnd) {
        String status = normalize(row.status());
        String providerStatus = normalize(row.providerStatus());
        if (!"FINISHED".equals(status) && (LIVE_STATUSES.contains(status) || LIVE_STATUSES.contains(providerStatus))) {
            return true;
        }
        Instant kickoff = parseKickoff(row.kickoffUtc());
        if (kickoff == null) {
            return false;
        }
        if (!kickoff.isBefore(windowStart) && !kickoff.isAfter(windowEnd)) {
            return true;
        }
        if (!"FINISHED".equals(status)
                && (OVERDUE_UNFINISHED_STATUSES.contains(status) || OVERDUE_UNFINISHED_STATUSES.contains(providerStatus))
                && !kickoff.isAfter(now)) {
            return true;
        }
        return false;
    }

    private static Instant parseKickoff(String kickoffUtc) {
        if (kickoffUtc == null || kickoffUtc.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(kickoffUtc.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message;
    }

    private static long positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static long nonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    public enum TickOutcome {
        DISABLED,
        SKIPPED_RUNNING,
        NOT_DUE,
        SYNCED,
        FAILED
    }

    private record FixtureScheduleRow(String status, String providerStatus, String kickoffUtc) { }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "premierhub.football-data.auto-sync.enabled", havingValue = "true")
    static class SchedulingConfiguration {
    }
}
