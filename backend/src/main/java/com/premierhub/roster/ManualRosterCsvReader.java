package com.premierhub.roster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Strict UTF-8 CSV for manually verified player identities. No quoted fields. */
public final class ManualRosterCsvReader {
    private static final String HEADER = "season,club_id,player_id,name,fantasy_position";
    private static final int FIRST_MANUAL_ID = 2_000_000_000;
    private static final int LAST_MANUAL_ID = 2_099_999_999;

    public record Row(int line, int season, int clubId, int playerId, String name, String position) { }

    public List<Row> read(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return readRows(reader);
        }
    }

    public List<Row> read(Reader input) throws IOException {
        return readRows(input instanceof BufferedReader buffered ? buffered : new BufferedReader(input));
    }

    private List<Row> readRows(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(HEADER)) {
            throw invalid(1, "Expected header " + HEADER);
        }
        List<Row> rows = new ArrayList<>();
        Set<String> memberships = new HashSet<>();
        Map<Integer, String> identities = new HashMap<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) continue;
            if (line.contains("\"") || line.contains("\t")) {
                throw invalid(lineNumber, "Quoted fields and tabs are not supported");
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 5) throw invalid(lineNumber, "Expected exactly 5 columns");
            int season = number(fields[0], "season", lineNumber);
            int clubId = number(fields[1], "club_id", lineNumber);
            int playerId = number(fields[2], "player_id", lineNumber);
            String name = fields[3].strip();
            String code = fields[4].strip().toUpperCase(Locale.ROOT);
            if (season != 2026) throw invalid(lineNumber, "Only season 2026 is supported");
            if (clubId <= 0) throw invalid(lineNumber, "club_id must be positive");
            if (playerId < FIRST_MANUAL_ID || playerId > LAST_MANUAL_ID) {
                throw invalid(lineNumber, "player_id must be in 2000000000..2099999999");
            }
            if (name.isEmpty() || name.length() > 200) throw invalid(lineNumber, "name must contain 1..200 characters");
            String position = switch (code) {
                case "GK" -> "GOALKEEPER";
                case "DEF" -> "DEFENDER";
                case "MID" -> "MIDFIELDER";
                case "FWD" -> "FORWARD";
                default -> throw invalid(lineNumber, "Unknown fantasy_position: " + code);
            };
            if (!memberships.add(season + ":" + clubId + ":" + playerId)) {
                throw invalid(lineNumber, "Duplicate player_id for the same season and club: " + playerId);
            }
            String previousName = identities.putIfAbsent(playerId, name);
            if (previousName != null && !previousName.equals(name)) {
                throw invalid(lineNumber, "One player_id has conflicting names: " + playerId);
            }
            rows.add(new Row(lineNumber, season, clubId, playerId, name, position));
        }
        if (rows.isEmpty()) throw invalid(2, "File has no player rows");
        return List.copyOf(rows);
    }

    private static int number(String raw, String field, int line) {
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException invalidNumber) {
            throw invalid(line, field + " must be a 32-bit integer");
        }
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException("CSV line " + line + ": " + message);
    }
}
