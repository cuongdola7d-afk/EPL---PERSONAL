package com.premierhub.repository;

import com.premierhub.model.Club;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class InMemoryClubRepository implements ClubRepository {
    private final List<Club> clubs;

    public InMemoryClubRepository(List<Club> clubs) {
        this.clubs = List.copyOf(clubs);
        Set<Integer> ids = new HashSet<>();
        for (Club club : this.clubs) {
            if (!ids.add(club.getId())) {
                throw new IllegalArgumentException("Duplicate club id: " + club.getId());
            }
        }
    }

    @Override
    public List<Club> findAll() {
        return clubs;
    }

    @Override
    public Optional<Club> findById(int id) {
        return clubs.stream().filter(club -> club.getId() == id).findFirst();
    }
}
