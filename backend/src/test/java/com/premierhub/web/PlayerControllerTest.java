package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.error.InvalidFilterException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static com.premierhub.web.ErrorResponseAssertions.expectError;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {
    private final PlayerResponse saka = new PlayerResponse(2, "Bukayo Saka", 1,
            "Arsenal", "FORWARD", 12, 10);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballQueries service;

    @Test
    void getAllReturnsJsonArrayWithUnchangedFields() throws Exception {
        when(service.players(2024, null, null)).thenReturn(List.of(saka));

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].club").value("Arsenal"))
                .andExpect(jsonPath("$[0].position").value("FORWARD"));
    }

    @Test
    void getByIdSucceedsAndMissingIdReturns404() throws Exception {
        when(service.player(2, 2024)).thenReturn(Optional.of(saka));

        mockMvc.perform(get("/api/players/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bukayo Saka"));
        expectError(mockMvc.perform(get("/api/players/999")), HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "/api/players/999");
    }

    @Test
    void validPositionAndCombinedFiltersStillReturnPlayers() throws Exception {
        when(service.players(2024, null, " forward ")).thenReturn(List.of(saka));
        when(service.players(2024, "Arsenal", "Forward")).thenReturn(List.of(saka));

        mockMvc.perform(get("/api/players").param("position", " forward "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
        mockMvc.perform(get("/api/players").param("club", "Arsenal")
                        .param("position", "Forward"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void unknownPositionReturnsInvalidFilter() throws Exception {
        when(service.players(2024, null, "STRIKER"))
                .thenThrow(new InvalidFilterException("Unknown position: STRIKER"));

        expectError(mockMvc.perform(get("/api/players").param("position", "STRIKER")),
                HttpStatus.BAD_REQUEST, "INVALID_FILTER", "/api/players");
    }

    @Test
    void blankOptionalFiltersAndInvalidIdReturnValidationError() throws Exception {
        expectError(mockMvc.perform(get("/api/players").param("club", "  ")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/players");
        expectError(mockMvc.perform(get("/api/players").param("position", "")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/players");
        expectError(mockMvc.perform(get("/api/players/0")), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "/api/players/0");
    }

    @Test
    void validFilterWithoutMatchesReturnsEmptyArray() throws Exception {
        when(service.players(2024, "Unknown", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/players").param("club", "Unknown"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
