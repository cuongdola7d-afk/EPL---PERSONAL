package com.premierhub.diagnostics;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:read-latency-context;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "premierhub.auth-proxy.enabled=false",
        "PREMIERHUB_GOOGLE_CLIENT_ID=", "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "premierhub.diagnostics.read-latency.enabled=true", "premierhub.diagnostics.read-latency.probe-enabled=true",
        "PREMIERHUB_READ_LATENCY_KEY=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "PREMIERHUB_AUTH_PROXY_SECRET=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
        "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class ReadLatencyContextTest {
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test void enabledInstrumentationStartsWithBootAndMeasuresActualPublicApi() throws Exception {
        assertInstanceOf(MeasuredDataSource.class, source);
        assertTrue(source.unwrap(HikariDataSource.class).isRunning());
        mvc.perform(get("/api/clubs").param("season", "2026"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"));
        assertNull(ReadTrace.current());
        mvc.perform(get(ReadLatencyFilter.PROBE_PATH).param("workload", "select1")
                        .header("X-PrismaXI-Read-Latency-Key", "a".repeat(64))
                        .header("X-PrismaXI-Proxy-Secret", "b".repeat(64)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.measurement.statements").value(1))
                .andExpect(jsonPath("$.measurement.acquisitions").value(1));
        mvc.perform(get(ReadLatencyFilter.PROBE_PATH).param("workload", "select1"))
                .andExpect(status().isNotFound());
        assertNull(ReadTrace.current());
    }
}
