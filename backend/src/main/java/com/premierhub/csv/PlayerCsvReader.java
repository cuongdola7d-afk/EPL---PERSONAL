package com.premierhub.csv;

import com.premierhub.model.Player;
import com.premierhub.model.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reads simple UTF-8 CSV whose fields do not contain commas or quotes. */
public final class PlayerCsvReader {
    private static final String EXPECTED_HEADER = "id,name,clubId,position,goals,assists";

    public List<Player> read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public List<Player> read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("CSV input stream must not be null");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return read(reader);
        }
    }

    private List<Player> read(BufferedReader reader) throws IOException {
        List<Player> players = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
            validateHeader(reader.readLine());
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                Player player = parsePlayer(line, lineNumber);
                if (!ids.add(player.getId())) {
                    throw invalidRow(lineNumber, "Duplicate player id: " + player.getId());
                }
                players.add(player);
            }
        return players;
    }

    private void validateHeader(String header) {
        if (header == null) {
            throw invalidRow(1, "Missing header " + EXPECTED_HEADER);
        }
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }
        String[] columns = splitRow(header, 1, 6);
        String normalized = String.join(",", java.util.Arrays.stream(columns)
                .map(String::strip)
                .toList());
        if (!normalized.equals(EXPECTED_HEADER)) {
            throw invalidRow(1, "Expected header " + EXPECTED_HEADER);
        }
    }

    private Player parsePlayer(String line, int lineNumber) {
        String[] values = splitRow(line, lineNumber, 6);
        try {
            int id = parseInteger(values[0], "Player id", lineNumber);
            int clubId = parseInteger(values[2], "Club id", lineNumber);
            Position position;
            try {
                position = Position.valueOf(values[3].strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw invalidRow(lineNumber, "Unknown position: " + values[3].strip());
            }
            int goals = parseInteger(values[4], "Goals", lineNumber);
            int assists = parseInteger(values[5], "Assists", lineNumber);
            return new Player(id, values[1], clubId, position, goals, assists);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage().startsWith("CSV line ")) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    "CSV line " + lineNumber + ": " + exception.getMessage(), exception);
        }
    }

    private int parseInteger(String value, String field, int lineNumber) {
        try {
            return Integer.parseInt(value.strip());
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
