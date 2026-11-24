package com.premierhub.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record MatchDetailResponse(MatchResponse match,
                                  List<MatchPlayerStatResponse> homePlayers,
                                  List<MatchPlayerStatResponse> awayPlayers,
                                  String evidenceStatus, String evidenceError,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) MatchLineupResponse homeLineup,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) MatchLineupResponse awayLineup,
                                  String dataStatus,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Instant provisionalFetchedAt) {
    public MatchDetailResponse(MatchResponse match, List<MatchPlayerStatResponse> homePlayers,
                               List<MatchPlayerStatResponse> awayPlayers, String evidenceStatus, String evidenceError) {
        this(match, homePlayers, awayPlayers, evidenceStatus, evidenceError, null, null,
                homePlayers.isEmpty() && awayPlayers.isEmpty() ? "NONE" : "CONFIRMED", null);
    }

    public MatchDetailResponse(MatchResponse match, List<MatchPlayerStatResponse> homePlayers,
                               List<MatchPlayerStatResponse> awayPlayers, String evidenceStatus, String evidenceError,
                               MatchLineupResponse homeLineup, MatchLineupResponse awayLineup) {
        this(match, homePlayers, awayPlayers, evidenceStatus, evidenceError, homeLineup, awayLineup,
                homePlayers.isEmpty() && awayPlayers.isEmpty() ? "NONE" : "CONFIRMED", null);
    }
}
