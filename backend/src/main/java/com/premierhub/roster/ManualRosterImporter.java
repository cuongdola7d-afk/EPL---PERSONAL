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
import java.util.HashSet;
import java.util.Set;

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
        Set<Integer> clubs = new HashSet<>(jdbc.queryForList(
                "SELECT club_id FROM season_clubs WHERE league_id=? AND season_year=?",
                Integer.class, LEAGUE, SEASON));
        Map<Integer, String> names = new HashMap<>();
        jdbc.query("SELECT id, name FROM players", rs -> {
            names.put(rs.getInt("id"), rs.getString("name"));
        });
        Map<SeasonMembershipKey, String> positions = new HashMap<>();
        jdbc.query("SELECT player_id, club_id, position FROM player_season_stats WHERE league_id=? AND season_year=?",
                rs -> {
                    positions.put(new SeasonMembershipKey(rs.getInt("player_id"), rs.getInt("club_id")),
                            rs.getString("position"));
                }, LEAGUE, SEASON);
        Map<Integer, Map<MembershipKey, Interval>> savedIntervals = new HashMap<>();
        jdbc.query("""
                SELECT player_id, club_id, start_date, end_date FROM manual_player_memberships
                WHERE league_id=? AND season_year=?
                """, rs -> {
            int clubId = rs.getInt("club_id");
            LocalDate start = rs.getDate("start_date").toLocalDate();
            Date end = rs.getDate("end_date");
            savedIntervals.computeIfAbsent(rs.getInt("player_id"), id -> new HashMap<>())
                    .put(new MembershipKey(clubId, start),
                            new Interval(clubId, start, end == null ? null : end.toLocalDate()));
        }, LEAGUE, SEASON);
        // Validate the whole batch before inserting a player. The transaction also protects against write failures.
        Map<SeasonMembershipKey, String> validatedPositions = new HashMap<>(positions);
        for (var row : rows) {
            if (!clubs.contains(row.clubId())) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": club_id is not in Premier League season 2026: " + row.clubId());
            }
            // Provider IDs may be reused, but only new manual identities use the reserved range.
            if (!names.containsKey(row.playerId()) && (row.playerId() < 2_000_000_000 || row.playerId() > 2_099_999_999)) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": new player_id must be in 2000000000..2099999999; "
                        + "IDs outside this range must already exist: " + row.playerId());
            }
            if (names.containsKey(row.playerId()) && !names.get(row.playerId()).equals(row.name())) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": player_id belongs to a different name: " + row.playerId());
            }
            var key = new SeasonMembershipKey(row.playerId(), row.clubId());
            if (validatedPositions.containsKey(key) && !row.position().equals(validatedPositions.get(key))) {
                throw new IllegalArgumentException("CSV line " + row.line()
                        + ": player_id already has a different position for this club and season: " + row.playerId());
            }
            validatedPositions.put(key, row.position());
        }
        validateIntervals(rows, savedIntervals);
        List<Object[]> newPlayers = new ArrayList<>();
        List<Object[]> newMemberships = new ArrayList<>();
        List<Object[]> newIntervals = new ArrayList<>();
        List<Object[]> updatedIntervals = new ArrayList<>();
        for (var row : rows) {
            if (!names.containsKey(row.playerId())) {
                newPlayers.add(new Object[] {row.playerId(), row.name()});
                names.put(row.playerId(), row.name());
            }
            var seasonKey = new SeasonMembershipKey(row.playerId(), row.clubId());
            if (!positions.containsKey(seasonKey)) {
                newMemberships.add(new Object[] {LEAGUE, row.season(), row.playerId(), row.clubId(), row.position()});
                positions.put(seasonKey, row.position());
            }
            Interval saved = savedIntervals.getOrDefault(row.playerId(), Map.of())
                    .get(new MembershipKey(row.clubId(), row.startDate()));
            if (saved == null) {
                newIntervals.add(new Object[] {LEAGUE, SEASON, row.playerId(), row.clubId(), Date.valueOf(row.startDate()),
                        row.endDate() == null ? null : Date.valueOf(row.endDate())});
            } else if (!java.util.Objects.equals(saved.end(), row.endDate())) {
                updatedIntervals.add(new Object[] {row.endDate() == null ? null : Date.valueOf(row.endDate()),
                        LEAGUE, SEASON, row.playerId(), row.clubId(), Date.valueOf(row.startDate())});
            }
        }
        batchUpdate("INSERT INTO players (id, name) VALUES (?, ?)", newPlayers);
        batchUpdate("""
                INSERT INTO player_season_stats
                (league_id, season_year, player_id, club_id, position, appearances, minutes, goals, assists)
                VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, NULL)
                """, newMemberships);
        batchUpdate("""
                UPDATE manual_player_memberships SET end_date=?
                WHERE league_id=? AND season_year=? AND player_id=? AND club_id=? AND start_date=?
                """, updatedIntervals);
        batchUpdate("""
                INSERT INTO manual_player_memberships
                (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """, newIntervals);
        return new Result(rows.size(), newPlayers.size(), newMemberships.size(), newIntervals.size(), updatedIntervals.size());
    }

    private void batchUpdate(String sql, List<Object[]> arguments) {
        if (!arguments.isEmpty()) jdbc.batchUpdate(sql, arguments);
    }

    private void validateIntervals(List<ManualRosterCsvReader.Row> rows,
                                   Map<Integer, Map<MembershipKey, Interval>> savedIntervals) {
        Map<Integer, Map<MembershipKey, Interval>> byPlayer = new HashMap<>();
        for (var row : rows) {
            byPlayer.computeIfAbsent(row.playerId(), id -> new HashMap<>(savedIntervals.getOrDefault(id, Map.of())))
                    .put(new MembershipKey(row.clubId(), row.startDate()),
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
    private record SeasonMembershipKey(int playerId, int clubId) { }
    private record Interval(int clubId, LocalDate start, LocalDate end) { }

    private int count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    public record Result(int rows, int playersInserted, int membershipsInserted,
                         int intervalsInserted, int intervalsUpdated) { }
}
