package com.premierhub.web.dto;

import java.util.List;

public record ClubStatisticsResponse(int clubId, int season, Double averageRating,
                                     int ratedAppearances, int recordedMatches,
                                     List<PlayerStatistics> players) {
    public record PlayerStatistics(int playerId, int appearances, Integer goals,
                                   Integer assists, Double averageRating, int ratedAppearances) { }
}
