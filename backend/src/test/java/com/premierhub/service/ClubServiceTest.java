package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.repository.InMemoryClubRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClubServiceTest {
    private final Club arsenal = new Club(1, "Arsenal", "London");
    private final Club city = new Club(2, "Manchester City", "Manchester");
    private final ClubService service = new ClubService(
            new InMemoryClubRepository(List.of(arsenal, city)));

    @Test
    void returnsAllClubsAndFindsById() {
        assertEquals(List.of(arsenal, city), service.getAll());
        assertEquals(city, service.findById(2).orElseThrow());
        assertTrue(service.findById(999).isEmpty());
    }

    @Test
    void searchesByKeywordIgnoringCaseAndSurroundingWhitespace() {
        assertEquals(List.of(city), service.searchByName("  mAnChEsTeR  "));
        assertTrue(service.searchByName("Liverpool").isEmpty());
    }

    @Test
    void rejectsNullAndBlankKeywords() {
        assertThrows(IllegalArgumentException.class, () -> service.searchByName(null));
        assertThrows(IllegalArgumentException.class, () -> service.searchByName("   "));
    }
}
