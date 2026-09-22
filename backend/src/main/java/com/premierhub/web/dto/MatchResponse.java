package com.premierhub.web.dto;

import com.premierhub.model.Match;
import java.time.LocalDate;

public record MatchResponse(int id, int homeClubId, String homeClub,
                            int awayClubId, String awayClub, int matchweek,
                            LocalDate date, String status, Integer homeGoals,
                            Integer awayGoals) {
    public static MatchResponse from(Match match, String homeClub, String awayClub) {
        return new MatchResponse(match.getId(), match.getHomeClubId(), homeClub,
                match.getAwayClubId(), awayClub, match.getMatchweek(),
                match.getMatchDate(), match.getStatus().name(),
                match.getHomeGoals(), match.getAwayGoals());
    }
}
