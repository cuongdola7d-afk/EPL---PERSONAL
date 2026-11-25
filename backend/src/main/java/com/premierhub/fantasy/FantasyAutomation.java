package com.premierhub.fantasy;

import com.premierhub.accounts.Account;
import com.premierhub.accounts.AccountRepository;
import com.premierhub.accounts.SystemAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically automates official Fantasy GW6-GW38 transitions by invoking the exact existing
 * admin operations ({@link GameweekService#publishDeadline} and {@link FantasyResultService#publish}).
 * Never recalculates published results and never creates accounts automatically.
 */
@Component
public class FantasyAutomation {
    private static final Logger log = LoggerFactory.getLogger(FantasyAutomation.class);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MIN_OFFICIAL_GW = 6;
    private static final int MAX_OFFICIAL_GW = 38;

    private final GameweekService gameweekService;
    private final GameweekRepository gameweekRepository;
    private final FantasyResultService resultService;
    private final AccountRepository accountRepository;
    private final Clock clock;
    private final boolean enabled;
    private final boolean dryRun;
    private final long backoffInitialMinutes;
    private final long backoffMaxMinutes;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Integer> warnedExpiredDeadlineGws = ConcurrentHashMap.newKeySet();
    private volatile Instant lastFailureAt;
    private volatile int consecutiveFailures;

    @Autowired
    public FantasyAutomation(
            GameweekService gameweekService,
            GameweekRepository gameweekRepository,
            FantasyResultService resultService,
            AccountRepository accountRepository,
            @Qualifier("fantasyGameweekClock") Clock clock,
            @Value("${premierhub.fantasy.automation.enabled:false}") boolean enabled,
            @Value("${premierhub.fantasy.automation.dry-run:true}") boolean dryRun,
            @Value("${premierhub.fantasy.automation.backoff-initial-minutes:10}") long backoffInitialMinutes,
            @Value("${premierhub.fantasy.automation.backoff-max-minutes:60}") long backoffMaxMinutes) {
        this.gameweekService = gameweekService;
        this.gameweekRepository = gameweekRepository;
        this.resultService = resultService;
        this.accountRepository = accountRepository;
        this.clock = clock;
        this.enabled = enabled;
        this.dryRun = dryRun;
        this.backoffInitialMinutes = Math.max(1L, backoffInitialMinutes);
        this.backoffMaxMinutes = Math.max(this.backoffInitialMinutes, backoffMaxMinutes);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Scheduled(
            fixedDelayString = "${premierhub.fantasy.automation.interval-ms:600000}",
            initialDelayString = "${premierhub.fantasy.automation.initial-delay-ms:45000}"
    )
    public void scheduledTick() {
        runIfDue();
    }

    public RunReport runIfDue() {
        if (!enabled) {
            return RunReport.skipped(Outcome.DISABLED);
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("Fantasy automation skipped: previous run is still in progress");
            return RunReport.skipped(Outcome.SKIPPED_RUNNING);
        }
        try {
            Instant now = clock.instant();
            if (consecutiveFailures > 0 && lastFailureAt != null) {
                Duration backoff = backoffDuration(consecutiveFailures);
                if (now.isBefore(lastFailureAt.plus(backoff))) {
                    return RunReport.skipped(Outcome.BACKOFF);
                }
            }

            Optional<Account> systemAccount = accountRepository.findByEmail(SystemAccount.EMAIL);
            if (systemAccount.isEmpty() || !"ADMIN".equals(systemAccount.get().role())) {
                log.error("Fantasy automation aborted: system account '{}' (ADMIN) not found in accounts table; "
                        + "apply migration before enabling automation", SystemAccount.EMAIL);
                return RunReport.skipped(Outcome.MISSING_SYSTEM_ACCOUNT);
            }
            long systemId = systemAccount.get().id();

            List<Integer> opened = new ArrayList<>();
            List<Integer> published = new ArrayList<>();
            try {
                tryAutoOpenNextGameweek(systemId, opened);
                tryAutoPublishReadyGameweeks(systemId, published);
                consecutiveFailures = 0;
                lastFailureAt = null;
                return new RunReport(dryRun ? Outcome.DRY_RUN : Outcome.COMPLETED,
                        List.copyOf(opened), List.copyOf(published));
            } catch (Exception ex) {
                recordFailure(now, ex);
                return RunReport.skipped(Outcome.FAILED);
            }
        } finally {
            running.set(false);
        }
    }

    private void tryAutoOpenNextGameweek(long systemId, List<Integer> opened) {
        GameweekService.Overview overview = gameweekService.overview();
        Instant now = overview.serverTimeUtc();
        List<GameweekService.View> official = overview.gameweeks().stream()
                .filter(v -> v.gameweek() >= MIN_OFFICIAL_GW && v.gameweek() <= MAX_OFFICIAL_GW)
                .toList();

        boolean anyOpen = official.stream()
                .anyMatch(v -> v.configured() && v.status() == GameweekService.Status.OPEN);
        if (anyOpen) {
            log.debug("Fantasy automation: skipping auto-open because an official gameweek is already OPEN");
            return;
        }

        // Find the latest configured official GW whose effective status is not OPEN (LOCKED / AWAITING_RESULTS / PUBLISHED).
        GameweekService.View latestClosed = null;
        for (GameweekService.View view : official) {
            if (view.configured() && view.status() != null && view.status() != GameweekService.Status.OPEN) {
                latestClosed = view;
            }
        }
        if (latestClosed == null) {
            log.debug("Fantasy automation: no closed official gameweek found to trigger next GW open");
            return;
        }
        int nextGw = latestClosed.gameweek() + 1;
        if (nextGw > MAX_OFFICIAL_GW) {
            return;
        }
        GameweekService.View next = overview.gameweeks().get(nextGw - 1);
        if (next.configured()) {
            return;
        }

        if (!next.scheduleComplete() || next.candidateDeadlineUtc() == null) {
            log.warn("Fantasy automation: chưa thể mở GW{} — chưa đủ 10 trận có giờ đá UTC", nextGw);
            return;
        }
        if (!now.isBefore(next.candidateDeadlineUtc())) {
            if (warnedExpiredDeadlineGws.add(nextGw)) {
                log.warn("Fantasy automation: chưa thể mở GW{} — deadline dự kiến ({}) đã qua",
                        nextGw, next.candidateDeadlineUtc());
            }
            return;
        }
        LocalDate rosterAsOf = now.atZone(VIETNAM).toLocalDate();
        if (!gameweekRepository.hasRoster(rosterAsOf)) {
            log.warn("Fantasy automation: chưa thể mở GW{} — chưa có dữ liệu đội hình (roster) tại ngày {}",
                    nextGw, rosterAsOf);
            return;
        }

        String reason = "Tự động mở khi GW" + latestClosed.gameweek() + " khóa";
        if (dryRun) {
            log.info("Fantasy automation [dry-run]: Sẽ mở GW{} (deadlineUtc={}, rosterAsOf={}, lý do='{}')",
                    nextGw, next.candidateDeadlineUtc(), rosterAsOf, reason);
            opened.add(nextGw);
            return;
        }

        try {
            GameweekService.View created = gameweekService.publishDeadline(nextGw, systemId, reason);
            log.info("Fantasy automation: Đã mở GW{} (deadlineUtc={}, rosterAsOf={})",
                    created.gameweek(), created.deadlineUtc(), created.rosterAsOf());
            opened.add(nextGw);
        } catch (ResponseStatusException conflict) {
            log.warn("Fantasy automation: bỏ qua mở GW{} ({})", nextGw, conflict.getReason());
        }
    }

    private void tryAutoPublishReadyGameweeks(long systemId, List<Integer> published) {
        GameweekService.Overview overview = gameweekService.overview();
        for (GameweekService.View view : overview.gameweeks()) {
            int gw = view.gameweek();
            if (gw < MIN_OFFICIAL_GW || gw > MAX_OFFICIAL_GW) {
                continue;
            }
            if (!view.configured() || view.status() != GameweekService.Status.AWAITING_RESULTS) {
                continue;
            }
            FantasyResultService.Readiness readiness = resultService.readiness(gw);
            if (!readiness.ready()) {
                log.debug("Fantasy automation: GW{} chưa sẵn sàng công bố ({} blocker)",
                        gw, readiness.issues().size());
                continue;
            }
            // Never trigger recalculate automatically.
            if (readiness.currentVersion() != 0) {
                log.debug("Fantasy automation: GW{} đã có phiên bản kết quả v{}, không tự tái tính",
                        gw, readiness.currentVersion());
                continue;
            }

            String reason = "Tự động công bố khi đủ 10/10 trận";
            if (dryRun) {
                log.info("Fantasy automation [dry-run]: Sẽ công bố GW{} (fixtures={}, participants={}, lý do='{}')",
                        gw, readiness.fixtures(), readiness.participants(), reason);
                published.add(gw);
                continue;
            }

            try {
                FantasyResultService.Published result = resultService.publish(
                        gw, readiness.currentVersion(), systemId, reason, false);
                log.info("Fantasy automation: Đã công bố GW{} v{} (participants={}, unchanged={})",
                        result.gameweek(), result.version(), result.participants(), result.unchanged());
                published.add(gw);
            } catch (FantasyResultService.NotReady notReady) {
                log.debug("Fantasy automation: GW{} không còn sẵn sàng lúc công bố ({} blocker)",
                        gw, notReady.readiness().issues().size());
            } catch (ResponseStatusException conflict) {
                log.warn("Fantasy automation: bỏ qua công bố GW{} ({})", gw, conflict.getReason());
            }
        }
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

    private void recordFailure(Instant failedAt, Exception ex) {
        int failures = ++consecutiveFailures;
        lastFailureAt = failedAt;
        Duration backoff = backoffDuration(failures);
        log.warn("Fantasy automation failed (attempt={}, backoffMinutes={}): {}",
                failures, backoff.toMinutes(), ex.getMessage());
    }

    public enum Outcome {
        DISABLED,
        SKIPPED_RUNNING,
        BACKOFF,
        MISSING_SYSTEM_ACCOUNT,
        DRY_RUN,
        COMPLETED,
        FAILED
    }

    public record RunReport(Outcome outcome, List<Integer> openedGameweeks, List<Integer> publishedGameweeks) {
        static RunReport skipped(Outcome outcome) {
            return new RunReport(outcome, List.of(), List.of());
        }
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "premierhub.fantasy.automation.enabled", havingValue = "true")
    static class SchedulingConfiguration {
    }
}
