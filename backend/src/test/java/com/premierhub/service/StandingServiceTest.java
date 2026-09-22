package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;
import com.premierhub.repository.InMemoryMatchRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StandingServiceTest {
    private final Club alpha = club(1, "Alpha");
    private final Club beta = club(2, "Beta");
    private final Club gamma = club(3, "Gamma");
    private final Club delta = club(4, "Delta");

    @Test
    void returnsFullTableOrderedByPointsAndPositionsFromOne() {
        StandingService service = service(List.of(beta, alpha, gamma),
                List.of(finished(1, 1, 2, 2, 0)));

        assertEquals(List.of("Alpha", "Gamma", "Beta"), names(service));
        assertEquals(List.of(1, 2, 3), service.getStandings(null).stream()
                .map(StandingService.RankedStanding::position).toList());
        assertEquals(3, service.getStandings(null).get(0).standing().getPoints());
    }

    @Test
    void breaksEqualPointsByGoalDifference() {
        StandingService service = service(List.of(alpha, beta, gamma, delta), List.of(
                finished(1, 1, 3, 1, 0),
                finished(2, 2, 4, 2, 0)));

        assertEquals(List.of("Beta", "Alpha"), names(service).subList(0, 2));
    }

    @Test
    void breaksEqualPointsAndDifferenceByGoalsFor() {
        StandingService service = service(List.of(alpha, beta, gamma, delta), List.of(
                finished(1, 1, 3, 2, 0),
                finished(2, 2, 4, 3, 1)));

        assertEquals(List.of("Beta", "Alpha"), names(service).subList(0, 2));
    }

    @Test
    void breaksIdenticalStatisticsByClubName() {
        StandingService service = service(List.of(beta, alpha, gamma, delta), List.of(
                finished(1, 1, 3, 1, 0),
                finished(2, 2, 4, 1, 0)));

        assertEquals(List.of("Alpha", "Beta"), names(service).subList(0, 2));
    }

    @Test
    void findsClubByIdWithItsTablePositionAndHandlesMissingClub() {
        StandingService service = service(List.of(alpha, beta),
                List.of(finished(1, 1, 2, 1, 0)));

        assertEquals(2, service.findByClubId(2).orElseThrow().position());
        assertTrue(service.findByClubId(999).isEmpty());
    }

    @Test
    void limitsTableAndAllowsLimitLargerThanClubCount() {
        StandingService service = service(List.of(alpha, beta, gamma), List.of());

        assertEquals(2, service.getStandings(2).size());
        assertEquals(3, service.getStandings(10).size());
        assertEquals(List.of(1, 2), service.getStandings(2).stream()
                .map(StandingService.RankedStanding::position).toList());
    }

    @Test
    void rejectsNonPositiveLimit() {
        StandingService service = service(List.of(alpha), List.of());

        assertThrows(IllegalArgumentException.class, () -> service.getStandings(0));
        assertThrows(IllegalArgumentException.class, () -> service.getStandings(-1));
    }

    @Test
    void returnsEmptyTableWhenThereAreNoClubs() {
        StandingService service = service(List.of(), List.of());

        assertTrue(service.getStandings(null).isEmpty());
        assertTrue(service.findByClubId(1).isEmpty());
    }

    @Test
    void ignoresScheduledMatch() {
        Match scheduled = new Match(1, 1, 2, 1, LocalDate.of(2025, 8, 16),
                MatchStatus.SCHEDULED, null, null);
        StandingService service = service(List.of(beta, alpha), List.of(scheduled));

        assertEquals(List.of("Alpha", "Beta"), names(service));
        assertEquals(0, service.getStandings(null).get(0).standing().getPlayed());
    }

    private StandingService service(List<Club> clubs, List<Match> matches) {
        return new StandingService(new LeagueTableService(),
                new InMemoryMatchRepository(matches), clubs);
    }

    private Club club(int id, String name) {
        return new Club(id, name, "City");
    }

    private Match finished(int id, int home, int away, int homeGoals, int awayGoals) {
        return new Match(id, home, away, 1, LocalDate.of(2025, 8, 16),
                MatchStatus.FINISHED, homeGoals, awayGoals);
    }

    private List<String> names(StandingService service) {
        return service.getStandings(null).stream()
                .map(entry -> entry.standing().getClub().getName()).toList();
    }
}
