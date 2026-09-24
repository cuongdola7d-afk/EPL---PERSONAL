package com.premierhub.web.dto;

public record MatchPlayerStatResponse(int playerId, String playerName, int clubId,
                                      String position, Integer minutes, Integer goals,
                                      Integer assists, Integer yellowCards, Integer redCards,
                                      String rating, Integer shotsOn, Integer passesKey,
                                      Integer tackles, Integer saves) {
}
