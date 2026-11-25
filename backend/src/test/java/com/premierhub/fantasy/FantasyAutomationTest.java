package com.premierhub.fantasy;

import com.premierhub.accounts.Account;
import com.premierhub.accounts.AccountRepository;
import com.premierhub.accounts.SystemAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FantasyAutomationTest {
    private static final Instant NOW = Instant.parse("2026-11-24T17:00:00Z");
    private static final Account SYSTEM = new Account(
            99L, SystemAccount.EMAIL, SystemAccount.DISPLAY_NAME, null, "ADMIN",
            Instant.parse("2026-11-01T00:00:00Z"));

    private GameweekService gameweekService;
    private GameweekRepository gameweekRepository;
    private FantasyResultService resultService;
    private AccountRepository accountRepository;
    private Clock clock;

    @BeforeEach
    void setUp() {
        gameweekService = mock(GameweekService.class);
        gameweekRepository = mock(GameweekRepository.class);
        resultService = mock(FantasyResultService.class);
        accountRepository = mock(AccountRepository.class);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        when(accountRepository.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.of(SYSTEM));
        when(gameweekRepository.hasRoster(any(LocalDate.class))).thenReturn(true);
    }

    private static GameweekService.Overview overviewWith(GameweekService.View... overrides) {
        List<GameweekService.View> all = new ArrayList<>();
        for (int gw = 1; gw <= 38; gw++) {
            all.add(new GameweekService.View(2026, gw, gw <= 5 ? "REPLAY" : "OFFICIAL",
                    false, null, null, null, null, false, null, false, 0, List.of(), null));
        }
        for (GameweekService.View v : overrides) {
            all.set(v.gameweek() - 1, v);
        }
        return new GameweekService.Overview(NOW, 11, all);
    }

    private static GameweekService.View configuredGw(int gw, GameweekService.Status status) {
        return new GameweekService.View(2026, gw, "OFFICIAL", true,
                NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(7)),
                NOW.minus(Duration.ofDays(3)), null, true, status,
                status == GameweekService.Status.OPEN, 1, List.of(), LocalDate.of(2026, 11, 10));
    }

    private static GameweekService.View unconfiguredGw(int gw, boolean scheduleComplete, Instant candidateDeadline) {
        return new GameweekService.View(2026, gw, "OFFICIAL", false,
                null, null, candidateDeadline, null, scheduleComplete, null, false, 0, List.of(), null);
    }

    @Test
    void opensNextGameweekWhenPreviousLockedAndConditionsMet() {
        Instant gw12Deadline = NOW.plus(Duration.ofDays(3));
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(11, GameweekService.Status.LOCKED),
                unconfiguredGw(12, true, gw12Deadline)
        ));
        when(gameweekService.publishDeadline(eq(12), eq(99L), anyString()))
                .thenReturn(configuredGw(12, GameweekService.Status.OPEN));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(FantasyAutomation.Outcome.COMPLETED, report.outcome());
        assertEquals(List.of(12), report.openedGameweeks());
        verify(gameweekService).publishDeadline(12, 99L, "Tự động mở khi GW11 khóa");
        verify(resultService, never()).publish(anyInt(), anyInt(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    void doesNotOpenWhenAnotherOfficialGameweekIsStillOpen() {
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(10, GameweekService.Status.PUBLISHED),
                configuredGw(11, GameweekService.Status.OPEN),
                unconfiguredGw(12, true, NOW.plus(Duration.ofDays(5)))
        ));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(), report.openedGameweeks());
        verify(gameweekService, never()).publishDeadline(anyInt(), anyLong(), anyString());
    }

    @Test
    void doesNotOpenWhenScheduleIncomplete() {
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(11, GameweekService.Status.PUBLISHED),
                unconfiguredGw(12, false, null)
        ));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(), report.openedGameweeks());
        verify(gameweekService, never()).publishDeadline(anyInt(), anyLong(), anyString());
    }

    @Test
    void doesNotOpenWhenCandidateDeadlinePassed() {
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(11, GameweekService.Status.PUBLISHED),
                unconfiguredGw(12, true, NOW.minus(Duration.ofHours(1)))
        ));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var first = automation.runIfDue();
        var second = automation.runIfDue();

        assertEquals(List.of(), first.openedGameweeks());
        assertEquals(List.of(), second.openedGameweeks());
        verify(gameweekService, never()).publishDeadline(anyInt(), anyLong(), anyString());
    }

    @Test
    void doesNotOpenWhenRosterUnavailable() {
        when(gameweekRepository.hasRoster(any(LocalDate.class))).thenReturn(false);
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(11, GameweekService.Status.PUBLISHED),
                unconfiguredGw(12, true, NOW.plus(Duration.ofDays(3)))
        ));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(), report.openedGameweeks());
        verify(gameweekService, never()).publishDeadline(anyInt(), anyLong(), anyString());
    }

    @Test
    void dryRunWritesNothingForBothOpenAndPublish() {
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(11, GameweekService.Status.AWAITING_RESULTS),
                unconfiguredGw(12, true, NOW.plus(Duration.ofDays(3)))
        ));
        when(resultService.readiness(11)).thenReturn(new FantasyResultService.Readiness(
                2026, 11, true, 10, 2, 0, List.of(), List.of()));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, true, 10, 60);

        var report = automation.runIfDue();

        assertEquals(FantasyAutomation.Outcome.DRY_RUN, report.outcome());
        assertEquals(List.of(12), report.openedGameweeks());
        assertEquals(List.of(11), report.publishedGameweeks());
        verify(gameweekService, never()).publishDeadline(anyInt(), anyLong(), anyString());
        verify(resultService, never()).publish(anyInt(), anyInt(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    void publishesWhenReadyAndNeverRecalculates() {
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(10, GameweekService.Status.AWAITING_RESULTS),
                configuredGw(11, GameweekService.Status.AWAITING_RESULTS),
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        // GW10 already has version 1 -> must NEVER trigger recalculate
        when(resultService.readiness(10)).thenReturn(new FantasyResultService.Readiness(
                2026, 10, true, 10, 1, 1, List.of(), List.of()));
        // GW11 is ready at version 0 -> publish with recalculate=false
        when(resultService.readiness(11)).thenReturn(new FantasyResultService.Readiness(
                2026, 11, true, 10, 0, 0, List.of(), List.of()));
        when(resultService.publish(11, 0, 99L, "Tự động công bố khi đủ 10/10 trận", false))
                .thenReturn(new FantasyResultService.Published(2026, 11, 1, 0, NOW, false));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(11), report.publishedGameweeks());
        verify(resultService).publish(11, 0, 99L, "Tự động công bố khi đủ 10/10 trận", false);
        verify(resultService, never()).publish(eq(10), anyInt(), anyLong(), anyString(), anyBoolean());
        verify(resultService, never()).publish(anyInt(), anyInt(), anyLong(), anyString(), eq(true));
    }

    @Test
    void doesNotPublishWhenReadinessNotReady() {
        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(11, GameweekService.Status.AWAITING_RESULTS),
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        when(resultService.readiness(11)).thenReturn(new FantasyResultService.Readiness(
                2026, 11, false, 10, 0, 0,
                List.of(new FantasyResultService.Issue(1000560649, null, null, "FIXTURE_PENDING", "Chưa xong")),
                List.of()));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(), report.publishedGameweeks());
        verify(resultService, never()).publish(anyInt(), anyInt(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    void abortsWithoutCreatingAccountWhenSystemAccountMissing() {
        when(accountRepository.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.empty());

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(FantasyAutomation.Outcome.MISSING_SYSTEM_ACCOUNT, report.outcome());
        verifyNoInteractions(gameweekService, resultService);
    }

    @Test
    void backsOffAfterUnexpectedError() {
        when(gameweekService.overview()).thenThrow(new RuntimeException("db timeout"));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        assertEquals(FantasyAutomation.Outcome.FAILED, automation.runIfDue().outcome());
        assertEquals(1, automation.consecutiveFailures());
        assertEquals(FantasyAutomation.Outcome.BACKOFF, automation.runIfDue().outcome());
        Mockito.verify(gameweekService, times(1)).overview();
    }
}
