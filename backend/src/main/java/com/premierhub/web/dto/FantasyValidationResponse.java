package com.premierhub.web.dto;

import java.util.List;

public record FantasyValidationResponse(boolean valid, int totalOvr, List<Issue> issues) {
    public record Issue(String code, String slotKey, Integer playerId, String message) {
    }
}
