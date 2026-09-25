package com.premierhub.seed;

import com.premierhub.service.FootballQueries;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class Gw1SnapshotService {
    private static final int LEAGUE = 39;
    private static final int SEASON = 2024;
    private static final int GAMEWEEK = 1;
    private static final String FIXTURES = "SELECT id FROM fixtures WHERE league_id=39 AND season_year=2024 AND gameweek=1";
    private static final String RESOURCE = "data/gw1-2024-snapshot.json";
    private static final Set<String> METADATA_COLUMNS = Set.of(
            "synced_at", "captured_at", "payload_hash");

    // Foreign key order matters: parent rows are inserted before their children.
    private static final List<TableSpec> TABLES = List.of(
            table("seasons", "league_id,season_year", "league_id,season_year",
                    "league_id,season_year", "", "", "league_id=39 AND season_year=2024", 1),
            table("clubs", "id,name,city", "id", "id", "", "",
                    "id IN (SELECT club_id FROM season_clubs WHERE league_id=39 AND season_year=2024)", 20),
            table("season_clubs", "league_id,season_year,club_id", "league_id,season_year,club_id",
                    "league_id,season_year,club_id", "", "",
                    "league_id=39 AND season_year=2024", 20),
            table("players", "id,name", "id", "id", "", "",
                    "id IN (SELECT player_id FROM fixture_player_stats WHERE fixture_id IN (" + FIXTURES + ")) "
                            + "OR id IN (SELECT player_id FROM player_season_stats "
                            + "WHERE league_id=39 AND season_year=2024)", 432),
            table("player_season_stats", "league_id,season_year,player_id,club_id,position,"
                            + "appearances,minutes,goals,assists", "league_id,season_year,player_id,club_id",
                    "league_id,season_year,player_id,club_id,appearances,minutes,goals,assists", "", "",
                    "league_id=39 AND season_year=2024", 63),
            table("fixtures", "id,league_id,season_year,home_club_id,away_club_id,gameweek,"
                            + "match_date,status,provider_status,home_goals,away_goals,payload_hash,synced_at",
                    "id", "id,league_id,season_year,home_club_id,away_club_id,gameweek,home_goals,away_goals",
                    "match_date", "synced_at", "league_id=39 AND season_year=2024 AND gameweek=1", 10),
            table("standings", "league_id,season_year,club_id,position,played,won,drawn,lost,"
                            + "goals_for,goals_against,goal_difference,points",
                    "league_id,season_year,club_id", "league_id,season_year,club_id,position,played,won,"
                            + "drawn,lost,goals_for,goals_against,goal_difference,points", "", "",
                    "league_id=39 AND season_year=2024", 20),
            table("fixture_player_stats", "fixture_id,player_id,club_id,position,minutes,goals,"
                            + "assists,yellow_cards,red_cards,rating,shots_on,passes_key,tackles,saves,"
                            + "raw_statistics,synced_at", "fixture_id,player_id",
                    "fixture_id,player_id,club_id,minutes,goals,assists,yellow_cards,red_cards,"
                            + "shots_on,passes_key,tackles,saves", "", "synced_at",
                    "fixture_id IN (" + FIXTURES + ")", 400),
            table("fixture_score_evidence", "fixture_id,payload_json,captured_at", "fixture_id",
                    "fixture_id", "", "captured_at", "fixture_id IN (" + FIXTURES + ")", 10)
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final FootballQueries queries;
    private final String datasourceUrl;

    public Gw1SnapshotService(JdbcTemplate jdbc, ObjectMapper mapper, FootballQueries queries,
                              @Value("${spring.datasource.url}") String datasourceUrl) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.queries = queries;
        this.datasourceUrl = datasourceUrl;
    }

    public void exportLocal(Path output) throws IOException {
        require(datasourceUrl.startsWith("jdbc:h2:"), "Chỉ xuất snapshot từ H2 local.");
        ObjectNode root = mapper.createObjectNode();
        root.put("formatVersion", 1);
        root.put("leagueId", LEAGUE);
        root.put("season", SEASON);
        root.put("gameweek", GAMEWEEK);
        ObjectNode tables = root.putObject("tables");
        for (TableSpec spec : TABLES) {
            ArrayNode rows = tables.putArray(spec.name());
            String sql = "SELECT " + String.join(",", spec.columns()) + " FROM " + spec.name()
                    + " WHERE " + spec.scope() + " ORDER BY " + String.join(",", spec.keys());
            jdbc.query(sql, rs -> {
                ObjectNode row = rows.addObject();
                for (String column : spec.columns()) {
                    String value = rs.getString(column);
                    if (value == null) row.putNull(column); else row.put(column, value);
                }
            });
        }
        validateSnapshot(root);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.writeString(output, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportSummary importBundled() throws IOException {
        JsonNode root;
        try (var input = new ClassPathResource(RESOURCE).getInputStream()) {
            root = mapper.readTree(input);
        }
        validateSnapshot(root);
        Map<String, Integer> inserted = new LinkedHashMap<>();
        for (TableSpec spec : TABLES) {
            int count = 0;
            for (JsonNode row : root.path("tables").path(spec.name())) {
                if (insertIfAbsent(spec, row)) count++;
            }
            inserted.put(spec.name(), count);
        }
        verifyImportedData();
        return new ImportSummary(Map.copyOf(inserted), 10);
    }

    private boolean insertIfAbsent(TableSpec spec, JsonNode row) {
        String where = spec.keys().stream().map(key -> key + "=?")
                .collect(Collectors.joining(" AND "));
        Object[] keys = spec.keys().stream().map(key -> typedValue(spec, key, row)).toArray();
        String select = "SELECT " + String.join(",", spec.columns()) + " FROM " + spec.name()
                + " WHERE " + where;
        List<Map<String, String>> existing = jdbc.query(select, ps -> {
            for (int index = 0; index < keys.length; index++) ps.setObject(index + 1, keys[index]);
        }, (rs, number) -> {
            Map<String, String> values = new HashMap<>();
            for (String column : spec.columns()) values.put(column, rs.getString(column));
            return values;
        });
        require(existing.size() <= 1, "Khóa trùng trong bảng " + spec.name());
        if (!existing.isEmpty()) {
            for (String column : spec.columns()) {
                if (METADATA_COLUMNS.contains(column)) continue;
                String wanted = row.path(column).isNull() ? null : row.path(column).asText();
                require(Objects.equals(wanted, existing.getFirst().get(column)),
                        "Dữ liệu production khác snapshot tại " + spec.name() + "." + column
                                + " (khóa " + keyLabel(spec, row) + "). Không ghi đè.");
            }
            return false;
        }
        String columns = String.join(",", spec.columns());
        String placeholders = String.join(",", spec.columns().stream().map(column -> "?").toList());
        Object[] values = spec.columns().stream().map(column -> typedValue(spec, column, row)).toArray();
        jdbc.update("INSERT INTO " + spec.name() + " (" + columns + ") VALUES (" + placeholders + ")",
                values);
        return true;
    }

    private void validateSnapshot(JsonNode root) {
        require(root.path("formatVersion").asInt(-1) == 1 &&
                root.path("leagueId").asInt(-1) == LEAGUE &&
                root.path("season").asInt(-1) == SEASON &&
                root.path("gameweek").asInt(-1) == GAMEWEEK,
                "Snapshot không phải Premier League Gameweek 1 mùa 2024/25.");
        JsonNode tables = root.path("tables");
        require(tables.isObject(), "Snapshot thiếu bảng dữ liệu.");
        for (TableSpec spec : TABLES) {
            JsonNode rows = tables.path(spec.name());
            require(rows.isArray() && rows.size() == spec.expected(),
                    "Snapshot cần đúng " + spec.expected() + " dòng " + spec.name() + ".");
            Set<String> seen = new HashSet<>();
            for (JsonNode row : rows) {
                require(row.isObject() && row.size() == spec.columns().size(),
                        "Dòng " + spec.name() + " không đúng cấu trúc.");
                for (String column : spec.columns()) {
                    JsonNode value = row.path(column);
                    require(value.isTextual() || value.isNull(),
                            "Giá trị " + spec.name() + "." + column + " không hợp lệ.");
                }
                for (String key : spec.keys()) {
                    require(row.path(key).isTextual() && !row.path(key).asText().isBlank(),
                            "Thiếu khóa " + spec.name() + "." + key);
                }
                require(seen.add(keyLabel(spec, row)), "Snapshot có khóa trùng trong " + spec.name());
                if (spec.columns().contains("league_id")) {
                    require("39".equals(row.path("league_id").asText()), "Sai league trong " + spec.name());
                }
                if (spec.columns().contains("season_year")) {
                    require("2024".equals(row.path("season_year").asText()), "Sai mùa trong " + spec.name());
                }
                if (spec.name().equals("fixtures")) {
                    require("1".equals(row.path("gameweek").asText()), "Snapshot có trận ngoài Gameweek 1.");
                }
            }
        }
        Set<String> fixtureIds = new HashSet<>();
        for (JsonNode row : tables.path("fixtures")) fixtureIds.add(row.path("id").asText());
        for (String table : List.of("fixture_player_stats", "fixture_score_evidence")) {
            for (JsonNode row : tables.path(table)) {
                require(fixtureIds.contains(row.path("fixture_id").asText()),
                        "Snapshot có " + table + " ngoài Gameweek 1.");
            }
        }
    }

    private void verifyImportedData() {
        require(queries.clubs(SEASON, null).size() == 20, "Club API không có đúng 20 đội.");
        require(queries.players(SEASON, null, null).size() >= 63,
                "Player API thiếu thống kê mùa đã lưu.");
        require(queries.matches(SEASON, null, GAMEWEEK, null).size() == 10,
                "Match API không có đúng 10 trận Gameweek 1.");
        require(queries.standings(SEASON, null).size() == 20,
                "Standing API không có đúng 20 đội.");
        for (var match : queries.matches(SEASON, null, GAMEWEEK, null)) {
            var detail = queries.matchDetail(match.id(), SEASON).orElseThrow();
            long complete = Stream.concat(detail.homePlayers().stream(), detail.awayPlayers().stream())
                    .filter(player -> "COMPLETE".equals(player.score().status())).count();
            require("VERIFIED".equals(detail.evidenceStatus()) &&
                    detail.homePlayers().size() + detail.awayPlayers().size() == 40 && complete == 40,
                    "Fixture " + match.id() + " chưa có 40/40 điểm hoàn chỉnh: "
                            + detail.evidenceError());
        }
    }

    private static Object typedValue(TableSpec spec, String column, JsonNode row) {
        JsonNode value = row.path(column);
        if (value.isNull()) return null;
        String text = value.asText();
        if (spec.integers().contains(column)) return Integer.valueOf(text);
        if (spec.dates().contains(column)) return Date.valueOf(text);
        if (spec.timestamps().contains(column)) return Timestamp.valueOf(text);
        return text;
    }

    private static String keyLabel(TableSpec spec, JsonNode row) {
        return spec.keys().stream().map(key -> row.path(key).asText())
                .collect(Collectors.joining("/"));
    }

    private static TableSpec table(String name, String columns, String keys, String integers,
                                   String dates, String timestamps, String scope, int expected) {
        return new TableSpec(name, names(columns), names(keys), Set.copyOf(names(integers)),
                Set.copyOf(names(dates)), Set.copyOf(names(timestamps)), scope, expected);
    }

    private static List<String> names(String text) {
        if (text.isBlank()) return List.of();
        return List.of(text.split(","));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record TableSpec(String name, List<String> columns, List<String> keys,
                             Set<String> integers, Set<String> dates, Set<String> timestamps,
                             String scope, int expected) {
    }

    public record ImportSummary(Map<String, Integer> inserted, int verifiedFixtures) {
    }
}
