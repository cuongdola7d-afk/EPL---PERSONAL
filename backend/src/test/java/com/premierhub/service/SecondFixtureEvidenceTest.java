package com.premierhub.service;

import com.premierhub.web.dto.MatchDetailResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:second-fixture-evidence-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class SecondFixtureEvidenceTest {
    private static final int FIXTURE_ID = 1208022;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FootballQueries queries;
    @Autowired private FixtureEvidenceService evidenceService;
    @Autowired private ObjectMapper mapper;

    private JsonNode evidence;

    @BeforeEach
    void seedFixture() throws Exception {
        evidence = mapper.readTree(Files.readString(Path.of("data/fixture-1208022-evidence.json")));
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        jdbc.update("INSERT INTO clubs VALUES (57, 'Ipswich', 'Ipswich')");
        jdbc.update("INSERT INTO clubs VALUES (40, 'Liverpool', 'Liverpool')");
        jdbc.update("INSERT INTO fixtures VALUES (1208022, 39, 2024, 57, 40, 1, "
                + "DATE '2024-08-17', 'FINISHED', 'FT', 0, 2, 'hash', CURRENT_TIMESTAMP)");

        Set<Integer> entered = new HashSet<>();
        for (JsonNode event : evidence.path("events")) {
            if ("subst".equals(event.path("type").asText())) {
                entered.add(event.path("assistId").asInt());
            }
        }
        for (JsonNode lineup : evidence.path("lineups")) {
            int team = lineup.path("teamId").asInt();
            for (JsonNode id : lineup.path("starters")) {
                insertPlayer(id.asInt(), team, 90);
            }
            for (JsonNode id : lineup.path("bench")) {
                insertPlayer(id.asInt(), team, entered.contains(id.asInt()) ? 30 : null);
            }
        }
    }

    @Test
    void secondFixtureCanBeImportedTwiceAndCompletesAllPlayers() {
        assertEquals("MISSING", detail().evidenceStatus());
        evidenceService.save(evidence);
        evidenceService.save(evidence);

        MatchDetailResponse detail = detail();
        assertEquals("VERIFIED", detail.evidenceStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM fixture_score_evidence", Integer.class));
        assertEquals(40, players(detail).filter(player -> "COMPLETE".equals(player.score().status())).count());
        assertEquals(9, players(detail).filter(player -> player.minutes() == null).count());
        assertEquals(9, players(detail).filter(player -> player.playerId() == 306)
                .findFirst().orElseThrow().score().confirmedPoints());
        assertEquals(6, players(detail).filter(player -> player.playerId() == 2678)
                .findFirst().orElseThrow().score().confirmedPoints());
        MatchPlayerStatResponse unused = players(detail).filter(player -> player.minutes() == null)
                .findFirst().orElseThrow();
        assertNull(unused.goals());
        assertNull(unused.assists());
        assertEquals(0, unused.inferred().minutes());
        assertEquals(0, unused.inferred().goals());
        assertEquals(0, unused.inferred().assists());
        assertNull(jdbc.queryForObject("SELECT minutes FROM fixture_player_stats WHERE fixture_id=? "
                + "AND player_id=?", Integer.class, FIXTURE_ID, unused.playerId()));
    }

    @Test
    void changedGoalEvidenceLeavesMissingValuesProvisional() {
        JsonNode changed = evidence.deepCopy();
        ArrayNode events = (ArrayNode) changed.path("events");
        for (int index = 0; index < events.size(); index++) {
            if ("Goal".equals(events.get(index).path("type").asText())) {
                events.remove(index);
                break;
            }
        }
        evidenceService.save(changed);

        MatchDetailResponse detail = detail();
        assertEquals("INVALID", detail.evidenceStatus());
        assertTrue(detail.evidenceError().contains("bàn thắng"));
        assertEquals(38, players(detail).filter(player ->
                "PROVISIONAL".equals(player.score().status())).count());
        assertTrue(players(detail).allMatch(player -> player.inferred() == null));
    }

    @Test
    void conflictingAssistDoesNotInferAnyMissingValue() {
        JsonNode changed = evidence.deepCopy();
        for (JsonNode event : changed.path("events")) {
            if ("Goal".equals(event.path("type").asText()) &&
                    event.path("assistId").asInt(-1) == 306) {
                ((ObjectNode) event).putNull("assistId");
            }
        }
        evidenceService.save(changed);

        MatchDetailResponse detail = detail();
        assertEquals("INVALID", detail.evidenceStatus());
        assertTrue(detail.evidenceError().contains("Kiến tạo"));
        assertTrue(players(detail).allMatch(player -> player.inferred() == null));
    }

    private void insertPlayer(int id, int team, Integer minutes) {
        jdbc.update("INSERT INTO players VALUES (?, ?)", id, "Player " + id);
        Integer goals = id == 2678 || id == 306 ? 1 : null;
        Integer assists = minutes == null ? null : id == 306 ? 1 : 0;
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (1208022, ?, ?, 'F', ?, ?, ?, 0, 0, NULL, NULL, NULL, NULL, NULL,
                        '{}', CURRENT_TIMESTAMP)
                """, id, team, minutes, goals, assists);
    }

    private MatchDetailResponse detail() {
        return queries.matchDetail(FIXTURE_ID, 2024).orElseThrow();
    }

    private Stream<MatchPlayerStatResponse> players(MatchDetailResponse detail) {
        return Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream());
    }
}
