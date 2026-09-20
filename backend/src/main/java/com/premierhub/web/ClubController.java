package com.premierhub.web;

import com.premierhub.service.PremierHubService;
import com.premierhub.web.dto.ClubResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
    private final PremierHubService service;

    public ClubController(PremierHubService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClubResponse> getAll() {
        return service.getClubs().stream()
                .map(ClubResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClubResponse getById(@PathVariable int id) {
        return service.findClubById(id)
                .map(ClubResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Club not found: " + id));
    }

    @GetMapping("/search")
    public List<ClubResponse> search(
            @RequestParam @NotBlank(message = "keyword must not be blank") String keyword) {
        return service.findClubsByName(keyword).stream()
                .map(ClubResponse::from)
                .toList();
    }
}
