package com.premierhub.web.dto;

public record PlayerMatchResponse(MatchResponse match, int clubId, MatchPlayerStatResponse stats,
                                  String evidenceStatus) {
}
