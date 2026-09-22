package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Player;
import com.premierhub.model.Position;
import com.premierhub.repository.PlayerRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PlayerService {
    private final PlayerRepository repository;
    private final Map<Integer, Club> clubsById;

    public PlayerService(PlayerRepository repository, List<Club> clubs) {
        this.repository = Objects.requireNonNull(repository, "Player repository must not be null");
        Map<Integer, Club> clubsById = new HashMap<>();
        for (Club club : List.copyOf(Objects.requireNonNull(clubs, "Clubs must not be null"))) {
            if (clubsById.putIfAbsent(club.getId(), club) != null) {
                throw new IllegalArgumentException("Duplicate club id: " + club.getId());
            }
        }
        for (Player player : repository.findAll()) {
            if (!clubsById.containsKey(player.getClubId())) {
                throw new IllegalArgumentException("Player " + player.getId()
                        + " references unknown club id: " + player.getClubId());
            }
        }
        this.clubsById = Map.copyOf(clubsById);
    }

    public List<Player> findPlayers(String club, String position) {
        String clubFilter = normalize(club);
        Position positionFilter = EnumFilterParser.parse(position, Position.class, "position");
        return repository.findAll().stream()
                .filter(player -> clubFilter == null
                        || normalize(clubsById.get(player.getClubId()).getName()).equals(clubFilter))
                .filter(player -> positionFilter == null
                        || player.getPosition() == positionFilter)
                .toList();
    }

    public Optional<Player> findById(int id) {
        return repository.findById(id);
    }

    public Optional<String> findClubName(int clubId) {
        return Optional.ofNullable(clubsById.get(clubId)).map(Club::getName);
    }

    private String normalize(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }
}
