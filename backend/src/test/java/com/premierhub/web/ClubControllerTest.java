package com.premierhub.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ClubControllerTest {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getAllReturnsJsonArray() throws Exception {
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void getExistingClubReturnsClub() throws Exception {
        mockMvc.perform(get("/api/clubs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Arsenal"))
                .andExpect(jsonPath("$.city").value("London"));
    }

    @Test
    void getMissingClubReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/clubs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchReturnsMatchingClubs() throws Exception {
        mockMvc.perform(get("/api/clubs/search").param("keyword", "united"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].name").value("Manchester United"))
                .andExpect(jsonPath("$[0].city").value("Manchester"));
    }

    @Test
    void blankSearchKeywordReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/clubs/search").param("keyword", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingSearchKeywordReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/clubs/search"))
                .andExpect(status().isBadRequest());
    }
}
