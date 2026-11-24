package com.premierhub.web.dto;

public record PlayerMatchResponse(MatchResponse match, int clubId, MatchPlayerStatResponse stats,
                                  String evidenceStatus, String dataStatus) {
    public PlayerMatchResponse(MatchResponse match, int clubId, MatchPlayerStatResponse stats, String evidenceStatus) {
        this(match, clubId, stats, evidenceStatus, stats != null ? "CONFIRMED" : "NONE");
    }
}
