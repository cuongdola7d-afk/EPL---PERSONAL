package com.premierhub.manualstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict UTF-8 CSV for manually sourced 2026/27 player-match statistics. */
public final class ManualMatchStatsCsvReader {
    public static final String HEADER = "season,fixture_id,player_id,status,rating,minutes,goals,assists,"
            + "yellow_cards,red_cards,source_url,checked_at";

    public record Row(int line, int season, int fixtureId, int playerId, String status,
                      BigDecimal rating, Integer minutes, Integer goals, Integer assists,
                      Integer yellowCards, Integer redCards, String sourceUrl, String checkedAt) {
        public BigDecimal fantasyPoints() {
            return status.equals("DID_NOT_PLAY") ? BigDecimal.ZERO : rating;
        }
    }

    public List<Row> read(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(HEADER)) {
                throw invalid(1, "Expected header " + HEADER);
            }
            List<Row> rows = new ArrayList<>();
            Set<String> keys = new HashSet<>();
            String line;
            int number = 1;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.isBlank()) continue;
                if (line.contains("\"") || line.contains("\t")) {
                    throw invalid(number, "Quoted fields and tabs are not supported");
                }
                String[] fields = line.split(",", -1);
                if (fields.length != 12) throw invalid(number, "Expected exactly 12 columns");
                int season = requiredInt(fields[0], "season", number);
                int fixtureId = requiredInt(fields[1], "fixture_id", number);
                int playerId = requiredInt(fields[2], "player_id", number);
                String status = fields[3].strip();
                BigDecimal rating = rating(fields[4], number);
                Integer minutes = optionalInt(fields[5], "minutes", number, 180);
                Integer goals = optionalInt(fields[6], "goals", number, 20);
                Integer assists = optionalInt(fields[7], "assists", number, 20);
                Integer yellow = optionalInt(fields[8], "yellow_cards", number, 2);
                Integer red = optionalInt(fields[9], "red_cards", number, 1);
                String sourceUrl = sourceUrl(fields[10], number);
                String checkedAt = checkedAt(fields[11], number);
                if (season != 2026) throw invalid(number, "Only season 2026 is supported");
                if (fixtureId <= 0 || playerId <= 0) {
                    throw invalid(number, "fixture_id and player_id must be positive");
                }
                if (!status.equals("PLAYED") && !status.equals("DID_NOT_PLAY")) {
                    throw invalid(number, "status must be PLAYED or DID_NOT_PLAY");
                }
                if (status.equals("PLAYED") && minutes != null && minutes == 0) {
                    throw invalid(number, "PLAYED minutes must be positive or blank");
                }
                if (status.equals("DID_NOT_PLAY") && (rating != null || minutes == null || minutes != 0
                        || positive(goals) || positive(assists) || positive(yellow) || positive(red))) {
                    throw invalid(number, "DID_NOT_PLAY requires blank rating, 0 minutes and no positive events");
                }
                if (!keys.add(fixtureId + ":" + playerId)) {
                    throw invalid(number, "Duplicate fixture_id and player_id in one file");
                }
                rows.add(new Row(number, season, fixtureId, playerId, status, rating, minutes,
                        goals, assists, yellow, red, sourceUrl, checkedAt));
            }
            if (rows.isEmpty()) throw invalid(2, "File has no player-match rows");
            return List.copyOf(rows);
        }
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private static int requiredInt(String raw, String field, int line) {
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException error) {
            throw invalid(line, field + " must be a 32-bit integer");
        }
    }

    private static Integer optionalInt(String raw, String field, int line, int maximum) {
        if (raw.isBlank()) return null;
        int value = requiredInt(raw, field, line);
        if (value < 0 || value > maximum) {
            throw invalid(line, field + " must be between 0 and " + maximum + " or blank");
        }
        return value;
    }

    private static BigDecimal rating(String raw, int line) {
        if (raw.isBlank()) return null;
        String value = raw.strip();
        if (!value.matches("(?:[0-9]|10)(?:\\.[0-9]{1,2})?")) {
            throw invalid(line, "rating must be 0..10 with at most two decimal places or blank");
        }
        BigDecimal rating = new BigDecimal(value);
        if (rating.compareTo(BigDecimal.TEN) > 0) {
            throw invalid(line, "rating must not exceed 10");
        }
        return rating.setScale(2);
    }

    private static String sourceUrl(String raw, int line) {
        String value = raw.strip();
        if (value.length() > 1024) throw invalid(line, "source_url is too long");
        try {
            URI uri = URI.create(value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw invalid(line, "source_url must be an absolute HTTP(S) URL without credentials");
            }
            return value;
        } catch (IllegalArgumentException error) {
            throw invalid(line, "source_url must be an absolute HTTP(S) URL without credentials");
        }
    }

    private static String checkedAt(String raw, int line) {
        String value = raw.strip();
        if (!value.endsWith("Z")) throw invalid(line, "checked_at must be an ISO-8601 UTC instant ending in Z");
        try {
            return Instant.parse(value).toString();
        } catch (DateTimeParseException error) {
            throw invalid(line, "checked_at must be an ISO-8601 UTC instant ending in Z");
        }
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException("CSV line " + line + ": " + message);
    }
}
