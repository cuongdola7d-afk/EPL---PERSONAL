package com.premierhub.fpl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Transactional, idempotent importer for manual_fixture_player_extra_stats (season 2026/27, GW1-GW38).
 * Never touches manual_fixture_player_stats and never overwrites non-NULL saved values.
 */
@Service
public class ManualExtraStatsImporter {
    private static final int LEAGUE_ID = 39;
    private static final int SEASON_YEAR = 2026;
    private static final int MIN_FPL_GAMEWEEK = 1;
    private static final int MAX_FPL_GAMEWEEK = 38;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ManualExtraStatsCsvReader reader = new ManualExtraStatsCsvReader();

    public ManualExtraStatsImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Result importFile(Path file) throws IOException {
        List<ManualExtraStatsCsvReader.Row> rows = reader.read(file);
        return transactions.execute(status -> importRows(rows));
    }

    private Result importRows(List<ManualExtraStatsCsvReader.Row> rows) {
        List<Validated> validated = new ArrayList<>();
        for (var row : rows) {
            List<Fixture> fixtures = jdbc.query("""
                    SELECT league_id, season_year, gameweek, home_club_id, away_club_id, match_date, status
                    FROM fixtures WHERE id = ?
                    """, (rs, index) -> new Fixture(
                    rs.getInt("league_id"),
                    rs.getInt("season_year"),
                    rs.getInt("gameweek"),
                    rs.getInt("home_club_id"),
                    rs.getInt("away_club_id"),
                    rs.getDate("match_date").toLocalDate(),
                    rs.getString("status")
            ), row.fixtureId());
            if (fixtures.size() != 1 || fixtures.getFirst().league() != LEAGUE_ID
                    || fixtures.getFirst().season() != SEASON_YEAR) {
                throw invalid(row, "fixture_id is not a Premier League 2026/27 fixture");
            }
            Fixture fixture = fixtures.getFirst();
            if (fixture.gameweek() < MIN_FPL_GAMEWEEK || fixture.gameweek() > MAX_FPL_GAMEWEEK) {
                throw invalid(row, "fixture gameweek must be between " + MIN_FPL_GAMEWEEK + " and "
                        + MAX_FPL_GAMEWEEK + " (got GW" + fixture.gameweek() + ")");
            }
            if (!fixture.status().equals("FINISHED")) {
                throw invalid(row, "fixture_id is not FINISHED");
            }
            List<Integer> clubIds = jdbc.queryForList("""
                    SELECT m.club_id FROM manual_player_memberships m
                    JOIN player_season_stats ps ON ps.league_id = m.league_id
                      AND ps.season_year = m.season_year AND ps.player_id = m.player_id
                      AND ps.club_id = m.club_id
                    WHERE m.league_id = ? AND m.season_year = ? AND m.player_id = ?
                      AND m.club_id IN (?, ?) AND m.start_date <= ?
                      AND (m.end_date IS NULL OR m.end_date > ?)
                    """, Integer.class, LEAGUE_ID, SEASON_YEAR, row.playerId(),
                    fixture.homeClubId(), fixture.awayClubId(),
                    Date.valueOf(fixture.date()), Date.valueOf(fixture.date()));
            if (clubIds.size() != 1) {
                throw invalid(row, "player_id must have exactly one evidenced membership in a fixture club on match date");
            }
            int clubId = clubIds.getFirst();

            List<Saved> existing = jdbc.query("""
                    SELECT league_id, season_year, club_id, starts, clean_sheets, goals_conceded, own_goals,
                           penalties_saved, penalties_missed, saves, bonus, bps,
                           clearances_blocks_interceptions, recoveries, tackles, defensive_contribution,
                           expected_goals, expected_assists, expected_goal_involvements, expected_goals_conceded,
                           influence, creativity, threat, ict_index, source
                    FROM manual_fixture_player_extra_stats
                    WHERE fixture_id = ? AND player_id = ?
                    """, (rs, index) -> new Saved(
                    rs.getInt("league_id"),
                    rs.getInt("season_year"),
                    rs.getInt("club_id"),
                    rs.getObject("starts", Integer.class),
                    rs.getObject("clean_sheets", Integer.class),
                    rs.getObject("goals_conceded", Integer.class),
                    rs.getObject("own_goals", Integer.class),
                    rs.getObject("penalties_saved", Integer.class),
                    rs.getObject("penalties_missed", Integer.class),
                    rs.getObject("saves", Integer.class),
                    rs.getObject("bonus", Integer.class),
                    rs.getObject("bps", Integer.class),
                    rs.getObject("clearances_blocks_interceptions", Integer.class),
                    rs.getObject("recoveries", Integer.class),
                    rs.getObject("tackles", Integer.class),
                    rs.getObject("defensive_contribution", Integer.class),
                    rs.getBigDecimal("expected_goals"),
                    rs.getBigDecimal("expected_assists"),
                    rs.getBigDecimal("expected_goal_involvements"),
                    rs.getBigDecimal("expected_goals_conceded"),
                    rs.getBigDecimal("influence"),
                    rs.getBigDecimal("creativity"),
                    rs.getBigDecimal("threat"),
                    rs.getBigDecimal("ict_index"),
                    rs.getString("source")
            ), row.fixtureId(), row.playerId());

            if (!existing.isEmpty() && !same(existing.getFirst(), row, clubId)) {
                throw invalid(row, "conflicting saved extra-stats row; existing data was not overwritten");
            }
            validated.add(new Validated(row, clubId, existing.isEmpty()));
        }

        int inserted = 0;
        for (var item : validated) {
            if (!item.insert()) continue;
            var r = item.row();
            jdbc.update("""
                    INSERT INTO manual_fixture_player_extra_stats
                    (fixture_id, player_id, league_id, season_year, club_id,
                     starts, clean_sheets, goals_conceded, own_goals, penalties_saved, penalties_missed,
                     saves, bonus, bps, clearances_blocks_interceptions, recoveries, tackles,
                     defensive_contribution, expected_goals, expected_assists,
                     expected_goal_involvements, expected_goals_conceded,
                     influence, creativity, threat, ict_index, source, fetched_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    r.fixtureId(), r.playerId(), LEAGUE_ID, SEASON_YEAR, item.clubId(),
                    r.starts(), r.cleanSheets(), r.goalsConceded(), r.ownGoals(),
                    r.penaltiesSaved(), r.penaltiesMissed(), r.saves(), r.bonus(), r.bps(),
                    r.clearancesBlocksInterceptions(), r.recoveries(), r.tackles(),
                    r.defensiveContribution(), r.expectedGoals(), r.expectedAssists(),
                    r.expectedGoalInvolvements(), r.expectedGoalsConceded(),
                    r.influence(), r.creativity(), r.threat(), r.ictIndex(),
                    r.source(), Timestamp.from(r.fetchedAt()));
            inserted++;
        }
        return new Result(rows.size(), inserted);
    }

    private static boolean same(Saved s, ManualExtraStatsCsvReader.Row r, int clubId) {
        return s.league() == LEAGUE_ID && s.season() == SEASON_YEAR && s.clubId() == clubId
                && Objects.equals(s.starts(), r.starts())
                && Objects.equals(s.cleanSheets(), r.cleanSheets())
                && Objects.equals(s.goalsConceded(), r.goalsConceded())
                && Objects.equals(s.ownGoals(), r.ownGoals())
                && Objects.equals(s.penaltiesSaved(), r.penaltiesSaved())
                && Objects.equals(s.penaltiesMissed(), r.penaltiesMissed())
                && Objects.equals(s.saves(), r.saves())
                && Objects.equals(s.bonus(), r.bonus())
                && Objects.equals(s.bps(), r.bps())
                && Objects.equals(s.clearancesBlocksInterceptions(), r.clearancesBlocksInterceptions())
                && Objects.equals(s.recoveries(), r.recoveries())
                && Objects.equals(s.tackles(), r.tackles())
                && Objects.equals(s.defensiveContribution(), r.defensiveContribution())
                && sameDecimal(s.expectedGoals(), r.expectedGoals())
                && sameDecimal(s.expectedAssists(), r.expectedAssists())
                && sameDecimal(s.expectedGoalInvolvements(), r.expectedGoalInvolvements())
                && sameDecimal(s.expectedGoalsConceded(), r.expectedGoalsConceded())
                && sameDecimal(s.influence(), r.influence())
                && sameDecimal(s.creativity(), r.creativity())
                && sameDecimal(s.threat(), r.threat())
                && sameDecimal(s.ictIndex(), r.ictIndex())
                && Objects.equals(s.source(), r.source());
    }

    private static boolean sameDecimal(BigDecimal saved, BigDecimal incoming) {
        return saved == null ? incoming == null : incoming != null && saved.compareTo(incoming) == 0;
    }

    private static IllegalArgumentException invalid(ManualExtraStatsCsvReader.Row row, String message) {
        return new IllegalArgumentException("CSV line " + row.line() + ": " + message
                + " (fixture_id=" + row.fixtureId() + ", player_id=" + row.playerId() + ")");
    }

    private record Fixture(int league, int season, int gameweek, int homeClubId, int awayClubId,
                           LocalDate date, String status) {
    }

    private record Saved(
            int league, int season, int clubId,
            Integer starts, Integer cleanSheets, Integer goalsConceded, Integer ownGoals,
            Integer penaltiesSaved, Integer penaltiesMissed, Integer saves, Integer bonus, Integer bps,
            Integer clearancesBlocksInterceptions, Integer recoveries, Integer tackles, Integer defensiveContribution,
            BigDecimal expectedGoals, BigDecimal expectedAssists,
            BigDecimal expectedGoalInvolvements, BigDecimal expectedGoalsConceded,
            BigDecimal influence, BigDecimal creativity, BigDecimal threat, BigDecimal ictIndex,
            String source
    ) {
    }

    private record Validated(ManualExtraStatsCsvReader.Row row, int clubId, boolean insert) {
    }

    public record Result(int rows, int inserted) {
    }
}
