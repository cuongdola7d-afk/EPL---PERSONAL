package com.premierhub.service;

import com.premierhub.web.dto.MatchDetailResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:fixture-evidence-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class FixtureEvidenceServiceTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FootballQueries queries;
    @Autowired private FixtureEvidenceService evidenceService;
    @Autowired private ObjectMapper mapper;
    @Autowired private WebApplicationContext context;

    private JsonNode evidence;

    @BeforeEach
    void seedFixture() throws Exception {
        evidence = mapper.readTree(Files.readString(Path.of("data/fixture-1208021-evidence.json")));
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        jdbc.update("INSERT INTO clubs VALUES (33, 'Manchester United', 'Manchester')");
        jdbc.update("INSERT INTO clubs VALUES (36, 'Fulham', 'London')");
        jdbc.update("INSERT INTO fixtures VALUES (1208021, 39, 2024, 33, 36, 1, "
                + "DATE '2024-08-16', 'FINISHED', 'FT', 1, 0, 'hash', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO fixtures VALUES (1209000, 39, 2024, 33, 36, 2, "
                + "DATE '2024-08-17', 'FINISHED', 'FT', 0, 0, 'hash', CURRENT_TIMESTAMP)");

        Set<Integer> entered = Set.of(284324, 70100, 532, 18772, 903,
                19025, 2887, 19221, 191971, 19480);
        Set<Integer> all = new HashSet<>();
        for (JsonNode lineup : evidence.path("lineups")) {
            int team = lineup.path("teamId").asInt();
            for (JsonNode idNode : lineup.path("starters")) {
                insertPlayer(idNode.asInt(), team, 90, all);
            }
            for (JsonNode idNode : lineup.path("bench")) {
                int id = idNode.asInt();
                insertPlayer(id, team, entered.contains(id) ? 29 : null, all);
            }
        }
        assertEquals(40, all.size());
    }

    @Test
    void verifiedEvidenceCompletesThisFixtureWithoutChangingRawValuesAndCanRunTwice() throws Exception {
        assertEquals("MISSING", detail(1208021).evidenceStatus());
        evidenceService.save(evidence);
        evidenceService.save(evidence);
        MatchDetailResponse detail = detail(1208021);

        assertEquals("VERIFIED", detail.evidenceStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM fixture_score_evidence", Integer.class));
        assertEquals(40, Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream())
                .filter(player -> "COMPLETE".equals(player.score().status())).count());
        MatchPlayerStatResponse scorer = player(detail, 70100);
        assertEquals(5, scorer.score().confirmedPoints());
        assertEquals(1, scorer.goals());
        MatchPlayerStatResponse unused = player(detail, 174);
        assertNull(unused.minutes());
        assertNull(unused.goals());
        assertNull(unused.assists());
        assertEquals(0, unused.inferred().minutes());
        assertEquals(0, unused.inferred().goals());
        assertEquals(0, unused.inferred().assists());
        assertEquals(0, unused.score().confirmedPoints());
        assertNull(jdbc.queryForObject("SELECT minutes FROM fixture_player_stats WHERE fixture_id=1208021 "
                + "AND player_id=174", Integer.class));
        MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/api/matches/1208021/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.homePlayers.length()").value(20))
                .andExpect(jsonPath("$.awayPlayers.length()").value(20))
                .andExpect(jsonPath("$.homePlayers[?(@.playerId == 174)].minutes")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$.homePlayers[?(@.playerId == 174)].inferred.minutes")
                        .value(org.hamcrest.Matchers.hasItem(0)));
        assertEquals("NOT_APPLICABLE", detail(1209000).evidenceStatus());
        assertEquals("PROVISIONAL", player(detail(1209000), 174).score().status());
    }

    @Test
    void conflictingGoalDoesNotInferAnyMissingValue() throws Exception {
        JsonNode changed = evidence.deepCopy();
        for (JsonNode event : changed.path("events")) {
            if ("Goal".equals(event.path("type").asText())) {
                ((ObjectNode) event).put("playerId", 1485);
            }
        }
        evidenceService.save(changed);
        MatchDetailResponse detail = detail(1208021);

        assertEquals("INVALID", detail.evidenceStatus());
        assertTrue(detail.evidenceError().contains("Bàn thắng"));
        assertNull(player(detail, 174).inferred());
        assertEquals(39, Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream())
                .filter(player -> "PROVISIONAL".equals(player.score().status())).count());
        MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/api/matches/1208021/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceStatus").value("INVALID"))
                .andExpect(jsonPath("$.evidenceError").isNotEmpty());
    }

    @Test
    void lineupOrSubstitutionMismatchKeepsRawScore() {
        JsonNode changed = evidence.deepCopy();
        ((tools.jackson.databind.node.ArrayNode) changed.path("lineups").get(0).path("bench"))
                .remove(0);
        evidenceService.save(changed);
        MatchDetailResponse detail = detail(1208021);
        assertEquals("INVALID", detail.evidenceStatus());
        assertTrue(detail.evidenceError().contains("dự bị"));
        assertNull(player(detail, 174).inferred());

        changed = evidence.deepCopy();
        for (JsonNode event : changed.path("events")) {
            if ("subst".equals(event.path("type").asText())) {
                ((ObjectNode) event).put("assistId", 174);
                break;
            }
        }
        evidenceService.save(changed);
        detail = detail(1208021);
        assertEquals("INVALID", detail.evidenceStatus());
        assertTrue(detail.evidenceError().contains("phút"));
        assertNull(player(detail, 9971).inferred());
    }

    private void insertPlayer(int id, int team, Integer minutes, Set<Integer> all) {
        assertTrue(all.add(id));
        jdbc.update("INSERT INTO players VALUES (?, ?)", id, "Player " + id);
        Integer goals = id == 70100 ? 1 : null;
        Integer assists = minutes == null ? null : id == 284324 ? 1 : 0;
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (1208021, ?, ?, 'F', ?, ?, ?, 0, 0, NULL, NULL, NULL, NULL, NULL,
                        '{}', CURRENT_TIMESTAMP)
                """, id, team, minutes, goals, assists);
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (1209000, ?, ?, 'F', ?, ?, ?, 0, 0, NULL, NULL, NULL, NULL, NULL,
                        '{}', CURRENT_TIMESTAMP)
                """, id, team, minutes, goals, assists);
    }

    private MatchDetailResponse detail(int fixtureId) {
        return queries.matchDetail(fixtureId, 2024).orElseThrow();
    }

    private MatchPlayerStatResponse player(MatchDetailResponse detail, int id) {
        return Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream())
                .filter(row -> row.playerId() == id).findFirst().orElseThrow();
    }
}
