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

    @Test
    void autoShiftsOpenGameweekWhenFirstFixtureMovesLaterAndAuditsAsSystem() {
        Instant oldKickoff = Instant.parse("2026-11-27T20:00:00Z");
        Instant oldDeadline = Instant.parse("2026-11-27T19:30:00Z");
        Instant newKickoff = Instant.parse("2026-11-28T12:30:00Z");
        Instant expectedNewDeadline = Instant.parse("2026-11-28T12:00:00Z");

        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        when(gameweekRepository.find(12)).thenReturn(Optional.of(new GameweekRepository.Configuration(
                12, oldDeadline, NOW.minus(Duration.ofDays(1)), 1000560658, oldKickoff,
                "OPEN", null, 1, LocalDate.of(2026, 11, 24))));
        var shiftedFixture = new GameweekRepository.Fixture(
                1000560658, 12, newKickoff, "SCHEDULED", "Arsenal FC", "Chelsea FC");
        var postponedEarlier = new GameweekRepository.Fixture(
                1000560659, 12, oldKickoff.minus(Duration.ofHours(2)), "POSTPONED", "Fulham FC", "Everton FC");
        when(gameweekRepository.fixtures()).thenReturn(List.of(postponedEarlier, shiftedFixture));
        when(gameweekRepository.shiftDeadlineForFirstFixture(
                eq(12), eq(shiftedFixture), eq(expectedNewDeadline), eq(NOW), eq(99L), contains("Arsenal FC – Chelsea FC")))
                .thenReturn(Optional.of(new GameweekRepository.AutoShiftResult(
                        12, 2, oldDeadline, expectedNewDeadline, 1000560658, newKickoff)));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(FantasyAutomation.Outcome.COMPLETED, report.outcome());
        assertEquals(List.of(12), report.shiftedGameweeks());
        verify(gameweekRepository).shiftDeadlineForFirstFixture(
                eq(12), eq(shiftedFixture), eq(expectedNewDeadline), eq(NOW), eq(99L),
                argThat(reason -> reason.startsWith("Tự động dời theo giờ trận đầu: Arsenal FC – Chelsea FC")));
    }

    @Test
    void autoShiftsWhenAnotherFixtureBecomesEarlier() {
        Instant oldKickoff = Instant.parse("2026-11-28T15:00:00Z");
        Instant oldDeadline = Instant.parse("2026-11-28T14:30:00Z");
        Instant earlierKickoff = Instant.parse("2026-11-27T20:00:00Z");
        Instant expectedNewDeadline = Instant.parse("2026-11-27T19:30:00Z");

        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        when(gameweekRepository.find(12)).thenReturn(Optional.of(new GameweekRepository.Configuration(
                12, oldDeadline, NOW.minus(Duration.ofDays(1)), 1000560660, oldKickoff,
                "OPEN", null, 1, LocalDate.of(2026, 11, 24))));
        var newFirst = new GameweekRepository.Fixture(
                1000560658, 12, earlierKickoff, "SCHEDULED", "Brentford FC", "Everton FC");
        var oldFirst = new GameweekRepository.Fixture(
                1000560660, 12, oldKickoff, "SCHEDULED", "Arsenal FC", "Chelsea FC");
        when(gameweekRepository.fixtures()).thenReturn(List.of(oldFirst, newFirst));
        when(gameweekRepository.shiftDeadlineForFirstFixture(
                eq(12), eq(newFirst), eq(expectedNewDeadline), eq(NOW), eq(99L), anyString()))
                .thenReturn(Optional.of(new GameweekRepository.AutoShiftResult(
                        12, 2, oldDeadline, expectedNewDeadline, 1000560658, earlierKickoff)));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(12), report.shiftedGameweeks());
        verify(gameweekRepository).shiftDeadlineForFirstFixture(
                eq(12), eq(newFirst), eq(expectedNewDeadline), eq(NOW), eq(99L), anyString());
    }

    @Test
    void doesNotOverwriteManualAdminDeadlineWhenFirstFixtureUnchanged() {
        Instant kickoff = Instant.parse("2026-11-27T20:00:00Z");
        Instant manualDeadline = Instant.parse("2026-11-27T18:00:00Z"); // Admin manually set 2h before kickoff

        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        when(gameweekRepository.find(12)).thenReturn(Optional.of(new GameweekRepository.Configuration(
                12, manualDeadline, NOW.minus(Duration.ofDays(1)), 1000560658, kickoff,
                "OPEN", null, 2, LocalDate.of(2026, 11, 24))));
        var sameFirst = new GameweekRepository.Fixture(
                1000560658, 12, kickoff, "SCHEDULED", "Arsenal FC", "Chelsea FC");
        when(gameweekRepository.fixtures()).thenReturn(List.of(sameFirst));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(), report.shiftedGameweeks());
        verify(gameweekRepository, never()).shiftDeadlineForFirstFixture(
                anyInt(), any(), any(), any(), anyLong(), anyString());
    }

    @Test
    void locksImmediatelyWhenMovedUpFirstFixtureProducesPastDeadline() {
        Instant oldKickoff = Instant.parse("2026-11-27T20:00:00Z");
        Instant oldDeadline = Instant.parse("2026-11-27T19:30:00Z");
        // Moved up to 15 minutes from NOW -> candidate deadline (NOW - 15m) is in the past -> lock immediately at NOW.
        Instant movedUpKickoff = NOW.plus(Duration.ofMinutes(15));

        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        when(gameweekRepository.find(12)).thenReturn(Optional.of(new GameweekRepository.Configuration(
                12, oldDeadline, NOW.minus(Duration.ofDays(1)), 1000560658, oldKickoff,
                "OPEN", null, 1, LocalDate.of(2026, 11, 24))));
        var movedUp = new GameweekRepository.Fixture(
                1000560658, 12, movedUpKickoff, "SCHEDULED", "Arsenal FC", "Chelsea FC");
        when(gameweekRepository.fixtures()).thenReturn(List.of(movedUp));
        when(gameweekRepository.shiftDeadlineForFirstFixture(
                eq(12), eq(movedUp), eq(NOW), eq(NOW), eq(99L), contains("khóa ngay")))
                .thenReturn(Optional.of(new GameweekRepository.AutoShiftResult(
                        12, 2, oldDeadline, NOW, 1000560658, movedUpKickoff)));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, false, 10, 60);

        var report = automation.runIfDue();

        assertEquals(List.of(12), report.shiftedGameweeks());
        verify(gameweekRepository).shiftDeadlineForFirstFixture(
                eq(12), eq(movedUp), eq(NOW), eq(NOW), eq(99L), contains("khóa ngay"));
    }

    @Test
    void dryRunLogsShiftWithoutWritingToRepository() {
        Instant oldKickoff = Instant.parse("2026-11-27T20:00:00Z");
        Instant oldDeadline = Instant.parse("2026-11-27T19:30:00Z");
        Instant newKickoff = Instant.parse("2026-11-28T12:30:00Z");

        when(gameweekService.overview()).thenReturn(overviewWith(
                configuredGw(12, GameweekService.Status.OPEN)
        ));
        when(gameweekRepository.find(12)).thenReturn(Optional.of(new GameweekRepository.Configuration(
                12, oldDeadline, NOW.minus(Duration.ofDays(1)), 1000560658, oldKickoff,
                "OPEN", null, 1, LocalDate.of(2026, 11, 24))));
        var shiftedFixture = new GameweekRepository.Fixture(
                1000560658, 12, newKickoff, "SCHEDULED", "Arsenal FC", "Chelsea FC");
        when(gameweekRepository.fixtures()).thenReturn(List.of(shiftedFixture));

        var automation = new FantasyAutomation(
                gameweekService, gameweekRepository, resultService, accountRepository,
                clock, true, true, 10, 60);

        var report = automation.runIfDue();

        assertEquals(FantasyAutomation.Outcome.DRY_RUN, report.outcome());
        assertEquals(List.of(12), report.shiftedGameweeks());
        verify(gameweekRepository, never()).shiftDeadlineForFirstFixture(
                anyInt(), any(), any(), any(), anyLong(), anyString());
    }
}
