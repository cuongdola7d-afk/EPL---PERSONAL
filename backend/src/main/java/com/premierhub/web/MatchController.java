package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.MatchResponse;
import com.premierhub.web.dto.MatchDetailResponse;
import com.premierhub.web.error.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final FootballQueries service;

    public MatchController(FootballQueries service) {
        this.service = service;
    }

    @GetMapping
    public List<MatchResponse> getAll(
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "club must not be blank") String club,
            @RequestParam(required = false) @Min(1) Integer matchweek,
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "status must not be blank") String status,
            @RequestParam(defaultValue = "2024") int season) {
        return service.matches(season, club, matchweek, status);
    }

    @GetMapping("/{id}")
    public MatchResponse getById(@PathVariable @Positive int id,
                                 @RequestParam(defaultValue = "2024") int season) {
        return service.match(id, season)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }

    @GetMapping("/{id}/details")
    public MatchDetailResponse getDetails(@PathVariable @Positive int id,
                                          @RequestParam(defaultValue = "2024") int season) {
        return service.matchDetail(id, season)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }
}
