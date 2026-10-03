package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.service.ClubStatisticsService;
import com.premierhub.web.dto.ClubStatisticsResponse;
import com.premierhub.web.dto.ClubResponse;
import com.premierhub.web.error.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
    private final FootballQueries service;
    private final ClubStatisticsService statistics;

    public ClubController(FootballQueries service, ClubStatisticsService statistics) {
        this.service = service;
        this.statistics = statistics;
    }

    @GetMapping
    public List<ClubResponse> getAll(@RequestParam(defaultValue = "2024") int season) {
        return service.clubs(season, null);
    }

    @GetMapping("/{id}")
    public ClubResponse getById(@PathVariable @Positive int id,
                                @RequestParam(defaultValue = "2024") int season) {
        return service.club(id, season)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + id));
    }

    @GetMapping("/search")
    public List<ClubResponse> search(
            @RequestParam @NotBlank(message = "keyword must not be blank") String keyword,
            @RequestParam(defaultValue = "2024") int season) {
        return service.clubs(season, keyword);
    }

    @GetMapping("/{id}/statistics")
    public ClubStatisticsResponse getStatistics(@PathVariable @Positive int id,
                                                @RequestParam(defaultValue = "2026") int season) {
        service.club(id, season)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + id));
        return statistics.statistics(id, season);
    }
}
