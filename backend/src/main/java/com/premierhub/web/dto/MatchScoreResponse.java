package com.premierhub.web.dto;

import java.util.List;

public record MatchScoreResponse(String status, int confirmedPoints, List<Part> parts) {
    public record Part(String code, String label, Integer points, String detail) {
    }
}
