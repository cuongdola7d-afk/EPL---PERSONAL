package com.premierhub.web.dto;

public record MatchGoalEventResponse(
        Integer playerId,
        String playerName,
        int clubId,
        Integer minute,
        String goalType
) {
}
