package com.premierhub.profiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads the reviewed 2026/27 profile format. An empty cell represents SQL NULL. */
public final class PlayerProfileCsvReader {
    private static final String HEADER = "player_id,club_id,nationality,birth_date,height_cm,preferred_foot,shirt_number,fc27_overall";

    public record Row(int line, int playerId, int clubId, String nationality, LocalDate birthDate,
                      Integer heightCm, String preferredFoot, Integer shirtNumber, Integer fc27Overall) { }

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
        if (header == null || !header.replaceFirst("^\\uFEFF", "").strip().equals(HEADER)) {
            throw invalid(1, "Expected header " + HEADER);
        }
        List<Row> rows = new ArrayList<>();
        Set<Integer> playerIds = new HashSet<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) continue;
            if (line.contains("\"") || line.contains("\t")) {
                throw invalid(lineNumber, "Quoted fields and tabs are not supported");
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 8) throw invalid(lineNumber, "Expected exactly 8 columns");
            int playerId = requiredNumber(fields[0], "player_id", lineNumber);
            int clubId = requiredNumber(fields[1], "club_id", lineNumber);
            if (playerId <= 0 || clubId <= 0) throw invalid(lineNumber, "IDs must be positive");
            if (!playerIds.add(playerId)) throw invalid(lineNumber, "Duplicate player_id: " + playerId);

            String nationality = optionalText(fields[2], "nationality", lineNumber);
            LocalDate birthDate = optionalDate(fields[3], lineNumber);
            Integer heightCm = optionalNumber(fields[4], "height_cm", 100, 250, lineNumber);
            String preferredFoot = fields[5].strip();
            if (preferredFoot.isEmpty()) preferredFoot = null;
            else if (!Set.of("LEFT", "RIGHT", "BOTH").contains(preferredFoot)) {
                throw invalid(lineNumber, "preferred_foot must be LEFT, RIGHT, BOTH or blank");
            }
            Integer shirtNumber = optionalNumber(fields[6], "shirt_number", 1, 99, lineNumber);
            Integer fc27Overall = optionalNumber(fields[7], "fc27_overall", 1, 99, lineNumber);
            rows.add(new Row(lineNumber, playerId, clubId, nationality, birthDate, heightCm,
                    preferredFoot, shirtNumber, fc27Overall));
        }
        if (rows.isEmpty()) throw invalid(2, "File has no player rows");
        return List.copyOf(rows);
    }

    private static int requiredNumber(String raw, String field, int line) {
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException exception) {
            throw invalid(line, field + " must be a 32-bit integer");
        }
    }

    private static Integer optionalNumber(String raw, String field, int min, int max, int line) {
        if (raw.isBlank()) return null;
        int value = requiredNumber(raw, field, line);
        if (value < min || value > max) throw invalid(line, field + " must be " + min + ".." + max + " or blank");
        return value;
    }

    private static String optionalText(String raw, String field, int line) {
        String value = raw.strip();
        if (value.isEmpty()) return null;
        if (value.length() > 100 || !value.matches("[\\p{L}\\p{M} .'-]+")) {
            throw invalid(line, field + " must be a country name of at most 100 letters");
        }
        return value;
    }

    private static LocalDate optionalDate(String raw, int line) {
        String value = raw.strip();
        if (value.isEmpty()) return null;
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw invalid(line, "birth_date must be YYYY-MM-DD or blank");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid(line, "birth_date is not a valid date");
        }
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException("CSV line " + line + ": " + message);
    }
}
