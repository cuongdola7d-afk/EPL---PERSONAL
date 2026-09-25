package com.premierhub.seed;

import com.premierhub.service.FootballQueries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gw1-snapshot-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "debug=false"
})
class Gw1SnapshotServiceTest {
    @Autowired private Gw1SnapshotService snapshots;
    @Autowired private FootballQueries queries;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext context;

    @Test
    void cleanDatabaseImportServesFourApisAndTenCompleteFixturesWithoutDuplicates() throws Exception {
        assertEquals(0, count("fixture_player_stats"));
        var first = snapshots.importBundled();
        assertEquals(400, first.inserted().get("fixture_player_stats"));
        assertEquals(10, first.inserted().get("fixture_score_evidence"));
        assertEquals(10, first.verifiedFixtures());

        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/api/clubs").param("season", "2024"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20));
        mvc.perform(get("/api/players").param("season", "2024"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(63));
        mvc.perform(get("/api/matches").param("season", "2024").param("matchweek", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
        mvc.perform(get("/api/standings").param("season", "2024"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20));

        for (var match : queries.matches(2024, null, 1, null)) {
            var detail = queries.matchDetail(match.id(), 2024).orElseThrow();
            assertEquals("VERIFIED", detail.evidenceStatus(), "fixture " + match.id());
            assertEquals(40, Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream())
                    .filter(player -> "COMPLETE".equals(player.score().status())).count(),
                    "fixture " + match.id());
            mvc.perform(get("/api/matches/{id}/details", match.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evidenceStatus").value("VERIFIED"));
        }

        int rawNulls = jdbc.queryForObject("SELECT COUNT(*) FROM fixture_player_stats WHERE minutes IS NULL", Integer.class);
        assertTrue(rawNulls > 0, "Provider NULL values must remain in the snapshot");
        var second = snapshots.importBundled();
        assertTrue(second.inserted().values().stream().allMatch(value -> value == 0));
        assertEquals(400, count("fixture_player_stats"));
        assertEquals(10, count("fixture_score_evidence"));
        assertEquals(rawNulls,
                jdbc.queryForObject("SELECT COUNT(*) FROM fixture_player_stats WHERE minutes IS NULL", Integer.class));

        jdbc.update("UPDATE clubs SET name=? WHERE id=?", "Local production edit", matchClubId());
        var conflict = assertThrows(IllegalStateException.class, snapshots::importBundled);
        assertTrue(conflict.getMessage().contains("Không ghi đè"));
        assertEquals(400, count("fixture_player_stats"));
        assertEquals(10, count("fixture_score_evidence"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int matchClubId() {
        return queries.clubs(2024, null).getFirst().id();
    }
}
