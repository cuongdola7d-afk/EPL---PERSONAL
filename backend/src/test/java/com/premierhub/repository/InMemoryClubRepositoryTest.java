package com.premierhub.repository;

import com.premierhub.model.Club;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryClubRepositoryTest {
    private final Club arsenal = new Club(1, "Arsenal", "London");
    private final Club chelsea = new Club(2, "Chelsea", "London");

    @Test
    void findAllReturnsImmutableSnapshot() {
        List<Club> source = new ArrayList<>(List.of(arsenal));
        InMemoryClubRepository repository = new InMemoryClubRepository(source);
        source.add(chelsea);

        assertEquals(List.of(arsenal), repository.findAll());
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findAll().add(chelsea));
    }

    @Test
    void findByIdReturnsClubOrEmpty() {
        InMemoryClubRepository repository = new InMemoryClubRepository(List.of(arsenal));

        assertEquals(arsenal, repository.findById(1).orElseThrow());
        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    void rejectsDuplicateClubIds() {
        Club duplicate = new Club(1, "Other", "City");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new InMemoryClubRepository(List.of(arsenal, duplicate)));
        assertTrue(exception.getMessage().contains("1"));
    }
}
