package com.premierhub.roster;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        validateIntervals(rows);
        int playersInserted = 0;
        int membershipsInserted = 0;
        int intervalsInserted = 0;
        int intervalsUpdated = 0;
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
            List<Date> savedEnds = jdbc.queryForList("""
                    SELECT end_date FROM manual_player_memberships
                    WHERE league_id=? AND season_year=? AND player_id=? AND club_id=? AND start_date=?
                    """, Date.class, LEAGUE, SEASON, row.playerId(), row.clubId(), Date.valueOf(row.startDate()));
            if (savedEnds.isEmpty()) {
                jdbc.update("""
                        INSERT INTO manual_player_memberships
                        (league_id, season_year, player_id, club_id, start_date, end_date)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, LEAGUE, SEASON, row.playerId(), row.clubId(), Date.valueOf(row.startDate()),
                        row.endDate() == null ? null : Date.valueOf(row.endDate()));
                intervalsInserted++;
            } else if (!java.util.Objects.equals(savedEnds.getFirst(),
                    row.endDate() == null ? null : Date.valueOf(row.endDate()))) {
                jdbc.update("""
                        UPDATE manual_player_memberships SET end_date=?
                        WHERE league_id=? AND season_year=? AND player_id=? AND club_id=? AND start_date=?
                        """, row.endDate() == null ? null : Date.valueOf(row.endDate()),
                        LEAGUE, SEASON, row.playerId(), row.clubId(), Date.valueOf(row.startDate()));
                intervalsUpdated++;
            }
        }
        return new Result(rows.size(), playersInserted, membershipsInserted, intervalsInserted, intervalsUpdated);
    }

    private void validateIntervals(List<ManualRosterCsvReader.Row> rows) {
        Map<Integer, Map<MembershipKey, Interval>> byPlayer = new HashMap<>();
        for (var row : rows) {
            byPlayer.computeIfAbsent(row.playerId(), id -> {
                Map<MembershipKey, Interval> existing = new HashMap<>();
                jdbc.query("""
                        SELECT club_id, start_date, end_date FROM manual_player_memberships
                        WHERE league_id=? AND season_year=? AND player_id=?
                        """, rs -> {
                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    Date end = rs.getDate("end_date");
                    int clubId = rs.getInt("club_id");
                    existing.put(new MembershipKey(clubId, start),
                            new Interval(clubId, start, end == null ? null : end.toLocalDate()));
                }, LEAGUE, SEASON, id);
                return existing;
            }).put(new MembershipKey(row.clubId(), row.startDate()),
                    new Interval(row.clubId(), row.startDate(), row.endDate()));
        }
        for (var entry : byPlayer.entrySet()) {
            List<Interval> intervals = new ArrayList<>(entry.getValue().values());
            intervals.sort(Comparator.comparing(Interval::start));
            for (int i = 1; i < intervals.size(); i++) {
                Interval prior = intervals.get(i - 1);
                Interval next = intervals.get(i);
                if (prior.end() == null || next.start().isBefore(prior.end())) {
                    throw new IllegalArgumentException("Overlapping club memberships for player_id "
                            + entry.getKey() + ": club " + prior.clubId() + " and club " + next.clubId());
                }
            }
        }
    }

    private record MembershipKey(int clubId, LocalDate start) { }
    private record Interval(int clubId, LocalDate start, LocalDate end) { }

    private int count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    public record Result(int rows, int playersInserted, int membershipsInserted,
                         int intervalsInserted, int intervalsUpdated) { }
}
