package com.premierhub.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class TeamOfWeekRepository {
    public record Appearance(int playerId, String name, int clubId, String club,
                             int fixtureId, BigDecimal rating, List<String> eligiblePositions) { }
    public record Pool(List<Appearance> appearances, int completedFixtures, int recordedFixtures) { }
    private record Key(int playerId, int fixtureId) { }

    private final JdbcTemplate jdbc;

    public TeamOfWeekRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pool read(int gameweek) {
        var rows = new LinkedHashMap<Key, Appearance>();
        // Club comes from the match row. No current-roster or as-of-date filter.
        jdbc.query("""
                SELECT s.player_id, p.name, s.club_id, c.name AS club_name,
                       s.fixture_id, s.rating, e.position_code
                FROM manual_fixture_player_stats s
                JOIN fixtures f ON f.id=s.fixture_id AND f.league_id=s.league_id
                    AND f.season_year=s.season_year
                JOIN players p ON p.id=s.player_id
                JOIN clubs c ON c.id=s.club_id
                LEFT JOIN player_eligible_positions e ON e.player_id=s.player_id
                    AND e.league_id=s.league_id AND e.season_year=s.season_year
                WHERE s.league_id=39 AND s.season_year=2026 AND f.gameweek=?
                    AND s.participation_status='PLAYED'
                    AND s.club_id IN (f.home_club_id, f.away_club_id)
                ORDER BY s.player_id, s.fixture_id, e.position_code
                """, (RowCallbackHandler) rs -> {
                    var key = new Key(rs.getInt("player_id"), rs.getInt("fixture_id"));
                    var appearance = rows.get(key);
                    if (appearance == null) {
                        appearance = new Appearance(key.playerId(), rs.getString("name"),
                                rs.getInt("club_id"), rs.getString("club_name"), key.fixtureId(),
                                rs.getBigDecimal("rating"), new ArrayList<>());
                        rows.put(key, appearance);
                    }
                    String position = rs.getString("position_code");
                    if (position != null) appearance.eligiblePositions().add(position);
                }, gameweek);
        var coverage = jdbc.queryForMap("""
                SELECT COUNT(*) AS completed, COALESCE(SUM(CASE WHEN EXISTS (
                    SELECT 1 FROM manual_fixture_player_stats s WHERE s.fixture_id=f.id
                        AND s.league_id=f.league_id AND s.season_year=f.season_year
                    ) THEN 1 ELSE 0 END),0) AS recorded
                FROM fixtures f WHERE f.league_id=39 AND f.season_year=2026
                    AND f.gameweek=? AND f.status='FINISHED'
                """, gameweek);
        var appearances = rows.values().stream().map(row -> new Appearance(row.playerId(),
                row.name(), row.clubId(), row.club(), row.fixtureId(), row.rating(),
                List.copyOf(row.eligiblePositions()))).toList();
        return new Pool(appearances, ((Number) coverage.get("completed")).intValue(),
                ((Number) coverage.get("recorded")).intValue());
    }
}
