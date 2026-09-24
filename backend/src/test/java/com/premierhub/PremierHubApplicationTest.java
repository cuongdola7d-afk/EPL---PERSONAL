package com.premierhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:premierhub-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class PremierHubApplicationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void realServicesAndErrorAdviceWorkTogether() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/clubs/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/api/players").param("position", "STRIKER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
        mockMvc.perform(get("/api/matches").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
    }

    @Test
    void healthIsPublicWithoutInternalDetailsAndSensitiveEndpointIsHidden() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
        int envStatus = mockMvc.perform(get("/actuator/env"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(200, envStatus);
    }

    @Test
    void oldApiRoutesReadDatabaseAndKeepJsonFields() throws Exception {
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        jdbc.update("INSERT INTO clubs VALUES (42, 'Arsenal', 'London')");
        jdbc.update("INSERT INTO clubs VALUES (43, 'Chelsea', 'London')");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2024, 42)");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2024, 43)");
        jdbc.update("INSERT INTO players VALUES (101, 'Sample Player')");
        jdbc.update("INSERT INTO player_season_stats VALUES (39, 2024, 101, 42, 'FORWARD', 1, 90, 1, 0)");
        jdbc.update("INSERT INTO fixtures VALUES (800, 39, 2024, 42, 43, 1, DATE '2024-08-16', "
                + "'FINISHED', 'FT', 2, 1, 'hash', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO standings VALUES (39, 2024, 42, 1, 1, 1, 0, 0, 2, 1, 1, 3)");
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Arsenal"));
        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sample Player"));
        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].homeClub").value("Arsenal"));
        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].points").value(3));
    }

    @Test
    void matchDetailsReadBothTeamsAndPreserveMissingStatistics() throws Exception {
        jdbc.update("INSERT INTO seasons VALUES (39, 2024)");
        jdbc.update("INSERT INTO clubs VALUES (42, 'Arsenal', 'London')");
        jdbc.update("INSERT INTO clubs VALUES (43, 'Chelsea', 'London')");
        jdbc.update("INSERT INTO players VALUES (101, 'Home Player')");
        jdbc.update("INSERT INTO players VALUES (102, 'Away Player')");
        jdbc.update("INSERT INTO fixtures VALUES (800, 39, 2024, 42, 43, 1, DATE '2024-08-16', "
                + "'FINISHED', 'FT', 2, 1, 'hash', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO fixtures VALUES (801, 39, 2024, 42, 43, 1, DATE '2024-08-17', "
                + "'SCHEDULED', 'NS', NULL, NULL, 'hash', CURRENT_TIMESTAMP)");
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (800, 101, 42, 'F', 90, 1, NULL, 0, 0, '8.1', 2, 3, NULL, NULL,
                        '{}', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (800, 102, 43, 'G', NULL, NULL, NULL, NULL, NULL, NULL,
                        NULL, NULL, NULL, NULL, '{}', CURRENT_TIMESTAMP)
                """);
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/matches/800/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.id").value(800))
                .andExpect(jsonPath("$.homePlayers.length()").value(1))
                .andExpect(jsonPath("$.awayPlayers.length()").value(1))
                .andExpect(jsonPath("$.homePlayers[0].goals").value(1))
                .andExpect(jsonPath("$.homePlayers[0].assists").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.awayPlayers[0].minutes").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.awayPlayers[0].yellowCards").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(get("/api/matches/801/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homePlayers.length()").value(0))
                .andExpect(jsonPath("$.awayPlayers.length()").value(0));
        mockMvc.perform(get("/api/matches/999/details"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
