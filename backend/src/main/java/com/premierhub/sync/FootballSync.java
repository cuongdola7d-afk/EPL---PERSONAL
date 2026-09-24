package com.premierhub.sync;

import tools.jackson.databind.JsonNode;
import com.premierhub.service.FootballQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.HexFormat;

@Service
public class FootballSync {
    private static final int LEAGUE = FootballQueries.LEAGUE_ID;
    private final JdbcTemplate jdbc;
    private final ApiFootballClient api;
    private final TransactionTemplate transactions;

    public FootballSync(JdbcTemplate jdbc, ApiFootballClient api,
                        TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.api = api;
        this.transactions = transactions;
    }

    public SyncResult sync(int season, int gameweek, int budget, boolean refreshPlayers,
                           boolean refreshStats) throws IOException, InterruptedException {
        FootballQueries.validateSeason(season);
        if (gameweek < 1 || gameweek > 38) throw new IllegalArgumentException("Gameweek must be 1..38");
        if (budget < 3 || budget > 90) throw new IllegalArgumentException("Request budget must be 3..90");
        ensureSeason(season);
        int fixturesSynced = 0;
        int statisticsSynced = 0;
        int playerPagesSynced = 0;
        boolean complete = true;
        try {
            if (state(season, "teams", 0) == null) {
                JsonNode teams = api.get("/teams?league=39&season=" + season, budget);
                for (JsonNode item : teams.path("response")) {
                    JsonNode team = item.path("team");
                    saveClub(team.path("id").asInt(), team.path("name").asText(),
                            text(item.path("venue"), "city"), season);
                }
                if (teams.path("response").size() == 0) throw new IOException("No teams returned");
                saveState(season, "teams", 0, hash(teams.path("response")));
            }

            JsonNode fixtureResponse = api.get("/fixtures?league=39&season=" + season
                    + "&round=" + ApiFootballClient.encode("Regular Season - " + gameweek), budget);
            for (JsonNode item : fixtureResponse.path("response")) {
                int id = item.path("fixture").path("id").asInt();
                if (id <= 0) throw new IOException("Fixture has no provider ID");
                String fingerprint = hash(item);
                if (!fingerprint.equals(state(season, "fixture", id))) {
                    saveFixture(item, season, gameweek, fingerprint);
                    fixturesSynced++;
                }
            }
            if (fixtureResponse.path("response").size() == 0) throw new IOException("No fixtures returned for gameweek");
            saveState(season, "gameweek", gameweek, hash(fixtureResponse.path("response")));

            JsonNode standings = api.get("/standings?league=39&season=" + season, budget);
            JsonNode rows = standings.path("response").path(0).path("league").path("standings").path(0);
            if (!rows.isArray() || rows.isEmpty()) throw new IOException("No standings returned");
            if (!hash(rows).equals(state(season, "standings", 0))) {
                transactions.executeWithoutResult(tx -> {
                    jdbc.update("DELETE FROM standings WHERE league_id = ? AND season_year = ?", LEAGUE, season);
                    for (JsonNode row : rows) saveStanding(row, season);
                    saveState(season, "standings", 0, hash(rows));
                });
            }

            // A fixture fingerprint changes when its date, status or score changes. The explicit
            // refresh flag also catches provider corrections that do not change the fixture JSON.
            for (JsonNode item : fixtureResponse.path("response")) {
                int id = item.path("fixture").path("id").asInt();
                String status = text(item.path("fixture").path("status"), "short");
                if (!isFinished(status)) continue;
                String fingerprint = hash(item);
                if (!refreshStats && fingerprint.equals(state(season, "fixture_players", id))) continue;
                JsonNode stats = api.get("/fixtures/players?fixture=" + id, budget).path("response");
                if (!stats.isArray() || stats.isEmpty()) throw new IOException("No player stats for fixture " + id);
                transactions.executeWithoutResult(tx -> {
                    jdbc.update("DELETE FROM fixture_player_stats WHERE fixture_id = ?", id);
                    for (JsonNode team : stats) saveFixturePlayers(team, id, season);
                    saveState(season, "fixture_players", id, fingerprint);
                });
                statisticsSynced++;
            }

            // The Free plan exposes at most pages 1..3 for one search. Partition by team
            // instead of paging the whole league (which reports 57 pages but denies page 4).
            for (Integer clubId : jdbc.queryForList("SELECT club_id FROM season_clubs "
                    + "WHERE league_id=? AND season_year=? ORDER BY club_id", Integer.class, LEAGUE, season)) {
                String total = state(season, "team_page_count", clubId);
                int totalPages = total == null ? 1 : Integer.parseInt(total);
                if (totalPages > 3) complete = false;
                for (int page = 1; page <= Math.min(totalPages, 3); page++) {
                    int checkpoint = clubId * 100 + page;
                    if (!refreshPlayers && state(season, "team_player_page", checkpoint) != null) continue;
                    JsonNode response = api.get("/players?team=" + clubId + "&season=" + season
                            + "&page=" + page, budget);
                    if (page == 1) {
                        totalPages = response.path("paging").path("total").asInt();
                        if (totalPages < 1) throw new IOException("Player pagination unavailable for team " + clubId);
                        saveState(season, "team_page_count", clubId, String.valueOf(totalPages));
                        if (totalPages > 3) complete = false;
                    }
                    int savedPage = page;
                    transactions.executeWithoutResult(tx -> {
                        for (JsonNode player : response.path("response")) saveSeasonPlayer(player, season);
                        saveState(season, "team_player_page", clubId * 100 + savedPage,
                                hash(response.path("response")));
                    });
                    playerPagesSynced++;
                }
            }
        } catch (ApiFootballClient.RequestBudgetReachedException exhausted) {
            complete = false;
        }
        List<Integer> truncatedTeams = jdbc.query("SELECT item_id, payload_hash FROM sync_states "
                + "WHERE league_id=? AND season_year=? AND scope='team_page_count'",
                (rs, row) -> Integer.parseInt(rs.getString("payload_hash")) > 3
                        ? rs.getInt("item_id") : null, LEAGUE, season).stream()
                .filter(id -> id != null).toList();
        return new SyncResult(season, gameweek, api.calls(), api.dailyRemaining(),
                fixturesSynced, statisticsSynced, playerPagesSynced,
                complete && truncatedTeams.isEmpty(), truncatedTeams);
    }

