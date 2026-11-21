package com.premierhub.fpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict UTF-8 CSV reader for FPL extended per-match metrics (GW11+ 2026/27). */
public final class ManualExtraStatsCsvReader {
    public static final String HEADER =
            "season,fixture_id,player_id,starts,clean_sheets,goals_conceded,own_goals,"
            + "penalties_saved,penalties_missed,saves,bonus,bps,"
            + "clearances_blocks_interceptions,recoveries,tackles,defensive_contribution,"
            + "expected_goals,expected_assists,expected_goal_involvements,expected_goals_conceded,"
            + "influence,creativity,threat,ict_index,source,fetched_at";

    private static final BigDecimal MAX_DECIMAL_5_2 = new BigDecimal("999.99");
    private static final BigDecimal MAX_DECIMAL_6_1 = new BigDecimal("99999.9");

    public record Row(
            int line,
            int season,
            int fixtureId,
            int playerId,
            Integer starts,
            Integer cleanSheets,
            Integer goalsConceded,
            Integer ownGoals,
            Integer penaltiesSaved,
            Integer penaltiesMissed,
            Integer saves,
            Integer bonus,
            Integer bps,
            Integer clearancesBlocksInterceptions,
            Integer recoveries,
            Integer tackles,
            Integer defensiveContribution,
            BigDecimal expectedGoals,
            BigDecimal expectedAssists,
            BigDecimal expectedGoalInvolvements,
            BigDecimal expectedGoalsConceded,
            BigDecimal influence,
            BigDecimal creativity,
            BigDecimal threat,
            BigDecimal ictIndex,
            String source,
            Instant fetchedAt
    ) {
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
                String[] f = line.split(",", -1);
                if (f.length != 26) {
                    throw invalid(number, "Expected exactly 26 columns, got " + f.length);
                }
                int season = requiredInt(f[0], "season", number);
                int fixtureId = requiredInt(f[1], "fixture_id", number);
                int playerId = requiredInt(f[2], "player_id", number);
                if (season != 2026) throw invalid(number, "Only season 2026 is supported");
                if (fixtureId <= 0 || playerId <= 0) {
                    throw invalid(number, "fixture_id and player_id must be positive");
                }
                if (!keys.add(fixtureId + ":" + playerId)) {
                    throw invalid(number, "Duplicate fixture_id and player_id in one file");
                }
                String source = f[24].strip();
                if (!source.equals("FPL")) {
                    throw invalid(number, "source must be FPL");
                }
                Instant fetchedAt = requiredInstant(f[25], number);
                rows.add(new Row(
                        number, season, fixtureId, playerId,
                        optionalInt(f[3], "starts", number, 0, 1),
                        optionalInt(f[4], "clean_sheets", number, 0, 1),
                        optionalInt(f[5], "goals_conceded", number, 0, 30),
                        optionalInt(f[6], "own_goals", number, 0, 10),
                        optionalInt(f[7], "penalties_saved", number, 0, 10),
                        optionalInt(f[8], "penalties_missed", number, 0, 10),
                        optionalInt(f[9], "saves", number, 0, 50),
                        optionalInt(f[10], "bonus", number, 0, 3),
                        optionalInt(f[11], "bps", number, -200, 500),
                        optionalInt(f[12], "clearances_blocks_interceptions", number, 0, 100),
                        optionalInt(f[13], "recoveries", number, 0, 100),
                        optionalInt(f[14], "tackles", number, 0, 100),
                        optionalInt(f[15], "defensive_contribution", number, 0, 200),
                        optionalDecimal(f[16], "expected_goals", number, 2, MAX_DECIMAL_5_2),
                        optionalDecimal(f[17], "expected_assists", number, 2, MAX_DECIMAL_5_2),
                        optionalDecimal(f[18], "expected_goal_involvements", number, 2, MAX_DECIMAL_5_2),
                        optionalDecimal(f[19], "expected_goals_conceded", number, 2, MAX_DECIMAL_5_2),
                        optionalDecimal(f[20], "influence", number, 1, MAX_DECIMAL_6_1),
                        optionalDecimal(f[21], "creativity", number, 1, MAX_DECIMAL_6_1),
                        optionalDecimal(f[22], "threat", number, 1, MAX_DECIMAL_6_1),
                        optionalDecimal(f[23], "ict_index", number, 1, MAX_DECIMAL_6_1),
                        source,
                        fetchedAt
                ));
            }
            if (rows.isEmpty()) throw invalid(2, "File has no extra-stats rows");
            return List.copyOf(rows);
        }
    }

    private static int requiredInt(String raw, String field, int line) {
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException error) {
            throw invalid(line, field + " must be a 32-bit integer");
        }
    }

    private static Integer optionalInt(String raw, String field, int line, int min, int max) {
        if (raw.isBlank()) return null;
        int value = requiredInt(raw, field, line);
        if (value < min || value > max) {
            throw invalid(line, field + " must be between " + min + " and " + max + " or blank");
        }
        return value;
    }

    private static BigDecimal optionalDecimal(String raw, String field, int line, int scale, BigDecimal maxAbs) {
        if (raw.isBlank()) return null;
        BigDecimal value;
        try {
            value = new BigDecimal(raw.strip());
        } catch (NumberFormatException error) {
            throw invalid(line, field + " must be a decimal number or blank");
        }
        if (value.scale() > scale || value.abs().compareTo(maxAbs) > 0) {
            throw invalid(line, field + " out of range or scale>" + scale);
        }
        return value.setScale(scale);
    }

    private static Instant requiredInstant(String raw, int line) {
        if (raw.isBlank()) throw invalid(line, "fetched_at must not be blank");
        try {
            return Instant.parse(raw.strip());
        } catch (DateTimeParseException error) {
            throw invalid(line, "fetched_at must be ISO-8601 UTC instant");
        }
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException("CSV line " + line + ": " + message);
    }
}
