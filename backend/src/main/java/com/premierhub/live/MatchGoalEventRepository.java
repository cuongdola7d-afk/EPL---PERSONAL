package com.premierhub.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MatchGoalEventRepository {
    public static final String SOURCE_FPL_LIVE = "FPL_LIVE";
    public static final String TYPE_GOAL = "GOAL";
    public static final String TYPE_OWN_GOAL = "OWN_GOAL";
    public static final String TYPE_PENALTY = "PENALTY";

    private static final Logger LOG = LoggerFactory.getLogger(MatchGoalEventRepository.class);

    private final JdbcTemplate jdbc;
    private volatile String cachedMinuteColumn;

    public MatchGoalEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String minuteCol() {
        String col = cachedMinuteColumn;
        if (col != null) {
            return col;
        }
        boolean mysql = false;
        try {
            if (jdbc.getDataSource() != null) {
                try (Connection conn = jdbc.getDataSource().getConnection()) {
                    String product = conn.getMetaData().getDatabaseProductName();
                    if (product != null) {
                        String lower = product.toLowerCase(Locale.ROOT);
                        mysql = lower.contains("mysql") || lower.contains("mariadb");
                    }
                }
            }
        } catch (Exception ignored) {
            mysql = false;
        }
        col = mysql ? "`minute`" : "\"minute\"";
        cachedMinuteColumn = col;
        return col;
    }

    public List<StoredGoalEvent> findByFixture(int fixtureId) {
        String minCol = minuteCol();
        return jdbc.query(
                "SELECT e.id, e.fixture_id, e.club_id, e.player_id, p.name AS player_name, "
                        + "e." + minCol + " AS goal_minute, e.goal_type, e.source, e.detected_at, e.seq "
                        + "FROM match_goal_events e "
                        + "LEFT JOIN players p ON p.id = e.player_id "
                        + "WHERE e.fixture_id = ? "
                        + "ORDER BY CASE WHEN e." + minCol + " IS NULL THEN 1 ELSE 0 END, "
                        + "e." + minCol + " ASC, e.detected_at ASC, e.id ASC",
                (rs, row) -> new StoredGoalEvent(
                        rs.getLong("id"),
                        rs.getInt("fixture_id"),
                        rs.getInt("club_id"),
                        rs.getObject("player_id", Integer.class),
                        rs.getString("player_name"),
                        rs.getObject("goal_minute", Integer.class),
                        rs.getString("goal_type"),
                        rs.getString("source"),
                        rs.getTimestamp("detected_at").toInstant(),
                        rs.getInt("seq")
                ), fixtureId);
    }

    /**
     * Synchronizes {@code match_goal_events} for a fixture:
     * - inserts new goal events without duplicating existing (fixture_id, player_id, goal_type, seq) rows;
     * - preserves previously stored non-null minutes (never overwrites a stored minute with null);
     * - deletes removed goals (e.g. VAR disallow where a player's goal count drops, removing highest seq rows).
     */
    public void syncFixtureGoals(int fixtureId, List<GoalRecord> desiredGoals, Instant detectedAt) {
        String minCol = minuteCol();
        List<ExistingRow> existingRows = jdbc.query(
                "SELECT id, club_id, player_id, " + minCol + " AS goal_minute, goal_type, seq "
                        + "FROM match_goal_events "
                        + "WHERE fixture_id = ? "
                        + "ORDER BY seq ASC, id ASC",
                (rs, row) -> new ExistingRow(
                        rs.getLong("id"),
                        rs.getInt("club_id"),
                        rs.getObject("player_id", Integer.class),
                        rs.getObject("goal_minute", Integer.class),
                        rs.getString("goal_type"),
                        rs.getInt("seq")
                ), fixtureId);

        if ((desiredGoals == null || desiredGoals.isEmpty()) && existingRows.isEmpty()) {
            return;
        }

        List<GoalRecord> safeDesired = desiredGoals == null ? List.of() : desiredGoals;
        Set<Integer> validPlayerIds = resolveExistingPlayerIds(safeDesired);

        List<GoalRecord> normalizedDesired = new ArrayList<>(safeDesired.size());
        Map<String, Integer> nullPlayerSeqs = new HashMap<>();
        for (GoalRecord rec : safeDesired) {
            Integer playerId = rec.playerId();
            if (playerId != null && !validPlayerIds.contains(playerId)) {
                LOG.warn("Skipping player_id={} for match_goal_events fixture={} because player does not exist in players table; storing with player_id=NULL",
                        playerId, fixtureId);
                playerId = null;
            }
            int seq = rec.seq();
            if (playerId == null) {
                String nullKey = rec.clubId() + ":" + rec.goalType();
                seq = nullPlayerSeqs.merge(nullKey, 1, Integer::sum);
            }
            normalizedDesired.add(new GoalRecord(rec.clubId(), playerId, rec.minute(), rec.goalType(), seq));
        }

        Map<EventKey, ExistingRow> existingByKey = new LinkedHashMap<>();
        List<Long> duplicateIdsToDelete = new ArrayList<>();
        for (ExistingRow row : existingRows) {
            EventKey key = EventKey.of(row.playerId(), row.clubId(), row.goalType(), row.seq());
            if (existingByKey.containsKey(key)) {
                duplicateIdsToDelete.add(row.id());
            } else {
                existingByKey.put(key, row);
            }
        }

        Map<EventKey, GoalRecord> desiredByKey = new LinkedHashMap<>();
        for (GoalRecord rec : normalizedDesired) {
            desiredByKey.putIfAbsent(EventKey.of(rec.playerId(), rec.clubId(), rec.goalType(), rec.seq()), rec);
        }

        for (Long dupId : duplicateIdsToDelete) {
            jdbc.update("DELETE FROM match_goal_events WHERE id = ?", dupId);
        }

        // Delete goals no longer present (e.g. VAR disallow drops count -> highest seq removed)
        for (Map.Entry<EventKey, ExistingRow> entry : existingByKey.entrySet()) {
            if (!desiredByKey.containsKey(entry.getKey())) {
                jdbc.update("DELETE FROM match_goal_events WHERE id = ?", entry.getValue().id());
            }
        }

        Timestamp ts = Timestamp.from(detectedAt);
        for (Map.Entry<EventKey, GoalRecord> entry : desiredByKey.entrySet()) {
            EventKey key = entry.getKey();
            GoalRecord desired = entry.getValue();
            ExistingRow existing = existingByKey.get(key);
            if (existing == null) {
                jdbc.update(
                        "INSERT INTO match_goal_events "
                                + "(fixture_id, club_id, player_id, " + minCol + ", goal_type, source, detected_at, seq) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        fixtureId,
                        desired.clubId(),
                        desired.playerId(),
                        desired.minute(),
                        desired.goalType(),
                        SOURCE_FPL_LIVE,
                        ts,
                        desired.seq());
            } else {
                // Preserve stored non-null minute; only fill if DB had NULL and incoming has a minute.
                Integer resolvedMinute = existing.minute() != null ? existing.minute() : desired.minute();
                if (!Objects.equals(existing.minute(), resolvedMinute) || existing.clubId() != desired.clubId()) {
                    jdbc.update("UPDATE match_goal_events SET " + minCol + " = ?, club_id = ? WHERE id = ?",
                            resolvedMinute, desired.clubId(), existing.id());
                }
            }
        }
    }

    private Set<Integer> resolveExistingPlayerIds(List<GoalRecord> goals) {
        Set<Integer> candidateIds = goals.stream()
                .map(GoalRecord::playerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        String placeholders = candidateIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Integer> found = jdbc.query(
                "SELECT id FROM players WHERE id IN (" + placeholders + ")",
                (rs, row) -> rs.getInt(1),
                candidateIds.toArray()
        );
        return new HashSet<>(found);
    }

    private record ExistingRow(long id, int clubId, Integer playerId, Integer minute, String goalType, int seq) {
    }

    private record EventKey(Integer playerId, Integer nullScopeClubId, String goalType, int seq) {
        static EventKey of(Integer playerId, int clubId, String goalType, int seq) {
            return new EventKey(playerId, playerId == null ? clubId : null, goalType, seq);
        }
    }

    public record GoalRecord(int clubId, Integer playerId, Integer minute, String goalType, int seq) {
    }

    public record StoredGoalEvent(
            long id,
            int fixtureId,
            int clubId,
            Integer playerId,
            String playerName,
            Integer minute,
            String goalType,
            String source,
            Instant detectedAt,
            int seq
    ) {
    }
}
