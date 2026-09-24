package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.StandingResponse;
import com.premierhub.web.error.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/standings")
public class StandingController {
    private final FootballQueries service;

    public StandingController(FootballQueries service) {
        this.service = service;
    }

    @GetMapping
    public List<StandingResponse> getAll(@RequestParam(required = false) @Min(1) Integer limit,
                                         @RequestParam(defaultValue = "2024") int season) {
        return service.standings(season, limit);
    }

    @GetMapping("/{clubId}")
    public StandingResponse getByClubId(@PathVariable @Positive int clubId,
                                        @RequestParam(defaultValue = "2024") int season) {
        return service.standing(clubId, season)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Standing not found for club: " + clubId));
    }
}
