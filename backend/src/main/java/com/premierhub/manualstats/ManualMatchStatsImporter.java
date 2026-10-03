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

@Service
public class ManualMatchStatsImporter {
    private static final int LEAGUE = 39;
    private static final int SEASON = 2026;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ManualSeasonStatsService seasonStats;
    private final ManualMatchStatsCsvReader reader = new ManualMatchStatsCsvReader();

    public ManualMatchStatsImporter(JdbcTemplate jdbc, TransactionTemplate transactions,
                                    ManualSeasonStatsService seasonStats) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.seasonStats = seasonStats;
    }

    public Result importFile(Path file) throws IOException {
        List<ManualMatchStatsCsvReader.Row> rows = reader.read(file);
        return transactions.execute(status -> importRows(rows));
    }

    private Result importRows(List<ManualMatchStatsCsvReader.Row> rows) {
        List<Validated> validated = new ArrayList<>();
        for (var row : rows) {
            List<Fixture> fixtures = jdbc.query("""
                    SELECT league_id, season_year, home_club_id, away_club_id, match_date, status
                    FROM fixtures WHERE id=?
                    """, (rs, index) -> new Fixture(rs.getInt("league_id"), rs.getInt("season_year"),
                    rs.getInt("home_club_id"), rs.getInt("away_club_id"),
                    rs.getDate("match_date").toLocalDate(), rs.getString("status")), row.fixtureId());
            if (fixtures.size() != 1 || fixtures.getFirst().league() != LEAGUE
                    || fixtures.getFirst().season() != SEASON) {
                throw invalid(row, "fixture_id is not a Premier League 2026/27 fixture");
            }
            Fixture fixture = fixtures.getFirst();
            if (!fixture.status().equals("FINISHED")) {
                throw invalid(row, "fixture_id is not FINISHED");
            }
            List<Integer> clubIds = jdbc.queryForList("""
                    SELECT m.club_id FROM manual_player_memberships m
                    JOIN player_season_stats ps ON ps.league_id=m.league_id
                      AND ps.season_year=m.season_year AND ps.player_id=m.player_id
                      AND ps.club_id=m.club_id
                    WHERE m.league_id=? AND m.season_year=? AND m.player_id=?
                      AND m.club_id IN (?, ?) AND m.start_date<=?
                      AND (m.end_date IS NULL OR m.end_date>?)
                    """, Integer.class, LEAGUE, SEASON, row.playerId(), fixture.homeClubId(),
                    fixture.awayClubId(), Date.valueOf(fixture.date()), Date.valueOf(fixture.date()));
            if (clubIds.size() != 1) {
                throw invalid(row, "player_id must have exactly one evidenced membership in a fixture club on match date");
            }
            int clubId = clubIds.getFirst();
            Integer rawCount = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM fixture_player_stats WHERE fixture_id=? AND player_id=?
                    """, Integer.class, row.fixtureId(), row.playerId());
            if (rawCount != null && rawCount > 0) {
                throw invalid(row, "a provider fixture_player_stats row already owns this player-match key");
            }
            List<Saved> existing = jdbc.query("""
                    SELECT league_id, season_year, club_id, participation_status, rating,
                           fantasy_points, minutes, goals, assists, yellow_cards, red_cards
                    FROM manual_fixture_player_stats WHERE fixture_id=? AND player_id=?
                    """, (rs, index) -> new Saved(rs.getInt("league_id"), rs.getInt("season_year"),
                    rs.getInt("club_id"), rs.getString("participation_status"),
                    rs.getBigDecimal("rating"), rs.getBigDecimal("fantasy_points"),
                    rs.getObject("minutes", Integer.class), rs.getObject("goals", Integer.class),
                    rs.getObject("assists", Integer.class), rs.getObject("yellow_cards", Integer.class),
                    rs.getObject("red_cards", Integer.class)), row.fixtureId(), row.playerId());
            if (!existing.isEmpty() && !same(existing.getFirst(), row, clubId)) {
                throw invalid(row, "conflicting saved player-match row; existing data was not overwritten");
            }
            validated.add(new Validated(row, clubId, existing.isEmpty()));
        }
        int inserted = 0;
        for (var item : validated) {
            if (!item.insert()) continue;
            var row = item.row();
            jdbc.update("""
                    INSERT INTO manual_fixture_player_stats
                    (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                     rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row.fixtureId(), row.playerId(), LEAGUE, SEASON, item.clubId(),
                    row.status(), row.rating(), row.fantasyPoints(), row.minutes(), row.goals(),
                    row.assists(), row.yellowCards(), row.redCards());
            inserted++;
        }
        if (inserted > 0) seasonStats.rebuildLatestComplete();
        return new Result(rows.size(), inserted);
    }

    private static boolean same(Saved saved, ManualMatchStatsCsvReader.Row row, int clubId) {
        return saved.league() == LEAGUE && saved.season() == SEASON && saved.clubId() == clubId
                && saved.status().equals(row.status())
                && sameDecimal(saved.rating(), row.rating())
                && sameDecimal(saved.fantasyPoints(), row.fantasyPoints())
                && Objects.equals(saved.minutes(), row.minutes())
                && Objects.equals(saved.goals(), row.goals())
                && Objects.equals(saved.assists(), row.assists())
                && Objects.equals(saved.yellowCards(), row.yellowCards())
                && Objects.equals(saved.redCards(), row.redCards());
    }

    private static boolean sameDecimal(BigDecimal saved, BigDecimal incoming) {
        return saved == null ? incoming == null : incoming != null && saved.compareTo(incoming) == 0;
    }

    private static IllegalArgumentException invalid(ManualMatchStatsCsvReader.Row row, String message) {
        return new IllegalArgumentException("CSV line " + row.line() + ": " + message
                + " (fixture_id=" + row.fixtureId() + ", player_id=" + row.playerId() + ")");
    }

    private record Fixture(int league, int season, int homeClubId, int awayClubId,
                           LocalDate date, String status) { }
    private record Saved(int league, int season, int clubId, String status, BigDecimal rating,
                         BigDecimal fantasyPoints, Integer minutes, Integer goals, Integer assists,
                         Integer yellowCards, Integer redCards) { }
    private record Validated(ManualMatchStatsCsvReader.Row row, int clubId, boolean insert) { }
    public record Result(int rows, int inserted) { }
}
