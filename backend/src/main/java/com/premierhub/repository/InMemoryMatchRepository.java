package com.premierhub.repository;

import com.premierhub.model.Match;
import java.util.List;
import java.util.Optional;

public final class InMemoryMatchRepository implements MatchRepository {
    private final List<Match> matches;

    public InMemoryMatchRepository(List<Match> matches) {
        this.matches = List.copyOf(matches);
    }

    @Override
    public List<Match> findAll() {
        return matches;
    }

    @Override
    public Optional<Match> findById(int id) {
        return matches.stream().filter(match -> match.getId() == id).findFirst();
    }
}
