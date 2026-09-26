package com.premierhub.roster;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class ManualRosterImporter {
    private static final int LEAGUE = 39;
    private static final int SEASON = 2026;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ManualRosterCsvReader reader = new ManualRosterCsvReader();

    public ManualRosterImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Result importFile(Path file) throws IOException {
        List<ManualRosterCsvReader.Row> rows = reader.read(file);
        return transactions.execute(status -> importRows(rows));
    }

    private Result importRows(List<ManualRosterCsvReader.Row> rows) {
        if (count("SELECT COUNT(*) FROM seasons WHERE league_id=? AND season_year=?", LEAGUE, SEASON) != 1) {
            throw new IllegalStateException("Import 2026 clubs and season before the manual player roster");
        }
        // Validate the whole batch before inserting a player. The transaction also protects against write failures.
        for (var row : rows) {
            if (count("SELECT COUNT(*) FROM season_clubs WHERE league_id=? AND season_year=? AND club_id=?",
                    LEAGUE, row.season(), row.clubId()) != 1) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": club_id is not in Premier League season 2026: " + row.clubId());
            }
            List<String> names = jdbc.queryForList("SELECT name FROM players WHERE id=?", String.class, row.playerId());
            if (!names.isEmpty() && !names.getFirst().equals(row.name())) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": player_id belongs to a different name: " + row.playerId());
            }
            List<String> positions = jdbc.queryForList("""
                    SELECT position FROM player_season_stats
                    WHERE league_id=? AND season_year=? AND player_id=? AND club_id=?
                    """, String.class, LEAGUE, row.season(), row.playerId(), row.clubId());
            if (!positions.isEmpty() && !row.position().equals(positions.getFirst())) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": player_id already has a different position for this club and season: " + row.playerId());
            }
        }
        int playersInserted = 0;
        int membershipsInserted = 0;
        for (var row : rows) {
            if (count("SELECT COUNT(*) FROM players WHERE id=?", row.playerId()) == 0) {
                jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", row.playerId(), row.name());
                playersInserted++;
            }
            if (count("""
                    SELECT COUNT(*) FROM player_season_stats
                    WHERE league_id=? AND season_year=? AND player_id=? AND club_id=?
                    """, LEAGUE, row.season(), row.playerId(), row.clubId()) == 0) {
                jdbc.update("""
                        INSERT INTO player_season_stats
                        (league_id, season_year, player_id, club_id, position,
                         appearances, minutes, goals, assists)
                        VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, NULL)
                        """, LEAGUE, row.season(), row.playerId(), row.clubId(), row.position());
                membershipsInserted++;
            }
        }
        return new Result(rows.size(), playersInserted, membershipsInserted);
    }

    private int count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    public record Result(int rows, int playersInserted, int membershipsInserted) { }
}
