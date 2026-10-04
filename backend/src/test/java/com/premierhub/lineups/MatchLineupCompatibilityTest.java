package com.premierhub.lineups;

import com.premierhub.roster.ManualRosterCsvReader;
import com.premierhub.service.FixtureEvidenceService;
import com.premierhub.service.FootballQueries;
import com.premierhub.service.MatchScoringService;
import com.premierhub.web.MatchController;
import com.premierhub.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class MatchLineupCompatibilityTest {
    private static final int FIXTURE = 1000560555;
    private static final int HOME = 1000000354;
    private static final int AWAY = 1000000065;
    private static final Path DATA = Path.of("data/gw2-first-two");
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private FootballQueries queries;
    private MockMvc mvc;

    @BeforeEach void setup() throws Exception {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        queries = new FootballQueries(jdbc, new MatchScoringService(), new FixtureEvidenceService(jdbc, JsonMapper.builder().build()));
        mvc = MockMvcBuilders.standaloneSetup(new MatchController(queries)).setControllerAdvice(new GlobalExceptionHandler()).build();
        jdbc.update("INSERT INTO seasons VALUES (39,2026)");
        jdbc.update("INSERT INTO clubs (id,name) VALUES (?,'Crystal Palace FC'),(?,'Manchester City FC')", HOME, AWAY);
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,?),(39,2026,?)", HOME, AWAY);
        jdbc.update("INSERT INTO fixtures (id,league_id,season_year,home_club_id,away_club_id,gameweek,match_date,status,provider_status,payload_hash,synced_at) "
                + "VALUES (?,39,2026,?,?,2,DATE '2026-08-28','FINISHED','FT',?,CURRENT_TIMESTAMP)", FIXTURE, HOME, AWAY, "0".repeat(64));
        Map<Integer, Integer> clubs = new HashMap<>();
        for (var row : new ManualRosterCsvReader().read(DATA.resolve("manual-players-2026-GW2-first-two.csv"))) {
            if (row.clubId() != HOME && row.clubId() != AWAY) continue;
            clubs.put(row.playerId(), row.clubId());
            jdbc.update("INSERT INTO players VALUES (?,?)", row.playerId(), row.name());
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,?,?,?)", row.playerId(), row.clubId(), row.position());
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?,?,?)", row.playerId(), row.clubId(), Date.valueOf(row.startDate()), Date.valueOf(row.endDate()));
        }
        for (String line : Files.readAllLines(DATA.resolve("manual-match-stats-2026-GW2-first-two.csv")).subList(1,81)) {
            String[] row = line.split(",", -1);
            if (Integer.parseInt(row[1]) != FIXTURE) continue;
            int id = Integer.parseInt(row[2]);
            jdbc.update("INSERT INTO manual_fixture_player_stats (fixture_id,player_id,league_id,season_year,club_id,participation_status,rating,fantasy_points) "
                    + "VALUES (?,?,39,2026,?,?,?,?)", FIXTURE, id, clubs.get(id), row[3], row[4].isEmpty() ? null : new BigDecimal(row[4]), row[3].equals("DID_NOT_PLAY") ? BigDecimal.ZERO : row[4].isEmpty() ? null : new BigDecimal(row[4]));
        }
    }

    @AfterEach void close() { database.shutdown(); }

    private JsonNode detail() throws Exception {
        var response = mvc.perform(get("/api/matches/" + FIXTURE + "/details?season=2026")).andReturn().getResponse();
        assertEquals(200, response.getStatus(), response.getContentAsString());
        return JsonMapper.builder().build().readTree(response.getContentAsString());
    }

    @Test void legacySchemaWithoutSourceNoteReturns200AndPreservesSqlDefaults() throws Exception {
        jdbc.execute("DROP TABLE club_season_formations");
        jdbc.execute("CREATE TABLE club_season_formations (league_id INTEGER,season_year INTEGER,club_id INTEGER,default_formation VARCHAR(20),updated_on DATE,scope_from_gw INTEGER,scope_to_gw INTEGER,verified_matches INTEGER,formation_counts TEXT,fixture_ids TEXT)");
        jdbc.update("INSERT INTO club_season_formations VALUES (39,2026,?,'5-4-1',DATE '2026-10-04',1,5,1,'5-4-1=1','1000560555')", HOME);
        var before = jdbc.queryForList("SELECT * FROM club_season_formations");
        JsonNode result = detail();
        assertEquals("5-4-1", result.path("homeLineup").path("formation").asText());
        assertEquals("VERIFIED", result.path("homeLineup").path("startersStatus").asText());
        assertEquals(before, jdbc.queryForList("SELECT * FROM club_season_formations"));
        assertEquals(20, result.path("homePlayers").size());
    }

    @Test void absentMetadataUsesPackagedUserDefaultsAndExactPreviouslyCollectedRolesWithoutWrites() throws Exception {
        jdbc.execute("DROP TABLE fixture_lineup_players");
        jdbc.execute("DROP TABLE fixture_lineups");
        jdbc.execute("DROP TABLE club_season_formations");
        var before = jdbc.queryForList("SELECT * FROM manual_fixture_player_stats ORDER BY player_id");
        JsonNode result = detail();
        assertEquals("3-4-3", result.path("homeLineup").path("formation").asText());
        assertEquals("4-2-3-1", result.path("awayLineup").path("formation").asText());
        for (String side : new String[] {"homeLineup", "awayLineup"}) {
            JsonNode lineup = result.path(side);
            assertEquals("CLUB_DEFAULT", lineup.path("formationSource").asText());
            assertTrue(lineup.path("formationSourceNote").asText().contains("User confirmed"));
            assertEquals(0, lineup.path("verifiedMatches").asInt());
            assertEquals("VERIFIED", lineup.path("startersStatus").asText());
            int starters = 0;
            for (JsonNode player : lineup.path("players")) if (player.path("role").asText().equals("STARTER")) starters++;
            assertEquals(11, starters);
        }
        assertEquals(before, jdbc.queryForList("SELECT * FROM manual_fixture_player_stats ORDER BY player_id"));
    }

    @Test void nullSeasonPositionAndIncompleteParticipationDoNotCrashOrInventStarters() throws Exception {
        jdbc.update("UPDATE player_season_stats SET position=NULL WHERE player_id=2000020100");
        JsonNode result = detail();
        for (JsonNode player : result.path("homePlayers")) if (player.path("playerId").asInt() == 2000020100) assertTrue(player.path("position").isNull());
        jdbc.update("UPDATE manual_fixture_player_stats SET participation_status='DID_NOT_PLAY',rating=NULL WHERE player_id=2000020100");
        result = detail();
        assertEquals("INCOMPLETE", result.path("homeLineup").path("startersStatus").asText());
    }

    @Test void failuresInRequiredStatisticsAreNotSilentlyConvertedIntoEmptyLineups() {
        jdbc.execute("DROP TABLE manual_fixture_player_stats");
        assertThrows(BadSqlGrammarException.class, () -> queries.matchDetail(FIXTURE,2026));
    }
}
