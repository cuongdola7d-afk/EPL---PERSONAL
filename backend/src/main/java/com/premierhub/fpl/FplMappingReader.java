package com.premierhub.fpl;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FplMappingReader {
    public static final String TEAM_HEADER = "fpl_team_id,club_id,fpl_name,club_name";
    public static final String PLAYER_HEADER = "fpl_element_id,player_id,fpl_name,player_name,source,verified_on";
    public static final String CLASSPATH_TEAM_MAP = "data/fpl-2026/team-map.csv";
    public static final String CLASSPATH_PLAYER_MAP = "data/fpl-2026/player-map.csv";

    public record TeamMapping(int fplTeamId, int clubId, String fplName, String clubName) {
    }

    public record PlayerMapping(int fplElementId, int playerId, String fplName, String playerName,
                                String source, LocalDate verifiedOn) {
    }

    public Map<Integer, TeamMapping> readBundledTeams() throws IOException {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_TEAM_MAP);
        if (resource.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return readTeams(reader);
            }
        }
        Path local = Path.of("data/fpl-2026/team-map.csv");
        if (Files.exists(local)) {
            return readTeams(local);
        }
        return readTeams(Path.of("backend/data/fpl-2026/team-map.csv"));
    }

    public Map<Integer, PlayerMapping> readBundledPlayers() throws IOException {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_PLAYER_MAP);
        if (resource.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return readPlayers(reader);
            }
        }
        Path local = Path.of("data/fpl-2026/player-map.csv");
        if (Files.exists(local)) {
            return readPlayers(local);
        }
        return readPlayers(Path.of("backend/data/fpl-2026/player-map.csv"));
    }

    public Map<Integer, TeamMapping> readTeams(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return readTeams(reader);
        }
    }

    public Map<Integer, TeamMapping> readTeams(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(TEAM_HEADER)) {
            throw new IllegalArgumentException("team-map.csv line 1: Expected header " + TEAM_HEADER);
        }
        Map<Integer, TeamMapping> byFplId = new HashMap<>();
        Set<Integer> clubIds = new HashSet<>();
        String line;
        int number = 1;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank()) continue;
            rejectUnsafe(line, "team-map.csv", number);
            String[] fields = line.split(",", -1);
            if (fields.length != 4) {
                throw new IllegalArgumentException("team-map.csv line " + number + ": Expected 4 columns");
            }
            int fplTeamId = positiveInt(fields[0], "fpl_team_id", "team-map.csv", number);
            int clubId = positiveInt(fields[1], "club_id", "team-map.csv", number);
            String fplName = requiredText(fields[2], "fpl_name", "team-map.csv", number);
            String clubName = requiredText(fields[3], "club_name", "team-map.csv", number);
            if (byFplId.containsKey(fplTeamId)) {
                throw new IllegalArgumentException("team-map.csv line " + number + ": Duplicate fpl_team_id " + fplTeamId);
            }
            if (!clubIds.add(clubId)) {
                throw new IllegalArgumentException("team-map.csv line " + number + ": Duplicate club_id " + clubId);
            }
            byFplId.put(fplTeamId, new TeamMapping(fplTeamId, clubId, fplName, clubName));
        }
        if (byFplId.isEmpty()) {
            throw new IllegalArgumentException("team-map.csv has no mappings");
        }
        return Map.copyOf(byFplId);
    }

    public Map<Integer, PlayerMapping> readPlayers(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return readPlayers(reader);
        }
    }

    public Map<Integer, PlayerMapping> readPlayers(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(PLAYER_HEADER)) {
            throw new IllegalArgumentException("player-map.csv line 1: Expected header " + PLAYER_HEADER);
        }
        Map<Integer, PlayerMapping> byFplId = new HashMap<>();
        Set<Integer> playerIds = new HashSet<>();
        String line;
        int number = 1;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank()) continue;
            rejectUnsafe(line, "player-map.csv", number);
            String[] fields = line.split(",", -1);
            if (fields.length != 6) {
                throw new IllegalArgumentException("player-map.csv line " + number + ": Expected 6 columns");
            }
            int fplElementId = positiveInt(fields[0], "fpl_element_id", "player-map.csv", number);
            int playerId = positiveInt(fields[1], "player_id", "player-map.csv", number);
            String fplName = requiredText(fields[2], "fpl_name", "player-map.csv", number);
            String playerName = requiredText(fields[3], "player_name", "player-map.csv", number);
            String source = requiredText(fields[4], "source", "player-map.csv", number);
            LocalDate verifiedOn = LocalDate.parse(requiredText(fields[5], "verified_on", "player-map.csv", number));
            if (byFplId.containsKey(fplElementId)) {
                throw new IllegalArgumentException("player-map.csv line " + number
                        + ": Duplicate fpl_element_id " + fplElementId);
            }
            if (!playerIds.add(playerId)) {
                throw new IllegalArgumentException("player-map.csv line " + number
                        + ": Duplicate player_id " + playerId);
            }
            byFplId.put(fplElementId, new PlayerMapping(fplElementId, playerId, fplName, playerName, source, verifiedOn));
        }
        return Map.copyOf(byFplId);
    }

    private static void rejectUnsafe(String line, String file, int number) {
        if (line.contains("\"") || line.contains("\t")) {
            throw new IllegalArgumentException(file + " line " + number + ": Quoted fields and tabs are not supported");
        }
    }

    private static int positiveInt(String raw, String field, String file, int number) {
        try {
            int value = Integer.parseInt(raw.strip());
            if (value <= 0) {
                throw new IllegalArgumentException(file + " line " + number + ": " + field + " must be positive");
            }
            return value;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(file + " line " + number + ": " + field + " must be an integer", error);
        }
    }

    private static String requiredText(String raw, String field, String file, int number) {
        String value = raw.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(file + " line " + number + ": " + field + " must not be blank");
        }
        return value;
    }
}
