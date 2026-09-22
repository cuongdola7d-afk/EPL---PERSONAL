package com.premierhub.web;

import com.premierhub.model.Match;
import com.premierhub.service.MatchService;
import com.premierhub.web.dto.MatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    @GetMapping
    public List<MatchResponse> getAll(@RequestParam(required = false) String club,
                                      @RequestParam(required = false) Integer matchweek,
                                      @RequestParam(required = false) String status) {
        if (matchweek != null && matchweek < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Matchweek must be positive");
        }
        return service.findMatches(club, matchweek, status).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MatchResponse getById(@PathVariable int id) {
        return service.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Match not found: " + id));
    }

    private MatchResponse toResponse(Match match) {
        return MatchResponse.from(match, service.clubName(match.getHomeClubId()),
                service.clubName(match.getAwayClubId()));
    }
}
