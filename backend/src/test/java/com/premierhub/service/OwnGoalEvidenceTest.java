package com.premierhub.service;

import com.premierhub.web.dto.MatchDetailResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:own-goal-evidence-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class OwnGoalEvidenceTest {
    private static final int FIXTURE_ID = 1208028;
    private static final Path EVIDENCE_FILE = Path.of("data/fixture-1208028-evidence.json");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FootballQueries queries;
    @Autowired private FixtureEvidenceService evidenceService;
    @Autowired private ObjectMapper mapper;

    private JsonNode evidence;

    @BeforeEach
    void seedFixture() throws Exception {
        evidence = mapper.readTree(Files.readString(EVIDENCE_FILE));
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        jdbc.update("INSERT INTO clubs VALUES (55, 'Brentford', 'Brentford')");
        jdbc.update("INSERT INTO clubs VALUES (52, 'Crystal Palace', 'London')");
        jdbc.update("INSERT INTO fixtures VALUES (1208028, 39, 2024, 55, 52, 1, "
                + "DATE '2024-08-18', 'FINISHED', 'FT', 2, 1, 'hash', CURRENT_TIMESTAMP)");

        Set<Integer> entered = new HashSet<>();
        for (JsonNode event : evidence.path("events")) {
            if ("subst".equals(event.path("type").asText())) {
                entered.add(event.path("assistId").asInt());
            }
        }
        Set<Integer> all = new HashSet<>();
        for (JsonNode lineup : evidence.path("lineups")) {
            int teamId = lineup.path("teamId").asInt();
            for (JsonNode player : lineup.path("starters")) {
                insertPlayer(player.asInt(), teamId, 90, all);
            }
            for (JsonNode player : lineup.path("bench")) {
                int id = player.asInt();
                insertPlayer(id, teamId, entered.contains(id) ? 30 : null, all);
            }
        }
        assertEquals(40, all.size());
    }

    @Test
    void ownGoalCountsForOpposingTeamWithoutPlayerGoalPointsAndImportIsIdempotent() throws Exception {
        var command = new FixtureEvidenceCommand(evidenceService, queries, mapper);
        var args = new DefaultApplicationArguments(
                "--premierhub.fixture-evidence.file=" + EVIDENCE_FILE);
        command.run(args);
        command.run(args);

        MatchDetailResponse detail = detail();
        assertEquals("VERIFIED", detail.evidenceStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM fixture_score_evidence", Integer.class));
        assertEquals(40, players(detail).filter(player ->
                "COMPLETE".equals(player.score().status())).count());
        assertEquals(2, detail.match().homeGoals());
        assertEquals(1, detail.match().awayGoals());

        MatchPlayerStatResponse ownGoalScorer = players(detail).filter(player ->
                player.playerId() == 19789).findFirst().orElseThrow();
        assertEquals(55, ownGoalScorer.clubId());
        assertNull(ownGoalScorer.goals());
        assertEquals(0, ownGoalScorer.inferred().goals());
        assertEquals(2, ownGoalScorer.score().confirmedPoints());
        assertEquals(0, ownGoalScorer.score().parts().stream()
                .filter(part -> "goals".equals(part.code())).findFirst().orElseThrow().points());
        assertNull(jdbc.queryForObject("SELECT goals FROM fixture_player_stats "
                + "WHERE fixture_id=? AND player_id=?", Integer.class, FIXTURE_ID, 19789));
    }

    @Test
    void wrongBeneficiaryDoesNotStoreEvidence() throws Exception {
        JsonNode changed = evidence.deepCopy();
        for (JsonNode event : changed.path("events")) {
            if ("Own Goal".equals(event.path("detail").asText())) {
                ((ObjectNode) event).put("teamId", 55);
            }
        }
        Path file = Files.createTempFile(Path.of("target"), "invalid-own-goal-", ".json");
        try {
            Files.writeString(file, changed.toString());
            var command = new FixtureEvidenceCommand(evidenceService, queries, mapper);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> command.run(new DefaultApplicationArguments(
                            "--premierhub.fixture-evidence.file=" + file)));
            assertTrue(error.getMessage().contains("phản lưới"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fixture_score_evidence", Integer.class));
            assertEquals("MISSING", detail().evidenceStatus());
            assertEquals(38, players(detail()).filter(player ->
                    "PROVISIONAL".equals(player.score().status())).count());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void insertPlayer(int id, int teamId, Integer minutes, Set<Integer> all) {
        assertTrue(all.add(id));
        jdbc.update("INSERT INTO players VALUES (?, ?)", id, "Player " + id);
        Integer goals = id == 20589 || id == 20649 ? 1 : null;
        Integer assists = minutes == null ? null : id == 20649 ? 1 : 0;
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (1208028, ?, ?, ?, ?, ?, ?, 0, 0, NULL, NULL, NULL, NULL, NULL,
                        '{}', CURRENT_TIMESTAMP)
                """, id, teamId, id == 19789 ? "D" : "F", minutes, goals, assists);
    }

    private MatchDetailResponse detail() {
        return queries.matchDetail(FIXTURE_ID, 2024).orElseThrow();
    }

    private Stream<MatchPlayerStatResponse> players(MatchDetailResponse detail) {
        return Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream());
    }
}
