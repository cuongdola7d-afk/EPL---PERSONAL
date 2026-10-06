package com.premierhub.service;

import com.premierhub.web.dto.FantasyLineupRequest;
import com.premierhub.web.dto.FantasyValidationResponse;
import com.premierhub.web.dto.FantasyValidationResponse.Issue;
import com.premierhub.web.dto.PlayerResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FantasyLineupService {
    public static final int MAX_OVR = 910;
    public static final LocalDate AS_OF = LocalDate.of(2026, 10, 2);
    // Rows and keys mirror frontend/src/fantasy/lineup.js. Side-specific CB/CM
    // labels are normalized here; these are permissions, not broad groups.
    private static final Map<String, List<List<String>>> FORMATIONS = Map.of(
            "4-2-1-3", List.of(List.of("LW", "ST", "RW"), List.of("CAM"),
                    List.of("CM", "CM"), List.of("LB", "CB", "CB", "RB"), List.of("GK")),
            "4-3-3", List.of(List.of("LW", "ST", "RW"), List.of("CM", "CM", "CM"),
                    List.of("LB", "CB", "CB", "RB"), List.of("GK")),
            "4-4-2", List.of(List.of("ST", "ST"), List.of("LM", "CM", "CM", "RM"),
                    List.of("LB", "CB", "CB", "RB"), List.of("GK")),
            "3-5-2", List.of(List.of("ST", "ST"), List.of("LM", "CM", "CM", "CM", "RM"),
                    List.of("CB", "CB", "CB"), List.of("GK")));

    private final FootballQueries queries;

    public FantasyLineupService(FootballQueries queries) {
        this.queries = queries;
    }

    public FantasyValidationResponse validate(FantasyLineupRequest request) {
        return inspect(request, true).validation();
    }

    public FantasyValidationResponse validate(FantasyLineupRequest request, LocalDate asOf) {
        return inspect(request, true, asOf).validation();
    }

    public record Inspection(FantasyValidationResponse validation, Map<String, String> slots,
                             Map<Integer, PlayerResponse> players) { }

    public static Map<String,String> normalizedSlots(String formation) {
        var rows=FORMATIONS.get(formation);
        if(rows==null) return Map.of();
        var slots=new LinkedHashMap<String,String>();
        for(int r=0;r<rows.size();r++) for(int c=0;c<rows.get(r).size();c++) slots.put(r+"-"+c,rows.get(r).get(c));
        return Map.copyOf(slots);
    }

    // The same roster read supplies validation and the submission snapshot.
    public Inspection inspect(FantasyLineupRequest request, boolean complete) {
        // Legacy guest builder only. Multiplayer always passes the stored GW date.
        return inspect(request, complete, AS_OF);
    }

    public Inspection inspect(FantasyLineupRequest request, boolean complete, LocalDate asOf) {
        java.util.Objects.requireNonNull(asOf, "Gameweek roster reference date is required");
        List<Issue> issues = new ArrayList<>();
        List<List<String>> rows = FORMATIONS.get(request.formation());
        if (rows == null) {
            return new Inspection(new FantasyValidationResponse(false, 0, List.of(new Issue(
                    "FORMATION", null, null, "Sơ đồ không được hỗ trợ cho Fantasy 2026/27."))), Map.of(), Map.of());
        }
        Map<String, String> slots = new LinkedHashMap<>();
        for (int row = 0; row < rows.size(); row++) {
            for (int column = 0; column < rows.get(row).size(); column++) {
                slots.put(row + "-" + column, rows.get(row).get(column));
            }
        }
        Map<Integer, PlayerResponse> players = queries.players(2026, null, null, asOf).stream()
                .collect(Collectors.toMap(PlayerResponse::id, Function.identity()));
        Map<Integer, Integer> clubCounts = new HashMap<>();
        var seen = new HashSet<Integer>();
        int total = 0;
        Map<String, Integer> picks = request.picks();
        if (picks.size() > 11 || complete && picks.size() != 11) {
            issues.add(new Issue("COUNT", null, null, "Đội hình cần đúng 11 cầu thủ khác nhau."));
        }
        for (var entry : picks.entrySet()) {
            String key = entry.getKey();
            Integer id = entry.getValue();
            if (!slots.containsKey(key)) {
                issues.add(new Issue("SLOT", key, id, "Ô " + key + " không có trong sơ đồ."));
            }
            if (!seen.add(id)) {
                issues.add(new Issue("DUPLICATE", key, id, "Cầu thủ #" + id + " bị chọn trùng ở ô " + key + "."));
            }
            PlayerResponse player = players.get(id);
            if (player == null) {
                issues.add(new Issue("ROSTER", key, id, "Ô " + key + ": cầu thủ #" + id + " không thuộc roster 2026/27 hiện hành."));
                continue;
            }
            String label = "Ô " + key + " (" + slots.getOrDefault(key, "?") + "), " + player.name() + ": ";
            if (player.fc27Overall() == null || player.fc27Overall() < 1 || player.fc27Overall() > 99) {
                issues.add(new Issue("OVR_MISSING", key, id, label + "chưa có OVR hợp lệ."));
            } else {
                total += player.fc27Overall();
            }
            if (player.primaryPosition() == null || player.eligiblePositions() == null
                    || !player.eligiblePositions().contains(player.primaryPosition())) {
                issues.add(new Issue("POSITION_MISSING", key, id, label + "chưa có vị trí hợp lệ."));
            } else if (slots.containsKey(key) && !player.eligiblePositions().contains(slots.get(key))) {
                issues.add(new Issue("POSITION", key, id, label + "không được chơi vị trí " + slots.get(key) + "."));
            }
            if (clubCounts.merge(player.clubId(), 1, Integer::sum) > 3) {
                issues.add(new Issue("CLUB_LIMIT", key, id, label + "vượt tối đa 3 cầu thủ của " + player.club() + "."));
            }
        }
        for (String key : slots.keySet()) {
            if (complete && picks.get(key) == null) {
                issues.add(new Issue("EMPTY_SLOT", key, null, "Ô " + key + " (" + slots.get(key) + ") chưa có cầu thủ."));
            }
        }
        if (total > MAX_OVR) {
            issues.add(new Issue("OVR_LIMIT", null, null, "Tổng OVR " + total + " vượt giới hạn " + MAX_OVR + "."));
        }
        return new Inspection(new FantasyValidationResponse(issues.isEmpty(), total, List.copyOf(issues)),
                Map.copyOf(slots), Map.copyOf(players));
    }
}
