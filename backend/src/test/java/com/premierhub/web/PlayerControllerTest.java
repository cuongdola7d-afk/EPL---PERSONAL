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
class PlayerControllerTest {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getAllReturnsArray() throws Exception {
        mockMvc.perform(get("/api/players")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void filtersByClub() throws Exception {
        mockMvc.perform(get("/api/players").param("club", "  arsenal "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].club").value("Arsenal"));
    }

    @Test
    void filtersByPosition() throws Exception {
        mockMvc.perform(get("/api/players").param("position", " forward "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void combinesFiltersAndReturnsEmptyArrayForNoMatch() throws Exception {
        mockMvc.perform(get("/api/players").param("club", "Arsenal")
                        .param("position", "Forward"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2));
        mockMvc.perform(get("/api/players").param("club", "Unknown"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void getByIdReturnsPlayer() throws Exception {
        mockMvc.perform(get("/api/players/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bukayo Saka"))
                .andExpect(jsonPath("$.position").value("FORWARD"));
    }

    @Test
    void missingIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/players/999")).andExpect(status().isNotFound());
    }
}
