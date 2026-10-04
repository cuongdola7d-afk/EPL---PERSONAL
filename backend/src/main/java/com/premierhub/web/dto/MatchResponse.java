package com.premierhub.web.dto;

import com.premierhub.model.Match;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;

public record MatchResponse(int id, int homeClubId, String homeClub,
                            int awayClubId, String awayClub, int matchweek,
                            LocalDate date, String status, Integer homeGoals,
                            Integer awayGoals, boolean hasManualStats,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Instant kickoffUtc) {
    public MatchResponse(int id, int homeClubId, String homeClub,
                         int awayClubId, String awayClub, int matchweek,
                         LocalDate date, String status, Integer homeGoals,
                         Integer awayGoals, boolean hasManualStats) {
        this(id, homeClubId, homeClub, awayClubId, awayClub, matchweek,
                date, status, homeGoals, awayGoals, hasManualStats, null);
    }

    public MatchResponse(int id, int homeClubId, String homeClub,
                         int awayClubId, String awayClub, int matchweek,
                         LocalDate date, String status, Integer homeGoals,
                         Integer awayGoals) {
        this(id, homeClubId, homeClub, awayClubId, awayClub, matchweek,
                date, status, homeGoals, awayGoals, false);
    }

    public static MatchResponse from(Match match, String homeClub, String awayClub) {
        return new MatchResponse(match.getId(), match.getHomeClubId(), homeClub,
                match.getAwayClubId(), awayClub, match.getMatchweek(),
                match.getMatchDate(), match.getStatus().name(),
                match.getHomeGoals(), match.getAwayGoals());
    }
}
