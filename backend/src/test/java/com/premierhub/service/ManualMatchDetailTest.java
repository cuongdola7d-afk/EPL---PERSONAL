package com.premierhub.service;

import com.premierhub.roster.ManualRosterCsvReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:manual-match-detail-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class ManualMatchDetailTest {
    private static final Path DATA = Path.of("data/gw2-first-two");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper mapper;

    private MockMvc mvc;

    @BeforeEach
    void seedFixturesAndSavedRows() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        Map<Integer, String> clubs = Map.of(
                1000000354, "Crystal Palace FC", 1000000065, "Manchester City FC",
                1000000064, "Liverpool FC", 1000000351, "Nottingham Forest FC",
                42, "Arsenal", 43, "Chelsea");
        for (var club : clubs.entrySet()) {
            jdbc.update("INSERT INTO clubs (id, name) VALUES (?, ?)", club.getKey(), club.getValue());
        }
        fixture(1000560555, 2026, 1000000354, 1000000065, "2026-08-28", 2, "FINISHED");
        fixture(1000560552, 2026, 1000000064, 1000000351, "2026-08-29", 2, "FINISHED");
        fixture(1000560553, 2026, 1000000354, 1000000351, "2026-08-30", 2, "FINISHED");
        fixture(1000560554, 2026, 1000000354, 1000000351, "2026-08-28", 2, "FINISHED");
        fixture(800, 2024, 42, 43, "2024-08-16", 1, "FINISHED");

        Map<Integer, Integer> clubByPlayer = new HashMap<>();
        Set<Integer> knownPlayers = new HashSet<>();
        for (var row : new ManualRosterCsvReader().read(DATA.resolve("manual-players-2026-GW2-first-two.csv"))) {
            int clubId = row.clubId();
            int playerId = row.playerId();
            clubByPlayer.put(playerId, clubId);
            if (knownPlayers.add(playerId)) {
                jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", playerId, row.name());
            }
            jdbc.update("INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position) "
                    + "VALUES (39, 2026, ?, ?, ?)", playerId, clubId, row.position());
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39, 2026, ?, ?, ?, ?)",
                    playerId, clubId, Date.valueOf(row.startDate()), Date.valueOf(row.endDate()));
        }
        assertEquals(80, clubByPlayer.size());

        int saved = 0;
        for (String line : Files.readAllLines(DATA.resolve("manual-match-stats-2026-GW2-first-two.csv"),
                StandardCharsets.UTF_8).stream().skip(1).toList()) {
            String[] fields = line.split(",", -1);
            int playerId = Integer.parseInt(fields[2]);
            BigDecimal rating = fields[4].isEmpty() ? null : new BigDecimal(fields[4]);
            BigDecimal fantasyPoints = fields[3].equals("DID_NOT_PLAY") ? BigDecimal.ZERO : rating;
            // A distinct stored value proves the detail endpoint does not recalculate 2026 points.
            if (playerId == 2000020100) fantasyPoints = new BigDecimal("9.75");
            jdbc.update("""
                    INSERT INTO manual_fixture_player_stats
                    (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                     rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                    VALUES (?, ?, 39, 2026, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Integer.parseInt(fields[1]), playerId, clubByPlayer.get(playerId), fields[3],
                    rating, fantasyPoints, number(fields[5]), number(fields[6]), number(fields[7]),
                    number(fields[8]), number(fields[9]));
            saved++;
        }
        assertEquals(80, saved);
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (1000560554, 2000020100, 39, 2026, 1000000354, 'PLAYED', 7.00,
                        7.00, 90, 0, 0, 0, 0)
                """);

        // A later transfer must not move Daniel Muñoz out of his Palace GW2 lineup.
        jdbc.update("INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position) "
                + "VALUES (39, 2026, 2000020095, 1000000351, 'MIDFIELDER')");
        jdbc.update("INSERT INTO manual_player_memberships VALUES "
                + "(39, 2026, 2000020095, 1000000351, DATE '2026-09-30', NULL)");

        // A saved match row without evidenced membership at kickoff must be excluded.
        jdbc.update("INSERT INTO players (id, name) VALUES (9999, 'Unmatched Player')");
        jdbc.update("INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position) "
                + "VALUES (39, 2026, 9999, 1000000354, 'FORWARD')");
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (1000560555, 9999, 39, 2026, 1000000354, 'PLAYED', 7.00, 7.00,
                        10, 0, 0, 0, 0)
                """);

        jdbc.update("INSERT INTO players (id, name) VALUES (901, '2024 Scorer')");
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (800, 901, 42, 'F', 90, 1, 0, 0, 0, '8.1', NULL, NULL, NULL, NULL,
                        '{}', CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void bothGw2DetailsShowOnlyTwentySavedPlayersPerClubAndStoredValues() throws Exception {
        JsonNode palaceCity = getJson("/api/matches/1000560555/details?season=2026");
        JsonNode liverpoolForest = getJson("/api/matches/1000560552/details?season=2026");
        assertEquals(20, palaceCity.path("homePlayers").size());
        assertEquals(20, palaceCity.path("awayPlayers").size());
        assertEquals(20, liverpoolForest.path("homePlayers").size());
        assertEquals(20, liverpoolForest.path("awayPlayers").size());
        assertEquals("MANUAL_VERIFIED", palaceCity.path("evidenceStatus").asText());
        assertEquals("MANUAL_VERIFIED", liverpoolForest.path("evidenceStatus").asText());
        assertTrue(palaceCity.path("match").path("hasManualStats").asBoolean());
        assertEquals(0, find(palaceCity.path("homePlayers"), 9999).size());

        JsonNode dean = find(palaceCity.path("homePlayers"), 2000020100);
        assertEquals("PLAYED", dean.path("participationStatus").asText());
        assertEquals("6.70", dean.path("rating").asText());
        assertEquals(0, new BigDecimal(dean.path("fantasyPoints").asText())
                .compareTo(new BigDecimal("9.75")));
        assertEquals(90, dean.path("minutes").asInt());
        assertTrue(dean.path("score").isNull());

        JsonNode daniel = find(palaceCity.path("homePlayers"), 2000020095);
        assertEquals(1000000354, daniel.path("clubId").asInt());
        assertEquals(0, find(palaceCity.path("awayPlayers"), 2000020095).size());
        JsonNode claudio = find(palaceCity.path("awayPlayers"), 2000030256);
        assertEquals("DID_NOT_PLAY", claudio.path("participationStatus").asText());
        assertEquals(0, claudio.path("minutes").asInt());
        assertTrue(claudio.path("rating").isNull());
        assertEquals(0, new BigDecimal(claudio.path("fantasyPoints").asText()).compareTo(BigDecimal.ZERO));

        for (int id : new int[] {2000004024, 2000004013}) {
            JsonNode player = find(palaceCity.path("awayPlayers"), id);
            assertEquals("PLAYED", player.path("participationStatus").asText());
            assertTrue(player.path("rating").isNull());
            assertTrue(player.path("fantasyPoints").isNull());
            assertTrue(player.path("minutes").asInt() > 0);
        }
        for (int id : new int[] {2000020077, 2000020076}) {
            JsonNode player = find(liverpoolForest.path("awayPlayers"), id);
            assertEquals("PLAYED", player.path("participationStatus").asText());
            assertTrue(player.path("rating").isNull());
            assertTrue(player.path("fantasyPoints").isNull());
        }
    }

    @Test
    void unfilled2026FixtureStaysEmptyAnd2024ScoringRemainsAvailable() throws Exception {
        JsonNode empty = getJson("/api/matches/1000560553/details?season=2026");
        assertFalse(empty.path("match").path("hasManualStats").asBoolean());
        assertEquals("MISSING", empty.path("evidenceStatus").asText());
        assertEquals(0, empty.path("homePlayers").size());
        assertEquals(0, empty.path("awayPlayers").size());

        JsonNode old = getJson("/api/matches/800/details?season=2024");
        JsonNode scorer = find(old.path("homePlayers"), 901);
        assertNotNull(scorer);
        assertFalse(scorer.path("score").isNull());
        assertEquals("COMPLETE", scorer.path("score").path("status").asText());
        assertEquals(6, scorer.path("score").path("confirmedPoints").asInt());
        assertTrue(scorer.path("participationStatus").isNull());
        assertTrue(scorer.path("fantasyPoints").isNull());
    }

    @Test
    void partialManualFixtureReturnsOnlyItsSavedPlayer() throws Exception {
        JsonNode partial = getJson("/api/matches/1000560554/details?season=2026");
        assertTrue(partial.path("match").path("hasManualStats").asBoolean());
        assertEquals(1, partial.path("homePlayers").size());
        assertEquals(0, partial.path("awayPlayers").size());
        assertEquals(2000020100, partial.path("homePlayers").get(0).path("playerId").asInt());
    }

    @Test
    void playerDirectoryTotalsUseSavedMatchesWithoutInventingMissingValues() throws Exception {
        jdbc.update("UPDATE manual_player_memberships SET start_date=DATE '2026-08-01' "
                + "WHERE player_id=2000004007 AND club_id=1000000065");
        JsonNode players = getJson("/api/players?season=2026&asOf=2026-08-28");
        JsonNode scorer = findPlayer(players, 2000004007);
        assertEquals(2, scorer.path("goals").asInt());
        assertEquals(0, scorer.path("assists").asInt());

        JsonNode unused = findPlayer(players, 2000020095);
        assertEquals(0, unused.path("goals").asInt());
        assertEquals(0, unused.path("assists").asInt());

        JsonNode beforeMatches = getJson("/api/players?season=2026&asOf=2026-08-01");
        assertTrue(findPlayer(beforeMatches, 2000004007).path("goals").isNull());

        jdbc.update("UPDATE manual_player_memberships SET end_date=NULL "
                + "WHERE player_id=2000004007 AND club_id=1000000065");
        fixture(1000560999, 2026, 1000000065, 1000000064, "2026-08-29", 2, "FINISHED");
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 minutes, goals, assists)
                VALUES (1000560999, 2000004007, 39, 2026, 1000000065, 'PLAYED', 90, 1, 1)
                """);
        JsonNode afterSecondMatch = getJson("/api/players?season=2026&asOf=2026-08-29");
        assertEquals(3, findPlayer(afterSecondMatch, 2000004007).path("goals").asInt());
        assertEquals(1, findPlayer(afterSecondMatch, 2000004007).path("assists").asInt());

        jdbc.update("UPDATE manual_fixture_player_stats SET goals=NULL, assists=NULL "
                + "WHERE fixture_id=1000560555 AND player_id=2000004007");
        JsonNode missing = getJson("/api/players?season=2026&asOf=2026-08-28");
        assertTrue(findPlayer(missing, 2000004007).path("goals").isNull());
        assertTrue(findPlayer(missing, 2000004007).path("assists").isNull());
    }

    private void fixture(int id, int season, int home, int away, String date, int week, String status) {
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                                      gameweek, match_date, status, provider_status, home_goals,
                                      away_goals, payload_hash, synced_at)
                VALUES (?, 39, ?, ?, ?, ?, ?, ?, 'FT', 1, 1, 'hash', CURRENT_TIMESTAMP)
                """, id, season, home, away, week, Date.valueOf(date), status);
    }

    private JsonNode getJson(String path) throws Exception {
        var response = mvc.perform(get(path)).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        return mapper.readTree(response.getContentAsString());
    }

    private static JsonNode find(JsonNode players, int playerId) {
        for (JsonNode player : players) {
            if (player.path("playerId").asInt() == playerId) return player;
        }
        return players.path(9999);
    }

    private static JsonNode findPlayer(JsonNode players, int playerId) {
        for (JsonNode player : players) {
            if (player.path("id").asInt() == playerId) return player;
        }
        return players.path(9999);
    }

    private static Integer number(String value) {
        return value.isEmpty() ? null : Integer.valueOf(value);
    }
}
