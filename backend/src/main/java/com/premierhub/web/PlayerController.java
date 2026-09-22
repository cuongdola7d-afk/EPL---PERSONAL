package com.premierhub.web;

import com.premierhub.model.Player;
import com.premierhub.service.PlayerService;
import com.premierhub.web.dto.PlayerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlayerResponse> getAll(@RequestParam(required = false) String club,
                                       @RequestParam(required = false) String position) {
        return service.findPlayers(club, position).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable int id) {
        return service.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Player not found: " + id));
    }

    private PlayerResponse toResponse(Player player) {
        String clubName = service.findClubName(player.getClubId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown club id: " + player.getClubId()));
        return PlayerResponse.from(player, clubName);
    }
}
