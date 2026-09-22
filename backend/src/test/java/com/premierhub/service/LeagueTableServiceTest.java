package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;
import com.premierhub.model.Standing;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeagueTableServiceTest {
    private static final LocalDate DATE = LocalDate.of(2025, 8, 16);
    private final Club alpha = new Club(1, "Alpha", "A City");
    private final Club beta = new Club(2, "Beta", "B City");
    private final Club gamma = new Club(3, "Gamma", "C City");
    private final Club delta = new Club(4, "Delta", "D City");
    private final LeagueTableService service = new LeagueTableService();

    @Test
    void calculatesWinAndKeepsClubWithoutMatches() {
        List<Standing> table = service.calculate(
                List.of(alpha, beta, gamma), List.of(finished(1, 1, 2, 2, 0)));

        assertStanding(table.get(0), "Alpha", 1, 1, 0, 0, 2, 0, 3);
        assertStanding(table.get(1), "Gamma", 0, 0, 0, 0, 0, 0, 0);
        assertStanding(table.get(2), "Beta", 1, 0, 0, 1, 0, 2, 0);
    }

    @Test
    void calculatesDrawForBothClubs() {
        List<Standing> table = service.calculate(
                List.of(alpha, beta), List.of(finished(1, 1, 2, 1, 1)));

        assertStanding(table.get(0), "Alpha", 1, 0, 1, 0, 1, 1, 1);
        assertStanding(table.get(1), "Beta", 1, 0, 1, 0, 1, 1, 1);
    }

    @Test
    void calculatesSeveralMatchesAndIgnoresScheduledMatch() {
        List<Match> matches = List.of(
                finished(1, 1, 2, 2, 0),
                finished(2, 2, 3, 1, 1),
                finished(3, 3, 1, 3, 0),
                new Match(4, 4, 1, 1, DATE, MatchStatus.SCHEDULED, null, null));

        List<Standing> table = service.calculate(
                List.of(alpha, beta, gamma, delta), matches);

        assertEquals(List.of("Gamma", "Alpha", "Beta", "Delta"),
                table.stream().map(s -> s.getClub().getName()).toList());
        assertStanding(table.get(0), "Gamma", 2, 1, 1, 0, 4, 1, 4);
        assertEquals(0, table.get(3).getPlayed());
    }

    @Test
    void ordersByPointsGoalDifferenceGoalsForThenName() {
        List<Match> matches = List.of(
                finished(1, 1, 4, 2, 0),
                finished(2, 2, 4, 3, 1),
                finished(3, 3, 4, 3, 1));

        List<Standing> table = service.calculate(
                List.of(gamma, beta, alpha, delta), matches);

        // Beta and Gamma have equal points, goal difference and goals for: name decides.
        assertEquals(List.of("Beta", "Gamma", "Alpha", "Delta"),
                table.stream().map(s -> s.getClub().getName()).toList());
    }

    @Test
    void rejectsDuplicateClubsAndUnknownReferencesInFinishedMatches() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(List.of(alpha, alpha), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(List.of(alpha),
                        List.of(finished(1, 1, 99, 1, 0))));
    }

    private Match finished(int id, int homeId, int awayId,
                           int homeGoals, int awayGoals) {
        return new Match(id, homeId, awayId, 1, DATE,
                MatchStatus.FINISHED, homeGoals, awayGoals);
    }

    private void assertStanding(Standing standing, String name, int played,
                                int wins, int draws, int losses,
                                int goalsFor, int goalsAgainst, int points) {
        assertEquals(name, standing.getClub().getName());
        assertEquals(played, standing.getPlayed());
        assertEquals(wins, standing.getWins());
        assertEquals(draws, standing.getDraws());
        assertEquals(losses, standing.getLosses());
        assertEquals(goalsFor, standing.getGoalsFor());
        assertEquals(goalsAgainst, standing.getGoalsAgainst());
        assertEquals(points, standing.getPoints());
    }
}
