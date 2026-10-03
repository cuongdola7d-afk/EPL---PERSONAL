package com.premierhub.manualstats;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Rebuilds 2026/27 season totals from complete, saved match statistics. */
@Service
public class ManualSeasonStatsService {
    private static final int LEAGUE = 39;
    private static final int SEASON = 2026;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public ManualSeasonStatsService(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Result rebuildThroughGameweek(int gameweek) {
        if (gameweek < 1 || gameweek > 38) {
            throw new IllegalArgumentException("gameweek must be between 1 and 38");
        }
        return transactions.execute(status -> rebuild(gameweek));
    }

    public Result rebuildLatestComplete() {
        int latest = 0;
        for (int gameweek = 1; gameweek <= 38; gameweek++) {
            Integer finished = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM fixtures
                    WHERE league_id=? AND season_year=? AND gameweek=? AND status='FINISHED'
                    """, Integer.class, LEAGUE, SEASON, gameweek);
            Integer fixtures = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM fixtures
                    WHERE league_id=? AND season_year=? AND gameweek=?
                    """, Integer.class, LEAGUE, SEASON, gameweek);
            Integer rows = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM manual_fixture_player_stats stats
                    JOIN fixtures fixture ON fixture.id=stats.fixture_id
                    WHERE fixture.league_id=? AND fixture.season_year=? AND fixture.gameweek=?
                      AND stats.league_id=? AND stats.season_year=?
                    """, Integer.class, LEAGUE, SEASON, gameweek, LEAGUE, SEASON);
            if (fixtures == null || fixtures != 10 || finished == null || finished != 10
                    || rows == null || rows != 400) break;
            latest = gameweek;
        }
        return latest == 0 ? new Result(0, 0, 0, 0, 0, 0, 0)
                : rebuildThroughGameweek(latest);
    }

    private Result rebuild(int gameweek) {
        List<Fixture> fixtures = jdbc.query("""
                SELECT id, gameweek, home_club_id, away_club_id, status
                FROM fixtures WHERE league_id=? AND season_year=? AND gameweek<=?
                ORDER BY gameweek, id
                """, (rs, index) -> new Fixture(rs.getInt("id"), rs.getInt("gameweek"),
                rs.getInt("home_club_id"), rs.getInt("away_club_id"), rs.getString("status")),
                LEAGUE, SEASON, gameweek);
        if (fixtures.size() != gameweek * 10) {
            throw new IllegalStateException("Expected exactly 10 fixtures per gameweek through GW" + gameweek);
        }
        Map<Integer, Fixture> byFixture = new HashMap<>();
        int[] weeklyCounts = new int[gameweek + 1];
        for (Fixture fixture : fixtures) {
            if (fixture.gameweek() < 1 || fixture.gameweek() > gameweek
                    || ++weeklyCounts[fixture.gameweek()] > 10 || !"FINISHED".equals(fixture.status())
                    || byFixture.putIfAbsent(fixture.id(), fixture) != null) {
                throw new IllegalStateException("Incomplete or duplicated fixture in GW" + fixture.gameweek());
            }
        }
        for (int week = 1; week <= gameweek; week++) {
            if (weeklyCounts[week] != 10) throw new IllegalStateException("GW" + week + " does not have 10 fixtures");
        }

        List<Stat> stats = jdbc.query("""
                SELECT s.fixture_id, s.player_id, s.club_id, s.participation_status,
                       s.minutes, s.goals, s.assists
                FROM manual_fixture_player_stats s
                JOIN fixtures f ON f.id=s.fixture_id AND f.league_id=s.league_id
                    AND f.season_year=s.season_year
                JOIN player_season_stats ps ON ps.league_id=s.league_id
                    AND ps.season_year=s.season_year AND ps.player_id=s.player_id
                    AND ps.club_id=s.club_id
                JOIN manual_player_memberships membership ON membership.league_id=s.league_id
                    AND membership.season_year=s.season_year AND membership.player_id=s.player_id
                    AND membership.club_id=s.club_id AND membership.start_date<=f.match_date
                    AND (membership.end_date IS NULL OR membership.end_date>f.match_date)
                WHERE s.league_id=? AND s.season_year=? AND f.gameweek<=?
                """, (rs, index) -> new Stat(rs.getInt("fixture_id"), rs.getInt("player_id"),
                rs.getInt("club_id"), rs.getString("participation_status"),
                rs.getObject("minutes", Integer.class), rs.getObject("goals", Integer.class),
                rs.getObject("assists", Integer.class)), LEAGUE, SEASON, gameweek);
        if (stats.size() != fixtures.size() * 40) {
            throw new IllegalStateException("Expected 40 valid player-match rows per fixture through GW" + gameweek);
        }
        Map<Integer, int[]> counts = new HashMap<>();
        Map<Integer, Set<Integer>> seenPlayers = new HashMap<>();
        Map<Key, Totals> totals = new HashMap<>();
        for (Stat stat : stats) {
            Fixture fixture = byFixture.get(stat.fixtureId());
            if (fixture == null) throw new IllegalStateException("Unexpected fixture " + stat.fixtureId());
            if (!seenPlayers.computeIfAbsent(fixture.id(), unused -> new HashSet<>())
                    .add(stat.playerId())) {
                throw new IllegalStateException("Duplicate membership for player " + stat.playerId());
            }
            int side = stat.clubId() == fixture.homeClubId() ? 0
                    : stat.clubId() == fixture.awayClubId() ? 1 : -1;
            if (side < 0) throw new IllegalStateException("Player belongs to another club in fixture " + fixture.id());
            counts.computeIfAbsent(fixture.id(), unused -> new int[2])[side]++;
            totals.computeIfAbsent(new Key(stat.playerId(), stat.clubId()), unused -> new Totals())
                    .add(stat);
        }
        for (Fixture fixture : fixtures) {
            int[] sides = counts.get(fixture.id());
            if (sides == null || sides[0] != 20 || sides[1] != 20) {
                throw new IllegalStateException("Fixture " + fixture.id() + " needs 20 players per club");
            }
        }

        List<SeasonRow> seasonRows = jdbc.query("""
                SELECT player_id, club_id, appearances, minutes, goals, assists
                FROM player_season_stats WHERE league_id=? AND season_year=?
                """, (rs, index) -> new SeasonRow(rs.getInt("player_id"), rs.getInt("club_id"),
                rs.getObject("appearances", Integer.class), rs.getObject("minutes", Integer.class),
                rs.getObject("goals", Integer.class), rs.getObject("assists", Integer.class)),
                LEAGUE, SEASON);
        List<Update> changes = new ArrayList<>();
        int missingMinutes = 0;
        int missingGoals = 0;
        int missingAssists = 0;
        for (SeasonRow row : seasonRows) {
            Totals total = totals.getOrDefault(new Key(row.playerId(), row.clubId()), new Totals());
            Integer minutes = total.missingMinutes ? null : total.minutes;
            Integer goals = total.missingGoals ? null : total.goals;
            Integer assists = total.missingAssists ? null : total.assists;
            if (minutes == null) missingMinutes++;
            if (goals == null) missingGoals++;
            if (assists == null) missingAssists++;
            if (!Objects.equals(row.appearances(), total.appearances)
                    || !Objects.equals(row.minutes(), minutes)
                    || !Objects.equals(row.goals(), goals)
                    || !Objects.equals(row.assists(), assists)) {
                changes.add(new Update(row.playerId(), row.clubId(), total.appearances,
                        minutes, goals, assists));
            }
        }
        for (Update change : changes) {
            int updated = jdbc.update("""
                    UPDATE player_season_stats SET appearances=?, minutes=?, goals=?, assists=?
                    WHERE league_id=? AND season_year=? AND player_id=? AND club_id=?
                    """, change.appearances(), change.minutes(), change.goals(), change.assists(),
                    LEAGUE, SEASON, change.playerId(), change.clubId());
            if (updated != 1) throw new IllegalStateException("Season row disappeared during rebuild");
        }
        return new Result(fixtures.size(), stats.size(), seasonRows.size(), changes.size(),
                missingMinutes, missingGoals, missingAssists);
    }

    private record Key(int playerId, int clubId) { }
    private record Fixture(int id, int gameweek, int homeClubId, int awayClubId, String status) { }
    private record Stat(int fixtureId, int playerId, int clubId, String status,
                        Integer minutes, Integer goals, Integer assists) { }
    private record SeasonRow(int playerId, int clubId, Integer appearances,
                             Integer minutes, Integer goals, Integer assists) { }
    private record Update(int playerId, int clubId, int appearances,
                          Integer minutes, Integer goals, Integer assists) { }

    private static final class Totals {
        private int appearances;
        private int minutes;
        private int goals;
        private int assists;
        private boolean missingMinutes;
        private boolean missingGoals;
        private boolean missingAssists;

        private void add(Stat stat) {
            if ("DID_NOT_PLAY".equals(stat.status())) return;
            if (!"PLAYED".equals(stat.status())) {
                throw new IllegalStateException("Unknown participation status for player " + stat.playerId());
            }
            appearances++;
            if (stat.minutes() == null) missingMinutes = true; else minutes += stat.minutes();
            if (stat.goals() == null) missingGoals = true; else goals += stat.goals();
            if (stat.assists() == null) missingAssists = true; else assists += stat.assists();
        }
    }

    public record Result(int fixtures, int matchRows, int seasonRows, int updatedRows,
                         int missingMinutes, int missingGoals, int missingAssists) { }
}
