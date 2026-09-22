package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.repository.ClubRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClubService {
    private final ClubRepository repository;

    public ClubService(ClubRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Club repository must not be null");
    }

    public List<Club> getAll() {
        return repository.findAll();
    }

    public Optional<Club> findById(int id) {
        return repository.findById(id);
    }

    public List<Club> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank");
        }
        return repository.findAll().stream()
                .filter(club -> club.matchesName(keyword))
                .toList();
    }
}
