package com.premierhub.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

public record MatchPlayerStatResponse(int playerId, String playerName, int clubId,
                                      String position, Integer minutes, Integer goals,
                                      Integer assists, Integer yellowCards, Integer redCards,
                                      String rating, Integer shotsOn, Integer passesKey,
                                      Integer tackles, Integer saves, MatchScoreResponse score,
                                      InferredMatchStatsResponse inferred,
                                      String participationStatus, BigDecimal fantasyPoints,
                                      @JsonInclude(JsonInclude.Include.NON_NULL) MatchPlayerExtraStatsResponse extra) {
    public MatchPlayerStatResponse(int playerId, String playerName, int clubId,
                                   String position, Integer minutes, Integer goals,
                                   Integer assists, Integer yellowCards, Integer redCards,
                                   String rating, Integer shotsOn, Integer passesKey,
                                   Integer tackles, Integer saves, MatchScoreResponse score,
                                   InferredMatchStatsResponse inferred,
                                   String participationStatus, BigDecimal fantasyPoints) {
        this(playerId, playerName, clubId, position, minutes, goals, assists,
                yellowCards, redCards, rating, shotsOn, passesKey, tackles, saves,
                score, inferred, participationStatus, fantasyPoints, null);
    }

    public MatchPlayerStatResponse(int playerId, String playerName, int clubId,
                                   String position, Integer minutes, Integer goals,
                                   Integer assists, Integer yellowCards, Integer redCards,
                                   String rating, Integer shotsOn, Integer passesKey,
                                   Integer tackles, Integer saves, MatchScoreResponse score,
                                   InferredMatchStatsResponse inferred) {
        this(playerId, playerName, clubId, position, minutes, goals, assists,
                yellowCards, redCards, rating, shotsOn, passesKey, tackles, saves,
                score, inferred, null, null, null);
    }
}
