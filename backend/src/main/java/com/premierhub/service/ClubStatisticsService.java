package com.premierhub.service;

import com.premierhub.web.dto.ClubStatisticsResponse;
import com.premierhub.web.dto.ClubStatisticsResponse.PlayerStatistics;
import com.premierhub.web.error.InvalidFilterException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClubStatisticsService {
    private final JdbcTemplate jdbc;

    public ClubStatisticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ClubStatisticsResponse statistics(int clubId, int season) {
        if (season != 2026) {
            throw new InvalidFilterException("Club rating statistics are available for season 2026 only");
        }
        // Membership is checked at the match date. A later transfer must not move
        // a player's earlier ratings or goals to their new club.
        String eligibleRows = """
                FROM manual_fixture_player_stats s
                JOIN fixtures f ON f.id=s.fixture_id AND f.league_id=s.league_id
                    AND f.season_year=s.season_year
                WHERE s.league_id=39 AND s.season_year=2026 AND s.club_id=?
                    AND f.status='FINISHED' AND s.club_id IN (f.home_club_id, f.away_club_id)
                    AND EXISTS (
                        SELECT 1 FROM manual_player_memberships m
                        WHERE m.league_id=s.league_id AND m.season_year=s.season_year
                            AND m.player_id=s.player_id AND m.club_id=s.club_id
                            AND m.start_date<=f.match_date
                            AND (m.end_date IS NULL OR m.end_date>f.match_date))
                """;
        var players = jdbc.query("""
                SELECT s.player_id,
                    COUNT(CASE WHEN s.participation_status='PLAYED' THEN 1 END) AS appearances,
                    CASE WHEN COUNT(CASE WHEN s.participation_status='PLAYED' AND s.goals IS NULL
                        THEN 1 END)>0 THEN NULL
                        ELSE SUM(CASE WHEN s.participation_status='PLAYED' THEN s.goals ELSE 0 END) END AS goals,
                    CASE WHEN COUNT(CASE WHEN s.participation_status='PLAYED' AND s.assists IS NULL
                        THEN 1 END)>0 THEN NULL
                        ELSE SUM(CASE WHEN s.participation_status='PLAYED' THEN s.assists ELSE 0 END) END AS assists,
                    AVG(CASE WHEN s.participation_status='PLAYED' THEN s.rating END) AS average_rating,
                    COUNT(CASE WHEN s.participation_status='PLAYED' THEN s.rating END) AS rated_appearances
                """ + eligibleRows + " GROUP BY s.player_id ORDER BY s.player_id",
                (rs, row) -> new PlayerStatistics(rs.getInt("player_id"), rs.getInt("appearances"),
                        rs.getObject("goals") == null ? null : rs.getInt("goals"),
                        rs.getObject("assists") == null ? null : rs.getInt("assists"),
                        rs.getObject("average_rating") == null ? null : rs.getDouble("average_rating"),
                        rs.getInt("rated_appearances")), clubId);
        int rated = players.stream().mapToInt(PlayerStatistics::ratedAppearances).sum();
        Double average = rated == 0 ? null : players.stream()
                .filter(player -> player.averageRating() != null)
                .mapToDouble(player -> player.averageRating() * player.ratedAppearances()).sum() / rated;
        int recorded = jdbc.queryForObject("SELECT COUNT(DISTINCT s.fixture_id) " + eligibleRows,
                Integer.class, clubId);
        return new ClubStatisticsResponse(clubId, season, average, rated, recorded, players);
    }
}
