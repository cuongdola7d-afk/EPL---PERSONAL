package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.Player;
import com.premierhub.model.Standing;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PremierHubService {
    private static final Comparator<Player> TOP_SCORER_ORDER =
            Comparator.comparingInt(Player::getGoals).reversed()
                    .thenComparing(Comparator.comparingInt(Player::getAssists).reversed())
                    .thenComparing(Player::getName);

    private final List<Club> clubs;
    private final List<Player> players;
    private final List<Match> matches;
    private final LeagueTableService leagueTableService;

    public PremierHubService(List<Club> clubs, List<Player> players, List<Match> matches) {
        this.clubs = List.copyOf(Objects.requireNonNull(clubs, "Clubs must not be null"));
        this.players = List.copyOf(Objects.requireNonNull(players, "Players must not be null"));
        this.matches = List.copyOf(Objects.requireNonNull(matches, "Matches must not be null"));
        this.leagueTableService = new LeagueTableService();
        validateClubReferences();
    }

    public List<Club> getClubs() {
        return clubs;
    }

    public List<Club> findClubsByName(String keyword) {
        return clubs.stream()
                .filter(club -> club.matchesName(keyword))
                .toList();
    }

    public List<Player> getPlayersByClub(int clubId) {
        requireKnownClub(clubId);
        return players.stream()
                .filter(player -> player.getClubId() == clubId)
                .toList();
    }

    public List<Player> findPlayersByName(String keyword) {
        return players.stream()
                .filter(player -> player.matchesName(keyword))
                .toList();
    }

    public List<Player> getTopScorers(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must not be negative");
        }
        return players.stream()
                .sorted(TOP_SCORER_ORDER)
                .limit(limit)
                .toList();
    }

    public List<Standing> getLeagueTable() {
        return leagueTableService.calculate(clubs, matches);
    }

    private void validateClubReferences() {
        Set<Integer> clubIds = new HashSet<>();
        for (Club club : clubs) {
            if (!clubIds.add(club.getId())) {
                throw new IllegalArgumentException("Duplicate club id: " + club.getId());
            }
        }
        for (Player player : players) {
            if (!clubIds.contains(player.getClubId())) {
                throw new IllegalArgumentException(
                        "Player " + player.getId() + " references unknown club id: "
                                + player.getClubId());
            }
        }
    }

    private void requireKnownClub(int clubId) {
        boolean exists = clubs.stream().anyMatch(club -> club.getId() == clubId);
        if (!exists) {
            throw new IllegalArgumentException("Unknown club id: " + clubId);
        }
    }
}
