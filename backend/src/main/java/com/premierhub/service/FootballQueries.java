package com.premierhub.service;

import com.premierhub.web.dto.ClubResponse;
import com.premierhub.web.dto.MatchResponse;
import com.premierhub.web.dto.MatchDetailResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import com.premierhub.web.dto.InferredMatchStatsResponse;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.dto.StandingResponse;
import com.premierhub.web.error.InvalidFilterException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FootballQueries {
    public static final int LEAGUE_ID = 39;
    public static final int DEFAULT_SEASON = 2024;
    private final JdbcTemplate jdbc;
    private final MatchScoringService scoring;
    private final FixtureEvidenceService evidence;

    public FootballQueries(JdbcTemplate jdbc, MatchScoringService scoring,
                           FixtureEvidenceService evidence) {
        this.jdbc = jdbc;
        this.scoring = scoring;
        this.evidence = evidence;
    }

    public List<ClubResponse> clubs(int season, String keyword) {
        validateSeason(season);
        String search = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        return jdbc.query("""
                SELECT c.id, c.name, COALESCE(c.city, '') AS city FROM clubs c
                JOIN season_clubs sc ON sc.club_id = c.id
                WHERE sc.league_id = ? AND sc.season_year = ? AND LOWER(c.name) LIKE ?
                ORDER BY c.name
                """, (rs, row) -> new ClubResponse(rs.getInt("id"), rs.getString("name"),
                rs.getString("city")), LEAGUE_ID, season, "%" + search + "%");
    }

    public Optional<ClubResponse> club(int id, int season) {
        return clubs(season, null).stream().filter(club -> club.id() == id).findFirst();
    }

    public List<PlayerResponse> players(int season, String club, String position) {
        validateSeason(season);
        String parsedPosition = filterEnum(position, "position", "GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD");
        return jdbc.query("""
                SELECT p.id, p.name, c.id AS club_id, c.name AS club_name, ps.position,
                       ps.goals, ps.assists FROM player_season_stats ps
                JOIN players p ON p.id = ps.player_id JOIN clubs c ON c.id = ps.club_id
                WHERE ps.league_id = ? AND ps.season_year = ?
                  AND (? IS NULL OR LOWER(c.name) = LOWER(?))
                  AND (? IS NULL OR ps.position = ?)
                ORDER BY p.name, c.name
                """, (rs, row) -> new PlayerResponse(rs.getInt("id"), rs.getString("name"),
                rs.getInt("club_id"), rs.getString("club_name"), rs.getString("position"),
                rs.getInt("goals"), rs.getInt("assists")), LEAGUE_ID, season,
                blankToNull(club), blankToNull(club), parsedPosition, parsedPosition);
    }

    public Optional<PlayerResponse> player(int id, int season) {
        return players(season, null, null).stream().filter(player -> player.id() == id).findFirst();
    }

    public List<MatchResponse> matches(int season, String club, Integer matchweek, String status) {
        validateSeason(season);
        String parsedStatus = filterEnum(status, "status", "SCHEDULED", "FINISHED", "POSTPONED",
                "CANCELLED", "SUSPENDED", "LIVE", "AWARDED");
        return jdbc.query("""
                SELECT f.id, f.home_club_id, home.name AS home_name, f.away_club_id,
                       away.name AS away_name, f.gameweek, f.match_date, f.status,
                       f.home_goals, f.away_goals FROM fixtures f
                JOIN clubs home ON home.id = f.home_club_id
                JOIN clubs away ON away.id = f.away_club_id
                WHERE f.league_id = ? AND f.season_year = ?
                  AND (? IS NULL OR LOWER(home.name) = LOWER(?) OR LOWER(away.name) = LOWER(?))
                  AND (? IS NULL OR f.gameweek = ?) AND (? IS NULL OR f.status = ?)
                ORDER BY f.match_date, f.id
                """, (rs, row) -> match(rs), LEAGUE_ID, season,
                blankToNull(club), blankToNull(club), blankToNull(club),
                matchweek, matchweek, parsedStatus, parsedStatus);
    }

    public Optional<MatchResponse> match(int id, int season) {
        return matches(season, null, null, null).stream().filter(match -> match.id() == id).findFirst();
    }

    public Optional<MatchDetailResponse> matchDetail(int id, int season) {
        return match(id, season).map(match -> {
            List<MatchPlayerStatResponse> rawPlayers = jdbc.query("""
                    SELECT s.player_id, p.name AS player_name, s.club_id, s.position,
                           s.minutes, s.goals, s.assists, s.yellow_cards, s.red_cards,
                           s.rating, s.shots_on, s.passes_key, s.tackles, s.saves
                    FROM fixture_player_stats s JOIN players p ON p.id = s.player_id
                    WHERE s.fixture_id = ? AND s.club_id IN (?, ?)
                    ORDER BY s.club_id, p.name, s.player_id
                    """, (rs, row) -> {
                String position = rs.getString("position");
                Integer minutes = (Integer) rs.getObject("minutes");
                Integer goals = (Integer) rs.getObject("goals");
                Integer assists = (Integer) rs.getObject("assists");
                Integer yellowCards = (Integer) rs.getObject("yellow_cards");
                Integer redCards = (Integer) rs.getObject("red_cards");
                return new MatchPlayerStatResponse(
                        rs.getInt("player_id"), rs.getString("player_name"), rs.getInt("club_id"),
                        position, minutes, goals, assists, yellowCards, redCards,
                        rs.getString("rating"), (Integer) rs.getObject("shots_on"),
                        (Integer) rs.getObject("passes_key"), (Integer) rs.getObject("tackles"),
                        (Integer) rs.getObject("saves"),
                        scoring.score(position, minutes, goals, assists, yellowCards, redCards), null);
            }, id, match.homeClubId(), match.awayClubId());
            FixtureEvidenceService.Review review = evidence.review(season, match, rawPlayers);
            List<MatchPlayerStatResponse> players = rawPlayers.stream().map(player -> {
                InferredMatchStatsResponse inferred = review.inferred().get(player.playerId());
                if (inferred == null) return player;
                Integer minutes = player.minutes() == null ? inferred.minutes() : player.minutes();
                Integer goals = player.goals() == null ? inferred.goals() : player.goals();
                Integer assists = player.assists() == null ? inferred.assists() : player.assists();
                return new MatchPlayerStatResponse(player.playerId(), player.playerName(),
                        player.clubId(), player.position(), player.minutes(), player.goals(),
                        player.assists(), player.yellowCards(), player.redCards(), player.rating(),
                        player.shotsOn(), player.passesKey(), player.tackles(), player.saves(),
                        scoring.score(player.position(), minutes, goals, assists,
                                player.yellowCards(), player.redCards()), inferred);
            }).toList();
            return new MatchDetailResponse(match,
                    players.stream().filter(player -> player.clubId() == match.homeClubId()).toList(),
                    players.stream().filter(player -> player.clubId() == match.awayClubId()).toList(),
                    review.status(), review.error());
        });
    }

    public List<StandingResponse> standings(int season, Integer limit) {
        validateSeason(season);
        List<StandingResponse> rows = jdbc.query("""
                SELECT s.position, s.club_id, c.name AS club_name, s.played, s.won, s.drawn,
                       s.lost, s.goals_for, s.goals_against, s.goal_difference, s.points
                FROM standings s JOIN clubs c ON c.id = s.club_id
                WHERE s.league_id = ? AND s.season_year = ? ORDER BY s.position
                """, (rs, row) -> new StandingResponse(rs.getInt("position"), rs.getInt("club_id"),
                rs.getString("club_name"), rs.getInt("played"), rs.getInt("won"),
                rs.getInt("drawn"), rs.getInt("lost"), rs.getInt("goals_for"),
                rs.getInt("goals_against"), rs.getInt("goal_difference"), rs.getInt("points")),
                LEAGUE_ID, season);
        return limit == null ? rows : rows.subList(0, Math.min(limit, rows.size()));
    }

    public Optional<StandingResponse> standing(int clubId, int season) {
        return standings(season, null).stream().filter(row -> row.clubId() == clubId).findFirst();
    }

    private static MatchResponse match(ResultSet rs) throws SQLException {
        return new MatchResponse(rs.getInt("id"), rs.getInt("home_club_id"),
                rs.getString("home_name"), rs.getInt("away_club_id"), rs.getString("away_name"),
                rs.getInt("gameweek"), rs.getDate("match_date").toLocalDate(),
                rs.getString("status"), (Integer) rs.getObject("home_goals"),
                (Integer) rs.getObject("away_goals"));
    }

    private static String blankToNull(String value) {
        return value == null ? null : value.strip();
    }

    private static String filterEnum(String value, String field, String... allowed) {
        if (value == null) return null;
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!Arrays.asList(allowed).contains(normalized)) {
            throw new InvalidFilterException("Invalid " + field + ": " + value);
        }
        return normalized;
    }

    public static void validateSeason(int season) {
        if (season < 2000 || season > 2100) {
            throw new IllegalArgumentException("Invalid season: " + season);
        }
    }
}
