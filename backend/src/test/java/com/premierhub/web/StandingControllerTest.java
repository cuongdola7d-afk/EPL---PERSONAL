package com.premierhub.web;

import com.premierhub.repository.InMemoryMatchRepository;
import com.premierhub.service.LeagueTableService;
import com.premierhub.service.StandingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class StandingControllerTest {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getAllReturnsOrderedJsonArrayWithExpectedFields() throws Exception {
        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].clubId").value(3))
                .andExpect(jsonPath("$[0].clubName").value("Liverpool"))
                .andExpect(jsonPath("$[0].played").value(3))
                .andExpect(jsonPath("$[0].won").value(1))
                .andExpect(jsonPath("$[0].drawn").value(2))
                .andExpect(jsonPath("$[0].lost").value(0))
                .andExpect(jsonPath("$[0].goalsFor").value(3))
                .andExpect(jsonPath("$[0].goalsAgainst").value(1))
                .andExpect(jsonPath("$[0].goalDifference").value(2))
                .andExpect(jsonPath("$[0].points").value(5));
    }

    @Test
    void findsStandingByClubIdAndMissingIdReturns404() throws Exception {
        mockMvc.perform(get("/api/standings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Arsenal"))
                .andExpect(jsonPath("$.position").value(3));
        mockMvc.perform(get("/api/standings/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void limitReturnsTopFiveAndInvalidLimitReturns400() throws Exception {
        mockMvc.perform(get("/api/standings").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[4].position").value(5));
        mockMvc.perform(get("/api/standings").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyDataReturns200WithEmptyArray() throws Exception {
        StandingService empty = new StandingService(new LeagueTableService(),
                new InMemoryMatchRepository(List.of()), List.of());
        MockMvc emptyMvc = MockMvcBuilders.standaloneSetup(new StandingController(empty)).build();

        emptyMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
