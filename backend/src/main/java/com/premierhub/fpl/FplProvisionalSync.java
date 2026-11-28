package com.premierhub.fpl;

import com.premierhub.service.FootballQueries;
import jakarta.annotation.PostConstruct;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically fetches provisional per-match FPL statistics after a match finishes
 * and writes ONLY to {@code fpl_provisional_player_stats} and {@code fpl_provisional_unmapped}.
 * Never writes to manual stats, lineups, season totals, or Fantasy tables.
 */
@Component
public class FplProvisionalSync {
    private static final Logger log = LoggerFactory.getLogger(FplProvisionalSync.class);
    private static final int LEAGUE = FootballQueries.LEAGUE_ID;
    private static final int SEASON = FplCollectorService.SEASON_YEAR;
    static final Duration LIVE_WINDOW_MAX_ELAPSED = Duration.ofMinutes(130);
    static final Duration LIVE_CADENCE = Duration.ofMinutes(2);
    static final Duration POST_MATCH_CADENCE = Duration.ofMinutes(10);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final FplCollectorService collector;
    private final FplJsonParser parser;
    private final FplSource defaultSource;
    private final FplMappingReader mappingReader;
    private final Clock clock;

    private final boolean enabled;
    private final Duration minElapsed;
    private final Duration maxElapsed;
    private final Duration bootstrapCacheTtl;
    private final long backoffInitialMinutes;
    private final long backoffMaxMinutes;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean disabledByMappingError;
    private volatile FplMappingReader.BundledMappings cachedMappings;
    private volatile Instant lastFailureAt;
    private volatile int consecutiveFailures;
    private volatile CachedBootstrap cachedBootstrap;
    private volatile Instant lastSyncedAt;

    @Autowired
    public FplProvisionalSync(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            FplCollectorService collector,
            FplJsonParser parser,
            FplClient client,
            @Value("${premierhub.fpl.provisional-sync.enabled:false}") boolean enabled,
            @Value("${premierhub.fpl.provisional-sync.min-elapsed-minutes:5}") long minElapsedMinutes,
            @Value("${premierhub.fpl.provisional-sync.max-elapsed-hours:72}") long maxElapsedHours,
            @Value("${premierhub.fpl.provisional-sync.bootstrap-cache-hours:6}") long bootstrapCacheHours,
            @Value("${premierhub.fpl.provisional-sync.backoff-initial-minutes:10}") long backoffInitialMinutes,
            @Value("${premierhub.fpl.provisional-sync.backoff-max-minutes:60}") long backoffMaxMinutes) {
        this(jdbc, transactions, collector, parser, client, new FplMappingReader(), Clock.systemUTC(), enabled,
                Duration.ofMinutes(positive(minElapsedMinutes, "minElapsedMinutes")),
                Duration.ofHours(positive(maxElapsedHours, "maxElapsedHours")),
                Duration.ofHours(positive(bootstrapCacheHours, "bootstrapCacheHours")),
                positive(backoffInitialMinutes, "backoffInitialMinutes"),
                positive(backoffMaxMinutes, "backoffMaxMinutes"));
    }

    FplProvisionalSync(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            FplCollectorService collector,
            FplJsonParser parser,
            FplSource source,
            Clock clock,
            boolean enabled,
            Duration minElapsed,
            Duration maxElapsed,
            Duration bootstrapCacheTtl,
            long backoffInitialMinutes,
            long backoffMaxMinutes) {
        this(jdbc, transactions, collector, parser, source, new FplMappingReader(), clock, enabled,
                minElapsed, maxElapsed, bootstrapCacheTtl, backoffInitialMinutes, backoffMaxMinutes);
    }

