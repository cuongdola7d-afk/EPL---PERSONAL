package com.premierhub.lineups;

import com.premierhub.web.dto.MatchLineupResponse;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Previously collected evidence, packaged without importing or changing database rows. */
final class BundledMatchLineups {
    record ClubDefault(String formation, LocalDate updatedOn, int from, int to, int count,
                       String frequencies, String ids, String sourceNote) { }
    record TeamEvidence(LocalDate verifiedOn, String sourceNote, List<MatchLineupResponse.Player> players) { }
    private record Team(int fixtureId, int clubId) { }

    private final Map<Integer, ClubDefault> defaults = new HashMap<>();
    private final Map<Team, TeamEvidence> teams = new HashMap<>();

    BundledMatchLineups() {
        for (String[] row : rows("clubs.csv", LineupBatchImporter.CLUBS)) {
            require(row[1].equals("2026") && row[9].equals("USER") && !row[10].isBlank(), "Invalid bundled club default source");
            Formation.lines(row[2]);
            var value = new ClubDefault(row[2], LocalDate.parse(row[3]), Integer.parseInt(row[4]),
                    Integer.parseInt(row[5]), Integer.parseInt(row[6]), row[7], row[8], row[10]);
            require(defaults.putIfAbsent(Integer.parseInt(row[0]), value) == null, "Repeated bundled club");
        }
        var groups = new HashMap<Team, List<MatchLineupResponse.Player>>();
        var dates = new HashMap<Team, LocalDate>();
        var sources = new HashMap<Team, String>();
        for (String[] row : rows("players.csv", LineupBatchImporter.PLAYERS)) {
            require(row[2].equals("2026") && !row[11].isBlank(), "Invalid bundled role source");
            // This fallback reads roles only. Fixture positions and substitution times are read from verified SQL metadata.
            var key = new Team(Integer.parseInt(row[0]), Integer.parseInt(row[1]));
            LocalDate date = LocalDate.parse(row[10]);
            require(!dates.containsKey(key) || dates.get(key).equals(date), "Inconsistent bundled role dates");
            require(!sources.containsKey(key) || sources.get(key).equals(row[11]), "Inconsistent bundled role sources");
            dates.put(key, date);
            sources.put(key, row[11]);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new MatchLineupResponse.Player(
                    Integer.parseInt(row[3]), row[4], null, null, null, null, null, null));
        }
        groups.forEach((key, players) -> teams.put(key, new TeamEvidence(dates.get(key), sources.get(key), List.copyOf(players))));
    }

    ClubDefault clubDefault(int clubId) { return defaults.get(clubId); }
    TeamEvidence team(int fixtureId, int clubId) { return teams.get(new Team(fixtureId, clubId)); }

    private static List<String[]> rows(String name, String header) {
        var resource = new ClassPathResource("data/match-lineups-2026/" + name);
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            require(header.equals(reader.readLine()), "Invalid bundled CSV header: " + name);
            int columns = header.split(",", -1).length;
            return reader.lines().filter(line -> !line.isBlank()).map(line -> {
                String[] fields = line.split(",", -1);
                require(fields.length == columns && !line.contains("\""), "Invalid bundled CSV row: " + name);
                return fields;
            }).toList();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read packaged match lineup evidence: " + name, failure);
        }
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
