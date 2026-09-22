package com.premierhub.csv;

import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reads simple UTF-8 CSV whose fields do not contain commas or quotes. */
public final class MatchCsvReader {
    private static final String EXPECTED_HEADER =
            "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals";

    public List<Match> read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public List<Match> read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("CSV input stream must not be null");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return read(reader);
        }
    }

    private List<Match> read(BufferedReader reader) throws IOException {
        List<Match> matches = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        validateHeader(reader.readLine());
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            Match match = parseMatch(line, lineNumber);
            if (!ids.add(match.getId())) {
                throw invalidRow(lineNumber, "Duplicate match id: " + match.getId());
            }
            matches.add(match);
        }
        return matches;
    }

    private void validateHeader(String header) {
        if (header == null) {
            throw invalidRow(1, "Missing header " + EXPECTED_HEADER);
        }
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }
        String[] columns = splitRow(header, 1, 8);
        String normalized = String.join(",", java.util.Arrays.stream(columns)
                .map(String::strip)
                .toList());
        if (!normalized.equals(EXPECTED_HEADER)) {
            throw invalidRow(1, "Expected header " + EXPECTED_HEADER);
        }
    }

    private Match parseMatch(String line, int lineNumber) {
        String[] values = splitRow(line, lineNumber, 8);
        try {
            int id = parseInteger(values[0], "Match id", lineNumber);
            int homeClubId = parseInteger(values[1], "Home club id", lineNumber);
            int awayClubId = parseInteger(values[2], "Away club id", lineNumber);
            int matchweek = parseInteger(values[3], "Matchweek", lineNumber);
            LocalDate date = parseDate(values[4], lineNumber);
            MatchStatus status = parseStatus(values[5], lineNumber);
            Integer homeGoals = parseOptionalInteger(values[6], "Home goals", lineNumber);
            Integer awayGoals = parseOptionalInteger(values[7], "Away goals", lineNumber);
            return new Match(id, homeClubId, awayClubId, matchweek, date,
                    status, homeGoals, awayGoals);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage().startsWith("CSV line ")) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    "CSV line " + lineNumber + ": " + exception.getMessage(), exception);
        }
    }

    private MatchStatus parseStatus(String value, int lineNumber) {
        try {
            return MatchStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidRow(lineNumber, "Unknown match status: " + value.strip());
        }
    }

    private LocalDate parseDate(String value, int lineNumber) {
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "CSV line " + lineNumber + ": Date must use yyyy-MM-dd", exception);
        }
    }

    private int parseInteger(String value, String field, int lineNumber) {
        Integer result = parseOptionalInteger(value, field, lineNumber);
        if (result == null) {
            throw invalidRow(lineNumber, field + " must be a 32-bit integer");
        }
        return result;
    }

    private Integer parseOptionalInteger(String value, String field, int lineNumber) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "CSV line " + lineNumber + ": " + field + " must be a 32-bit integer", exception);
        }
    }

    private String[] splitRow(String line, int lineNumber, int expectedColumns) {
        if (line.contains("\"")) {
            throw invalidRow(lineNumber, "Quoted fields are not supported");
        }
        String[] values = line.split(",", -1);
        if (values.length != expectedColumns) {
            throw invalidRow(lineNumber, "Expected exactly " + expectedColumns + " columns");
        }
        return values;
    }

    private IllegalArgumentException invalidRow(int lineNumber, String message) {
        return new IllegalArgumentException("CSV line " + lineNumber + ": " + message);
    }
}
