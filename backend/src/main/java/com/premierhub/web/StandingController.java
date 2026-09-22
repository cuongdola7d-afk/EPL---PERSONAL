package com.premierhub.web;

import com.premierhub.service.StandingService;
import com.premierhub.web.dto.StandingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/standings")
public class StandingController {
    private final StandingService service;

    public StandingController(StandingService service) {
        this.service = service;
    }

    @GetMapping
    public List<StandingResponse> getAll(@RequestParam(required = false) Integer limit) {
        if (limit != null && limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Limit must be positive");
        }
        return service.getStandings(limit).stream()
                .map(StandingResponse::from).toList();
    }

    @GetMapping("/{clubId}")
    public StandingResponse getByClubId(@PathVariable int clubId) {
        return service.findByClubId(clubId).map(StandingResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Standing not found for club: " + clubId));
    }
}
