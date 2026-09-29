package com.premierhub.web;

import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.dto.PlayerMatchResponse;
import com.premierhub.web.error.ResourceNotFoundException;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final FootballQueries service;

    public PlayerController(FootballQueries service) {
        this.service = service;
    }

    @GetMapping
    public List<PlayerResponse> getAll(
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "club must not be blank") String club,
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "position must not be blank") String position,
            @RequestParam(defaultValue = "2024") int season,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return asOf == null ? service.players(season, club, position)
                : service.players(season, club, position, asOf);
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable @Positive int id,
                                  @RequestParam(defaultValue = "2024") int season,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return (asOf == null ? service.player(id, season) : service.player(id, season, asOf))
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    @GetMapping("/{id}/matches")
    public List<PlayerMatchResponse> getMatches(@PathVariable @Positive int id,
                                                @RequestParam(defaultValue = "2024") int season) {
        if (season == 2026) {
            if (!service.hasPlayerInSeason(id, season)) {
                throw new ResourceNotFoundException("Player not found: " + id);
            }
        } else {
            service.player(id, season)
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
        }
        return service.playerMatches(id, season);
    }
}
