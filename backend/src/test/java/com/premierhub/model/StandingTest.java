package com.premierhub.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandingTest {
    private final Club club = new Club(1, "Club", "City");

    @Test
    void derivesGoalDifferenceAndPoints() {
        Standing standing = new Standing(club, 4, 2, 1, 1, 7, 4);

        assertEquals(3, standing.getGoalDifference());
        assertEquals(7, standing.getPoints());
    }

    @Test
    void rejectsNegativeAndInconsistentValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Standing(club, 1, 1, 0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Standing(club, 2, 1, 0, 0, 1, 0));
    }
}
