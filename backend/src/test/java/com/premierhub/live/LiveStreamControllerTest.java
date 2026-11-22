package com.premierhub.live;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LiveStreamControllerTest {

    @Nested
    @SpringBootTest(properties = {
            "spring.datasource.url=jdbc:h2:mem:live-stream-default-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "premierhub.live.scheduling.enabled=false",
            "premierhub.live.test-stream.enabled=false",
            "PREMIERHUB_CORS_ALLOWED_ORIGINS=https://premierhub.vercel.app"
    })
    @AutoConfigureMockMvc
    class DefaultModeTests {
        @Autowired
        private MockMvc mockMvc;

        @Test
        void mainStreamReturnsSseHeadersAndImmediateHelloAndSnapshotEvents() throws Exception {
            mockMvc.perform(get("/api/live/stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, no-transform"))
                    .andExpect(header().string("X-Accel-Buffering", "no"))
                    .andExpect(header().string(HttpHeaders.CONNECTION, "keep-alive"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(content().string(containsString("event:hello")))
                    .andExpect(content().string(containsString("\"connectionId\":\"sse-")))
                    .andExpect(content().string(containsString("\"serverTime\":")))
                    .andExpect(content().string(containsString("event:snapshot")));

            mockMvc.perform(get("/api/live/matches").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void testStreamReturns404WhenDisabled() throws Exception {
            mockMvc.perform(get("/api/live/test-stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("RESOURCE_NOT_FOUND")));
        }

        @Test
        void corsAllowsConfiguredVercelOriginAndRejectsUnknownOrigin() throws Exception {
            mockMvc.perform(get("/api/live/stream")
                            .header(HttpHeaders.ORIGIN, "https://premierhub.vercel.app")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://premierhub.vercel.app"));

            mockMvc.perform(get("/api/live/stream")
                            .header(HttpHeaders.ORIGIN, "https://untrusted.example.com")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "spring.datasource.url=jdbc:h2:mem:live-stream-enabled-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "premierhub.live.scheduling.enabled=false",
            "premierhub.live.test-stream.enabled=true",
            "premierhub.live.max-connections=2"
    })
    @AutoConfigureMockMvc
    class EnabledTestStreamAndCapacityTests {
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private LiveStreamService liveStreamService;

        @Test
        void testStreamEmitsHelloAndFormattedTicksAndEnforcesCapacityLimit() throws Exception {
            MvcResult conn1 = mockMvc.perform(get("/api/live/test-stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, no-transform"))
                    .andExpect(header().string("X-Accel-Buffering", "no"))
                    .andExpect(header().string(HttpHeaders.CONNECTION, "keep-alive"))
                    .andReturn();

            liveStreamService.sendTestStreamTicks();

            String body1 = conn1.getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertTrue(body1.contains("event:hello"), body1);
            org.junit.jupiter.api.Assertions.assertTrue(body1.contains("event:tick"), body1);
            org.junit.jupiter.api.Assertions.assertTrue(body1.contains("\"seq\":1"), body1);
            org.junit.jupiter.api.Assertions.assertTrue(body1.contains("\"serverTime\":"), body1);
            org.junit.jupiter.api.Assertions.assertTrue(body1.contains("\"connectionId\":\"sse-"), body1);

            // Second connection reaches max-connections=2
            mockMvc.perform(get("/api/live/stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk());

            // Third connection gets 503 Service Unavailable
            mockMvc.perform(get("/api/live/stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(containsString("LIVE_STREAM_CAPACITY_EXCEEDED")));
        }
    }
}
