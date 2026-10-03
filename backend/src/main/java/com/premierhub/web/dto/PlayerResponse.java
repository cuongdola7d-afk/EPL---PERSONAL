package com.premierhub.web.dto;

import com.premierhub.model.Player;
import java.time.LocalDate;
import java.util.List;

public record PlayerResponse(int id, String name, int clubId, String club,
                             String position, Integer goals, Integer assists, Integer fc27Overall,
                             String nationality, LocalDate birthDate, Integer heightCm,
                             String preferredFoot, Integer shirtNumber,
                             String primaryPosition, List<String> eligiblePositions,
                             String positionStatus) {
    public static PlayerResponse from(Player player, String clubName) {
        return new PlayerResponse(player.getId(), player.getName(), player.getClubId(),
                clubName, player.getPosition().name(), player.getGoals(), player.getAssists(),
                null, null, null, null, null, null, null, List.of(), "NOT_APPLICABLE");
    }
}
