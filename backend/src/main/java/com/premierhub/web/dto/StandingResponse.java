package com.premierhub.web.dto;

import com.premierhub.model.Standing;
import com.premierhub.service.StandingService.RankedStanding;

public record StandingResponse(int position, int clubId, String clubName,
                               int played, int won, int drawn, int lost,
                               int goalsFor, int goalsAgainst,
                               int goalDifference, int points) {
    public static StandingResponse from(RankedStanding entry) {
        Standing standing = entry.standing();
        return new StandingResponse(entry.position(), standing.getClub().getId(),
                standing.getClub().getName(), standing.getPlayed(),
                standing.getWins(), standing.getDraws(), standing.getLosses(),
                standing.getGoalsFor(), standing.getGoalsAgainst(),
                standing.getGoalDifference(), standing.getPoints());
    }
}
