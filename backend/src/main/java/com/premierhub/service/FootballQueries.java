package com.premierhub.service;

import com.premierhub.web.dto.ClubResponse;
import com.premierhub.web.dto.MatchResponse;
import com.premierhub.web.dto.MatchDetailResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import com.premierhub.web.dto.InferredMatchStatsResponse;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.dto.PlayerMatchResponse;
import com.premierhub.web.dto.StandingResponse;
import com.premierhub.web.error.InvalidFilterException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

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
        return players(season, club, position, null);
    }

    public List<PlayerResponse> players(int season, String club, String position, LocalDate asOf) {
        validateSeason(season);
        String parsedPosition = filterEnum(position, "position", "GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD");
        if (season == 2026) {
            LocalDate effectiveDate = asOf == null ? LocalDate.now(ZoneOffset.UTC) : asOf;
            return jdbc.query("""
                    SELECT p.id, p.name, c.id AS club_id, c.name AS club_name, ps.position,
                           ps.goals, ps.assists, profile.fc27_overall, profile.nationality,
                           profile.birth_date, profile.height_cm, profile.preferred_foot,
                           profile.shirt_number
                    FROM manual_player_memberships membership
                    JOIN player_season_stats ps ON ps.league_id=membership.league_id
                        AND ps.season_year=membership.season_year AND ps.player_id=membership.player_id
                        AND ps.club_id=membership.club_id
                    JOIN players p ON p.id=membership.player_id
                    JOIN clubs c ON c.id=membership.club_id
                    LEFT JOIN player_season_profiles profile ON profile.league_id=membership.league_id
                        AND profile.season_year=membership.season_year
                        AND profile.player_id=membership.player_id AND profile.club_id=membership.club_id
                    WHERE membership.league_id=? AND membership.season_year=?
                      AND membership.start_date<=?
                      AND (membership.end_date IS NULL OR membership.end_date>?)
                      AND (? IS NULL OR LOWER(c.name)=LOWER(?))
                      AND (? IS NULL OR ps.position=?)
                    ORDER BY p.name, c.name
                    """, (rs, row) -> playerResponse(rs), LEAGUE_ID, season,
                    Date.valueOf(effectiveDate), Date.valueOf(effectiveDate),
                    blankToNull(club), blankToNull(club), parsedPosition, parsedPosition);
        }
        return jdbc.query("""
                SELECT p.id, p.name, c.id AS club_id, c.name AS club_name, ps.position,
                       ps.goals, ps.assists, NULL AS fc27_overall, NULL AS nationality,
                       NULL AS birth_date, NULL AS height_cm, NULL AS preferred_foot,
                       NULL AS shirt_number FROM player_season_stats ps
                JOIN players p ON p.id = ps.player_id JOIN clubs c ON c.id = ps.club_id
                WHERE ps.league_id = ? AND ps.season_year = ?
                  AND (? IS NULL OR LOWER(c.name) = LOWER(?))
                  AND (? IS NULL OR ps.position = ?)
                ORDER BY p.name, c.name
                """, (rs, row) -> playerResponse(rs), LEAGUE_ID, season,
                blankToNull(club), blankToNull(club), parsedPosition, parsedPosition);
    }

    public Optional<PlayerResponse> player(int id, int season) {
        return player(id, season, null);
    }

    public Optional<PlayerResponse> player(int id, int season, LocalDate asOf) {
        return players(season, null, null, asOf).stream().filter(player -> player.id() == id).findFirst();
    }

    public boolean hasPlayerInSeason(int id, int season) {
        validateSeason(season);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM player_season_stats
                WHERE league_id=? AND season_year=? AND player_id=?
                """, Integer.class, LEAGUE_ID, season, id);
        return count != null && count > 0;
    }

    public List<PlayerMatchResponse> playerMatches(int id, int season) {
        validateSeason(season);
        Map<Integer, MatchPlayerStatResponse> manualStats = new HashMap<>();
        if (season == 2026) {
            jdbc.query("""
                    SELECT m.fixture_id, m.player_id, m.club_id, m.minutes, m.goals, m.assists,
                           m.yellow_cards, m.red_cards, m.rating, m.fantasy_points,
                           m.participation_status, p.name AS player_name, ps.position
                    FROM manual_fixture_player_stats m
                    JOIN fixtures f ON f.id=m.fixture_id AND f.league_id=m.league_id
                      AND f.season_year=m.season_year
                    JOIN manual_player_memberships membership ON membership.league_id=m.league_id
                      AND membership.season_year=m.season_year AND membership.player_id=m.player_id
                      AND membership.club_id=m.club_id AND membership.start_date<=f.match_date
                      AND (membership.end_date IS NULL OR membership.end_date>f.match_date)
                    JOIN players p ON p.id=m.player_id
                    JOIN player_season_stats ps ON ps.league_id=m.league_id
                      AND ps.season_year=m.season_year AND ps.player_id=m.player_id
                      AND ps.club_id=m.club_id
                    WHERE m.league_id=? AND m.season_year=? AND m.player_id=?
                      AND m.club_id IN (f.home_club_id, f.away_club_id)
                    """, (RowCallbackHandler) rs -> manualStats.put(rs.getInt("fixture_id"),
                    manualPlayerStat(rs)), LEAGUE_ID, season, id);
        }
        Set<Integer> fixtureIdsWithStats = Set.copyOf(jdbc.queryForList("""
                SELECT s.fixture_id FROM fixture_player_stats s
                JOIN fixtures f ON f.id = s.fixture_id
                WHERE s.player_id = ? AND f.league_id = ? AND f.season_year = ?
                """, Integer.class, id, LEAGUE_ID, season));
        Map<Integer, Integer> fixtureClubs = new HashMap<>();
        if (season == 2026) {
            jdbc.query("""
                    SELECT f.id, m.club_id FROM fixtures f
                    JOIN manual_player_memberships m ON m.league_id=f.league_id
                      AND m.season_year=f.season_year AND m.club_id IN (f.home_club_id, f.away_club_id)
                    WHERE m.player_id=? AND f.league_id=? AND f.season_year=?
                      AND m.start_date<=f.match_date
                      AND (m.end_date IS NULL OR m.end_date>f.match_date)
                    """, (RowCallbackHandler) rs -> fixtureClubs.put(rs.getInt("id"), rs.getInt("club_id")),
                    id, LEAGUE_ID, season);
        } else {
            jdbc.query("""
                    SELECT f.id, ps.club_id FROM fixtures f
                    JOIN player_season_stats ps ON ps.league_id=f.league_id
                      AND ps.season_year=f.season_year AND ps.club_id IN (f.home_club_id, f.away_club_id)
                    WHERE ps.player_id=? AND f.league_id=? AND f.season_year=?
                    """, (RowCallbackHandler) rs -> fixtureClubs.put(rs.getInt("id"), rs.getInt("club_id")),
                    id, LEAGUE_ID, season);
        }
        List<MatchResponse> fixtures = matches(season, null, null, null);
        return fixtures.stream().filter(match -> manualStats.containsKey(match.id())
                || fixtureIdsWithStats.contains(match.id()) ||
                fixtureClubs.containsKey(match.id())).map(match -> {
            MatchPlayerStatResponse manual = manualStats.get(match.id());
            if (manual != null) {
                return new PlayerMatchResponse(match, manual.clubId(), manual, "MANUAL_VERIFIED");
            }
            if (!fixtureIdsWithStats.contains(match.id())) {
                return new PlayerMatchResponse(match, fixtureClubs.get(match.id()), null, null);
            }
            MatchDetailResponse detail = matchDetail(match.id(), season).orElseThrow();
            MatchPlayerStatResponse stats = java.util.stream.Stream.concat(
                    detail.homePlayers().stream(), detail.awayPlayers().stream())
                    .filter(row -> row.playerId() == id).findFirst().orElse(null);
            return new PlayerMatchResponse(match,
                    stats == null ? fixtureClubs.get(match.id()) : stats.clubId(),
                    stats, detail.evidenceStatus());
        }).toList();
    }

    private static PlayerResponse playerResponse(ResultSet rs) throws SQLException {
        Date birthDate = rs.getDate("birth_date");
        return new PlayerResponse(rs.getInt("id"), rs.getString("name"),
                rs.getInt("club_id"), rs.getString("club_name"), rs.getString("position"),
                rs.getObject("goals", Integer.class), rs.getObject("assists", Integer.class),
                rs.getObject("fc27_overall", Integer.class), rs.getString("nationality"),
                birthDate == null ? null : birthDate.toLocalDate(),
                rs.getObject("height_cm", Integer.class), rs.getString("preferred_foot"),
                rs.getObject("shirt_number", Integer.class));
    }

    public List<MatchResponse> matches(int season, String club, Integer matchweek, String status) {
        validateSeason(season);
        String parsedStatus = filterEnum(status, "status", "SCHEDULED", "FINISHED", "POSTPONED",
                "CANCELLED", "SUSPENDED", "LIVE", "AWARDED");
        return jdbc.query("""
                SELECT f.id, f.home_club_id, home.name AS home_name, f.away_club_id,
                       away.name AS away_name, f.gameweek, f.match_date, f.status,
                       f.home_goals, f.away_goals,
                       EXISTS (SELECT 1 FROM manual_fixture_player_stats ms
                               JOIN manual_player_memberships membership
                                 ON membership.league_id=ms.league_id
                                AND membership.season_year=ms.season_year
                                AND membership.player_id=ms.player_id
                                AND membership.club_id=ms.club_id
                                AND membership.start_date<=f.match_date
                                AND (membership.end_date IS NULL OR membership.end_date>f.match_date)
                               WHERE ms.fixture_id=f.id AND ms.league_id=f.league_id
                                 AND ms.season_year=f.season_year
                                 AND ms.club_id IN (f.home_club_id, f.away_club_id)) AS has_manual_stats
                FROM fixtures f
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
            if (season == 2026) {
                List<MatchPlayerStatResponse> players = jdbc.query("""
                        SELECT m.player_id, p.name AS player_name, m.club_id, ps.position,
                               m.participation_status, m.rating, m.fantasy_points,
                               m.minutes, m.goals, m.assists, m.yellow_cards, m.red_cards
                        FROM manual_fixture_player_stats m
                        JOIN fixtures f ON f.id=m.fixture_id AND f.league_id=m.league_id
                          AND f.season_year=m.season_year
                        JOIN manual_player_memberships membership ON membership.league_id=m.league_id
                          AND membership.season_year=m.season_year AND membership.player_id=m.player_id
                          AND membership.club_id=m.club_id AND membership.start_date<=f.match_date
                          AND (membership.end_date IS NULL OR membership.end_date>f.match_date)
                        JOIN player_season_stats ps ON ps.league_id=m.league_id
                          AND ps.season_year=m.season_year AND ps.player_id=m.player_id
                          AND ps.club_id=m.club_id
                        JOIN players p ON p.id=m.player_id
                        WHERE m.fixture_id=? AND m.league_id=? AND m.season_year=?
                          AND m.club_id IN (f.home_club_id, f.away_club_id)
                        ORDER BY m.club_id, p.name, m.player_id
                        """, (rs, row) -> manualPlayerStat(rs), id, LEAGUE_ID, season);
                return new MatchDetailResponse(match,
                        players.stream().filter(player -> player.clubId() == match.homeClubId()).toList(),
                        players.stream().filter(player -> player.clubId() == match.awayClubId()).toList(),
                        players.isEmpty() ? "MISSING" : "MANUAL_VERIFIED", null);
            }
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
                (Integer) rs.getObject("away_goals"), rs.getBoolean("has_manual_stats"));
    }

    private static MatchPlayerStatResponse manualPlayerStat(ResultSet rs) throws SQLException {
        BigDecimal rating = rs.getBigDecimal("rating");
        return new MatchPlayerStatResponse(rs.getInt("player_id"), rs.getString("player_name"),
                rs.getInt("club_id"), shortPosition(rs.getString("position")),
                rs.getObject("minutes", Integer.class), rs.getObject("goals", Integer.class),
                rs.getObject("assists", Integer.class), rs.getObject("yellow_cards", Integer.class),
                rs.getObject("red_cards", Integer.class),
                rating == null ? null : rating.toPlainString(), null, null, null, null,
                null, null, rs.getString("participation_status"), rs.getBigDecimal("fantasy_points"));
    }

    private static String shortPosition(String position) {
        return switch (position) {
            case "GOALKEEPER" -> "G";
            case "DEFENDER" -> "D";
            case "MIDFIELDER" -> "M";
            case "FORWARD" -> "F";
            default -> null;
        };
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
