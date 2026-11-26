package com.premierhub.web.dto;

import java.util.List;

public record ClubOverviewResponse(
        int clubId,
        int season,
        String formation,
        List<FormationCount> formationCounts,
        int lineupMatches,
        List<XiPlayer> xi,
        List<LeaderEntry> topScorers,
        List<LeaderEntry> topAssists,
        List<RatingLeaderEntry> topRatings
) {
    public record FormationCount(String formation, int matches) {
    }

    public record XiPlayer(
            int playerId,
            String name,
            String shortName,
            String position,
            int rowIndex,
            int slotIndex,
            double x,
            double y,
            int startsAtPosition
    ) {
    }

    public record LeaderEntry(
            int rank,
            int playerId,
            String name,
            int appearances,
            int value
    ) {
    }

    public record RatingLeaderEntry(
            int rank,
            int playerId,
            String name,
            int appearances,
            int ratedAppearances,
            double value
    ) {
    }
}
