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

    @Test
    void sumsMinutesAndExtraStatsAndReturnsNullWhenAnyPlayedExtraRowIsMissing() {
        statsWithMinutes(100, 10, 1, "PLAYED", "7.50", 1, 0, 90);
        statsWithMinutes(101, 10, 1, "PLAYED", "7.10", 0, 2, 75);
        extraStats(100, 10, 1, 1, 4, 0, 3, 7, 6, "0.64");
        extraStats(101, 10, 1, 0, 2, 1, 5, 4, 8, "0.53");

        // Player 11 played 2 matches, but extra row is missing for fixture 101 -> extra totals must be null
        statsWithMinutes(100, 11, 1, "PLAYED", "7.00", 0, 0, 80);
        statsWithMinutes(101, 11, 1, "PLAYED", "6.80", 0, 0, 60);
        extraStats(100, 11, 1, 1, 0, 0, 2, 5, 4, "0.10");

        // Player 12 is DID_NOT_PLAY with stray extra row -> must not count into appearances or totals
        statsWithMinutes(100, 12, 1, "DID_NOT_PLAY", null, 0, 0, 0);
        extraStats(100, 12, 1, 1, 9, 9, 9, 9, 9, "5.00");

        var result = service.statistics(1, 2026);
        var p10 = result.players().stream().filter(r -> r.playerId() == 10).findFirst().orElseThrow();
        assertEquals(2, p10.appearances());
        assertEquals(1, p10.goals());
        assertEquals(2, p10.assists());
        assertEquals(7.30, p10.averageRating(), 0.0001);
        assertEquals(2, p10.ratedAppearances());
        assertEquals(165, p10.minutes());
        assertEquals(1, p10.cleanSheets());
        assertEquals(6, p10.saves());
        assertEquals(1, p10.goalsConceded());
        assertEquals(8, p10.tackles());
        assertEquals(11, p10.clearancesBlocksInterceptions());
        assertEquals(14, p10.recoveries());
        assertEquals(1.17, p10.expectedGoals(), 0.0001);

        var p11 = result.players().stream().filter(r -> r.playerId() == 11).findFirst().orElseThrow();
        assertEquals(140, p11.minutes());
        assertNull(p11.cleanSheets());
        assertNull(p11.saves());
        assertNull(p11.goalsConceded());
        assertNull(p11.tackles());
        assertNull(p11.clearancesBlocksInterceptions());
        assertNull(p11.recoveries());
        assertNull(p11.expectedGoals());

        var p12 = result.players().stream().filter(r -> r.playerId() == 12).findFirst().orElseThrow();
        assertEquals(0, p12.appearances());
        assertEquals(0, p12.minutes());
        assertEquals(0, p12.cleanSheets());
        assertEquals(0, p12.saves());
        assertEquals(0.0, p12.expectedGoals());
    }

    @Test
    void separatesExtraStatsByClubOnMidSeasonTransfer() {
        // Player 10 plays for club 1 in Aug (fixture 100), transfers to club 2 on Oct 1, plays fixture 103
        statsWithMinutes(100, 10, 1, "PLAYED", "7.40", 1, 0, 90);
        extraStats(100, 10, 1, 1, 0, 0, 4, 8, 5, "0.45");

        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,10,2)");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,10,2,DATE '2026-10-01',NULL)");
        statsWithMinutes(103, 10, 2, "PLAYED", "8.20", 2, 1, 88);
        extraStats(103, 10, 2, 0, 0, 2, 1, 2, 7, "1.35");

        var club1 = service.statistics(1, 2026).players().stream().filter(r -> r.playerId() == 10).findFirst().orElseThrow();
        assertEquals(90, club1.minutes());
        assertEquals(1, club1.cleanSheets());
        assertEquals(4, club1.tackles());
        assertEquals(8, club1.clearancesBlocksInterceptions());
        assertEquals(5, club1.recoveries());
        assertEquals(0.45, club1.expectedGoals(), 0.0001);

        var club2 = service.statistics(2, 2026).players().stream().filter(r -> r.playerId() == 10).findFirst().orElseThrow();
        assertEquals(88, club2.minutes());
        assertEquals(0, club2.cleanSheets());
        assertEquals(2, club2.goalsConceded());
        assertEquals(1, club2.tackles());
        assertEquals(2, club2.clearancesBlocksInterceptions());
        assertEquals(7, club2.recoveries());
        assertEquals(1.35, club2.expectedGoals(), 0.0001);
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

    private void statsWithMinutes(int fixture, int player, int clubId, String participation, String rating,
                                  Integer goals, Integer assists, Integer minutes) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats (fixture_id,player_id,league_id,season_year,club_id,
                    participation_status,rating,goals,assists,minutes)
                VALUES (?,?,39,2026,?,?,CAST(? AS DECIMAL(4,2)),?,?,?)
                """, fixture, player, clubId, participation, rating, goals, assists, minutes);
    }

    private void extraStats(int fixture, int player, int clubId, Integer cleanSheets, Integer saves,
                            Integer goalsConceded, Integer tackles, Integer cbi, Integer recoveries,
                            String expectedGoals) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_extra_stats (fixture_id,player_id,league_id,season_year,club_id,
                    clean_sheets,saves,goals_conceded,tackles,clearances_blocks_interceptions,recoveries,expected_goals,
                    source,fetched_at)
                VALUES (?,?,39,2026,?,?,?,?,?,?,?,CAST(? AS DECIMAL(5,2)),'FPL',CURRENT_TIMESTAMP)
                """, fixture, player, clubId, cleanSheets, saves, goalsConceded, tackles, cbi, recoveries, expectedGoals);
    }
}
