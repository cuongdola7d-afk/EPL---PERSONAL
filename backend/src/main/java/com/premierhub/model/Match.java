package com.premierhub.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.OptionalInt;

public final class Match {
    private final int id;
    private final int homeClubId;
    private final int awayClubId;
    private final LocalDate matchDate;
    private final MatchStatus status;
    private final Integer homeGoals;
    private final Integer awayGoals;

    public Match(int id, int homeClubId, int awayClubId, LocalDate matchDate,
                 MatchStatus status, Integer homeGoals, Integer awayGoals) {
        if (id <= 0) {
            throw new IllegalArgumentException("Match id must be positive");
        }
        if (homeClubId <= 0 || awayClubId <= 0) {
            throw new IllegalArgumentException("Club ids must be positive");
        }
        if (homeClubId == awayClubId) {
            throw new IllegalArgumentException("A club cannot play against itself");
        }
        this.matchDate = Objects.requireNonNull(matchDate, "Match date must not be null");
        this.status = Objects.requireNonNull(status, "Match status must not be null");
        validateScore(status, homeGoals, awayGoals);
        this.id = id;
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    private static void validateScore(MatchStatus status, Integer homeGoals, Integer awayGoals) {
        if (status == MatchStatus.SCHEDULED && (homeGoals != null || awayGoals != null)) {
            throw new IllegalArgumentException("A scheduled match must not have a score");
        }
        if (status == MatchStatus.FINISHED) {
            if (homeGoals == null || awayGoals == null) {
                throw new IllegalArgumentException("A finished match must have both scores");
            }
            if (homeGoals < 0 || awayGoals < 0) {
                throw new IllegalArgumentException("Scores must not be negative");
            }
        }
    }

    public int getId() {
        return id;
    }

    public int getHomeClubId() {
        return homeClubId;
    }

    public int getAwayClubId() {
        return awayClubId;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    public boolean isDraw() {
        return isFinished() && homeGoals.equals(awayGoals);
    }

    public OptionalInt getWinnerClubId() {
        if (!isFinished() || isDraw()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(homeGoals > awayGoals ? homeClubId : awayClubId);
    }

    public OptionalInt getLoserClubId() {
        if (!isFinished() || isDraw()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(homeGoals < awayGoals ? homeClubId : awayClubId);
    }

    public int getPointsFor(int clubId) {
        if (clubId != homeClubId && clubId != awayClubId) {
            throw new IllegalArgumentException("Club did not participate in this match: " + clubId);
        }
        if (!isFinished()) {
            throw new IllegalStateException("Points are unavailable for a scheduled match");
        }
        if (isDraw()) {
            return 1;
        }
        return getWinnerClubId().orElseThrow() == clubId ? 3 : 0;
    }
}
