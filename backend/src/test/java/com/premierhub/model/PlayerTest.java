package com.premierhub.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {
    @Test
    void createsPlayerAndTrimsName() {
        Player player = new Player(1, "  Sample Player ", 2, Position.FORWARD, 5, 3);

        assertEquals(1, player.getId());
        assertEquals("Sample Player", player.getName());
        assertEquals(2, player.getClubId());
        assertEquals(Position.FORWARD, player.getPosition());
        assertEquals(5, player.getGoals());
        assertEquals(3, player.getAssists());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositivePlayerId(int id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(id, "Player", 1, Position.FORWARD, 0, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveClubId(int clubId) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(1, "Player", clubId, Position.FORWARD, 0, 0));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingName(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(1, name, 1, Position.FORWARD, 0, 0));
    }

    @Test
    void rejectsMissingPositionAndNegativeStatistics() {
        assertThrows(NullPointerException.class,
                () -> new Player(1, "Player", 1, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(1, "Player", 1, Position.FORWARD, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(1, "Player", 1, Position.FORWARD, 0, -1));
    }

    @Test
    void matchesNameIgnoringCaseAndWhitespace() {
        Player player = new Player(1, "Sample Player", 1, Position.FORWARD, 0, 0);

        assertTrue(player.matchesName("  PLAYER "));
        assertFalse(player.matchesName("Goalkeeper"));
        assertFalse(player.matchesName(null));
        assertFalse(player.matchesName("   "));
    }
}
