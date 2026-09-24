package com.premierhub.config;

import com.premierhub.service.ClubService;
import com.premierhub.web.ClubController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClubController.class)
@Import(ApiCorsConfiguration.class)
@TestPropertySource(properties = "PREMIERHUB_CORS_ALLOWED_ORIGINS=https://premierhub.vercel.app/,https://preview.vercel.app")
class ApiCorsConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubService service;

    @Test
    void allowsConfiguredProductionOriginWithoutChangingSuccessfulApiResponse() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/clubs").header("Origin", "https://premierhub.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://premierhub.vercel.app"));
    }

    @Test
    void allowsLocalAndExplicitPreviewOrigins() throws Exception {
        mockMvc.perform(get("/api/clubs").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/clubs")
                        .header("Origin", "https://preview.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://preview.vercel.app"));
    }

    @Test
    void rejectsUnlistedOrigin() throws Exception {
        mockMvc.perform(get("/api/clubs").header("Origin", "https://other.vercel.app"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(options("/api/clubs")
                        .header("Origin", "https://other.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rejectsWildcardConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ApiCorsConfiguration("*"));
    }
}
