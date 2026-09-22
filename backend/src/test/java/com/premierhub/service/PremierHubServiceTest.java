package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;
import com.premierhub.model.Player;
import com.premierhub.model.Position;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PremierHubServiceTest {
    private final Club arsenal = new Club(1, "Arsenal", "London");
    private final Club city = new Club(2, "Manchester City", "Manchester");
    private final Player saka = new Player(1, "Bukayo Saka", 1,
            Position.FORWARD, 10, 8);
    private final Player haaland = new Player(2, "Erling Haaland", 2,
            Position.FORWARD, 12, 3);
    private final Player odegaard = new Player(3, "Martin Odegaard", 1,
            Position.MIDFIELDER, 5, 8);

    @Test
    void searchesPlayersAndFiltersByClub() {
        PremierHubService service = service();

        assertEquals(List.of(saka), service.findPlayersByName("saka"));
        assertEquals(List.of(saka, odegaard), service.getPlayersByClub(1));
        assertThrows(IllegalArgumentException.class, () -> service.getPlayersByClub(99));
    }

    @Test
    void returnsTopScorersByGoalsAssistsThenName() {
        Player another = new Player(4, "Aaron Forward", 2,
                Position.FORWARD, 5, 8);
        PremierHubService service = new PremierHubService(
                List.of(arsenal, city), List.of(odegaard, saka, haaland, another), List.of());

        assertEquals(List.of(haaland, saka, another), service.getTopScorers(3));
        assertThrows(IllegalArgumentException.class, () -> service.getTopScorers(-1));
    }

    @Test
    void filtersPlayersByPositionAndPreservesInputOrder() {
        PremierHubService service = service();

        assertEquals(List.of(saka, haaland), service.getPlayersByPosition(Position.FORWARD));
        assertEquals(List.of(odegaard), service.getPlayersByPosition(Position.MIDFIELDER));
        assertThrows(IllegalArgumentException.class,
                () -> service.getPlayersByPosition(null));
    }

    @Test
    void delegatesLeagueTableCalculation() {
        PremierHubService service = service();

        assertEquals("Arsenal", service.getLeagueTable().get(0).getClub().getName());
        assertEquals(3, service.getLeagueTable().get(0).getPoints());
    }

    @Test
    void rejectsPlayerWithUnknownClub() {
        Player unknown = new Player(4, "Unknown", 99, Position.DEFENDER, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> new PremierHubService(List.of(arsenal), List.of(unknown), List.of()));
    }

    private PremierHubService service() {
        Match match = new Match(1, 1, 2, 1, LocalDate.of(2025, 8, 16),
                MatchStatus.FINISHED, 2, 1);
        return new PremierHubService(
                List.of(arsenal, city), List.of(saka, haaland, odegaard), List.of(match));
    }
}
