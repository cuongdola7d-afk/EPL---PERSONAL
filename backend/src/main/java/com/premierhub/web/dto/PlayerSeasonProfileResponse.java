package com.premierhub.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlayerSeasonProfileResponse(
        int playerId,
        int season,
        String roleGroup,
        String comparisonGroup,
        String comparisonGroupLabel,
        Totals totals,
        TeamContribution teamContribution,
        CleanSheets cleanSheets,
        List<PercentileMetric> percentiles
) {
    public record Totals(
            int appearances,
            int clubFinishedMatches,
            Integer minutes,
            Integer goals,
            Integer assists,
            Integer yellowCards,
            Integer redCards,
            Double averageRating,
            int ratedMatches,
            BigDecimal expectedGoals,
            BigDecimal expectedAssists,
            Integer tackles,
            Integer clearancesBlocksInterceptions,
            Integer recoveries,
            Integer saves,
            Integer goalsConceded,
            Integer cleanSheets,
            Integer bps,
            Integer bonus
    ) {
    }

    public record TeamContribution(
            int teamGoals,
            Integer goals,
            Integer assists
    ) {
    }

    public record CleanSheets(
            Integer cleanSheets,
            int appearances
    ) {
    }

    public record PercentileMetric(
            String key,
            Double value,
            Integer percentile,
            int poolSize
    ) {
    }
}
