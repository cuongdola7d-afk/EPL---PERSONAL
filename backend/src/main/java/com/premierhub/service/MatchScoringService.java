package com.premierhub.service;

import com.premierhub.web.dto.MatchScoreResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchScoringService {
    public MatchScoreResponse score(String position, Integer minutes, Integer goals,
                                    Integer assists, Integer yellowCards, Integer redCards) {
        List<MatchScoreResponse.Part> parts = List.of(
                appearance(minutes), goals(position, goals),
                counted("assists", "Kiến tạo", assists, 3, "kiến tạo"),
                counted("yellowCards", "Thẻ vàng", yellowCards, -1, "thẻ vàng"),
                counted("redCards", "Thẻ đỏ", redCards, -3, "thẻ đỏ"));
        boolean complete = parts.stream().allMatch(part -> part.points() != null);
        int confirmedPoints = parts.stream().filter(part -> part.points() != null)
                .mapToInt(MatchScoreResponse.Part::points).sum();
        return new MatchScoreResponse(complete ? "COMPLETE" : "PROVISIONAL", confirmedPoints, parts);
    }

    private MatchScoreResponse.Part appearance(Integer minutes) {
        if (minutes == null || minutes < 0) {
            return new MatchScoreResponse.Part("appearance", "Ra sân", null, "Thiếu phút thi đấu");
        }
        int points = minutes == 0 ? 0 : minutes < 60 ? 1 : 2;
        return new MatchScoreResponse.Part("appearance", "Ra sân", points,
                minutes + " phút: " + signed(points));
    }

    private MatchScoreResponse.Part goals(String position, Integer goals) {
        if (goals == null || goals < 0) {
            return new MatchScoreResponse.Part("goals", "Bàn thắng", null, "Thiếu số bàn thắng");
        }
        if (goals == 0) {
            return new MatchScoreResponse.Part("goals", "Bàn thắng", 0, "0 bàn thắng: +0");
        }
        int rate = switch (position == null ? "" : position.strip().toUpperCase()) {
            case "G" -> 10;
            case "D" -> 6;
            case "M" -> 5;
            case "F" -> 4;
            default -> 0;
        };
        if (rate == 0) {
            return new MatchScoreResponse.Part("goals", "Bàn thắng", null,
                    "Thiếu vị trí để tính " + goals + " bàn thắng");
        }
        return new MatchScoreResponse.Part("goals", "Bàn thắng", goals * rate,
                goals + " bàn × " + rate + " điểm: " + signed(goals * rate));
    }

    private MatchScoreResponse.Part counted(String code, String label, Integer count,
                                             int rate, String unit) {
        if (count == null || count < 0) {
            return new MatchScoreResponse.Part(code, label, null, "Thiếu số " + unit);
        }
        return new MatchScoreResponse.Part(code, label, count * rate,
                count + " " + unit + " × " + signed(rate) + ": " + signed(count * rate));
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }
}
