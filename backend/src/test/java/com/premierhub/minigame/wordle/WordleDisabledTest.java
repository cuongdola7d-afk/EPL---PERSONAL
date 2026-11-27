package com.premierhub.minigame.wordle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wordle-disabled;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql,classpath:auth-schema.sql,classpath:fantasy-gameweek-schema.sql,classpath:fantasy-entry-schema.sql,classpath:fantasy-result-schema.sql",
        "premierhub.wordle.enabled=false",
        "premierhub.minigame.enabled=false",
        "premierhub.auth-proxy.enabled=false",
        "PREMIERHUB_GOOGLE_CLIENT_ID=",
        "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class WordleDisabledTest {
    @Autowired MockMvc mvc;

    @Test
    void returns404WhenWordleFeatureFlagIsDisabled() throws Exception {
        mvc.perform(get("/api/minigame/2026/wordle/today")
                        .with(user("a@example.test").roles("USER"))
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedInfoReturns404WhenWordleFeatureFlagIsDisabled() throws Exception {
        mvc.perform(get("/api/minigame/2026/wordle/info"))
                .andExpect(status().isNotFound());
    }
}
