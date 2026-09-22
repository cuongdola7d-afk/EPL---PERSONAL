package com.premierhub.repository;

import com.premierhub.model.Player;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    List<Player> findAll();

    Optional<Player> findById(int id);
}
