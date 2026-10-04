package com.premierhub.web;

import com.premierhub.service.TeamOfWeekService;
import com.premierhub.repository.TeamOfWeekRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamOfWeekController.class)
@Import(TeamOfWeekService.class)
class TeamOfWeekControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private TeamOfWeekRepository repository;

    @Test
    void exposesMissingDataForEverySupportedWeekButNoLegacyEndpoint() throws Exception {
        for (int week = 1; week <= 5; week++) {
            when(repository.read(week)).thenReturn(new TeamOfWeekRepository.Pool(List.of(), 0, 0));
            mvc.perform(get("/api/fantasy/2026/team-of-week").param("gameweek", String.valueOf(week)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.season").value(2026))
                    .andExpect(jsonPath("$.gameweek").value(week)).andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"))
                    .andExpect(jsonPath("$.totalRating").isEmpty()).andExpect(jsonPath("$.missingSlots.length()").value(11))
                    .andExpect(jsonPath("$.picks.length()").value(11));
        }
        mvc.perform(get("/api/fantasy/2024/team-of-week"))
                .andExpect(handler().handlerType(org.springframework.web.servlet.resource.ResourceHttpRequestHandler.class));
    }

    @Test
    void invalidWeekIsRejectedInsteadOfSilentlyUsingAnotherRound() throws Exception {
        mvc.perform(get("/api/fantasy/2026/team-of-week?gameweek=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fantasy/2026/team-of-week?gameweek=6")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fantasy/2026/team-of-week?gameweek=abc")).andExpect(status().isBadRequest());
    }
}
