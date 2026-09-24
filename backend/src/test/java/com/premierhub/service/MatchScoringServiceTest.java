package com.premierhub.service;

import com.premierhub.web.dto.MatchScoreResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchScoringServiceTest {
    private final MatchScoringService scoring = new MatchScoringService();

    @Test
    void goalRateDependsOnMatchPosition() {
        assertGoalPoints("G", 10);
        assertGoalPoints("D", 6);
        assertGoalPoints("M", 5);
        assertGoalPoints("F", 4);
    }

    @Test
    void appearanceChangesAtSixtyMinutes() {
        assertEquals(0, scoring.score("F", 0, 0, 0, 0, 0).confirmedPoints());
        assertEquals(1, scoring.score("F", 1, 0, 0, 0, 0).confirmedPoints());
        assertEquals(1, scoring.score("F", 59, 0, 0, 0, 0).confirmedPoints());
        assertEquals(2, scoring.score("F", 60, 0, 0, 0, 0).confirmedPoints());
    }

    @Test
    void assistsAndCardsContributeSignedPoints() {
        MatchScoreResponse score = scoring.score("M", 90, 0, 2, 2, 1);
        assertEquals("COMPLETE", score.status());
        assertEquals(3, score.confirmedPoints()); // 2 + 6 - 2 - 3
        assertEquals(6, part(score, "assists").points());
        assertEquals(-2, part(score, "yellowCards").points());
        assertEquals(-3, part(score, "redCards").points());
    }

    @Test
    void missingGoalIsNotZeroAndKnownPointsRemainProvisional() {
        MatchScoreResponse score = scoring.score("F", 90, null, 0, 0, 0);
        assertEquals("PROVISIONAL", score.status());
        assertEquals(2, score.confirmedPoints());
        assertNull(part(score, "goals").points());
        assertEquals(0, part(score, "assists").points());
    }

    @Test
    void missingMinutesAndOtherFieldsCannotProduceFinalScore() {
        MatchScoreResponse score = scoring.score("G", null, null, null, null, null);
        assertEquals("PROVISIONAL", score.status());
        assertEquals(0, score.confirmedPoints());
        score.parts().forEach(part -> assertNull(part.points()));
    }

    @Test
    void goalWithUnknownPositionCannotBeAwarded() {
        MatchScoreResponse score = scoring.score(null, 90, 1, 0, 0, 0);
        assertEquals("PROVISIONAL", score.status());
        assertEquals(2, score.confirmedPoints());
        assertNull(part(score, "goals").points());
    }

    private void assertGoalPoints(String position, int expected) {
        MatchScoreResponse score = scoring.score(position, 90, 1, 0, 0, 0);
        assertEquals("COMPLETE", score.status());
        assertEquals(expected, part(score, "goals").points());
        assertEquals(2 + expected, score.confirmedPoints());
    }

    private MatchScoreResponse.Part part(MatchScoreResponse score, String code) {
        return score.parts().stream().filter(part -> part.code().equals(code)).findFirst().orElseThrow();
    }
}
