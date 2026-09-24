package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.StandingResponse;
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

@WebMvcTest(StandingController.class)
class StandingControllerTest {
    private final StandingResponse arsenal = new StandingResponse(1, 1, "Arsenal",
            1, 1, 0, 0, 2, 1, 1, 3);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballQueries service;

    @Test
    void getAllReturnsJsonArrayWithUnchangedFields() throws Exception {
        when(service.standings(2024, null)).thenReturn(List.of(arsenal));

        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].clubId").value(1))
                .andExpect(jsonPath("$[0].clubName").value("Arsenal"))
                .andExpect(jsonPath("$[0].points").value(3));
    }

    @Test
    void findsClubAndReturns404WhenMissing() throws Exception {
        when(service.standing(1, 2024)).thenReturn(Optional.of(arsenal));

        mockMvc.perform(get("/api/standings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Arsenal"));
        expectError(mockMvc.perform(get("/api/standings/999")), HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "/api/standings/999");
    }

    @Test
    void validLimitAndLargerThanTableReturn200() throws Exception {
        when(service.standings(2024, 1)).thenReturn(List.of(arsenal));
        when(service.standings(2024, 50)).thenReturn(List.of(arsenal));

        mockMvc.perform(get("/api/standings").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/standings").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void invalidLimitAndWrongTypeReturnDistinctCodes() throws Exception {
        expectError(mockMvc.perform(get("/api/standings").param("limit", "0")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/standings");
        expectError(mockMvc.perform(get("/api/standings").param("limit", "abc")),
                HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "/api/standings");
        expectError(mockMvc.perform(get("/api/standings/0")), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "/api/standings/0");
    }

    @Test
    void emptyDataReturns200WithEmptyArray() throws Exception {
        when(service.standings(2024, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
