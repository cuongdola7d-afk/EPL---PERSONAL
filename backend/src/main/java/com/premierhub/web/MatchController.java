package com.premierhub.web;

import com.premierhub.model.Match;
import com.premierhub.service.MatchService;
import com.premierhub.web.dto.MatchResponse;
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
    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    @GetMapping
    public List<MatchResponse> getAll(
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "club must not be blank") String club,
            @RequestParam(required = false) @Min(1) Integer matchweek,
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "status must not be blank") String status) {
        return service.findMatches(club, matchweek, status).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MatchResponse getById(@PathVariable @Positive int id) {
        return service.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }

    private MatchResponse toResponse(Match match) {
        return MatchResponse.from(match, service.clubName(match.getHomeClubId()),
                service.clubName(match.getAwayClubId()));
    }
}