    FplProvisionalSync(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            FplCollectorService collector,
            FplJsonParser parser,
            FplSource source,
            FplMappingReader mappingReader,
            Clock clock,
            boolean enabled,
            Duration minElapsed,
            Duration maxElapsed,
            Duration bootstrapCacheTtl,
            long backoffInitialMinutes,
            long backoffMaxMinutes) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.collector = collector;
        this.parser = parser;
        this.defaultSource = source;
        this.mappingReader = mappingReader;
        this.clock = clock;
        this.enabled = enabled;
        this.minElapsed = minElapsed;
        this.maxElapsed = maxElapsed;
        this.bootstrapCacheTtl = bootstrapCacheTtl;
        this.backoffInitialMinutes = backoffInitialMinutes;
        this.backoffMaxMinutes = Math.max(backoffInitialMinutes, backoffMaxMinutes);
    }

    @PostConstruct
    void initializeMappings() {
        if (!enabled) {
            return;
        }
        ensureBundledMappingsLoaded();
    }

    boolean isDisabledByMappingError() {
        return disabledByMappingError;
    }

    private FplMappingReader.BundledMappings ensureBundledMappingsLoaded() {
        FplMappingReader.BundledMappings current = cachedMappings;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedMappings != null) {
                return cachedMappings;
            }
            if (disabledByMappingError) {
                return null;
            }
            try {
                FplMappingReader.BundledMappings loaded = mappingReader.readBundledMappings();
                if (loaded.teams().isEmpty() || loaded.players().isEmpty()) {
                    throw new IllegalStateException("Bundled FPL mapping CSV is empty: teams="
                            + loaded.teams().size() + ", players=" + loaded.players().size());
                }
                cachedMappings = loaded;
                log.info("FPL provisional sync loaded mappings: teams={} ({}), players={} ({})",
                        loaded.teams().size(), loaded.teamsSource(),
                        loaded.players().size(), loaded.playersSource());
                return loaded;
            } catch (Exception ex) {
                disabledByMappingError = true;
                String message = ex.getMessage() == null || ex.getMessage().isBlank()
                        ? ex.getClass().getSimpleName()
                        : ex.getClass().getSimpleName() + ": " + ex.getMessage();
                log.error("FPL provisional sync disabled: failed to load bundled team-map.csv/player-map.csv ({}). "
                        + "Job will not run.", message);
                return null;
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${premierhub.fpl.provisional-sync.check-interval-ms:60000}",
            initialDelayString = "${premierhub.fpl.provisional-sync.initial-delay-ms:30000}"
    )
    public void scheduledTick() {
        runScheduledTick();
    }

    public SyncSummary runScheduledTick() {
        return runInternal(defaultSource, null, null, true);
    }

    public SyncSummary runIfDue() {
        return runIfDue(defaultSource);
    }

    public SyncSummary runIfDue(FplSource source) {
        return runIfDue(source, null, null);
    }

    SyncSummary runIfDue(
            FplSource source,
            Map<Integer, FplMappingReader.TeamMapping> teamMapOverride,
            Map<Integer, FplMappingReader.PlayerMapping> playerMapOverride) {
        return runInternal(source, teamMapOverride, playerMapOverride, false);
    }

    SyncSummary runScheduledTick(
            FplSource source,
            Map<Integer, FplMappingReader.TeamMapping> teamMapOverride,
            Map<Integer, FplMappingReader.PlayerMapping> playerMapOverride) {
        return runInternal(source, teamMapOverride, playerMapOverride, true);
    }

    private SyncSummary runInternal(
            FplSource source,
            Map<Integer, FplMappingReader.TeamMapping> teamMapOverride,
            Map<Integer, FplMappingReader.PlayerMapping> playerMapOverride,
            boolean enforceCadence) {
        if (!enabled || disabledByMappingError) {
            return SyncSummary.of(TickOutcome.DISABLED);
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("FPL provisional sync skipped: previous run is still in progress");
            return SyncSummary.of(TickOutcome.SKIPPED_RUNNING);
        }
        try {
            Map<Integer, FplMappingReader.TeamMapping> teamMap = teamMapOverride;
            Map<Integer, FplMappingReader.PlayerMapping> playerMap = playerMapOverride;
            if (teamMap == null || playerMap == null) {
                FplMappingReader.BundledMappings bundled = ensureBundledMappingsLoaded();
                if (bundled == null) {
                    return SyncSummary.of(TickOutcome.DISABLED);
                }
                if (teamMap == null) {
                    teamMap = bundled.teams();
                }
                if (playerMap == null) {
                    playerMap = bundled.players();
                }
            }

            Instant now = clock.instant();
            if (consecutiveFailures > 0 && lastFailureAt != null) {
                Duration backoff = backoffDuration(consecutiveFailures);
                if (now.isBefore(lastFailureAt.plus(backoff))) {
                    return SyncSummary.of(TickOutcome.BACKOFF);
                }
            }

            CandidateBatch candidateBatch = findCandidateBatch(now);
            Map<Integer, Set<Integer>> candidatesByGw = candidateBatch.byGameweek();
            if (candidatesByGw.isEmpty()) {
                return SyncSummary.of(TickOutcome.NO_CANDIDATES);
            }

            if (enforceCadence && lastSyncedAt != null) {
                Duration requiredCadence = candidateBatch.hasLiveWindowMatch() ? LIVE_CADENCE : POST_MATCH_CADENCE;
                if (now.isBefore(lastSyncedAt.plus(requiredCadence))) {
                    return SyncSummary.of(TickOutcome.NOT_DUE);
                }
            }

            Instant startedAt = clock.instant();
            try {
                FplModels.Bootstrap bootstrap = getOrFetchBootstrap(source, startedAt);

                int fixturesWritten = 0;
                int rowsWritten = 0;
                int unmappedWritten = 0;
                int skippedNotFinished = 0;
                int skippedScoreMismatch = 0;
                int elementSummaryCalls = 0;

                for (var entry : candidatesByGw.entrySet()) {
                    int gw = entry.getKey();
                    Set<Integer> fixtureIds = entry.getValue();
                    String fixturesJson = source.fetchFixtures(gw);
                    String liveJson = source.fetchEventLive(gw);
                    List<FplModels.FplFixture> fplFixtures = parser.parseFixtures(fixturesJson, gw);
                    List<FplModels.LiveElement> liveElements = parser.parseEventLive(liveJson);

                    FplCollectorService.ProvisionalExtraction extracted = collector.extractForProvisional(
                            gw, bootstrap, fplFixtures, liveElements, teamMap, playerMap, fixtureIds, source);
                    skippedNotFinished += extracted.skippedNotFinished();
                    skippedScoreMismatch += extracted.skippedScoreMismatch();
                    elementSummaryCalls += extracted.elementSummaryCalls();

                    Instant fetchedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
                    for (FplCollectorService.ProvisionalFixture pf : extracted.fixtures()) {
                        FixtureWriteResult write = writeFixtureProvisional(pf, fetchedAt);
                        if (write.written()) {
                            fixturesWritten++;
                            rowsWritten += write.playerRows();
                            unmappedWritten += write.unmappedRows();
                        }
                    }
                }

                Instant finishedAt = clock.instant();
                long durationMs = Math.max(0L, Duration.between(startedAt, finishedAt).toMillis());
                consecutiveFailures = 0;
                lastFailureAt = null;
                lastSyncedAt = finishedAt;

                if (fixturesWritten > 0 || skippedScoreMismatch > 0) {
                    log.info("FPL provisional sync completed: gws={} liveCadence={} fixturesWritten={} playerRows={} "
                                    + "unmapped={} skippedNotFinished={} skippedScoreMismatch={} "
                                    + "elementSummaryCalls={} durationMs={}",
                            candidatesByGw.keySet(), candidateBatch.hasLiveWindowMatch(),
                            fixturesWritten, rowsWritten, unmappedWritten,
                            skippedNotFinished, skippedScoreMismatch, elementSummaryCalls, durationMs);
                }
                return new SyncSummary(TickOutcome.SYNCED, fixturesWritten, rowsWritten, unmappedWritten,
                        skippedNotFinished, skippedScoreMismatch, elementSummaryCalls);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                recordFailure(startedAt, ex);
                return SyncSummary.of(TickOutcome.FAILED);
            } catch (Exception ex) {
                recordFailure(startedAt, ex);
                return SyncSummary.of(TickOutcome.FAILED);
            }
        } finally {
            running.set(false);
        }
    }

    Map<Integer, Set<Integer>> findCandidatesByGameweek(Instant now) {
        return findCandidateBatch(now).byGameweek();
    }

    private CandidateBatch findCandidateBatch(Instant now) {
        List<CandidateRow> rows = jdbc.query("""
                SELECT f.id, f.gameweek, d.kickoff_utc
                FROM fixtures f
                JOIN football_data_fixtures d ON d.fixture_id = f.id
                WHERE f.league_id = ? AND f.season_year = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM manual_fixture_player_stats m
                      WHERE m.fixture_id = f.id AND m.league_id = f.league_id AND m.season_year = f.season_year
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM fpl_provisional_player_stats p
                      WHERE p.fixture_id = f.id AND p.fpl_status = 'FINAL_FPL'
                  )
                ORDER BY f.gameweek, f.id
                """, (rs, rowNum) -> new CandidateRow(
                rs.getInt("id"),
                rs.getInt("gameweek"),
                rs.getString("kickoff_utc")
        ), LEAGUE, SEASON);

        Map<Integer, Set<Integer>> byGw = new LinkedHashMap<>();
        boolean hasLiveWindow = false;
        for (CandidateRow row : rows) {
            Instant kickoff = parseKickoff(row.kickoffUtc());
            if (kickoff == null) {
                continue;
            }
            Duration elapsed = Duration.between(kickoff, now);
            if (elapsed.compareTo(minElapsed) >= 0 && elapsed.compareTo(maxElapsed) <= 0) {
                byGw.computeIfAbsent(row.gameweek(), ignored -> new LinkedHashSet<>()).add(row.fixtureId());
                if (elapsed.compareTo(LIVE_WINDOW_MAX_ELAPSED) <= 0) {
                    hasLiveWindow = true;
                }
            }
        }
        return new CandidateBatch(byGw, hasLiveWindow);
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

    private FplModels.Bootstrap getOrFetchBootstrap(FplSource source, Instant now)
            throws IOException, InterruptedException {
        CachedBootstrap cached = this.cachedBootstrap;
        if (cached != null && now.isBefore(cached.fetchedAt().plus(bootstrapCacheTtl))) {
            return cached.bootstrap();
        }
        String json = source.fetchBootstrapStatic();
        FplModels.Bootstrap parsed = parser.parseBootstrap(json);
        this.cachedBootstrap = new CachedBootstrap(parsed, now);
        return parsed;
    }

    private FixtureWriteResult writeFixtureProvisional(
            FplCollectorService.ProvisionalFixture pf,
            Instant fetchedAt
    ) {
        return transactions.execute(status -> {
            Integer confirmedCount = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM manual_fixture_player_stats
                    WHERE fixture_id = ? AND league_id = ? AND season_year = ?
                    """, Integer.class, pf.fixtureId(), LEAGUE, SEASON);
            if (confirmedCount != null && confirmedCount > 0) {
                return new FixtureWriteResult(false, 0, 0);
            }

            jdbc.update("DELETE FROM fpl_provisional_player_stats WHERE fixture_id = ?", pf.fixtureId());
            jdbc.update("DELETE FROM fpl_provisional_unmapped WHERE fixture_id = ?", pf.fixtureId());

            Timestamp ts = Timestamp.from(fetchedAt);
            int insertedPlayers = 0;
            for (FplCollectorService.ProvisionalPlayerRow row : pf.players()) {
                Integer clubId = resolvePlayerClubForFixture(
                        row.playerId(), row.fplTeamClubId(), pf.homeClubId(), pf.awayClubId(), pf.matchDate());
                if (clubId == null) {
                    log.warn("FPL provisional sync skipped player_id={} in fixture_id={}: club could not be resolved to fixture clubs ({}, {})",
                            row.playerId(), pf.fixtureId(), pf.homeClubId(), pf.awayClubId());
                    continue;
                }
                Integer playerExists = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM players WHERE id = ?", Integer.class, row.playerId());
                if (playerExists == null || playerExists == 0) {
                    log.warn("FPL provisional sync skipped player_id={} in fixture_id={}: missing in players table",
                            row.playerId(), pf.fixtureId());
                    continue;
                }
                FplModels.PlayerStats s = row.stats();
                jdbc.update("""
                        INSERT INTO fpl_provisional_player_stats
                        (fixture_id, player_id, league_id, season_year, club_id,
                         minutes, goals, assists, yellow_cards, red_cards, starts,
                         clean_sheets, goals_conceded, own_goals, penalties_saved, penalties_missed,
                         saves, bonus, bps, clearances_blocks_interceptions, recoveries, tackles,
                         defensive_contribution, expected_goals, expected_assists,
                         expected_goal_involvements, expected_goals_conceded,
                         influence, creativity, threat, ict_index, fpl_status, fetched_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        pf.fixtureId(), row.playerId(), LEAGUE, SEASON, clubId,
                        s.minutes(), s.goalsScored(), s.assists(), s.yellowCards(), s.redCards(), s.starts(),
                        s.cleanSheets(), s.goalsConceded(), s.ownGoals(), s.penaltiesSaved(), s.penaltiesMissed(),
                        s.saves(), s.bonus(), s.bps(), s.clearancesBlocksInterceptions(), s.recoveries(), s.tackles(),
                        s.defensiveContribution(), s.expectedGoals(), s.expectedAssists(),
                        s.expectedGoalInvolvements(), s.expectedGoalsConceded(),
                        s.influence(), s.creativity(), s.threat(), s.ictIndex(), pf.fplStatus(), ts);
                insertedPlayers++;
            }

            int insertedUnmapped = 0;
            for (FplCollectorService.ProvisionalUnmappedRow u : pf.unmapped()) {
                log.warn("FPL provisional sync unmapped player in fixture_id={}: fpl_element_id={} name='{}' club='{}' minutes={}",
                        pf.fixtureId(), u.fplElementId(), u.fplName(),
                        u.clubName() != null ? u.clubName() : "UNKNOWN", u.minutes());
                jdbc.update("""
                        INSERT INTO fpl_provisional_unmapped
                        (fixture_id, fpl_element_id, fpl_name, club_id, club_name, minutes, fetched_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        pf.fixtureId(), u.fplElementId(), u.fplName(), u.clubId(), u.clubName(), u.minutes(), ts);
                insertedUnmapped++;
            }
            return new FixtureWriteResult(true, insertedPlayers, insertedUnmapped);
        });
    }

    private Integer resolvePlayerClubForFixture(
            int playerId,
            Integer fplTeamClubId,
            int homeClubId,
            int awayClubId,
            LocalDate matchDate
    ) {
        List<Integer> membershipClubs = jdbc.queryForList("""
                SELECT m.club_id FROM manual_player_memberships m
                WHERE m.league_id = ? AND m.season_year = ? AND m.player_id = ?
                  AND m.club_id IN (?, ?) AND m.start_date <= ?
                  AND (m.end_date IS NULL OR m.end_date > ?)
                """, Integer.class, LEAGUE, SEASON, playerId, homeClubId, awayClubId,
                Date.valueOf(matchDate), Date.valueOf(matchDate));
        if (membershipClubs.size() == 1) {
            return membershipClubs.getFirst();
        }
        if (fplTeamClubId != null && (fplTeamClubId == homeClubId || fplTeamClubId == awayClubId)) {
            return fplTeamClubId;
        }
        return null;
    }

    private void recordFailure(Instant startedAt, Exception ex) {
        Instant failedAt = clock.instant();
        long durationMs = Math.max(0L, Duration.between(startedAt, failedAt).toMillis());
        int failures = ++consecutiveFailures;
        lastFailureAt = failedAt;
        Duration backoff = backoffDuration(failures);
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        log.warn("FPL provisional sync failed (attempt={}, backoffMinutes={}, durationMs={}): {}",
                failures, backoff.toMinutes(), durationMs, message);
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

    private static long positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private record CandidateRow(int fixtureId, int gameweek, String kickoffUtc) {
    }

    private record CandidateBatch(Map<Integer, Set<Integer>> byGameweek, boolean hasLiveWindowMatch) {
    }

    private record CachedBootstrap(FplModels.Bootstrap bootstrap, Instant fetchedAt) {
    }

    private record FixtureWriteResult(boolean written, int playerRows, int unmappedRows) {
    }

    public enum TickOutcome {
        DISABLED,
        SKIPPED_RUNNING,
        BACKOFF,
        NO_CANDIDATES,
        NOT_DUE,
        SYNCED,
        FAILED
    }

    public record SyncSummary(
            TickOutcome outcome,
            int fixturesWritten,
            int playerRowsWritten,
            int unmappedWritten,
            int skippedNotFinished,
            int skippedScoreMismatch,
            int elementSummaryCalls
    ) {
        static SyncSummary of(TickOutcome outcome) {
            return new SyncSummary(outcome, 0, 0, 0, 0, 0, 0);
        }
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "premierhub.fpl.provisional-sync.enabled", havingValue = "true")
    static class SchedulingConfiguration {
    }
}
