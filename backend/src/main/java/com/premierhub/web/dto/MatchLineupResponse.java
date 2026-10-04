package com.premierhub.web.dto;

import java.time.LocalDate;
import java.util.List;

public record MatchLineupResponse(String formation, String formationSource, LocalDate formationVerifiedOn,
                                 String formationSourceNote, Integer scopeFromGw, Integer scopeToGw,
                                 Integer verifiedMatches, String formationCounts, String fixtureIds,
                                 String startersStatus, LocalDate rolesVerifiedOn, String rolesSourceNote,
                                 List<Player> players) {
    public record Player(int playerId, String role, String matchPosition, Integer rowIndex, Integer slotIndex,
                         Integer substitutionInMinute, Integer substitutionOutMinute, String seasonPosition) { }
}
