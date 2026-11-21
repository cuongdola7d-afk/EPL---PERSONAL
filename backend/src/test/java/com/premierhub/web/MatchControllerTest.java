package com.premierhub.web;

import com.premierhub.lineups.MatchLineupQueries;
import com.premierhub.service.FixtureEvidenceService;
import com.premierhub.service.FootballQueries;
import com.premierhub.service.MatchScoringService;
import com.premierhub.web.dto.MatchResponse;
import com.premierhub.web.error.InvalidFilterException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static com.premierhub.web.ErrorResponseAssertions.expectError;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
class MatchControllerTest {
    private final MatchResponse match = new MatchResponse(1, 1, "Arsenal", 2, "Chelsea",
            1, LocalDate.of(2025, 8, 16), "FINISHED", 2, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootballQueries service;

    @Test
    void getAllReturnsJsonArrayWithUnchangedFields() throws Exception {
        when(service.matches(2024, null, null, null)).thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].homeClub").value("Arsenal"))
                .andExpect(jsonPath("$[0].awayClub").value("Chelsea"))
                .andExpect(jsonPath("$[0].matchweek").value(1))
                .andExpect(jsonPath("$[0].kickoffUtc").doesNotExist());
    }

    @Test
    void season2026ListAndDetailSerializeFullUtcKickoff() throws Exception {
        var timed = new MatchResponse(1, 1, "Arsenal", 2, "Chelsea", 6,
                LocalDate.of(2026, 10, 4), "SCHEDULED", null, null, false,
                Instant.parse("2026-10-04T20:00:00Z"));
        when(service.matches(2026, null, null, null)).thenReturn(List.of(timed));
        when(service.match(1, 2026)).thenReturn(Optional.of(timed));
        mockMvc.perform(get("/api/matches?season=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-10-04"))
                .andExpect(jsonPath("$[0].kickoffUtc").value("2026-10-04T20:00:00Z"));
        mockMvc.perform(get("/api/matches/1?season=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kickoffUtc").value("2026-10-04T20:00:00Z"));
    }

