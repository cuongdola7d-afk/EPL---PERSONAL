package com.premierhub.web;

import com.premierhub.model.Player;
import com.premierhub.service.PlayerService;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.error.ResourceNotFoundException;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlayerResponse> getAll(
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "club must not be blank") String club,
            @RequestParam(required = false)
            @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "position must not be blank") String position) {
        return service.findPlayers(club, position).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable @Positive int id) {
        return service.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private PlayerResponse toResponse(Player player) {
        String clubName = service.findClubName(player.getClubId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown club id: " + player.getClubId()));
        return PlayerResponse.from(player, clubName);
    }
}
