package com.premierhub.service;

import com.premierhub.web.dto.ClubSeasonTrendsResponse;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.AssisterEntry;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.MatchTrend;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.RankPoint;
import com.premierhub.web.dto.ClubSeasonTrendsResponse.ScorerEntry;
import com.premierhub.web.error.InvalidFilterException;
import com.premierhub.web.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClubSeasonTrendsService {
    private static final Logger log = LoggerFactory.getLogger(ClubSeasonTrendsService.class);
    private static final int LEAGUE_ID = FootballQueries.LEAGUE_ID;
    private static final int SUPPORTED_SEASON = 2026;

    private final JdbcTemplate jdbc;

    public ClubSeasonTrendsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ClubSeasonTrendsResponse trends(int clubId, int season) {
        if (season != SUPPORTED_SEASON) {
            throw new InvalidFilterException("Club season trends are available for season 2026 only");
        }

        Map<Integer, String> seasonClubs = loadSeasonClubs();
        if (!seasonClubs.containsKey(clubId)) {
            throw new ResourceNotFoundException("Club not found: " + clubId);
        }

        List<FinishedFixture> allFinished = loadFinishedFixtures();
        Map<Integer, Double> ratingsByFixture = loadTeamRatings(clubId);
        Map<FixtureClubKey, Double> xgByFixtureClub = loadExpectedGoals(clubId);

        List<MatchTrend> matches = allFinished.stream()
                .filter(f -> f.homeClubId() == clubId || f.awayClubId() == clubId)
                .sorted(Comparator
                        .comparing(FinishedFixture::sortInstant)
                        .thenComparing(FinishedFixture::matchDate)
                        .thenComparingInt(FinishedFixture::gameweek)
                        .thenComparingInt(FinishedFixture::fixtureId))
                .map(f -> toMatchTrend(f, clubId, ratingsByFixture, xgByFixtureClub))
                .toList();

        List<RankPoint> rankHistory = buildRankHistory(clubId, seasonClubs, allFinished);

        List<PlayerTotals> playerTotals = loadPlayerContributions(clubId);
        List<ScorerEntry> scorers = playerTotals.stream()
                .filter(p -> p.goals() > 0)
                .sorted(Comparator
                        .comparingInt(PlayerTotals::goals).reversed()
                        .thenComparing(PlayerTotals::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PlayerTotals::name))
                .map(p -> new ScorerEntry(p.playerId(), p.name(), p.goals()))
                .toList();

        List<AssisterEntry> assisters = playerTotals.stream()
                .filter(p -> p.assists() > 0)
                .sorted(Comparator
                        .comparingInt(PlayerTotals::assists).reversed()
                        .thenComparing(PlayerTotals::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PlayerTotals::name))
                .map(p -> new AssisterEntry(p.playerId(), p.name(), p.assists()))
                .toList();

        int totalGoalsFor = matches.stream().mapToInt(MatchTrend::goalsFor).sum();
        int totalScorerGoals = scorers.stream().mapToInt(ScorerEntry::goals).sum();
        int rawOwnGoals = totalGoalsFor - totalScorerGoals;
        int ownGoalsFor = rawOwnGoals;
        if (rawOwnGoals < 0) {
            List<Integer> suspectFixtures = findFixturesWithExcessPlayerGoals(clubId, matches);
            log.warn("Negative ownGoalsFor ({}) computed for clubId={} season={} (goalsFor={}, playerGoals={}, suspectFixtures={}); clamping to 0",
                    rawOwnGoals, clubId, season, totalGoalsFor, totalScorerGoals, suspectFixtures);
            ownGoalsFor = 0;
        }

        return new ClubSeasonTrendsResponse(clubId, season, matches, rankHistory, scorers, assisters, ownGoalsFor);
    }

    private Map<Integer, String> loadSeasonClubs() {
        Map<Integer, String> clubs = new LinkedHashMap<>();
        jdbc.query("""
                SELECT c.id, c.name
                FROM season_clubs sc
                JOIN clubs c ON c.id = sc.club_id
                WHERE sc.league_id = ? AND sc.season_year = ?
                ORDER BY c.id
                """, rs -> {
            clubs.put(rs.getInt("id"), rs.getString("name"));
        }, LEAGUE_ID, SUPPORTED_SEASON);
        return clubs;
    }

    private List<FinishedFixture> loadFinishedFixtures() {
        return jdbc.query("""
                SELECT f.id, f.gameweek, f.match_date, f.home_club_id, home.name AS home_name,
                       f.away_club_id, away.name AS away_name, f.home_goals, f.away_goals,
                       fd.kickoff_utc
                FROM fixtures f
                JOIN clubs home ON home.id = f.home_club_id
                JOIN clubs away ON away.id = f.away_club_id
                LEFT JOIN football_data_fixtures fd ON fd.fixture_id = f.id
                WHERE f.league_id = ? AND f.season_year = ?
                  AND f.status = 'FINISHED'
                  AND f.home_goals IS NOT NULL AND f.away_goals IS NOT NULL
                ORDER BY f.gameweek, f.match_date, f.id
                """, (rs, row) -> new FinishedFixture(
                rs.getInt("id"),
                rs.getInt("gameweek"),
                toLocalDate(rs.getDate("match_date")),
                parseKickoff(rs.getString("kickoff_utc")),
                rs.getInt("home_club_id"),
                rs.getString("home_name"),
                rs.getInt("away_club_id"),
                rs.getString("away_name"),
                rs.getInt("home_goals"),
                rs.getInt("away_goals")
        ), LEAGUE_ID, SUPPORTED_SEASON);
    }

    private Map<Integer, Double> loadTeamRatings(int clubId) {
        Map<Integer, Double> ratings = new HashMap<>();
        jdbc.query("""
                SELECT s.fixture_id, SUM(s.rating) AS rating_sum, COUNT(s.rating) AS rating_count
                FROM manual_fixture_player_stats s
                JOIN fixtures f ON f.id = s.fixture_id
                    AND f.league_id = s.league_id AND f.season_year = s.season_year
                WHERE s.league_id = ? AND s.season_year = ?
                  AND s.club_id = ?
                  AND f.status = 'FINISHED'
                  AND s.club_id IN (f.home_club_id, f.away_club_id)
                  AND s.participation_status = 'PLAYED'
                  AND s.rating IS NOT NULL
                GROUP BY s.fixture_id
                """, rs -> {
            int count = rs.getInt("rating_count");
            BigDecimal sum = rs.getBigDecimal("rating_sum");
            if (count > 0 && sum != null) {
                double avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue();
                ratings.put(rs.getInt("fixture_id"), avg);
            }
        }, LEAGUE_ID, SUPPORTED_SEASON, clubId);
        return ratings;
    }

    private Map<FixtureClubKey, Double> loadExpectedGoals(int clubId) {
        Map<FixtureClubKey, Double> xgMap = new HashMap<>();
        jdbc.query("""
                SELECT e.fixture_id, e.club_id,
                       SUM(e.expected_goals) AS xg_sum,
                       COUNT(e.expected_goals) AS xg_count
                FROM manual_fixture_player_extra_stats e
                JOIN manual_fixture_player_stats s
                    ON s.fixture_id = e.fixture_id AND s.player_id = e.player_id
                    AND s.league_id = e.league_id AND s.season_year = e.season_year
                JOIN fixtures f
                    ON f.id = e.fixture_id AND f.league_id = e.league_id AND f.season_year = e.season_year
                WHERE e.league_id = ? AND e.season_year = ?
                  AND f.status = 'FINISHED'
                  AND (f.home_club_id = ? OR f.away_club_id = ?)
                  AND e.club_id = s.club_id
                  AND e.club_id IN (f.home_club_id, f.away_club_id)
                  AND s.participation_status = 'PLAYED'
                GROUP BY e.fixture_id, e.club_id
                """, rs -> {
            int count = rs.getInt("xg_count");
            BigDecimal sum = rs.getBigDecimal("xg_sum");
            if (count > 0 && sum != null) {
                double rounded = sum.setScale(2, RoundingMode.HALF_UP).doubleValue();
                xgMap.put(new FixtureClubKey(rs.getInt("fixture_id"), rs.getInt("club_id")), rounded);
            }
        }, LEAGUE_ID, SUPPORTED_SEASON, clubId, clubId);
        return xgMap;
    }

    private List<PlayerTotals> loadPlayerContributions(int clubId) {
        return jdbc.query("""
                SELECT s.player_id, p.name,
                       COALESCE(SUM(s.goals), 0) AS goals,
                       COALESCE(SUM(s.assists), 0) AS assists
                FROM manual_fixture_player_stats s
                JOIN fixtures f ON f.id = s.fixture_id
                    AND f.league_id = s.league_id AND f.season_year = s.season_year
                JOIN players p ON p.id = s.player_id
                WHERE s.league_id = ? AND s.season_year = ?
                  AND s.club_id = ?
                  AND f.status = 'FINISHED'
                  AND s.club_id IN (f.home_club_id, f.away_club_id)
                  AND s.participation_status = 'PLAYED'
                GROUP BY s.player_id, p.name
                """, (rs, row) -> new PlayerTotals(
                rs.getInt("player_id"),
                rs.getString("name"),
                rs.getInt("goals"),
                rs.getInt("assists")
        ), LEAGUE_ID, SUPPORTED_SEASON, clubId);
    }

    private List<Integer> findFixturesWithExcessPlayerGoals(int clubId, List<MatchTrend> matches) {
        Map<Integer, Integer> playerGoalsByFixture = new HashMap<>();
        jdbc.query("""
                SELECT s.fixture_id, COALESCE(SUM(s.goals), 0) AS goals
                FROM manual_fixture_player_stats s
                JOIN fixtures f ON f.id = s.fixture_id
                    AND f.league_id = s.league_id AND f.season_year = s.season_year
                WHERE s.league_id = ? AND s.season_year = ?
                  AND s.club_id = ?
                  AND f.status = 'FINISHED'
                  AND s.club_id IN (f.home_club_id, f.away_club_id)
                  AND s.participation_status = 'PLAYED'
                GROUP BY s.fixture_id
                """, rs -> {
            playerGoalsByFixture.put(rs.getInt("fixture_id"), rs.getInt("goals"));
        }, LEAGUE_ID, SUPPORTED_SEASON, clubId);

        List<Integer> suspect = new ArrayList<>();
        for (MatchTrend match : matches) {
            int scored = playerGoalsByFixture.getOrDefault(match.fixtureId(), 0);
            if (scored > match.goalsFor()) {
                suspect.add(match.fixtureId());
            }
        }
        return suspect;
    }

    private static MatchTrend toMatchTrend(FinishedFixture fixture, int clubId,
                                           Map<Integer, Double> ratingsByFixture,
                                           Map<FixtureClubKey, Double> xgByFixtureClub) {
        boolean home = fixture.homeClubId() == clubId;
        int opponentClubId = home ? fixture.awayClubId() : fixture.homeClubId();
        String opponentName = home ? fixture.awayName() : fixture.homeName();
        int goalsFor = home ? fixture.homeGoals() : fixture.awayGoals();
        int goalsAgainst = home ? fixture.awayGoals() : fixture.homeGoals();
        Double teamRating = ratingsByFixture.get(fixture.fixtureId());
        Double xgFor = xgByFixtureClub.get(new FixtureClubKey(fixture.fixtureId(), clubId));
        Double xgAgainst = xgByFixtureClub.get(new FixtureClubKey(fixture.fixtureId(), opponentClubId));
        return new MatchTrend(
                fixture.fixtureId(),
                fixture.gameweek(),
                fixture.kickoffUtc(),
                home,
                opponentClubId,
                opponentName,
                goalsFor,
                goalsAgainst,
                teamRating,
                xgFor,
                xgAgainst
        );
    }

    private static List<RankPoint> buildRankHistory(int clubId, Map<Integer, String> seasonClubs,
                                                    List<FinishedFixture> allFinished) {
        if (allFinished.isEmpty()) {
            return List.of();
        }
        int maxGw = allFinished.stream().mapToInt(FinishedFixture::gameweek).max().orElse(0);
        if (maxGw <= 0) {
            return List.of();
        }

        Map<Integer, TableAccumulator> table = new LinkedHashMap<>();
        seasonClubs.forEach((id, name) -> table.put(id, new TableAccumulator(id, name)));
        for (FinishedFixture f : allFinished) {
            table.putIfAbsent(f.homeClubId(), new TableAccumulator(f.homeClubId(), f.homeName()));
            table.putIfAbsent(f.awayClubId(), new TableAccumulator(f.awayClubId(), f.awayName()));
        }

        Map<Integer, List<FinishedFixture>> fixturesByGw = allFinished.stream()
                .collect(Collectors.groupingBy(FinishedFixture::gameweek));

        Comparator<TableAccumulator> standingOrder = Comparator
                .comparingInt(TableAccumulator::points).reversed()
                .thenComparing(Comparator.comparingInt(TableAccumulator::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt(TableAccumulator::goalsFor).reversed())
                .thenComparing(TableAccumulator::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TableAccumulator::name);

        List<RankPoint> history = new ArrayList<>(maxGw);
        for (int gw = 1; gw <= maxGw; gw++) {
            for (FinishedFixture f : fixturesByGw.getOrDefault(gw, List.of())) {
                TableAccumulator home = table.get(f.homeClubId());
                TableAccumulator away = table.get(f.awayClubId());
                home.record(f.homeGoals(), f.awayGoals());
                away.record(f.awayGoals(), f.homeGoals());
            }
            List<TableAccumulator> sorted = new ArrayList<>(table.values());
            sorted.sort(standingOrder);
            int position = 0;
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).clubId == clubId) {
                    position = i + 1;
                    break;
                }
            }
            history.add(new RankPoint(gw, position));
        }
        return history;
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? LocalDate.MIN : date.toLocalDate();
    }

    private static Instant parseKickoff(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Instant.parse(raw.trim());
    }

    private record FinishedFixture(
            int fixtureId,
            int gameweek,
            LocalDate matchDate,
            Instant kickoffUtc,
            int homeClubId,
            String homeName,
            int awayClubId,
            String awayName,
            int homeGoals,
            int awayGoals
    ) {
        Instant sortInstant() {
            return kickoffUtc != null ? kickoffUtc : matchDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }

    private record FixtureClubKey(int fixtureId, int clubId) { }

    private record PlayerTotals(int playerId, String name, int goals, int assists) { }

    private static final class TableAccumulator {
        private final int clubId;
        private final String name;
        private int points;
        private int goalsFor;
        private int goalsAgainst;

        private TableAccumulator(int clubId, String name) {
            this.clubId = clubId;
            this.name = name;
        }

        private void record(int gf, int ga) {
            goalsFor += gf;
            goalsAgainst += ga;
            if (gf > ga) {
                points += 3;
            } else if (gf == ga) {
                points += 1;
            }
        }

        private int points() {
            return points;
        }

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        private int goalsFor() {
            return goalsFor;
        }

        private String name() {
            return name;
        }
    }
}