    private void ensureSeason(int season) {
        jdbc.update("INSERT INTO seasons (league_id, season_year) SELECT ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM seasons WHERE league_id = ? AND season_year = ?)",
                LEAGUE, season, LEAGUE, season);
    }

    private void saveClub(int id, String name, String city, int season) {
        if (id <= 0 || name == null || name.isBlank()) throw new IllegalArgumentException("Club has no ID/name");
        if (jdbc.update("UPDATE clubs SET name = ?, city = ? WHERE id = ?", name, city, id) == 0) {
            jdbc.update("INSERT INTO clubs (id, name, city) VALUES (?, ?, ?)", id, name, city);
        }
        jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) "
                + "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM season_clubs "
                + "WHERE league_id = ? AND season_year = ? AND club_id = ?)",
                LEAGUE, season, id, LEAGUE, season, id);
    }

    private void savePlayer(int id, String name) {
        if (id <= 0 || name == null || name.isBlank()) throw new IllegalArgumentException("Player has no ID/name");
        if (jdbc.update("UPDATE players SET name = ? WHERE id = ?", name, id) == 0) {
            jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", id, name);
        }
    }

    private void saveFixture(JsonNode item, int season, int gameweek, String fingerprint) {
        JsonNode fixture = item.path("fixture");
        int id = fixture.path("id").asInt();
        int home = item.path("teams").path("home").path("id").asInt();
        int away = item.path("teams").path("away").path("id").asInt();
        String shortStatus = text(fixture.path("status"), "short");
        String status = status(shortStatus);
        Integer homeGoals = number(item.path("goals"), "home");
        Integer awayGoals = number(item.path("goals"), "away");
        Date date = Date.valueOf(LocalDate.parse(fixture.path("date").asText().substring(0, 10)));
        if (jdbc.update("""
                UPDATE fixtures SET home_club_id=?, away_club_id=?, gameweek=?, match_date=?,
                status=?, provider_status=?, home_goals=?, away_goals=?, payload_hash=?, synced_at=?
                WHERE id=?
                """, home, away, gameweek, date, status, shortStatus, homeGoals, awayGoals,
                fingerprint, now(), id) == 0) {
            jdbc.update("""
                    INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                    gameweek, match_date, status, provider_status, home_goals, away_goals,
                    payload_hash, synced_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, LEAGUE, season, home, away, gameweek, date, status, shortStatus,
                    homeGoals, awayGoals, fingerprint, now());
        }
        saveState(season, "fixture", id, fingerprint);
    }

    private void saveStanding(JsonNode row, int season) {
        JsonNode all = row.path("all");
        jdbc.update("""
                INSERT INTO standings (league_id, season_year, club_id, position, played,
                won, drawn, lost, goals_for, goals_against, goal_difference, points)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, LEAGUE, season, row.path("team").path("id").asInt(), row.path("rank").asInt(),
                all.path("played").asInt(), all.path("win").asInt(), all.path("draw").asInt(),
                all.path("lose").asInt(), all.path("goals").path("for").asInt(),
                all.path("goals").path("against").asInt(), row.path("goalsDiff").asInt(),
                row.path("points").asInt());
    }

    private void saveFixturePlayers(JsonNode team, int fixtureId, int season) {
        int clubId = team.path("team").path("id").asInt();
        for (JsonNode entry : team.path("players")) {
            JsonNode player = entry.path("player");
            int playerId = player.path("id").asInt();
            savePlayer(playerId, player.path("name").asText());
            JsonNode stats = entry.path("statistics").path(0);
            jdbc.update("""
                    INSERT INTO fixture_player_stats (fixture_id, player_id, club_id, position,
                    minutes, goals, assists, yellow_cards, red_cards, rating, shots_on,
                    passes_key, tackles, saves, raw_statistics, synced_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, fixtureId, playerId, clubId, text(stats.path("games"), "position"),
                    number(stats.path("games"), "minutes"), number(stats.path("goals"), "total"),
                    number(stats.path("goals"), "assists"), number(stats.path("cards"), "yellow"),
                    number(stats.path("cards"), "red"), text(stats.path("games"), "rating"),
                    number(stats.path("shots"), "on"), number(stats.path("passes"), "key"),
                    number(stats.path("tackles"), "total"), number(stats.path("goals"), "saves"),
                    stats.toString(), now());
        }
    }

    private void saveSeasonPlayer(JsonNode entry, int season) {
        JsonNode player = entry.path("player");
        int playerId = player.path("id").asInt();
        savePlayer(playerId, player.path("name").asText());
        for (JsonNode stats : entry.path("statistics")) {
            if (stats.path("league").path("id").asInt() != LEAGUE
                    || stats.path("league").path("season").asInt() != season) continue;
            JsonNode team = stats.path("team");
            int clubId = team.path("id").asInt();
            // All league teams are seeded first; this also keeps a transferred player's older club.
            if (clubId <= 0) continue;
            saveClub(clubId, team.path("name").asText(), clubCity(clubId), season);
            String position = position(text(stats.path("games"), "position"));
            Integer appearances = number(stats.path("games"), "appearences");
            Integer minutes = number(stats.path("games"), "minutes");
            Integer goals = number(stats.path("goals"), "total");
            Integer assists = number(stats.path("goals"), "assists");
            if (jdbc.update("""
                    UPDATE player_season_stats SET position=?, appearances=?, minutes=?, goals=?, assists=?
                    WHERE league_id=? AND season_year=? AND player_id=? AND club_id=?
                    """, position, appearances, minutes, goals, assists, LEAGUE, season, playerId, clubId) == 0) {
                jdbc.update("""
                        INSERT INTO player_season_stats (league_id, season_year, player_id,
                        club_id, position, appearances, minutes, goals, assists)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, LEAGUE, season, playerId, clubId, position, appearances, minutes,
                        goals, assists);
            }
        }
    }

    private String clubCity(int clubId) {
        return jdbc.queryForObject("SELECT city FROM clubs WHERE id = ?", String.class, clubId);
    }

    private String state(int season, String scope, int item) {
        return jdbc.query("SELECT payload_hash FROM sync_states WHERE league_id=? AND season_year=? "
                + "AND scope=? AND item_id=?", rs -> rs.next() ? rs.getString(1) : null,
                LEAGUE, season, scope, item);
    }

    private void saveState(int season, String scope, int item, String hash) {
        if (jdbc.update("UPDATE sync_states SET payload_hash=?, synced_at=? WHERE league_id=? "
                + "AND season_year=? AND scope=? AND item_id=?",
                hash, now(), LEAGUE, season, scope, item) == 0) {
            jdbc.update("INSERT INTO sync_states (league_id, season_year, scope, item_id, "
                    + "payload_hash, synced_at) VALUES (?, ?, ?, ?, ?, ?)",
                    LEAGUE, season, scope, item, hash, now());
        }
    }

    private static String status(String code) {
        return switch (code) {
            case "FT", "AET", "PEN" -> "FINISHED";
            case "PST" -> "POSTPONED";
            case "CANC" -> "CANCELLED";
            case "SUSP", "INT" -> "SUSPENDED";
            case "AWD", "WO" -> "AWARDED";
            case "1H", "HT", "2H", "ET", "BT", "P", "LIVE" -> "LIVE";
            default -> "SCHEDULED";
        };
    }

    private static boolean isFinished(String code) {
        return code.equals("FT") || code.equals("AET") || code.equals("PEN");
    }

    private static String position(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "goalkeeper" -> "GOALKEEPER";
            case "defender" -> "DEFENDER";
            case "midfielder" -> "MIDFIELDER";
            case "attacker" -> "FORWARD";
            default -> "UNKNOWN";
        };
    }

    private static Integer number(JsonNode node, String key) {
        JsonNode value = node.path(key);
        return value.isNumber() ? value.asInt() : null;
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.path(key);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static Timestamp now() { return Timestamp.from(Instant.now()); }

    private static String hash(JsonNode value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record SyncResult(int season, int gameweek, int requests, int dailyRemaining,
                             int fixturesUpdated, int fixtureStatisticsUpdated,
                             int playerPagesUpdated, boolean complete,
                             List<Integer> truncatedClubIds) { }
}
