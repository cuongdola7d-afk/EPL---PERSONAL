package com.premierhub.web.dto;

import com.premierhub.model.Player;

public record PlayerResponse(int id, String name, int clubId, String club,
                             String position, Integer goals, Integer assists) {
    public static PlayerResponse from(Player player, String clubName) {
        return new PlayerResponse(player.getId(), player.getName(), player.getClubId(),
                clubName, player.getPosition().name(), player.getGoals(), player.getAssists());
    }
}
