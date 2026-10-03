package com.premierhub.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import com.premierhub.web.error.InvalidFilterException;

import static org.junit.jupiter.api.Assertions.*;

class ClubStatisticsServiceTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private ClubStatisticsService service;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        service = new ClubStatisticsService(jdbc);
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO clubs (id, name) VALUES (1, 'One'), (2, 'Two')");
        jdbc.update("INSERT INTO players VALUES (10, 'Rated twice'), (11, 'Rated once'), (12, 'Bench')");
        for (int player : new int[]{10, 11, 12}) {
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,?,1)", player);
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,1,DATE '2026-08-01',DATE '2026-10-01')", player);
        }
        fixture(100, "2026-08-15", "FINISHED", 1, 2);
        fixture(101, "2026-09-15", "FINISHED", 2, 1);
        fixture(102, "2026-09-20", "SCHEDULED", 1, 2);
        fixture(103, "2026-10-02", "FINISHED", 1, 2);
    }

    @AfterEach
    void close() { database.shutdown(); }

    @Test
    void weightsEachRatedAppearanceAndPreservesMissingStatistics() {
        stats(100, 10, "PLAYED", "8.00", 1, 0);
        stats(101, 10, "PLAYED", "6.00", 0, 1);
        stats(100, 11, "PLAYED", "9.00", 1, 0);
        stats(101, 11, "PLAYED", null, null, 0);
        stats(100, 12, "DID_NOT_PLAY", "0.00", 0, 0);
        var result = service.statistics(1, 2026);
        assertEquals(23.0 / 3, result.averageRating(), 0.00001);
        assertEquals(3, result.ratedAppearances());
        assertEquals(2, result.recordedMatches());
        var ungraded = result.players().stream().filter(row -> row.playerId() == 11).findFirst().orElseThrow();
        assertEquals(2, ungraded.appearances());
        assertNull(ungraded.goals());
        assertEquals(0, ungraded.assists());
        assertEquals(9.0, ungraded.averageRating());
        var bench = result.players().stream().filter(row -> row.playerId() == 12).findFirst().orElseThrow();
        assertEquals(0, bench.appearances());
        assertNull(bench.averageRating());
        assertEquals(0, bench.ratedAppearances());
    }

    @Test
    void excludesUnfinishedFixturesAndExpiredMembershipAndOtherClub() {
        stats(100, 10, "PLAYED", "8.00", 1, 0);
        stats(102, 10, "PLAYED", "1.00", 9, 0);
        stats(103, 10, "PLAYED", "1.00", 9, 0);
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,10,2)");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,10,2,DATE '2026-10-01',NULL)");
        jdbc.update("INSERT INTO manual_fixture_player_stats (fixture_id,player_id,league_id,season_year,club_id,participation_status,rating,goals,assists) VALUES (103,11,39,2026,2,'PLAYED',10,8,0)");
        var result = service.statistics(1, 2026);
        assertEquals(8.0, result.averageRating());
        assertEquals(1, result.players().getFirst().goals());
        assertEquals(1, result.recordedMatches());
        assertNull(service.statistics(2, 2026).averageRating());
    }

    @Test
    void noRatingsStayNullAndLegacySeasonIsNotRead() {
        assertNull(service.statistics(1, 2026).averageRating());
        assertEquals(0, service.statistics(1, 2026).ratedAppearances());
        assertThrows(InvalidFilterException.class, () -> service.statistics(1, 2024));
    }

    private void fixture(int id, String date, String status, int home, int away) {
        jdbc.update("""
                INSERT INTO fixtures (id,league_id,season_year,home_club_id,away_club_id,gameweek,
                    match_date,status,provider_status,payload_hash,synced_at)
                VALUES (?,39,2026,?,?,1,CAST(? AS DATE),?,'FT',?,CURRENT_TIMESTAMP)
                """, id, home, away, date, status, "0".repeat(64));
    }

    private void stats(int fixture, int player, String participation, String rating, Integer goals, Integer assists) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats (fixture_id,player_id,league_id,season_year,club_id,
                    participation_status,rating,goals,assists)
                VALUES (?,?,39,2026,1,?,CAST(? AS DECIMAL(4,2)),?,?)
                """, fixture, player, participation, rating, goals, assists);
    }
}
