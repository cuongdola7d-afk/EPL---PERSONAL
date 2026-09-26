package com.premierhub.sync;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A complete, validated basic-data response set. Never contains player statistics. */
public record FootballDataBatch(List<Team> teams, List<Fixture> fixtures, List<Standing> standings) {
    public static final int SEASON = 2026;

    public record Team(int id, String name) { }
    public record Fixture(int id, int home, int away, int round, Instant kickoff,
                          String status, String providerStatus, Integer homeGoals,
                          Integer awayGoals, String payload) { }
    public record Standing(int team, int position, int played, int won, int drawn, int lost,
                           int goalsFor, int goalsAgainst, int difference, int points) { }

    public static FootballDataBatch parse(JsonNode teamsBody, JsonNode matchesBody, JsonNode standingsBody) {
        for (JsonNode body : List.of(teamsBody, matchesBody, standingsBody)) {
            require(body.path("competition").path("id").asInt() == 2021
                    && "PL".equals(body.path("competition").path("code").asText()), "Expected competition PL/2021");
            require(!body.has("errorCode"), "Provider returned an error");
        }
        season(teamsBody.path("season"));
        season(standingsBody.path("season"));
        List<Team> teams = new ArrayList<>();
        Set<Integer> teamIds = new HashSet<>();
        for (JsonNode row : array(teamsBody, "teams")) {
            int id = providerId(row, "id");
            String name = text(row, "name");
            require(name.length() <= 200 && teamIds.add(id), "Invalid or duplicate team");
            teams.add(new Team(id, name));
        }
        require(teams.size() == 20, "Expected 20 Premier League teams");

        List<Fixture> fixtures = new ArrayList<>();
        Set<Integer> fixtureIds = new HashSet<>();
        for (JsonNode row : array(matchesBody, "matches")) {
            season(row.path("season"));
            int id = providerId(row, "id");
            int home = providerId(row.path("homeTeam"), "id");
            int away = providerId(row.path("awayTeam"), "id");
            require(fixtureIds.add(id) && home != away && teamIds.contains(home) && teamIds.contains(away),
                    "Fixture has duplicate ID or unknown teams");
            int round = integer(row, "matchday");
            require(round >= 1 && round <= 38, "Fixture matchday must be 1..38");
            Instant kickoff = Instant.parse(text(row, "utcDate"));
            String rawStatus = text(row, "status");
            String status = switch (rawStatus) {
                case "SCHEDULED", "TIMED" -> "SCHEDULED";
                case "IN_PLAY", "PAUSED", "EXTRA_TIME", "PENALTY_SHOOTOUT" -> "LIVE";
                case "FINISHED", "POSTPONED", "SUSPENDED", "CANCELLED", "AWARDED" -> rawStatus;
                default -> throw new IllegalArgumentException("Unsupported match status: " + rawStatus);
            };
            JsonNode score = row.path("score").path("fullTime");
            require(score.isObject() && score.has("home") && score.has("away"), "Missing fullTime score fields");
            Integer homeGoals = nullableScore(score, "home");
            Integer awayGoals = nullableScore(score, "away");
            require((homeGoals == null) == (awayGoals == null), "Incomplete score pair");
            require(!status.equals("FINISHED") || homeGoals != null, "Finished fixture lacks score");
            fixtures.add(new Fixture(id, home, away, round, kickoff, status, rawStatus,
                    homeGoals, awayGoals, row.toString()));
        }
        require(fixtures.size() == 380, "Expected all 380 season fixtures; refusing partial import");
        if (matchesBody.path("resultSet").has("count")) {
            require(integer(matchesBody.path("resultSet"), "count") == fixtures.size(), "Truncated matches response");
        }

        List<Standing> standings = new ArrayList<>();
        Set<Integer> standingIds = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        int totalTables = 0;
        for (JsonNode table : array(standingsBody, "standings")) {
            if (!"TOTAL".equals(table.path("type").asText())) continue;
            totalTables++;
            for (JsonNode row : array(table, "table")) {
                int team = providerId(row.path("team"), "id");
                int position = integer(row, "position");
                int played = nonnegative(row, "playedGames");
                int won = nonnegative(row, "won");
                int drawn = nonnegative(row, "draw");
                int lost = nonnegative(row, "lost");
                int goalsFor = nonnegative(row, "goalsFor");
                int goalsAgainst = nonnegative(row, "goalsAgainst");
                int difference = integer(row, "goalDifference");
                require(teamIds.contains(team) && standingIds.add(team) && ranks.add(position)
                        && position >= 1 && position <= 20, "Invalid TOTAL standing team/rank");
                require(played == won + drawn + lost && difference == goalsFor - goalsAgainst,
                        "Inconsistent TOTAL standing statistics");
                standings.add(new Standing(team, position, played, won, drawn, lost,
                        goalsFor, goalsAgainst, difference, integer(row, "points")));
            }
        }
        require(totalTables == 1 && standings.size() == 20, "Expected one TOTAL table with 20 rows");
        return new FootballDataBatch(List.copyOf(teams), List.copyOf(fixtures), List.copyOf(standings));
    }

    private static void season(JsonNode node) {
        require(text(node, "startDate").startsWith("2026-") && text(node, "endDate").startsWith("2027-"),
                "Response is not season 2026/27");
    }

    private static JsonNode array(JsonNode node, String field) {
        require(node.path(field).isArray(), "Missing array: " + field);
        return node.path(field);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        require(value.isTextual() && !value.asText().isBlank(), "Missing text: " + field);
        return value.asText();
    }

    private static int integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        require(value.isIntegralNumber() && value.canConvertToInt(), "Missing integer: " + field);
        return value.asInt();
    }

    private static int nonnegative(JsonNode node, String field) {
        int value = integer(node, field);
        require(value >= 0, "Negative value: " + field);
        return value;
    }

    private static int providerId(JsonNode node, String field) {
        int id = integer(node, field);
        require(id > 0 && id < 1_000_000_000, "Provider ID outside reserved range");
        return id;
    }

    private static Integer nullableScore(JsonNode node, String field) {
        return node.path(field).isNull() ? null : nonnegative(node, field);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
