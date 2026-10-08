package com.premierhub.minigame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import static com.premierhub.minigame.PlayerGuessRules.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerGuessRulesTest {
    @Test void wrongGuessRevealsOnlyNextHintWithoutExtraCharge() {
        State state = guess(initial(), false);
        assertEquals(new State(Status.IN_PROGRESS, 80, 1, 3, null), state);
        state = reveal(state);
        assertEquals(new State(Status.IN_PROGRESS, 70, 1, 4, null), state);
        assertEquals(new State(Status.WON, 70, 2, 4, 70), guess(state, true));
    }

    @Test void correctThirdGuessCanWinAtZero() {
        State state = initial();
        for (int i = 0; i < 6; i++) state = reveal(state);
        state = guess(guess(state, false), false);
        assertEquals(0, state.score());
        assertEquals(new State(Status.WON, 0, 3, 8, 0), guess(state, true));
    }

    @Test void thirdWrongGuessLosesEvenWithPositiveTemporaryScore() {
        State state = guess(guess(guess(initial(), false), false), false);
        assertEquals(new State(Status.LOST, 40, 3, 5, 0), state);
        assertThrows(IllegalArgumentException.class, () -> reveal(state));
        assertThrows(IllegalArgumentException.class, () -> guess(state, true));
    }

    @Test void allHintsAndExpiryDoNotChangeAlreadyFinishedResult() {
        State state = initial();
        for (int i = 0; i < 6; i++) state = reveal(state);
        State all = state;
        assertThrows(IllegalArgumentException.class, () -> reveal(all));
        State expired = expire(all);
        assertEquals(Status.EXPIRED, expired.status());
        assertEquals(0, expired.finalScore());
        assertEquals(40, expired.score());
        State won = guess(all, true);
        assertEquals(won, expire(won));
    }

    static Stream<PlayerGuessData.Candidate> invalidCandidates() {
        var date = LocalDate.of(2026, 10, 8);
        return Stream.of(
                candidate(null, date.minusYears(20), 180, "BOTH", 9, 80, "ST", 1),
                candidate("", date.minusYears(20), 180, "BOTH", 9, 80, "ST", 1),
                candidate("Vietnam", null, 180, "BOTH", 9, 80, "ST", 1),
                candidate("Vietnam", date.plusDays(1), 180, "BOTH", 9, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), null, "BOTH", 9, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 99, "BOTH", 9, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, null, 9, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, "UNKNOWN", 9, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", null, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", 0, 80, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", 9, null, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", 9, 74, "ST", 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", 9, 80, null, 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", 9, 80, "FORWARD", 1),
                candidate("Vietnam", date.minusYears(20), 180, "BOTH", 9, 80, "ST", 2));
    }
    private static PlayerGuessData.Candidate candidate(String nationality, LocalDate dob, Integer height, String foot,
                                                        Integer shirt, Integer ovr, String position, int count) {
        return new PlayerGuessData.Candidate(1, "Test player", 1, "Test club", nationality, dob, height, foot, shirt, ovr, position, count);
    }
    @ParameterizedTest @MethodSource("invalidCandidates")
    void missingInvalidOrAmbiguousDataNeverEntersAnswerPool(PlayerGuessData.Candidate candidate) {
        assertFalse(candidate.eligible(LocalDate.of(2026, 10, 8)));
    }
    @Test void thresholdManualOvrSpecificPositionAndBirthdayAreKept() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        var player = candidate("Vietnam", date.minusYears(26), 180, "BOTH", 9, 75, "RW", 1);
        assertTrue(player.eligible(date));
        var snapshot = player.snapshot(date, List.of());
        assertEquals(26, snapshot.age());
        assertEquals("RW", snapshot.primaryPosition());
        assertEquals(75, snapshot.fc27Overall());
    }
}
