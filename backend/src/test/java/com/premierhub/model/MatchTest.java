package com.premierhub.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {
    private static final LocalDate DATE = LocalDate.of(2025, 8, 16);

    @Test
    void determinesWinnerLoserAndPoints() {
        Match match = finishedMatch(2, 1);

        assertFalse(match.isDraw());
        assertEquals(1, match.getWinnerClubId().orElseThrow());
        assertEquals(2, match.getLoserClubId().orElseThrow());
        assertEquals(3, match.getPointsFor(1));
        assertEquals(0, match.getPointsFor(2));
    }

    @Test
    void determinesDrawAndPoints() {
        Match match = finishedMatch(1, 1);

        assertTrue(match.isDraw());
        assertTrue(match.getWinnerClubId().isEmpty());
        assertTrue(match.getLoserClubId().isEmpty());
        assertEquals(1, match.getPointsFor(1));
        assertEquals(1, match.getPointsFor(2));
    }

    @Test
    void representsScheduledMatchWithoutScore() {
        Match match = new Match(1, 1, 2, 1, DATE, MatchStatus.SCHEDULED, null, null);

        assertFalse(match.isFinished());
        assertFalse(match.isDraw());
        assertTrue(match.getWinnerClubId().isEmpty());
        assertTrue(match.getLoserClubId().isEmpty());
        assertThrows(IllegalStateException.class, () -> match.getPointsFor(1));
    }

    @Test
    void rejectsSameClubInvalidScoresAndInconsistentStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new Match(1, 1, 1, 1, DATE, MatchStatus.FINISHED, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Match(1, 1, 2, 1, DATE, MatchStatus.FINISHED, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Match(1, 1, 2, 1, DATE, MatchStatus.FINISHED, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Match(1, 1, 2, 1, DATE, MatchStatus.SCHEDULED, 0, 0));
    }

    @Test
    void rejectsInvalidIdsDatesStatusesAndUnrelatedClub() {
        assertThrows(IllegalArgumentException.class,
                () -> new Match(0, 1, 2, 1, DATE, MatchStatus.FINISHED, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Match(1, 0, 2, 1, DATE, MatchStatus.FINISHED, 0, 0));
        assertThrows(NullPointerException.class,
                () -> new Match(1, 1, 2, 1, null, MatchStatus.FINISHED, 0, 0));
        assertThrows(NullPointerException.class,
                () -> new Match(1, 1, 2, 1, DATE, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Match(1, 1, 2, 0, DATE, MatchStatus.FINISHED, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> finishedMatch(1, 0).getPointsFor(3));
    }

    private Match finishedMatch(int homeGoals, int awayGoals) {
        return new Match(1, 1, 2, 1, DATE, MatchStatus.FINISHED, homeGoals, awayGoals);
    }
}
