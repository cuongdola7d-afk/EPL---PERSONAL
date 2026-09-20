package com.premierhub.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubTest {
    @Test
    void createsClubAndTrimsSurroundingWhitespace() {
        Club club = new Club(1, "  Sample United  ", " Sample City ");

        assertEquals(1, club.getId());
        assertEquals("Sample United", club.getName());
        assertEquals("Sample City", club.getCity());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveId(int id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Club(id, "Sample United", "Sample City"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsMissingName(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new Club(1, name, "Sample City"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsMissingCity(String city) {
        assertThrows(IllegalArgumentException.class,
                () -> new Club(1, "Sample United", city));
    }

    @ParameterizedTest
    @ValueSource(strings = {"united", "SAMPLE", "  united  ", "Sample United"})
    void matchesNameIgnoringCaseAndSurroundingWhitespace(String keyword) {
        Club club = new Club(1, "Sample United", "Sample City");

        assertTrue(club.matchesName(keyword));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "City", "Arsenal"})
    void doesNotMatchMissingOrUnrelatedName(String keyword) {
        Club club = new Club(1, "Sample United", "Sample City");

        assertFalse(club.matchesName(keyword));
    }
}
