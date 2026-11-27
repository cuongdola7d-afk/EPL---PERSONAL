package com.premierhub.web.dto;

import java.time.Instant;
import java.util.List;

public record ClubSeasonTrendsResponse(
        int clubId,
        int season,
        List<MatchTrend> matches,
        List<RankPoint> rankHistory,
        List<ScorerEntry> scorers,
        List<AssisterEntry> assisters,
        int ownGoalsFor
) {
    public record MatchTrend(
            int fixtureId,
            int matchweek,
            Instant kickoffUtc,
            boolean home,
            int opponentClubId,
            String opponentName,
            int goalsFor,
            int goalsAgainst,
            Double teamRating,
            Double xgFor,
            Double xgAgainst
    ) { }

    public record RankPoint(int matchweek, int position) { }

    public record ScorerEntry(int playerId, String name, int goals) { }

    public record AssisterEntry(int playerId, String name, int assists) { }
}
