package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;
import com.premierhub.repository.InMemoryMatchRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MatchServiceTest {
    private final MatchService service = new MatchService(new InMemoryMatchRepository(List.of(
            match(1, 1, 2, 1, MatchStatus.FINISHED),
            match(2, 2, 1, 2, MatchStatus.FINISHED),
            match(3, 2, 3, 1, MatchStatus.SCHEDULED))),
            List.of(new Club(1, "Arsenal", "London"),
                    new Club(2, "Chelsea", "London"),
                    new Club(3, "Liverpool", "Liverpool")));

    @Test
    void findsAllAndHandlesEmptyRepository() {
        assertEquals(List.of(1, 2, 3), ids(service.findMatches(null, null, null)));
        assertTrue(new MatchService(new InMemoryMatchRepository(List.of()), List.of())
                .findMatches(null, null, null).isEmpty());
    }

    @Test
    void findsExistingAndMissingId() {
        assertEquals(2, service.findById(2).orElseThrow().getMatchweek());
        assertTrue(service.findById(999).isEmpty());
    }

    @Test
    void findsHomeAndAwayClubIgnoringCaseAndWhitespace() {
        assertEquals(List.of(1, 2), ids(service.findMatches("  aRsEnAl  ", null, null)));
    }

    @Test
    void filtersByMatchweekAndCombinesWithClub() {
        assertEquals(List.of(1, 3), ids(service.findMatches(null, 1, null)));
        assertEquals(List.of(2), ids(service.findMatches("Arsenal", 2, null)));
    }

    @Test
    void filtersStatusIgnoringCaseAndCombinesAllFilters() {
        assertEquals(List.of(3), ids(service.findMatches(null, null, "  sCheDuleD ")));
        assertEquals(List.of(1), ids(service.findMatches("Arsenal", 1, "finished")));
    }

    @Test
    void noResultIsEmptyAndInvalidMatchweekIsRejected() {
        assertTrue(service.findMatches("Arsenal", 3, null).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.findMatches(null, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.findMatches(null, -1, null));
    }

    private Match match(int id, int home, int away, int week, MatchStatus status) {
        Integer goals = status == MatchStatus.FINISHED ? 0 : null;
        return new Match(id, home, away, week, LocalDate.of(2025, 8, 16),
                status, goals, goals);
    }

    private List<Integer> ids(List<Match> matches) {
        return matches.stream().map(Match::getId).toList();
    }
}
