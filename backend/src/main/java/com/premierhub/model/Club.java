package com.premierhub.model;

import java.util.Locale;

public final class Club {
    private final int id;
    private final String name;
    private final String city;

    public Club(int id, String name, String city) {
        if (id <= 0) {
            throw new IllegalArgumentException("Club id must be positive");
        }
        this.id = id;
        this.name = requireText(name, "Club name");
        this.city = requireText(city, "Club city");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public boolean matchesName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        String normalizedKeyword = keyword.strip().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }
}
