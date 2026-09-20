package com.premierhub.service;

import com.premierhub.model.Club;
import com.premierhub.model.Match;
import com.premierhub.model.Standing;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LeagueTableService {
    private static final Comparator<Standing> TABLE_ORDER =
            Comparator.comparingInt(Standing::getPoints).reversed()
                    .thenComparing(Comparator.comparingInt(
                            Standing::getGoalDifference).reversed())
                    .thenComparing(Comparator.comparingInt(
                            Standing::getGoalsFor).reversed())
                    .thenComparing(standing -> standing.getClub().getName());

    public List<Standing> calculate(List<Club> clubs, List<Match> matches) {
        Objects.requireNonNull(clubs, "Clubs must not be null");
        Objects.requireNonNull(matches, "Matches must not be null");

        Map<Integer, MutableStanding> table = new LinkedHashMap<>();
        for (Club club : clubs) {
            Objects.requireNonNull(club, "Club list must not contain null");
            if (table.putIfAbsent(club.getId(), new MutableStanding(club)) != null) {
                throw new IllegalArgumentException("Duplicate club id: " + club.getId());
            }
        }

        for (Match match : matches) {
            Objects.requireNonNull(match, "Match list must not contain null");
            if (!match.isFinished()) {
                continue;
            }
            MutableStanding home = findClub(table, match.getHomeClubId(), match.getId());
            MutableStanding away = findClub(table, match.getAwayClubId(), match.getId());
            home.record(match.getHomeGoals(), match.getAwayGoals());
            away.record(match.getAwayGoals(), match.getHomeGoals());
        }

        return table.values().stream()
                .map(MutableStanding::toStanding)
                .sorted(TABLE_ORDER)
                .toList();
    }

    private MutableStanding findClub(Map<Integer, MutableStanding> table,
                                     int clubId, int matchId) {
        MutableStanding standing = table.get(clubId);
        if (standing == null) {
            throw new IllegalArgumentException(
                    "Match " + matchId + " references unknown club id: " + clubId);
        }
        return standing;
    }

    private static final class MutableStanding {
        private final Club club;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;

        private MutableStanding(Club club) {
            this.club = club;
        }

        private void record(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) {
                wins++;
            } else if (scored == conceded) {
                draws++;
            } else {
                losses++;
            }
        }

        private Standing toStanding() {
            return new Standing(club, played, wins, draws, losses,
                    goalsFor, goalsAgainst);
        }
    }
}