    @Test
    void getByIdSucceedsAndMissingIdReturns404() throws Exception {
        when(service.match(1, 2024)).thenReturn(Optional.of(match));

        mockMvc.perform(get("/api/matches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
        expectError(mockMvc.perform(get("/api/matches/999")), HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "/api/matches/999");
    }

    @Test
    void validFiltersAndCombinationStillReturnMatches() throws Exception {
        when(service.matches(2024, "Arsenal", 1, " finished ")).thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches").param("club", "Arsenal")
                        .param("matchweek", "1").param("status", " finished "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void invalidWeekAndWrongTypeReturnDistinctCodes() throws Exception {
        expectError(mockMvc.perform(get("/api/matches").param("matchweek", "0")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/matches");
        expectError(mockMvc.perform(get("/api/matches").param("matchweek", "abc")),
                HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", "/api/matches");
    }

    @Test
    void roundAliasFiltersSeason2026AndRejectsConflicts() throws Exception {
        when(service.matches(2026, null, 1, null)).thenReturn(List.of(match));
        mockMvc.perform(get("/api/matches?season=2026&round=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/matches?round=1&matchweek=2"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FILTER"));
        mockMvc.perform(get("/api/matches?round=0")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownStatusReturnsInvalidFilter() throws Exception {
        when(service.matches(2024, null, null, "UNKNOWN"))
                .thenThrow(new InvalidFilterException("Unknown status: UNKNOWN"));

        expectError(mockMvc.perform(get("/api/matches").param("status", "UNKNOWN")),
                HttpStatus.BAD_REQUEST, "INVALID_FILTER", "/api/matches");
    }

    @Test
    void blankOptionalFiltersAndInvalidIdReturnValidationError() throws Exception {
        expectError(mockMvc.perform(get("/api/matches").param("club", "")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/matches");
        expectError(mockMvc.perform(get("/api/matches").param("status", "   ")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/matches");
        expectError(mockMvc.perform(get("/api/matches/-1")), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "/api/matches/-1");
    }

    @Test
    void validFilterWithoutMatchesReturnsEmptyArray() throws Exception {
        when(service.matches(2024, "Unknown", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/matches").param("club", "Unknown"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void detailIncludesExtraStatsOnlyForSeason2026PlayersWhoHaveRowAndPreservesLegacyShape() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:match-controller-extra-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.update("INSERT INTO seasons VALUES (39, 2026), (39, 2024)");
        jdbc.update("INSERT INTO clubs (id, name) VALUES (1000000066, 'Manchester United FC'), "
                + "(1000000058, 'Aston Villa FC'), (42, 'Arsenal'), (43, 'Chelsea')");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, 1000000066), (39, 2026, 1000000058), "
                + "(39, 2024, 42), (39, 2024, 43)");
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560639, 39, 2026, 1000000066, 1000000058, 10, DATE '2026-11-08',
                        'FINISHED', 'FINISHED', 4, 4, 'hash2026', CURRENT_TIMESTAMP),
                       (800, 39, 2024, 42, 43, 1, DATE '2024-08-16',
                        'FINISHED', 'FT', 2, 1, 'hash2024', CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO players (id, name) VALUES (2000006008, 'Bruno Fernandes'), "
                + "(2000006099, 'Unused Sub'), (901, 'Bukayo Saka')");
        jdbc.update("""
                INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                VALUES (39, 2026, 2000006008, 1000000066, 'MIDFIELDER'),
                       (39, 2026, 2000006099, 1000000066, 'DEFENDER')
                """);
        jdbc.update("""
                INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (39, 2026, 2000006008, 1000000066, DATE '2026-08-01', NULL),
                       (39, 2026, 2000006099, 1000000066, DATE '2026-08-01', NULL)
                """);
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (1000560639, 2000006008, 39, 2026, 1000000066, 'PLAYED', 8.20, 8.20, 90, 1, 1, 0, 0),
                       (1000560639, 2000006099, 39, 2026, 1000000066, 'DID_NOT_PLAY', NULL, 0.00, 0, 0, 0, 0, 0)
                """);
        jdbc.update("""
                INSERT INTO manual_fixture_player_extra_stats
                (fixture_id, player_id, league_id, season_year, club_id, saves, goals_conceded,
                 bps, bonus, tackles, clearances_blocks_interceptions, recoveries, defensive_contribution,
                 expected_goals, expected_assists, source, fetched_at)
                VALUES (1000560639, 2000006008, 39, 2026, 1000000066, 0, 4, 38, 2, 3, 2, 5, 7,
                        0.99, 0.56, 'FPL', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO fixture_player_stats
                (fixture_id, player_id, club_id, position, minutes, goals, assists,
                 yellow_cards, red_cards, rating, shots_on, passes_key, tackles, saves,
                 raw_statistics, synced_at)
                VALUES (800, 901, 42, 'F', 90, 1, 0, 0, 0, '8.1', 2, 3, 1, 0, '{}', CURRENT_TIMESTAMP)
                """);

        FootballQueries queries = new FootballQueries(jdbc, new MatchScoringService(),
                new FixtureEvidenceService(jdbc, new ObjectMapper()));
        MockMvc h2Mvc = MockMvcBuilders.standaloneSetup(new MatchController(queries)).build();

        // 2026 match: player with extra row exposes extra.*, player without extra omits extra field, legacy fields intact
        h2Mvc.perform(get("/api/matches/1000560639/details?season=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceStatus").value("MANUAL_VERIFIED"))
                .andExpect(jsonPath("$.homePlayers[0].playerId").value(2000006008))
                .andExpect(jsonPath("$.homePlayers[0].rating").value("8.20"))
                .andExpect(jsonPath("$.homePlayers[0].goals").value(1))
                .andExpect(jsonPath("$.homePlayers[0].assists").value(1))
                .andExpect(jsonPath("$.homePlayers[0].extra.expectedGoals").value(0.99))
                .andExpect(jsonPath("$.homePlayers[0].extra.expectedAssists").value(0.56))
                .andExpect(jsonPath("$.homePlayers[0].extra.tackles").value(3))
                .andExpect(jsonPath("$.homePlayers[0].extra.bps").value(38))
                .andExpect(jsonPath("$.homePlayers[0].extra.bonus").value(2))
                .andExpect(jsonPath("$.homePlayers[0].extra.saves").value(0))
                .andExpect(jsonPath("$.homePlayers[0].extra.goalsConceded").value(4))
                .andExpect(jsonPath("$.homePlayers[0].extra.clearancesBlocksInterceptions").value(2))
                .andExpect(jsonPath("$.homePlayers[0].extra.recoveries").value(5))
                .andExpect(jsonPath("$.homePlayers[0].extra.defensiveContribution").value(7))
                .andExpect(jsonPath("$.homePlayers[1].playerId").value(2000006099))
                .andExpect(jsonPath("$.homePlayers[1].participationStatus").value("DID_NOT_PLAY"))
                .andExpect(jsonPath("$.homePlayers[1].extra").doesNotExist());

        // 2024 match: no extra field at all, legacy scoring & fields untouched
        h2Mvc.perform(get("/api/matches/800/details?season=2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homePlayers[0].playerId").value(901))
                .andExpect(jsonPath("$.homePlayers[0].score.confirmedPoints").value(6))
                .andExpect(jsonPath("$.homePlayers[0].extra").doesNotExist());
    }

}
