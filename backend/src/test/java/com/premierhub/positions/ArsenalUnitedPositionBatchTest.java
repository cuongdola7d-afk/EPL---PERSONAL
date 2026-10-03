package com.premierhub.positions;

import com.premierhub.roster.ManualRosterCsvReader;
import com.premierhub.service.FixtureEvidenceService;
import com.premierhub.service.FootballQueries;
import com.premierhub.service.MatchScoringService;
import com.premierhub.web.PlayerController;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArsenalUnitedPositionBatchTest {
    private static final int ARSENAL = 1000000057;
    private static final int UNITED = 1000000066;
    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 2);
    private static final Path CSV = Path.of("data/player-positions-arsenal-manchester-united-2026-10-02/players.csv");

    @Test
    void importsReviewedBatchTwiceAndReturnsPositionsFromListAndDetailApi() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:reviewed-positions-" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO seasons VALUES (39, 2024), (39, 2026)");
        jdbc.update("INSERT INTO clubs (id, name) VALUES (?, 'Arsenal')", ARSENAL);
        jdbc.update("INSERT INTO clubs (id, name) VALUES (?, 'Manchester United')", UNITED);
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, ?)", ARSENAL);
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, ?)", UNITED);

        Map<Integer, ManualRosterCsvReader.Row> roster = new HashMap<>();
        ManualRosterCsvReader reader = new ManualRosterCsvReader();
        for (int batch = 1; batch <= 4; batch++) {
            Path path = Path.of("data/roster-2026-09-30/batch-%02d.csv".formatted(batch));
            for (var row : reader.read(path)) {
                if ((row.clubId() == ARSENAL || row.clubId() == UNITED)
                        && !row.startDate().isAfter(AS_OF)
                        && (row.endDate() == null || row.endDate().isAfter(AS_OF))) {
                    if (roster.putIfAbsent(row.playerId(), row) != null) {
                        throw new IllegalStateException("Duplicate active roster ID " + row.playerId());
                    }
                }
            }
        }
        assertEquals(54, roster.size());
        for (var row : roster.values()) {
            jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", row.playerId(), row.name());
            jdbc.update("""
                    INSERT INTO player_season_stats
                    (league_id, season_year, player_id, club_id, position)
                    VALUES (39, 2026, ?, ?, ?)
                    """, row.playerId(), row.clubId(), row.position());
            jdbc.update("""
                    INSERT INTO manual_player_memberships
                    (league_id, season_year, player_id, club_id, start_date, end_date)
                    VALUES (39, 2026, ?, ?, ?, ?)
                    """, row.playerId(), row.clubId(), Date.valueOf(row.startDate()),
                    row.endDate() == null ? null : Date.valueOf(row.endDate()));
        }
        jdbc.update("""
                INSERT INTO player_season_stats
                (league_id, season_year, player_id, club_id, position, goals)
                VALUES (39, 2024, 2000001007, ?, 'FORWARD', 4)
                """, ARSENAL);
        jdbc.update("UPDATE player_season_stats SET goals=2 WHERE season_year=2026 AND player_id=2000001007");

        PlayerPositionImporter importer = new PlayerPositionImporter(jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        assertEquals(new PlayerPositionImporter.Result(46, 46, 0), importer.importFile(CSV));
        assertEquals(new PlayerPositionImporter.Result(46, 0, 0), importer.importFile(CSV));
        assertEquals(46, jdbc.queryForObject("SELECT COUNT(*) FROM player_specific_positions", Integer.class));
        assertEquals(90, jdbc.queryForObject("SELECT COUNT(*) FROM player_eligible_positions", Integer.class));
        assertEquals(54, jdbc.queryForObject("SELECT COUNT(*) FROM player_season_stats WHERE season_year=2026", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2026 AND player_id=2000001007", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2024 AND player_id=2000001007", Integer.class));

        FootballQueries queries = new FootballQueries(jdbc, new MatchScoringService(),
                new FixtureEvidenceService(jdbc, new ObjectMapper()));
        var players = queries.players(2026, null, null, AS_OF);
        assertEquals(54, players.size());
        var saka = players.stream().filter(player -> player.id() == 2000001007).findFirst().orElseThrow();
        var amad = players.stream().filter(player -> player.id() == 2000006015).findFirst().orElseThrow();
        var rice = players.stream().filter(player -> player.id() == 2000001023).findFirst().orElseThrow();
        assertEquals("RW", saka.primaryPosition());
        assertEquals(java.util.List.of("RW", "RM"), saka.eligiblePositions());
        assertEquals("RM", amad.primaryPosition());
        assertEquals(java.util.List.of("RM", "RB", "RW"), amad.eligiblePositions());
        assertEquals("MISSING", rice.positionStatus());
        assertNull(rice.primaryPosition());
        assertFalse(rice.eligiblePositions().contains("CM"));

        var mvc = MockMvcBuilders.standaloneSetup(new PlayerController(queries)).build();
        mvc.perform(get("/api/players").param("season", "2026").param("asOf", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 2000001007)].primaryPosition").value(hasItem("RW")))
                .andExpect(jsonPath("$[?(@.id == 2000006015)].eligiblePositions[2]").value(hasItem("RW")));
        mvc.perform(get("/api/players/2000001007").param("season", "2026")
                        .param("asOf", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryPosition").value("RW"))
                .andExpect(jsonPath("$.eligiblePositions[1]").value("RM"))
                .andExpect(jsonPath("$.positionStatus").value("VERIFIED"));
        mvc.perform(get("/api/players/2000001023").param("season", "2026")
                        .param("asOf", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryPosition").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.eligiblePositions").isEmpty())
                .andExpect(jsonPath("$.positionStatus").value("MISSING"));
    }
}
