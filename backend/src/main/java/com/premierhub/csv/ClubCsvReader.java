package com.premierhub.csv;

import com.premierhub.model.Club;

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
import java.util.Set;

/**
 * Reads UTF-8 CSV with header id,name,city and unquoted, single-line values.
 * Blank data lines are ignored; malformed rows and duplicate IDs are rejected.
 */
public final class ClubCsvReader {
    public List<Club> read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public List<Club> read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("CSV input stream must not be null");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return read(reader);
        }
    }

    private List<Club> read(BufferedReader reader) throws IOException {
        List<Club> clubs = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();

        String header = reader.readLine();
        if (header == null) {
            throw invalidRow(1, "Missing header id,name,city");
        }
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }
        String[] columns = splitRow(header, 1);
        if (!columns[0].strip().equals("id")
                || !columns[1].strip().equals("name")
                || !columns[2].strip().equals("city")) {
            throw invalidRow(1, "Expected header id,name,city");
        }

        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            Club club = parseClub(line, lineNumber);
            if (!ids.add(club.getId())) {
                throw invalidRow(lineNumber, "Duplicate club id: " + club.getId());
            }
            clubs.add(club);
        }
        return clubs;
    }

    private Club parseClub(String line, int lineNumber) {
        String[] values = splitRow(line, lineNumber);
        int id;
        try {
            id = Integer.parseInt(values[0].strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "CSV line " + lineNumber + ": Club id must be a 32-bit integer", exception);
        }

        try {
            return new Club(id, values[1], values[2]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "CSV line " + lineNumber + ": " + exception.getMessage(), exception);
        }
    }

    private String[] splitRow(String line, int lineNumber) {
        if (line.contains("\"")) {
            throw invalidRow(lineNumber, "Quoted fields are not supported");
        }
        // Preserve empty trailing fields so Club can reject a missing city.
        String[] values = line.split(",", -1);
        if (values.length != 3) {
            throw invalidRow(lineNumber, "Expected exactly 3 columns: id,name,city");
        }
        return values;
    }

    private IllegalArgumentException invalidRow(int lineNumber, String message) {
        return new IllegalArgumentException("CSV line " + lineNumber + ": " + message);
    }
}
