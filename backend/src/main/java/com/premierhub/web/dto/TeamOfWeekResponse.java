package com.premierhub.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeamOfWeekResponse(int season, int gameweek, String formation, String status,
                                 BigDecimal totalRating, int playedCount, int ratedCount,
                                 int candidateCount, int excludedMissingPositions,
                                 int excludedNullRatings, int completedFixtures, int recordedFixtures,
                                 List<Pick> picks, List<String> missingSlots, List<Conflict> conflicts) {
    public record Player(int playerId, String name, int clubId, String club, int fixtureId,
                         BigDecimal rating, List<String> eligiblePositions) { }
    public record Pick(String slot, Player player) { }
    public record Conflict(int playerId, String name, List<Integer> fixtureIds) { }
}
