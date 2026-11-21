package com.premierhub.web.dto;

import java.math.BigDecimal;

public record MatchPlayerExtraStatsResponse(
        Integer saves,
        Integer goalsConceded,
        Integer bps,
        Integer bonus,
        Integer tackles,
        Integer clearancesBlocksInterceptions,
        Integer recoveries,
        Integer defensiveContribution,
        BigDecimal expectedGoals,
        BigDecimal expectedAssists
) {
}
