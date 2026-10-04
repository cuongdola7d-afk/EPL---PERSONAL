package com.premierhub.fantasy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class GameweekRepository {
    public record Fixture(int id, int gameweek, Instant kickoffUtc, String status,
                          String homeClub, String awayClub) { }
    public record Configuration(int gameweek, Instant deadlineUtc, Instant deadlinePublishedAt,
                                int firstFixtureId, Instant firstKickoffUtc, String workflowStatus,
                                Instant resultsPublishedAt, int revision, LocalDate rosterAsOf) { }
    public record Change(int gameweek, int revision, Instant oldDeadlineUtc, Instant newDeadlineUtc,
                         Instant changedAt, String reason) { }
    private final JdbcTemplate jdbc;

    public GameweekRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Fixture> fixtures() {
        return jdbc.query("""
                SELECT f.id,f.gameweek,fd.kickoff_utc,f.status,h.name AS home_club,a.name AS away_club
                FROM fixtures f
                LEFT JOIN football_data_fixtures fd ON fd.fixture_id=f.id
                JOIN clubs h ON h.id=f.home_club_id JOIN clubs a ON a.id=f.away_club_id
                WHERE f.league_id=39 AND f.season_year=2026 ORDER BY f.gameweek,f.id
                """, (rs, row) -> new Fixture(rs.getInt("id"), rs.getInt("gameweek"),
                parseKickoff(rs.getString("kickoff_utc")), rs.getString("status"),
                rs.getString("home_club"), rs.getString("away_club")));
    }

    // Date-only input must never be expanded to a fictional midnight kickoff.
    private static Instant parseKickoff(String value) {
        if (value == null || !value.contains("T")) return null;
        try { return Instant.parse(value); }
        catch (java.time.format.DateTimeParseException invalid) { return null; }
    }

    public List<Configuration> configurations() {
        return jdbc.query("SELECT * FROM fantasy_gameweeks WHERE season=2026 ORDER BY gameweek",
                (rs, row) -> configuration(rs));
    }

    public List<Change> changes() {
        return jdbc.query("SELECT * FROM fantasy_deadline_changes WHERE season=2026 ORDER BY gameweek,revision",
                (rs, row) -> new Change(rs.getInt("gameweek"), rs.getInt("revision"),
                instant(rs, "old_deadline_utc"), instant(rs, "new_deadline_utc"),
                instant(rs, "changed_at"), rs.getString("reason")));
    }

    public Optional<Configuration> lock(int gameweek) {
        return jdbc.query("SELECT * FROM fantasy_gameweeks WHERE season=2026 AND gameweek=? FOR UPDATE",
                (rs, row) -> configuration(rs), gameweek).stream().findFirst();
    }

    public Optional<Configuration> find(int gameweek) {
        return jdbc.query("SELECT * FROM fantasy_gameweeks WHERE season=2026 AND gameweek=?",
                (rs, row) -> configuration(rs), gameweek).stream().findFirst();
    }

    public boolean hasRoster(LocalDate asOf) {
        return !jdbc.queryForList("""
                SELECT 1 FROM manual_player_memberships m
                JOIN player_season_stats s ON s.league_id=m.league_id AND s.season_year=m.season_year
                    AND s.player_id=m.player_id AND s.club_id=m.club_id
                WHERE m.league_id=39 AND m.season_year=2026 AND m.start_date<=?
                    AND (m.end_date IS NULL OR m.end_date>?) LIMIT 1
                """, java.sql.Date.valueOf(asOf), java.sql.Date.valueOf(asOf)).isEmpty();
    }

    public void create(int gameweek, Fixture first, Instant deadline, Instant now, LocalDate rosterAsOf) {
        jdbc.update("""
                INSERT INTO fantasy_gameweeks (season,gameweek,deadline_utc,deadline_published_at,
                    first_fixture_id,first_kickoff_utc,workflow_status,revision,updated_at,roster_as_of)
                VALUES (2026,?,?,?,?,?,'OPEN',1,?,?)
                """, gameweek, timestamp(deadline), timestamp(now), first.id(), timestamp(first.kickoffUtc()), timestamp(now),
                java.sql.Date.valueOf(rosterAsOf));
    }

    public void adjust(int gameweek, Instant deadline, int revision, Instant now) {
        jdbc.update("UPDATE fantasy_gameweeks SET deadline_utc=?,revision=?,updated_at=? WHERE season=2026 AND gameweek=?",
                timestamp(deadline), revision, timestamp(now), gameweek);
    }

    public void audit(int gameweek, int revision, Instant oldDeadline, Instant deadline,
                      Instant now, long actorId, String reason) {
        jdbc.update("""
                INSERT INTO fantasy_deadline_changes (season,gameweek,revision,old_deadline_utc,
                    new_deadline_utc,changed_at,changed_by,reason) VALUES (2026,?,?,?,?,?,?,?)
                """, gameweek, revision, timestamp(oldDeadline), timestamp(deadline), timestamp(now), actorId, reason);
    }

    private static Configuration configuration(ResultSet rs) throws SQLException {
        return new Configuration(rs.getInt("gameweek"), instant(rs, "deadline_utc"),
                instant(rs, "deadline_published_at"), rs.getInt("first_fixture_id"),
                instant(rs, "first_kickoff_utc"), rs.getString("workflow_status"),
                instant(rs, "results_published_at"), rs.getInt("revision"), rs.getDate("roster_as_of").toLocalDate());
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }

    static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.valueOf(LocalDateTime.ofInstant(
                value.truncatedTo(java.time.temporal.ChronoUnit.MICROS), ZoneOffset.UTC));
    }
}
