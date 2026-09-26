package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.MatchResponse;
import com.premierhub.web.error.InvalidFilterException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static com.premierhub.web.ErrorResponseAssertions.expectError;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
class MatchControllerTest {
    private final MatchResponse match = new MatchResponse(1, 1, "Arsenal", 2, "Chelsea",
            1, LocalDate.of(2025, 8, 16), "FINISHED", 2, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballQueries service;

    @Test
    void getAllReturnsJsonArrayWithUnchangedFields() throws Exception {
        when(service.matches(2024, null, null, null)).thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].homeClub").value("Arsenal"))
                .andExpect(jsonPath("$[0].awayClub").value("Chelsea"))
                .andExpect(jsonPath("$[0].matchweek").value(1));
    }

    @Test
    void getByIdSucceedsAndMissingIdReturns404() throws Exception {
        when(service.match(1, 2024)).thenReturn(Optional.of(match));

        mockMvc.perform(get("/api/matches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
        expectError(mockMvc.perform(get("/api/matches/999")), HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "/api/matches/999");
    }

    @Test
    void validFiltersAndCombinationStillReturnMatches() throws Exception {
        when(service.matches(2024, "Arsenal", 1, " finished ")).thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches").param("club", "Arsenal")
                        .param("matchweek", "1").param("status", " finished "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void invalidWeekAndWrongTypeReturnDistinctCodes() throws Exception {
        expectError(mockMvc.perform(get("/api/matches").param("matchweek", "0")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/matches");
        expectError(mockMvc.perform(get("/api/matches").param("matchweek", "abc")),
                HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "/api/matches");
    }

    @Test
    void roundAliasFiltersSeason2026AndRejectsConflicts() throws Exception {
        when(service.matches(2026, null, 1, null)).thenReturn(List.of(match));
        mockMvc.perform(get("/api/matches?season=2026&round=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/matches?round=1&matchweek=2"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FILTER"));
        mockMvc.perform(get("/api/matches?round=0")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownStatusReturnsInvalidFilter() throws Exception {
        when(service.matches(2024, null, null, "UNKNOWN"))
                .thenThrow(new InvalidFilterException("Unknown status: UNKNOWN"));

        expectError(mockMvc.perform(get("/api/matches").param("status", "UNKNOWN")),
                HttpStatus.BAD_REQUEST, "INVALID_FILTER", "/api/matches");
    }

    @Test
    void blankOptionalFiltersAndInvalidIdReturnValidationError() throws Exception {
        expectError(mockMvc.perform(get("/api/matches").param("club", "")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/matches");
        expectError(mockMvc.perform(get("/api/matches").param("status", "   ")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/matches");
        expectError(mockMvc.perform(get("/api/matches/-1")), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "/api/matches/-1");
    }

    @Test
    void validFilterWithoutMatchesReturnsEmptyArray() throws Exception {
        when(service.matches(2024, "Unknown", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/matches").param("club", "Unknown"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

}
