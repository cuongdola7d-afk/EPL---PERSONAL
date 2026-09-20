package com.premierhub.model;

import java.util.Objects;

public final class Standing {
    private final Club club;
    private final int played;
    private final int wins;
    private final int draws;
    private final int losses;
    private final int goalsFor;
    private final int goalsAgainst;
    private final int goalDifference;
    private final int points;

    public Standing(Club club, int played, int wins, int draws, int losses,
                    int goalsFor, int goalsAgainst) {
        this.club = Objects.requireNonNull(club, "Club must not be null");
        if (played < 0 || wins < 0 || draws < 0 || losses < 0
                || goalsFor < 0 || goalsAgainst < 0) {
            throw new IllegalArgumentException("Standing values must not be negative");
        }
        if (played != wins + draws + losses) {
            throw new IllegalArgumentException("Played must equal wins + draws + losses");
        }
        this.played = played;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDifference = goalsFor - goalsAgainst;
        this.points = wins * 3 + draws;
    }

    public Club getClub() {
        return club;
    }

    public int getPlayed() {
        return played;
    }

    public int getWins() {
        return wins;
    }

    public int getDraws() {
        return draws;
    }

    public int getLosses() {
        return losses;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public int getGoalDifference() {
        return goalDifference;
    }

    public int getPoints() {
        return points;
    }
}
