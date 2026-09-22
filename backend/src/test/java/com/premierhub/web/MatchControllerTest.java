package com.premierhub.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class MatchControllerTest {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getAllReturnsJsonArray() throws Exception {
        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void getByIdReturnsMatchAndMissingIdReturns404() throws Exception {
        mockMvc.perform(get("/api/matches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeClub").value("Arsenal"))
                .andExpect(jsonPath("$.matchweek").value(1));
        mockMvc.perform(get("/api/matches/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtersByClubIncludingAwayTeam() throws Exception {
        mockMvc.perform(get("/api/matches").param("club", "  arsenal  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].awayClub").value("Arsenal"));
    }

    @Test
    void filtersByMatchweekAndCombinesWithClub() throws Exception {
        mockMvc.perform(get("/api/matches").param("matchweek", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/matches").param("club", "Arsenal")
                        .param("matchweek", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void filtersByStatus() throws Exception {
        mockMvc.perform(get("/api/matches").param("status", " scheduled "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(8));
    }

    @Test
    void invalidMatchweekReturns400AndNoResultReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/matches").param("matchweek", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/matches").param("club", "Unknown"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
