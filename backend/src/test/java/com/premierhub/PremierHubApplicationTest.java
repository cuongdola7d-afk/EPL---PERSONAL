package com.premierhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PremierHubApplicationTest {
    @Autowired
    private WebApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void realServicesAndErrorAdviceWorkTogether() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/clubs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arsenal"));
        mockMvc.perform(get("/api/players").param("position", "STRIKER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
        mockMvc.perform(get("/api/matches").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
    }
}
