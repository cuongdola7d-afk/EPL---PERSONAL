package com.premierhub.clubs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Empty cells mean SQL NULL; this format contains no quoted/comma-bearing names. */
public final class ClubInformationCsvReader {
    public static final String HEADER = "club_id,season_year,manager_name,manager_status,stadium_name,verified_on";
    public record Row(int line, int clubId, String managerName, String managerStatus,
                      String stadiumName, LocalDate verifiedOn) { }

    public List<Row> read(Path file) throws IOException {
        try (var input = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(input);
        }
    }

    public List<Row> read(Reader input) throws IOException {
        var reader = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
        String header = reader.readLine();
        if (header == null || !header.replaceFirst("^\\uFEFF", "").equals(HEADER)) {
            throw invalid(1, "Expected header " + HEADER);
        }
        var rows = new ArrayList<Row>();
        var ids = new HashSet<Integer>();
        String line;
        int number = 1;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank()) continue;
            String[] fields = line.split(",", -1);
            if (fields.length != 6 || line.contains("\"") || line.contains("\t")) {
                throw invalid(number, "Expected 6 unquoted CSV columns");
            }
            int id;
            try { id = Integer.parseInt(fields[0]); }
            catch (NumberFormatException error) { throw invalid(number, "Invalid club_id"); }
            if (id <= 0 || !ids.add(id)) throw invalid(number, "Invalid or duplicate club_id=" + id);
            if (!fields[1].equals("2026")) throw invalid(number, "Only season_year=2026 is supported");
            String manager = text(fields[2], number), status = text(fields[3], number);
            String stadium = text(fields[4], number);
            if ((manager == null) != (status == null) || status != null && !Set.of("PERMANENT", "INTERIM").contains(status)) {
                throw invalid(number, "manager_status must be PERMANENT/INTERIM with a manager, or both blank");
            }
            LocalDate date;
            try {
                if (!fields[5].matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
                date = LocalDate.parse(fields[5]);
                if (date.isBefore(LocalDate.of(2026, 7, 1)) || date.isAfter(LocalDate.of(2027, 6, 30))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException | java.time.DateTimeException error) { throw invalid(number, "verified_on must be a valid date in 2026/27"); }
            rows.add(new Row(number, id, manager, status, stadium, date));
        }
        if (rows.isEmpty()) throw invalid(2, "No club rows");
        return List.copyOf(rows);
    }

    private static String text(String raw, int line) {
        String value = raw.strip();
        if (value.isEmpty()) return null;
        if (value.length() > 200 || value.equals("NULL") || value.chars().anyMatch(Character::isISOControl)) {
            throw invalid(line, "Use a name up to 200 characters, or a blank cell for NULL");
        }
        return value;
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException("CSV line " + line + ": " + message);
    }
}
