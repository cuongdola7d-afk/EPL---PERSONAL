package com.premierhub.positions;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PlayerPositionImporter {
    private static final int LEAGUE = 39;
    private static final int SEASON = 2026;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final PlayerPositionCsvReader reader = new PlayerPositionCsvReader();

    public PlayerPositionImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Result importFile(Path file) throws IOException {
        List<PlayerPositionCsvReader.Row> rows = reader.read(file);
        return transactions.execute(status -> importRows(rows));
    }

    private Result importRows(List<PlayerPositionCsvReader.Row> rows) {
        List<Change> changes = new ArrayList<>();
        for (var row : rows) {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM player_season_stats
                    WHERE league_id=? AND season_year=? AND player_id=?
                    """, Integer.class, LEAGUE, SEASON, row.playerId());
            if (count == null || count == 0) {
                throw invalid(row, "player_id is not in the 2026/27 roster");
            }
            PlayerPositionCsvReader.PositionSet current = saved(row.playerId());
            if (Objects.equals(current, row.incoming())) continue;
            if (!Objects.equals(current, row.expected())) {
                throw invalid(row, "conflict: saved positions changed; expected "
                        + row.expected() + ", found " + current);
            }
            changes.add(new Change(row.playerId(), current == null, row.incoming()));
        }
        int inserted = 0;
        int updated = 0;
        for (Change change : changes) {
            if (change.insert()) {
                jdbc.update("""
                        INSERT INTO player_specific_positions
                        (league_id, season_year, player_id, primary_position) VALUES (?, ?, ?, ?)
                        """, LEAGUE, SEASON, change.playerId(), change.positions().primary());
                inserted++;
            } else {
                jdbc.update("""
                        DELETE FROM player_eligible_positions
                        WHERE league_id=? AND season_year=? AND player_id=?
                        """, LEAGUE, SEASON, change.playerId());
                jdbc.update("""
                        UPDATE player_specific_positions SET primary_position=?
                        WHERE league_id=? AND season_year=? AND player_id=?
                        """, change.positions().primary(), LEAGUE, SEASON, change.playerId());
                updated++;
            }
            for (String code : change.positions().eligible()) {
                jdbc.update("""
                        INSERT INTO player_eligible_positions
                        (league_id, season_year, player_id, position_code) VALUES (?, ?, ?, ?)
                        """, LEAGUE, SEASON, change.playerId(), code);
            }
        }
        return new Result(rows.size(), inserted, updated);
    }

    private PlayerPositionCsvReader.PositionSet saved(int playerId) {
        List<String> primary = jdbc.query("""
                SELECT primary_position FROM player_specific_positions
                WHERE league_id=? AND season_year=? AND player_id=?
                """, (rs, row) -> rs.getString(1), LEAGUE, SEASON, playerId);
        if (primary.isEmpty()) return null;
        Set<String> eligible = Set.copyOf(jdbc.queryForList("""
                SELECT position_code FROM player_eligible_positions
                WHERE league_id=? AND season_year=? AND player_id=?
                """, String.class, LEAGUE, SEASON, playerId));
        return new PlayerPositionCsvReader.PositionSet(primary.getFirst(), eligible);
    }

    private static IllegalArgumentException invalid(PlayerPositionCsvReader.Row row, String message) {
        return new IllegalArgumentException("CSV line " + row.line() + ": " + message
                + " (player_id=" + row.playerId() + ")");
    }

    private record Change(int playerId, boolean insert, PlayerPositionCsvReader.PositionSet positions) { }
    public record Result(int rows, int inserted, int updated) { }
}
