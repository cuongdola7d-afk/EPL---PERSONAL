package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.dto.PlayerMatchResponse;
import com.premierhub.web.dto.MatchResponse;
import com.premierhub.web.error.InvalidFilterException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import static com.premierhub.web.ErrorResponseAssertions.expectError;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {
    private final PlayerResponse saka = new PlayerResponse(2, "Bukayo Saka", 1,
            "Arsenal", "FORWARD", 12, 10, null, null, null, null, null, null);

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
    void currentSeasonExposesOverallWithoutInventingMissingRatings() throws Exception {
        LocalDate asOf = LocalDate.of(2026, 10, 2);
        when(service.players(2026, null, null, asOf)).thenReturn(List.of(
                new PlayerResponse(2, "Bukayo Saka", 1, "Arsenal", "FORWARD", null, null, 88,
                        "England", LocalDate.of(2001, 9, 5), 178, "LEFT", 7),
                new PlayerResponse(3, "Bendito Mantato", 4, "Manchester United", "FORWARD",
                        null, null, null, null, null, null, null, null)));

        mockMvc.perform(get("/api/players").param("season", "2026").param("asOf", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fc27Overall").value(88))
                .andExpect(jsonPath("$[0].nationality").value("England"))
                .andExpect(jsonPath("$[0].birthDate").value("2001-09-05"))
                .andExpect(jsonPath("$[0].heightCm").value(178))
                .andExpect(jsonPath("$[0].preferredFoot").value("LEFT"))
                .andExpect(jsonPath("$[0].shirtNumber").value(7))
                .andExpect(jsonPath("$[1].fc27Overall").value(org.hamcrest.Matchers.nullValue()));
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
    void matchHistoryIsScopedByPlayerAndSeasonAndUnknownPlayerReturns404() throws Exception {
        when(service.player(2, 2024)).thenReturn(Optional.of(saka));
        when(service.playerMatches(2, 2024)).thenReturn(List.of(new PlayerMatchResponse(
                new MatchResponse(1208021, 1, "Arsenal", 3, "Fulham", 1,
                        LocalDate.of(2024, 8, 16), "FINISHED", 1, 0), 1, null, null)));
        mockMvc.perform(get("/api/players/2/matches").param("season", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].match.id").value(1208021))
                .andExpect(jsonPath("$[0].clubId").value(1))
                .andExpect(jsonPath("$[0].stats").value(org.hamcrest.Matchers.nullValue()));
        expectError(mockMvc.perform(get("/api/players/999/matches").param("season", "2026")),
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "/api/players/999/matches");
    }

    @Test
    void pastSeasonMemberCanReadHistoryAfterMembershipEnds() throws Exception {
        when(service.hasPlayerInSeason(2, 2026)).thenReturn(true);
        when(service.playerMatches(2, 2026)).thenReturn(List.of());

        mockMvc.perform(get("/api/players/2/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
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
