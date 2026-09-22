package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;
import com.premierhub.repository.MatchRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MatchService {
    private final MatchRepository repository;
    private final Map<Integer, Club> clubsById;

    public MatchService(MatchRepository repository, List<Club> clubs) {
        this.repository = repository;
        this.clubsById = clubs.stream().collect(Collectors.toUnmodifiableMap(
                Club::getId, Function.identity()));
        for (Match match : repository.findAll()) {
            clubName(match.getHomeClubId());
            clubName(match.getAwayClubId());
        }
    }

    public List<Match> findMatches(String club, Integer matchweek, String status) {
        if (matchweek != null && matchweek < 1) {
            throw new IllegalArgumentException("Matchweek must be positive");
        }
        String clubFilter = normalize(club);
        MatchStatus statusFilter = EnumFilterParser.parse(status, MatchStatus.class, "status");
        return repository.findAll().stream()
                .filter(match -> clubFilter == null
                        || normalize(clubName(match.getHomeClubId())).equals(clubFilter)
                        || normalize(clubName(match.getAwayClubId())).equals(clubFilter))
                .filter(match -> matchweek == null || match.getMatchweek() == matchweek)
                .filter(match -> statusFilter == null
                        || match.getStatus() == statusFilter)
                .toList();
    }

    public Optional<Match> findById(int id) {
        return repository.findById(id);
    }

    public String clubName(int id) {
        Club club = clubsById.get(id);
        if (club == null) {
            throw new IllegalStateException("Unknown club id: " + id);
        }
        return club.getName();
    }

    private String normalize(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }
}
