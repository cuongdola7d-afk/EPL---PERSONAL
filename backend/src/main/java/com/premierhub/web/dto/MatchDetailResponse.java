package com.premierhub.web.dto;

import java.util.List;

public record MatchDetailResponse(MatchResponse match,
                                  List<MatchPlayerStatResponse> homePlayers,
                                  List<MatchPlayerStatResponse> awayPlayers,
                                  String evidenceStatus, String evidenceError,
                                  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) MatchLineupResponse homeLineup,
                                  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) MatchLineupResponse awayLineup) {
    public MatchDetailResponse(MatchResponse match, List<MatchPlayerStatResponse> homePlayers,
                               List<MatchPlayerStatResponse> awayPlayers, String evidenceStatus, String evidenceError) {
        this(match, homePlayers, awayPlayers, evidenceStatus, evidenceError, null, null);
    }
}
