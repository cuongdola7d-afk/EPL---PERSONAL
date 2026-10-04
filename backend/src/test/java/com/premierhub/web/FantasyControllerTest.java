package com.premierhub.web;

import com.premierhub.config.ApiCorsConfiguration;
import com.premierhub.service.FantasyLineupService;
import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.PlayerResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(FantasyController.class)
@Import({FantasyLineupService.class, ApiCorsConfiguration.class})
class FantasyControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private FootballQueries queries;
    @MockitoBean private com.premierhub.fantasy.GameweekService gameweeks;

    @Test
    void ignoresForgedOvrAndPositionsAndReturnsSlotSpecificDatabaseErrors() throws Exception {
        when(queries.players(2026, null, null, FantasyLineupService.AS_OF)).thenReturn(List.of(
                new PlayerResponse(1, "Luke Shaw", 1, "MU", "DEFENDER", null, null,
                        null, null, null, null, null, null, "LB", List.of("LB"), "VERIFIED")));
        mvc.perform(post("/api/fantasy/2026/validate").contentType("application/json")
                        .content("""
                                {"formation":"4-4-2","picks":{"2-1":1},
                                 "fc27Overall":1,"eligiblePositions":["CB"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.totalOvr").value(0))
                .andExpect(jsonPath("$.issues[?(@.code == 'OVR_MISSING')].playerId").value(org.hamcrest.Matchers.hasItem(1)))
                .andExpect(jsonPath("$.issues[?(@.code == 'POSITION')].slotKey").value(org.hamcrest.Matchers.hasItem("2-1")));
    }

    @Test
    void requiresFormationAndPicksAndDoesNotExposeAnOldSeasonEndpoint() throws Exception {
        mvc.perform(post("/api/fantasy/2026/validate").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/fantasy/2026/validate").contentType("application/json")
                        .content("{\"formation\":\"4-4-2\",\"picks\":{\"0-0\":\"not-an-id\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].code").value("REQUEST"));
        mvc.perform(post("/api/fantasy/2024/validate").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(handler().handlerType(org.springframework.web.servlet.resource.ResourceHttpRequestHandler.class));
    }

    @Test
    void allowsPostPreflightOnlyForValidationEndpoint() throws Exception {
        mvc.perform(options("/api/fantasy/2026/validate")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", "POST"));
        mvc.perform(options("/api/players").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
