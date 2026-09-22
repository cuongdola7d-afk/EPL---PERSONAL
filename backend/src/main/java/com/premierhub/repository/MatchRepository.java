package com.premierhub.repository;

import com.premierhub.model.Match;
import java.util.List;
import java.util.Optional;

public interface MatchRepository {
    List<Match> findAll();

    Optional<Match> findById(int id);
}
