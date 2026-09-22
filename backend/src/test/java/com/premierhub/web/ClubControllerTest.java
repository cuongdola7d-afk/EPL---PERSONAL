package com.premierhub.web;

import com.premierhub.model.Club;
import com.premierhub.service.ClubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.List;
import java.util.Optional;
import static com.premierhub.web.ErrorResponseAssertions.expectError;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
class ClubControllerTest {
    private final Club arsenal = new Club(1, "Arsenal", "London");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubService service;

    @Test
    void getAllReturnsJsonArray() throws Exception {
        when(service.getAll()).thenReturn(List.of(arsenal));

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Arsenal"));
    }

    @Test
    void getExistingClubReturnsSameJsonFields() throws Exception {
        when(service.findById(1)).thenReturn(Optional.of(arsenal));

        mockMvc.perform(get("/api/clubs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Arsenal"))
                .andExpect(jsonPath("$.city").value("London"));
    }

    @Test
    void missingClubReturnsStructured404() throws Exception {
        when(service.findById(999)).thenReturn(Optional.empty());

        expectError(mockMvc.perform(get("/api/clubs/999")), HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "/api/clubs/999");
    }

    @Test
    void invalidIdReturnsValidationErrorAndWrongTypeReturnsTypeMismatch() throws Exception {
        expectError(mockMvc.perform(get("/api/clubs/0")), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "/api/clubs/0");
        expectError(mockMvc.perform(get("/api/clubs/-1")), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "/api/clubs/-1");
        expectError(mockMvc.perform(get("/api/clubs/abc")), HttpStatus.BAD_REQUEST,
                "TYPE_MISMATCH", "/api/clubs/abc");
    }

    @Test
    void searchReturnsMatches() throws Exception {
        when(service.searchByName("arsenal")).thenReturn(List.of(arsenal));

        mockMvc.perform(get("/api/clubs/search").param("keyword", "arsenal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Arsenal"));
    }

    @Test
    void blankAndMissingKeywordReturnDistinctCodes() throws Exception {
        expectError(mockMvc.perform(get("/api/clubs/search").param("keyword", "   ")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/clubs/search");
        expectError(mockMvc.perform(get("/api/clubs/search")), HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER", "/api/clubs/search");
    }

    @Test
    void unexpectedExceptionHidesInternalMessage() throws Exception {
        when(service.getAll()).thenThrow(new IllegalStateException("internal detail"));

        ResultActions result = mockMvc.perform(get("/api/clubs"));
        expectError(result, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "/api/clubs");
        result.andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
