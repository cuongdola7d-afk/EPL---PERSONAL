package com.premierhub.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.premierhub.web.dto.InferredMatchStatsResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import com.premierhub.web.dto.MatchResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FixtureEvidenceService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public FixtureEvidenceService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void save(JsonNode evidence) {
        int fixtureId = integer(evidence, "fixtureId", "ID fixture");
        require(jdbc.queryForObject("SELECT COUNT(*) FROM fixtures WHERE id=? AND league_id=39 "
                + "AND season_year=2024 AND gameweek=1", Integer.class, fixtureId) == 1,
                "Chỉ được nhập bằng chứng cho fixture Gameweek 1 mùa 2024/25 đã lưu.");
        require("API_FOOTBALL".equals(evidence.path("source").asText()),
                "Nguồn bằng chứng không hợp lệ.");
        String json = evidence.toString();
        Timestamp now = Timestamp.from(Instant.now());
        if (jdbc.update("UPDATE fixture_score_evidence SET payload_json=?, captured_at=? WHERE fixture_id=?",
                json, now, fixtureId) == 0) {
            jdbc.update("INSERT INTO fixture_score_evidence (fixture_id, payload_json, captured_at) "
                    + "VALUES (?, ?, ?)", fixtureId, json, now);
        }
    }

    public Review review(int season, MatchResponse match, List<MatchPlayerStatResponse> players) {
        if (season != 2024 || match.matchweek() != 1) {
            return new Review("NOT_APPLICABLE", null, Map.of());
        }
        List<String> rows = jdbc.query("SELECT payload_json FROM fixture_score_evidence WHERE fixture_id=?",
                (rs, row) -> rs.getString(1), match.id());
        if (rows.isEmpty()) return new Review("MISSING", "Chưa lưu bằng chứng sự kiện và đội hình.", Map.of());
        try {
            return validate(mapper.readTree(rows.getFirst()), match, players);
        } catch (JacksonException exception) {
            return new Review("INVALID", "JSON bằng chứng không hợp lệ.", Map.of());
        } catch (IllegalArgumentException exception) {
            return new Review("INVALID", exception.getMessage(), Map.of());
        }
    }

    Review validate(JsonNode root, MatchResponse match, List<MatchPlayerStatResponse> players) {
        require(root.path("fixtureId").asInt(-1) == match.id() &&
                "API_FOOTBALL".equals(root.path("source").asText()), "Sai fixture hoặc nguồn bằng chứng.");
        require("FINISHED".equals(match.status()) && match.homeGoals() != null &&
                match.awayGoals() != null, "Fixture chưa có kết quả hoàn chỉnh để kiểm chứng.");
        require(players.size() == 40, "Bằng chứng cần đúng 40 dòng cầu thủ–trận trong H2.");

        Map<Integer, MatchPlayerStatResponse> byId = new HashMap<>();
        Map<Integer, Set<Integer>> databaseTeams = new HashMap<>();
        databaseTeams.put(match.homeClubId(), new HashSet<>());
        databaseTeams.put(match.awayClubId(), new HashSet<>());
        for (MatchPlayerStatResponse player : players) {
            require(byId.putIfAbsent(player.playerId(), player) == null,
                    "H2 có ID cầu thủ trùng trong fixture.");
            Set<Integer> team = databaseTeams.get(player.clubId());
            require(team != null, "H2 có cầu thủ thuộc đội khác fixture.");
            team.add(player.playerId());
        }
        require(databaseTeams.values().stream().allMatch(team -> team.size() == 20),
                "H2 không có đúng 20 cầu thủ mỗi đội.");

        JsonNode lineupRows = root.path("lineups");
        require(lineupRows.isArray() && lineupRows.size() == 2, "Cần đội hình của đúng hai đội.");
        Map<Integer, Set<Integer>> starters = new HashMap<>();
        Map<Integer, Set<Integer>> benches = new HashMap<>();
        for (JsonNode lineup : lineupRows) {
            int teamId = integer(lineup, "teamId", "ID đội trong đội hình");
            require(databaseTeams.containsKey(teamId) && !starters.containsKey(teamId),
                    "Đội hình trùng hoặc sai đội.");
            Set<Integer> start = ids(lineup.path("starters"), 11, "đá chính");
            Set<Integer> bench = ids(lineup.path("bench"), 9, "dự bị");
            require(start.stream().noneMatch(bench::contains), "Cầu thủ nằm ở cả đá chính và dự bị.");
            Set<Integer> combined = new HashSet<>(start);
            combined.addAll(bench);
            require(combined.equals(databaseTeams.get(teamId)),
                    "40 ID H2 không khớp đội hình của đội " + teamId + ".");
            starters.put(teamId, start);
            benches.put(teamId, bench);
        }
        require(starters.size() == 2, "Thiếu đội hình một đội.");

        JsonNode eventRows = root.path("events");
        require(eventRows.isArray(), "Thiếu danh sách sự kiện trận.");
        Map<Integer, Integer> eventGoals = new HashMap<>();
        Map<Integer, Integer> eventAssists = new HashMap<>();
        Map<Integer, Set<Integer>> entered = Map.of(match.homeClubId(), new HashSet<>(),
                match.awayClubId(), new HashSet<>());
        Map<Integer, Set<Integer>> onField = Map.of(match.homeClubId(), new HashSet<>(starters.get(match.homeClubId())),
                match.awayClubId(), new HashSet<>(starters.get(match.awayClubId())));
        int homeGoals = 0;
        int awayGoals = 0;
        int substitutions = 0;
        for (JsonNode event : eventRows) {
            int teamId = integer(event, "teamId", "ID đội trong sự kiện");
            require(databaseTeams.containsKey(teamId), "Sự kiện thuộc đội khác fixture.");
            int playerId = integer(event, "playerId", "ID cầu thủ trong sự kiện");
            if ("Goal".equals(event.path("type").asText())) {
                require("Normal Goal".equals(event.path("detail").asText())
                                || "Penalty".equals(event.path("detail").asText()),
                        "Có bàn thắng chưa được hỗ trợ đối chiếu, gồm cả phản lưới.");
                require(databaseTeams.get(teamId).contains(playerId) && played(byId.get(playerId)),
                        "Người ghi bàn không khớp cầu thủ đã thi đấu trong H2.");
                eventGoals.merge(playerId, 1, Integer::sum);
                if (teamId == match.homeClubId()) homeGoals++; else awayGoals++;
                if (!event.path("assistId").isNull()) {
                    int assistId = integer(event, "assistId", "ID kiến tạo");
                    require(databaseTeams.get(teamId).contains(assistId) && played(byId.get(assistId)),
                            "Người kiến tạo không khớp cầu thủ đã thi đấu trong H2.");
                    eventAssists.merge(assistId, 1, Integer::sum);
                }
            } else if ("subst".equals(event.path("type").asText())) {
                int inId = integer(event, "assistId", "ID cầu thủ vào sân");
                require(onField.get(teamId).remove(playerId),
                        "Người ra sân không thuộc đội hình đang thi đấu.");
                require(benches.get(teamId).contains(inId) && entered.get(teamId).add(inId),
                        "Người vào sân không thuộc dự bị hoặc đã vào trước đó.");
                require(onField.get(teamId).add(inId), "Người vào sân đã có mặt trên sân.");
                require(played(byId.get(playerId)) && played(byId.get(inId)),
                        "Người vào/ra sân không khớp thống kê phút trong H2.");
                substitutions++;
            } else {
                throw new IllegalArgumentException("Loại sự kiện bằng chứng không hợp lệ.");
            }
        }
        require(homeGoals == match.homeGoals() && awayGoals == match.awayGoals(),
                "Sự kiện bàn thắng không khớp tỉ số đã lưu trong H2.");

        Map<Integer, InferredMatchStatsResponse> inferred = new HashMap<>();
        int unused = 0;
        for (MatchPlayerStatResponse player : players) {
            int id = player.playerId();
            int teamId = player.clubId();
            boolean appeared = starters.get(teamId).contains(id) || entered.get(teamId).contains(id);
            if (appeared) {
                require(played(player), "Cầu thủ đá chính/vào sân thiếu phút thi đấu: " + id);
            } else {
                require(benches.get(teamId).contains(id) && player.minutes() == null,
                        "Dự bị không vào sân lại có phút thi đấu: " + id);
                unused++;
            }
            int goals = eventGoals.getOrDefault(id, 0);
            int assists = eventAssists.getOrDefault(id, 0);
            require(player.goals() == null || player.goals() == goals,
                    "Bàn thắng dương/0 đã lưu không khớp sự kiện: " + id);
            require(player.assists() == null || player.assists() == assists,
                    "Kiến tạo dương/0 đã lưu không khớp sự kiện: " + id);
            inferred.put(id, new InferredMatchStatsResponse(
                    player.minutes() == null ? 0 : null,
                    player.goals() == null ? goals : null,
                    player.assists() == null ? assists : null));
        }
        require(unused + substitutions == 18,
                "Số dự bị không vào sân không khớp sự kiện thay người.");
        return new Review("VERIFIED", null, Map.copyOf(inferred));
    }

    private static boolean played(MatchPlayerStatResponse player) {
        return player != null && player.minutes() != null && player.minutes() > 0;
    }

    private static int integer(JsonNode node, String field, String label) {
        JsonNode value = node.path(field);
        require(value.isIntegralNumber() && value.canConvertToInt(), label + " không hợp lệ.");
        return value.asInt();
    }

    private static Set<Integer> ids(JsonNode values, int expected, String label) {
        require(values.isArray() && values.size() == expected,
                "Danh sách " + label + " phải có " + expected + " ID.");
        Set<Integer> ids = new HashSet<>();
        for (JsonNode value : values) {
            require(value.isIntegralNumber() && value.canConvertToInt() && ids.add(value.asInt()),
                    "ID " + label + " trùng hoặc không hợp lệ.");
        }
        return ids;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public record Review(String status, String error,
                         Map<Integer, InferredMatchStatsResponse> inferred) {
    }
}
