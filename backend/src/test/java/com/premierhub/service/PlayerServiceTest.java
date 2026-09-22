package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Player;
import com.premierhub.model.Position;
import com.premierhub.repository.InMemoryPlayerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlayerServiceTest {
    private final PlayerService service = new PlayerService(new InMemoryPlayerRepository(List.of(
            new Player(1, "Saka", 1, Position.FORWARD, 1, 2),
            new Player(2, "Raya", 1, Position.GOALKEEPER, 0, 0),
            new Player(3, "Salah", 2, Position.FORWARD, 2, 1))),
            List.of(new Club(1, "Arsenal", "London"), new Club(2, "Liverpool", "Liverpool")));

    @Test
    void findsAllAndEmptyRepository() {
        assertEquals(3, service.findPlayers(null, null).size());
        assertTrue(new PlayerService(new InMemoryPlayerRepository(List.of()), List.of())
                .findPlayers(null, null).isEmpty());
    }

    @Test
    void filtersClubIgnoringCaseAndWhitespace() {
        assertEquals(List.of(1, 2), ids(service.findPlayers("  aRsEnAl  ", null)));
    }

    @Test
    void filtersPositionIgnoringCaseAndWhitespace() {
        assertEquals(List.of(1, 3), ids(service.findPlayers(null, "  fOrWaRd  ")));
    }

    @Test
    void combinesFiltersWithAnd() {
        assertEquals(List.of(1), ids(service.findPlayers(" arsenal ", " forward ")));
    }

    @Test
    void noMatchReturnsEmptyList() {
        assertTrue(service.findPlayers("Chelsea", "FORWARD").isEmpty());
        assertTrue(service.findPlayers(null, "Striker").isEmpty());
    }

    @Test
    void findsExistingAndMissingId() {
        assertEquals("Saka", service.findById(1).orElseThrow().getName());
        assertTrue(service.findById(999).isEmpty());
    }

    private List<Integer> ids(List<Player> players) {
        return players.stream().map(Player::getId).toList();
    }
}
