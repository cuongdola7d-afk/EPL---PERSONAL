package com.premierhub.manualstats;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Fills verified NULL fields in existing manual player-match rows. Blank CSV cells mean no change. */
@Service
public class ManualMatchStatsPatchImporter {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ManualSeasonStatsService seasonStats;
    private final ManualMatchStatsCsvReader reader = new ManualMatchStatsCsvReader();

    public ManualMatchStatsPatchImporter(JdbcTemplate jdbc, TransactionTemplate transactions,
                                         ManualSeasonStatsService seasonStats) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.seasonStats = seasonStats;
    }

    public Result fillMissing(Path file, int fixtureId) throws IOException {
        if (fixtureId <= 0) throw new IllegalArgumentException("fixture_id must be positive");
        List<ManualMatchStatsCsvReader.Row> rows = reader.read(file);
        return transactions.execute(status -> fillRows(rows, fixtureId));
    }

    private Result fillRows(List<ManualMatchStatsCsvReader.Row> rows, int fixtureId) {
        List<ManualMatchStatsCsvReader.Row> changes = new ArrayList<>();
        int filledCells = 0;
        for (var row : rows) {
            if (row.fixtureId() != fixtureId) throw invalid(row, "row belongs to another fixture");
            List<Saved> savedRows = jdbc.query("""
                    SELECT s.league_id, s.season_year, s.club_id, s.participation_status,
                           s.rating, s.fantasy_points, s.minutes, s.goals, s.assists,
                           s.yellow_cards, s.red_cards, f.league_id AS fixture_league,
                           f.season_year AS fixture_season, f.home_club_id, f.away_club_id,
                           f.match_date, f.status AS fixture_status
                    FROM manual_fixture_player_stats s JOIN fixtures f ON f.id=s.fixture_id
                    WHERE s.fixture_id=? AND s.player_id=? FOR UPDATE
                    """, (rs, index) -> new Saved(rs.getInt("league_id"), rs.getInt("season_year"),
                    rs.getInt("club_id"), rs.getString("participation_status"),
                    rs.getBigDecimal("rating"), rs.getBigDecimal("fantasy_points"),
                    rs.getObject("minutes", Integer.class), rs.getObject("goals", Integer.class),
                    rs.getObject("assists", Integer.class), rs.getObject("yellow_cards", Integer.class),
                    rs.getObject("red_cards", Integer.class), rs.getInt("fixture_league"),
                    rs.getInt("fixture_season"), rs.getInt("home_club_id"), rs.getInt("away_club_id"),
                    rs.getDate("match_date").toLocalDate(), rs.getString("fixture_status")),
                    row.fixtureId(), row.playerId());
            if (savedRows.size() != 1) throw invalid(row, "existing manual player-match row is required");
            Saved saved = savedRows.getFirst();
            if (saved.league() != 39 || saved.season() != 2026 || saved.fixtureLeague() != 39
                    || saved.fixtureSeason() != 2026 || !saved.fixtureStatus().equals("FINISHED")) {
                throw invalid(row, "fixture and saved row must be finished Premier League 2026/27 data");
            }
            if (!saved.status().equals(row.status())) throw invalid(row, "participation status conflicts");
            if (saved.clubId() != saved.homeClubId() && saved.clubId() != saved.awayClubId()) {
                throw invalid(row, "saved club is not a fixture club");
            }
            Integer memberships = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM manual_player_memberships
                    WHERE league_id=39 AND season_year=2026 AND player_id=? AND club_id=?
                      AND start_date<=? AND (end_date IS NULL OR end_date>?)
                    """, Integer.class, row.playerId(), saved.clubId(),
                    Date.valueOf(saved.date()), Date.valueOf(saved.date()));
            if (memberships == null || memberships != 1) {
                throw invalid(row, "player has no unique evidenced club membership on match date");
            }
            int count = 0;
            count += check(saved.rating(), row.rating(), "rating", row);
            count += check(saved.fantasyPoints(), row.rating(), "fantasy_points", row);
            count += check(saved.minutes(), row.minutes(), "minutes", row);
            count += check(saved.goals(), row.goals(), "goals", row);
            count += check(saved.assists(), row.assists(), "assists", row);
            count += check(saved.yellowCards(), row.yellowCards(), "yellow_cards", row);
            count += check(saved.redCards(), row.redCards(), "red_cards", row);
            if (count > 0) changes.add(row);
            filledCells += count;
        }
        for (var row : changes) {
            jdbc.update("""
                    UPDATE manual_fixture_player_stats SET rating=COALESCE(rating, ?),
                        fantasy_points=COALESCE(fantasy_points, ?), minutes=COALESCE(minutes, ?),
                        goals=COALESCE(goals, ?), assists=COALESCE(assists, ?),
                        yellow_cards=COALESCE(yellow_cards, ?), red_cards=COALESCE(red_cards, ?)
                    WHERE fixture_id=? AND player_id=?
                    """, row.rating(), row.rating(), row.minutes(), row.goals(), row.assists(),
                    row.yellowCards(), row.redCards(), row.fixtureId(), row.playerId());
        }
        if (filledCells > 0) seasonStats.rebuildLatestComplete();
        return new Result(rows.size(), changes.size(), filledCells);
    }

    private static int check(Integer saved, Integer incoming, String field, ManualMatchStatsCsvReader.Row row) {
        if (incoming == null) return 0;
        if (saved == null) return 1;
        if (!Objects.equals(saved, incoming)) throw invalid(row, field + " conflicts with saved value");
        return 0;
    }

    private static int check(BigDecimal saved, BigDecimal incoming, String field,
                             ManualMatchStatsCsvReader.Row row) {
        if (incoming == null) return 0;
        if (saved == null) return 1;
        if (saved.compareTo(incoming) != 0) throw invalid(row, field + " conflicts with saved value");
        return 0;
    }

    private static IllegalArgumentException invalid(ManualMatchStatsCsvReader.Row row, String message) {
        return new IllegalArgumentException("CSV line " + row.line() + ": " + message
                + " (fixture_id=" + row.fixtureId() + ", player_id=" + row.playerId() + ")");
    }

    private record Saved(int league, int season, int clubId, String status, BigDecimal rating,
                         BigDecimal fantasyPoints, Integer minutes, Integer goals, Integer assists,
                         Integer yellowCards, Integer redCards, int fixtureLeague, int fixtureSeason,
                         int homeClubId, int awayClubId, LocalDate date, String fixtureStatus) { }
    public record Result(int rows, int updatedRows, int filledCells) { }
}
