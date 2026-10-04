package com.premierhub.service;

import com.premierhub.repository.TeamOfWeekRepository;
import com.premierhub.web.error.InvalidFilterException;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import static org.junit.jupiter.api.Assertions.*;

class TeamOfWeekServiceTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private TeamOfWeekService service;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        service = new TeamOfWeekService(new TeamOfWeekRepository(jdbc));
        jdbc.update("INSERT INTO seasons VALUES (39,2026),(39,2024)");
        jdbc.update("INSERT INTO clubs (id,name) VALUES (1,'Historical club'),(2,'Current club')");
        fixture(100, 1, 2026, "FINISHED");
        fixture(101, 2, 2026, "FINISHED");
        fixture(102, 1, 2024, "FINISHED");
        fixture(103, 1, 2026, "LIVE");
        for (int slot = 0; slot < 11; slot++) {
            int id = slot + 1;
            player(id, TeamOfWeekOptimizer.permission(TeamOfWeekOptimizer.SLOTS.get(slot)));
            stats(id, 100, 2026, "PLAYED", "8.00");
        }
        // Rated but no eligible positions, unrated PLAYED, and a rated DNP.
        player(20, null); stats(20, 100, 2026, "PLAYED", "10.00");
        player(21, "GK"); stats(21, 100, 2026, "PLAYED", null);
        player(22, "GK"); stats(22, 100, 2026, "DID_NOT_PLAY", "10.00");
        player(23, "GK"); stats(23, 101, 2026, "PLAYED", "10.00");
        player(24, "GK"); stats(24, 102, 2024, "PLAYED", "10.00");
        // PLAYED counts regardless of fixture status; both exclusion reasons remain visible.
        player(25, null); stats(25, 103, 2026, "PLAYED", null);
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,1,1),(39,2026,1,2)");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,1,1,DATE '2026-08-01',DATE '2026-08-30'),(39,2026,1,2,DATE '2026-08-30',NULL)");
    }

    @AfterEach
    void close() { database.shutdown(); }

    @Test
    void readsOnlyPlayedRatedRightWeekAndKeepsHistoricalClubWithoutCurrentRosterOrOvr() {
        var team = service.team(1);
        assertEquals("COMPLETE", team.status());
        assertEquals(new BigDecimal("88.00"), team.totalRating());
        assertEquals(14, team.playedCount());
        assertEquals(12, team.ratedCount());
        assertEquals(11, team.candidateCount());
        assertEquals(2, team.excludedMissingPositions());
        assertEquals(2, team.excludedNullRatings());
        assertEquals(1, team.completedFixtures());
        assertEquals(1, team.recordedFixtures());
        assertEquals("Historical club", team.picks().getFirst().player().club());
        assertEquals(1, team.picks().getFirst().player().playerId());
        assertEquals(11, team.picks().stream().map(pick -> pick.player().playerId()).distinct().count());
    }

    @Test
    void insufficientPositionsReturnUnfilledSlotsAndNoFullTeamTotal() {
        jdbc.update("DELETE FROM player_eligible_positions WHERE player_id=11");
        var team = service.team(1);
        assertEquals("INSUFFICIENT_DATA", team.status());
        assertNull(team.totalRating());
        assertEquals(java.util.List.of("RW"), team.missingSlots());
        assertNull(team.picks().getLast().player());
        assertEquals(3, team.excludedMissingPositions());
        assertThrows(InvalidFilterException.class, () -> service.team(0));
        assertThrows(InvalidFilterException.class, () -> service.team(6));
        var empty = service.team(5);
        assertEquals("INSUFFICIENT_DATA", empty.status());
        assertEquals(TeamOfWeekOptimizer.SLOTS, empty.missingSlots());
        assertNull(empty.totalRating());
    }

    @Test
    void multipleAppearancesBlockSelectionEvenIfOneRatingIsNull() {
        fixture(104, 1, 2026, "FINISHED");
        stats(1, 104, 2026, "PLAYED", null);
        var team = service.team(1);
        assertEquals("MULTIPLE_MATCHES", team.status());
        assertNull(team.totalRating());
        assertTrue(team.picks().stream().allMatch(pick -> pick.player() == null));
        assertEquals(1, team.conflicts().size());
        assertEquals(1, team.conflicts().getFirst().playerId());
        assertEquals(java.util.List.of(100, 104), team.conflicts().getFirst().fixtureIds());
    }

    private void player(int id, String position) {
        jdbc.update("INSERT INTO players VALUES (?,?)", id, "Player " + id);
        if (position != null) {
            jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,?)", id, position);
            jdbc.update("INSERT INTO player_eligible_positions VALUES (39,2026,?,?)", id, position);
        }
    }

    private void fixture(int id, int week, int season, String status) {
        jdbc.update("""
                INSERT INTO fixtures (id,league_id,season_year,home_club_id,away_club_id,gameweek,
                    match_date,status,provider_status,payload_hash,synced_at)
                VALUES (?,39,?,1,2,?,DATE '2026-08-21',?,'FT',?,CURRENT_TIMESTAMP)
                """, id, season, week, status, "0".repeat(64));
    }

    private void stats(int id, int fixture, int season, String participation, String rating) {
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats (fixture_id,player_id,club_id,league_id,
                    season_year,participation_status,rating) VALUES (?,?,1,39,?,?,?)
                """, fixture, id, season, participation, rating == null ? null : new BigDecimal(rating));
    }
}
