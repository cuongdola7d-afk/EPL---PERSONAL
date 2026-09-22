package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Player;
import com.premierhub.repository.PlayerRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class PlayerService {
    private final PlayerRepository repository;
    private final List<Club> clubs;

    public PlayerService(PlayerRepository repository, List<Club> clubs) {
        this.repository = repository;
        this.clubs = List.copyOf(clubs);
    }

    public List<Player> findPlayers(String club, String position) {
        String clubFilter = normalize(club);
        String positionFilter = normalize(position);
        return repository.findAll().stream()
                .filter(player -> clubFilter == null || clubs.stream().anyMatch(candidate ->
                        candidate.getId() == player.getClubId()
                                && normalize(candidate.getName()).equals(clubFilter)))
                .filter(player -> positionFilter == null
                        || normalize(player.getPosition().name()).equals(positionFilter))
                .toList();
    }

    public Optional<Player> findById(int id) {
        return repository.findById(id);
    }

    public Optional<String> findClubName(int clubId) {
        return clubs.stream().filter(club -> club.getId() == clubId)
                .map(Club::getName).findFirst();
    }

    private String normalize(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }
}
