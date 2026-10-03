package com.premierhub.positions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A reviewed row replaces one player's complete set of eligible 2026/27 positions. */
public final class PlayerPositionCsvReader {
    public static final String HEADER = "player_id,season_year,expected_primary_position,"
            + "expected_eligible_positions,primary_position,eligible_positions";
    public static final Set<String> CODES = Set.of("GK", "LB", "CB", "RB", "CM", "CAM",
            "LM", "RM", "LW", "ST", "RW");

    public record PositionSet(String primary, Set<String> eligible) { }
    public record Row(int line, int playerId, PositionSet expected, PositionSet incoming) { }

    public List<Row> read(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public List<Row> read(Reader input) throws IOException {
        BufferedReader reader = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
        String header = reader.readLine();
        if (header == null || !HEADER.equals(header.replaceFirst("^\\uFEFF", "").strip())) {
            throw invalid(1, "expected header " + HEADER);
        }
        List<Row> rows = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        String line;
        int number = 1;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank()) continue;
            if (line.contains("\"") || line.contains("\t")) throw invalid(number, "quotes and tabs are not supported");
            String[] fields = line.split(",", -1);
            if (fields.length != 6) throw invalid(number, "expected exactly 6 columns");
            int playerId;
            int season;
            try {
                playerId = Integer.parseInt(fields[0].strip());
                season = Integer.parseInt(fields[1].strip());
            } catch (NumberFormatException exception) {
                throw invalid(number, "player_id and season_year must be integers");
            }
            if (playerId <= 0 || season != 2026) throw invalid(number, "positive player_id and season_year=2026 required");
            if (!ids.add(playerId)) throw invalid(number, "duplicate player_id " + playerId);
            PositionSet expected = positionSet(fields[2], fields[3], number, true);
            PositionSet incoming = positionSet(fields[4], fields[5], number, false);
            rows.add(new Row(number, playerId, expected, incoming));
        }
        if (rows.isEmpty()) throw invalid(2, "file has no player rows");
        return List.copyOf(rows);
    }

    private static PositionSet positionSet(String rawPrimary, String rawEligible, int line, boolean optional) {
        String primary = rawPrimary.strip();
        String eligibleText = rawEligible.strip();
        if (optional && primary.isEmpty() && eligibleText.isEmpty()) return null;
        if (!CODES.contains(primary)) throw invalid(line, "invalid primary position " + primary);
        Set<String> eligible = new LinkedHashSet<>();
        for (String part : eligibleText.split("\\|", -1)) {
            String code = part.strip();
            if (!CODES.contains(code)) throw invalid(line, "invalid eligible position " + code);
            if (!eligible.add(code)) throw invalid(line, "duplicate eligible position " + code);
        }
        if (!eligible.contains(primary)) throw invalid(line, "primary position must be eligible");
        return new PositionSet(primary, Set.copyOf(eligible));
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException("CSV line " + line + ": " + message);
    }
}
