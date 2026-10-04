package com.premierhub.service;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.jupiter.api.Assertions.*;

class MatchKickoffQueryTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private FootballQueries queries;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        queries = new FootballQueries(jdbc, null, null);
        jdbc.update("INSERT INTO seasons VALUES (39, 2026), (39, 2024)");
        jdbc.update("INSERT INTO clubs (id, name) VALUES (1, 'Arsenal'), (2, 'Chelsea')");
        fixture(1, 2026, "2026-10-04", "2026-10-04T20:00:00Z");
        fixture(2, 2026, "2026-10-04", "2026-10-04T12:00:00Z");
        fixture(3, 2026, "2026-10-04", null);
        fixture(4, 2024, "2024-08-16", "2024-08-16T20:00:00Z");
    }

    @AfterEach
    void close() {
        database.shutdown();
    }

    @Test
    void listDetailAndClubFilterReturnStoredUtcWithoutChangingFixtureDates() {
        var rows = queries.matches(2026, "Arsenal", null, null);
        assertEquals(3, rows.size());
        assertEquals(Instant.parse("2026-10-04T20:00:00Z"), rows.getFirst().kickoffUtc());
        assertEquals(Instant.parse("2026-10-04T12:00:00Z"), rows.get(1).kickoffUtc());
        assertNull(rows.get(2).kickoffUtc());
        assertEquals(LocalDate.of(2026, 10, 4), rows.getFirst().date());
        var detail = queries.matchDetail(1, 2026).orElseThrow();
        assertEquals(rows.getFirst(), detail.match());
        assertEquals("2026-10-04T20:00:00Z", jdbc.queryForObject(
                "SELECT kickoff_utc FROM football_data_fixtures WHERE fixture_id=1", String.class));
        var legacy = queries.match(4, 2024).orElseThrow();
        assertNull(legacy.kickoffUtc());
        assertEquals(LocalDate.of(2024, 8, 16), legacy.date());
    }

    @Test
    void playerHistoryKeepsUtcAndUsesOriginalDateForMembershipEligibility() {
        jdbc.update("INSERT INTO players (id, name) VALUES (10, 'Test player')");
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,10,1)");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,10,1,DATE '2026-10-04',DATE '2026-10-05')");
        var rows = queries.playerMatches(10, 2026);
        assertEquals(3, rows.size());
        assertEquals(Instant.parse("2026-10-04T20:00:00Z"), rows.getFirst().match().kickoffUtc());
        assertEquals(LocalDate.of(2026, 10, 4), rows.getFirst().match().date());
        assertEquals("2026-10-05", jdbc.queryForObject(
                "SELECT end_date FROM manual_player_memberships WHERE player_id=10", java.sql.Date.class).toString());
    }

    private void fixture(int id, int season, String date, String kickoff) {
        jdbc.update("""
                INSERT INTO fixtures (id,league_id,season_year,home_club_id,away_club_id,
                    gameweek,match_date,status,provider_status,payload_hash,synced_at)
                VALUES (?,39,?,1,2,1,?,'SCHEDULED','NS',?,CURRENT_TIMESTAMP)
                """, id, season, date, "0".repeat(64));
        if (kickoff != null) {
            jdbc.update("INSERT INTO football_data_fixtures VALUES (?,?,?,'TIMED')", id, id, kickoff);
        }
    }
}
