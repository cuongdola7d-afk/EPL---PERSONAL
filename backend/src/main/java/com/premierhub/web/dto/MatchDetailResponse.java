package com.premierhub.web.dto;

import java.util.List;

public record MatchDetailResponse(MatchResponse match,
                                  List<MatchPlayerStatResponse> homePlayers,
                                  List<MatchPlayerStatResponse> awayPlayers) {
}
