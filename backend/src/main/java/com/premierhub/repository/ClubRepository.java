package com.premierhub.repository;

import com.premierhub.model.Club;
import java.util.List;
import java.util.Optional;

public interface ClubRepository {
    List<Club> findAll();

    Optional<Club> findById(int id);
}
