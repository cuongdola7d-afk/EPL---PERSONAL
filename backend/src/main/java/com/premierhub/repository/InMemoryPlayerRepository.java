package com.premierhub.repository;

import com.premierhub.model.Player;
import java.util.List;
import java.util.Optional;

public final class InMemoryPlayerRepository implements PlayerRepository {
    private final List<Player> players;

    public InMemoryPlayerRepository(List<Player> players) {
        this.players = List.copyOf(players);
    }

    @Override
    public List<Player> findAll() {
        return players;
    }

    @Override
    public Optional<Player> findById(int id) {
        return players.stream().filter(player -> player.getId() == id).findFirst();
    }
}
