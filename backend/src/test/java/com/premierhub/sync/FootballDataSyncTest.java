package com.premierhub.sync;

import com.premierhub.seed.Gw1SnapshotService;
import com.premierhub.service.FootballQueries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:football-data-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class FootballDataSyncTest {
    @Autowired FootballDataSync sync;
    @Autowired FootballQueries queries;
    @Autowired Gw1SnapshotService snapshot;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired WebApplicationContext context;
    @MockitoBean FootballDataClient client;

    @Test
    void importRepeatsUpdatesRollsBackAndKeepsLegacySeasonAndScores() throws Exception {
        snapshot.importBundled();
        var oldMatches = queries.matches(2024, null, null, null);
        var oldStandings = queries.standings(2024, null);
        var oldPlayers = queries.players(2024, null, null);
        var oldClubs = queries.clubs(2024, null);
        var input = responses();
        var batch = FootballDataBatch.parse(input[0], input[1], input[2]);

        // The first club would be inserted before this second club collides: all must roll back.
        jdbc.update("INSERT INTO clubs (id, name) VALUES (1000000034, 'Existing unrelated record')");
        assertThrows(IllegalStateException.class, () -> sync.importBatch(batch));
        assertEquals(0, count("football_data_teams"));
        assertEquals(0, count("football_data_fixtures"));
        assertTrue(queries.clubs(2026, null).isEmpty());
        jdbc.update("DELETE FROM clubs WHERE id=1000000034"); // Test-only sentinel.

        var first = sync.importBatch(batch);
        var second = sync.importBatch(batch);
        assertEquals(20, first.teamsInserted());
        assertEquals(380, first.fixturesInserted());
        assertEquals(0, second.teamsInserted());
        assertEquals(0, second.fixturesInserted());
        assertEquals(20, count("football_data_teams"));
        assertEquals(380, count("football_data_fixtures"));
        assertEquals(380, queries.matches(2026, null, null, null).size());
        assertEquals(20, queries.standings(2026, null).size());
        assertTrue(queries.players(2026, null, null).isEmpty());
        assertEquals(400, count("fixture_player_stats"));
        assertEquals(10, count("fixture_score_evidence"));
        var upcoming = queries.match(1001208022, 2026).orElseThrow();
        assertEquals("SCHEDULED", upcoming.status());
        assertNull(upcoming.homeGoals());
        assertNull(upcoming.awayGoals());

        ObjectNode changed = (ObjectNode) input[1].path("matches").get(1);
        changed.put("utcDate", "2026-08-22T23:30:00Z").put("status", "FINISHED");
        ((ObjectNode) changed.path("score").path("fullTime")).put("home", 2).put("away", 1);
        ObjectNode standing = (ObjectNode) input[2].path("standings").get(0).path("table").get(0);
        standing.put("playedGames", 1).put("won", 1).put("goalsFor", 2).put("goalsAgainst", 1)
                .put("goalDifference", 1).put("points", 3);
        sync.importBatch(FootballDataBatch.parse(input[0], input[1], input[2]));
        assertEquals(2, queries.match(1001208022, 2026).orElseThrow().homeGoals());
        assertEquals("2026-08-22T23:30:00Z", jdbc.queryForObject(
                "SELECT utc_date FROM football_data_fixtures WHERE provider_id=1208022", String.class));
        assertEquals(3, queries.standings(2026, null).getFirst().points());

        var beforeFailure = queries.matches(2026, null, null, null);
        for (int status : new int[]{401, 403, 429}) {
            doThrow(new IOException("HTTP " + status)).when(client).download();
            assertThrows(IOException.class, sync::sync);
            assertEquals(beforeFailure, queries.matches(2026, null, null, null));
            assertEquals(3, queries.standings(2026, null).getFirst().points());
        }
        assertEquals(oldMatches, queries.matches(2024, null, null, null));
        assertEquals(oldStandings, queries.standings(2024, null));
        assertEquals(oldPlayers, queries.players(2024, null, null));
        assertEquals(oldClubs, queries.clubs(2024, null));
        for (var match : oldMatches) {
            var detail = queries.matchDetail(match.id(), 2024).orElseThrow();
            assertEquals("VERIFIED", detail.evidenceStatus());
            assertEquals(40, Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream())
                    .filter(player -> player.score().status().equals("COMPLETE")).count());
        }
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/api/matches?season=2026&round=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].matchweek").value(1));
        mvc.perform(get("/api/matches/1001208023?season=2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.homeGoals").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/standings?season=2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20));
        mvc.perform(get("/api/matches?season=2024&matchweek=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void rejectsWrongSeasonMissingFieldsPartialResponsesAndUnknownStatusBeforeImport() {
        var input = responses();
        ((ObjectNode) input[0].path("season")).put("startDate", "2025-08-01");
        assertThrows(IllegalArgumentException.class, () -> FootballDataBatch.parse(input[0], input[1], input[2]));
        ((ObjectNode) input[0].path("season")).put("startDate", "2026-08-21");
        ObjectNode fixture = (ObjectNode) input[1].path("matches").get(0);
        fixture.put("status", "UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> FootballDataBatch.parse(input[0], input[1], input[2]));
        fixture.put("status", "FINISHED");
        ((ObjectNode) fixture.path("score").path("fullTime")).putNull("home").putNull("away");
        assertThrows(IllegalArgumentException.class, () -> FootballDataBatch.parse(input[0], input[1], input[2]));
        fixture.put("status", "TIMED");
        fixture.remove("utcDate");
        assertThrows(IllegalArgumentException.class, () -> FootballDataBatch.parse(input[0], input[1], input[2]));
        fixture.put("utcDate", "2026-08-21T19:00:00Z");
        ((tools.jackson.databind.node.ArrayNode) input[1].path("matches")).remove(0);
        assertThrows(IllegalArgumentException.class, () -> FootballDataBatch.parse(input[0], input[1], input[2]));
    }

    private ObjectNode[] responses() {
        ObjectNode teams = envelope();
        ObjectNode matches = envelope();
        ObjectNode standings = envelope();
        var teamRows = teams.putArray("teams");
        var rows = standings.putArray("standings").addObject().put("type", "TOTAL").putArray("table");
        for (int i = 0; i < 20; i++) {
            teamRows.addObject().put("id", 33 + i).put("name", "Test club " + i);
            var row = rows.addObject().put("position", i + 1).put("playedGames", 0).put("won", 0)
                    .put("draw", 0).put("lost", 0).put("goalsFor", 0).put("goalsAgainst", 0)
                    .put("goalDifference", 0).put("points", 0);
            row.putObject("team").put("id", 33 + i);
        }
        var fixtures = matches.putArray("matches");
        for (int i = 0; i < 380; i++) {
            var fixture = fixtures.addObject().put("id", 1208021 + i).put("matchday", i / 10 + 1)
                    .put("status", "TIMED").put("utcDate", "2026-08-21T19:00:00Z");
            fixture.putObject("season").put("startDate", "2026-08-21").put("endDate", "2027-05-30");
            fixture.putObject("homeTeam").put("id", 33 + i % 20);
            fixture.putObject("awayTeam").put("id", 33 + (i + 1) % 20);
            fixture.putObject("score").putObject("fullTime").putNull("home").putNull("away");
        }
        return new ObjectNode[]{teams, matches, standings};
    }

    private ObjectNode envelope() {
        ObjectNode node = mapper.createObjectNode();
        node.putObject("competition").put("id", 2021).put("code", "PL");
        node.putObject("season").put("startDate", "2026-08-21").put("endDate", "2027-05-30");
        return node;
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
