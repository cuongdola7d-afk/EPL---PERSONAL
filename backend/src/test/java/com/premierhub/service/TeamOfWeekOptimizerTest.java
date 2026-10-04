package com.premierhub.service;

import com.premierhub.repository.TeamOfWeekRepository.Appearance;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeamOfWeekOptimizerTest {
    private List<Appearance> squad() {
        var players = new ArrayList<Appearance>();
        for (int slot = 0; slot < 11; slot++) players.add(player(slot + 1, "8.00",
                TeamOfWeekOptimizer.permission(TeamOfWeekOptimizer.SLOTS.get(slot))));
        return players;
    }

    private Appearance player(int id, String rating, String... positions) {
        return new Appearance(id, "Player " + id, 1, "Same club", 100,
                rating == null ? null : new BigDecimal(rating), List.of(positions));
    }

    @Test
    void fillsExact433WithoutDuplicatesAndWithoutOvrOrClubLimits() {
        var selection = TeamOfWeekOptimizer.select(squad());
        assertEquals(11, selection.filledSlots());
        assertEquals(8800, selection.ratingCents());
        var seen = new HashSet<Integer>();
        for (int slot = 0; slot < 11; slot++) {
            var player = selection.players().get(slot);
            assertTrue(seen.add(player.playerId()));
            assertTrue(player.eligiblePositions().contains(TeamOfWeekOptimizer.permission(TeamOfWeekOptimizer.SLOTS.get(slot))));
            assertEquals(1, player.clubId());
        }
        assertEquals("CB", TeamOfWeekOptimizer.permission("LCB"));
        assertEquals("CB", TeamOfWeekOptimizer.permission("RCB"));
        assertEquals("CM", TeamOfWeekOptimizer.permission("LCM"));
        assertEquals("CM", TeamOfWeekOptimizer.permission("RCM"));
    }

    @Test
    void flexibleDefenderIsMovedToRbToBeatTheGreedyLbChoice() {
        var players = squad();
        players.set(4, player(5, "1.00", "RB"));
        players.add(player(30, "9.00", "LB", "RB"));
        var selection = TeamOfWeekOptimizer.select(players);
        assertEquals(11, selection.filledSlots());
        assertEquals(8900, selection.ratingCents()); // Greedy LB=30, RB=5 totals 8200.
        assertEquals(2, selection.players().get(1).playerId());
        assertEquals(30, selection.players().get(4).playerId());
    }

    @Test
    void nullRatingAndUnpermittedPositionsCannotFillTheMissingGoalkeeper() {
        var players = squad();
        players.set(0, player(1, null, "GK"));
        players.add(player(20, "10.00", "CAM", "RM"));
        var selection = TeamOfWeekOptimizer.select(players);
        assertEquals(10, selection.filledSlots());
        assertNull(selection.players().getFirst());
        assertEquals(8000, selection.ratingCents());
        assertThrows(IllegalArgumentException.class, () -> TeamOfWeekOptimizer.select(List.of(players.get(1), players.get(1))));
    }

    @Test
    void equalTotalsUseSmallestPlayerIdsBySlotRegardlessOfQueryOrder() {
        var players = squad();
        players.add(player(100, "8.00", "CB", "CM"));
        var expected = TeamOfWeekOptimizer.select(players).players().stream().map(Appearance::playerId).toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), expected);
        for (int iteration = 0; iteration < 12; iteration++) {
            Collections.shuffle(players, new Random(iteration));
            assertEquals(expected, TeamOfWeekOptimizer.select(players).players().stream().map(Appearance::playerId).toList());
        }
    }
}
