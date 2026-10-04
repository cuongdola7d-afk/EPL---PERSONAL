package com.premierhub.web;

import com.premierhub.service.TeamOfWeekService;
import com.premierhub.web.dto.TeamOfWeekResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamOfWeekController {
    private final TeamOfWeekService service;

    public TeamOfWeekController(TeamOfWeekService service) {
        this.service = service;
    }

    @GetMapping("/api/fantasy/2026/team-of-week")
    public TeamOfWeekResponse team(@RequestParam(defaultValue = "1") int gameweek) {
        return service.team(gameweek);
    }
}
