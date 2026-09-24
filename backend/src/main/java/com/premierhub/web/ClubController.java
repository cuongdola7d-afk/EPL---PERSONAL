package com.premierhub.web;

import com.premierhub.service.FootballQueries;
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

    public ClubController(FootballQueries service) {
        this.service = service;
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
}
