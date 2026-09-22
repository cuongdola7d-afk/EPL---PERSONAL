package com.premierhub.web;

import com.premierhub.service.ClubService;
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
    private final ClubService service;

    public ClubController(ClubService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClubResponse> getAll() {
        return service.getAll().stream()
                .map(ClubResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClubResponse getById(@PathVariable @Positive int id) {
        return service.findById(id)
                .map(ClubResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + id));
    }

    @GetMapping("/search")
    public List<ClubResponse> search(
            @RequestParam @NotBlank(message = "keyword must not be blank") String keyword) {
        return service.searchByName(keyword).stream()
                .map(ClubResponse::from)
                .toList();
    }
}
