package com.premierhub.model;

import java.util.Locale;
import java.util.Objects;

public final class Player {
    private final int id;
    private final String name;
    private final int clubId;
    private final Position position;
    private final int goals;
    private final int assists;

    public Player(int id, String name, int clubId, Position position, int goals, int assists) {
        if (id <= 0) {
            throw new IllegalArgumentException("Player id must be positive");
        }
        if (clubId <= 0) {
            throw new IllegalArgumentException("Club id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        if (goals < 0 || assists < 0) {
            throw new IllegalArgumentException("Goals and assists must not be negative");
        }
        this.id = id;
        this.name = name.strip();
        this.clubId = clubId;
        this.position = Objects.requireNonNull(position, "Position must not be null");
        this.goals = goals;
        this.assists = assists;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getClubId() {
        return clubId;
    }

    public Position getPosition() {
        return position;
    }

    public int getGoals() {
        return goals;
    }

    public int getAssists() {
        return assists;
    }

    public boolean matchesName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        String normalizedKeyword = keyword.strip().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }
}
