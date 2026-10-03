package com.premierhub.service;

import com.premierhub.web.dto.FantasyLineupRequest;
import com.premierhub.web.dto.FantasyValidationResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class FantasyLineupServiceTest {
    private JdbcTemplate jdbc;
    private FantasyLineupService service;
    private final Map<String, Integer> picks = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:fantasy-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        service = new FantasyLineupService(new FootballQueries(jdbc, new MatchScoringService(),
                new FixtureEvidenceService(jdbc, new ObjectMapper())));
        jdbc.update("INSERT INTO seasons VALUES (39, 2024), (39, 2026)");
        for (int club = 1; club <= 4; club++) {
            jdbc.update("INSERT INTO clubs (id, name) VALUES (?, ?)", club, "Club " + club);
        }
        List<String> positions = List.of("LW", "ST", "RW", "CAM", "CM", "CM", "LB", "CB", "CB", "RB", "GK");
        List<String> keys = List.of("0-0", "0-1", "0-2", "1-0", "2-0", "2-1", "3-0", "3-1", "3-2", "3-3", "4-0");
        for (int index = 0; index < 11; index++) {
            int id = index + 1, club = index % 4 + 1;
            String position = positions.get(index);
            jdbc.update("INSERT INTO players VALUES (?, ?)", id, "Player " + id);
            jdbc.update("INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position) VALUES (39, 2026, ?, ?, 'FORWARD')", id, club);
            jdbc.update("INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date) VALUES (39, 2026, ?, ?, '2026-07-01')", id, club);
            jdbc.update("INSERT INTO player_season_profiles (league_id, season_year, player_id, club_id, fc27_overall) VALUES (39, 2026, ?, ?, 78)", id, club);
            jdbc.update("INSERT INTO player_specific_positions VALUES (39, 2026, ?, ?)", id, position);
            jdbc.update("INSERT INTO player_eligible_positions VALUES (39, 2026, ?, ?)", id, position);
            picks.put(keys.get(index), id);
        }
        jdbc.update("INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position, goals) VALUES (39, 2024, 1, 1, 'FORWARD', 10)");
    }

    private FantasyValidationResponse check(String formation) {
        return service.validate(new FantasyLineupRequest(formation, picks));
    }

    private void hasCode(String code, FantasyValidationResponse result) {
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals(code)), result.toString());
    }

    @Test
    void validatesStoredOvrPositionsAndKeepsOldStatsUnchanged() {
        var result = check("4-2-1-3");
        assertTrue(result.valid(), result.toString());
        assertEquals(858, result.totalOvr());
        assertEquals(10, jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2024", Integer.class));
        assertNull(jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2026 AND player_id=1", Integer.class));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=81 WHERE player_id=1");
        hasCode("OVR_LIMIT", check("4-2-1-3"));
    }

    @Test
    void alternateRmAndRbWorkForBroadForwardButRwAloneDoesNot() {
        picks.clear(); picks.put("1-3", 3);
        hasCode("POSITION", check("4-4-2"));
        jdbc.update("INSERT INTO player_eligible_positions VALUES (39, 2026, 3, 'RM')");
        assertFalse(check("4-4-2").issues().stream().anyMatch(issue -> issue.code().equals("POSITION")));
        jdbc.update("INSERT INTO player_eligible_positions VALUES (39, 2026, 3, 'RB')");
        picks.clear(); picks.put("2-3", 3);
        assertFalse(check("4-4-2").issues().stream().anyMatch(issue -> issue.code().equals("POSITION")));
    }

    @Test
    void rejectsWrongSlotsDuplicatesMissingDataAndExpiredMembership() {
        picks.put("0-0", 2);
        hasCode("POSITION", check("4-2-1-3"));
        hasCode("DUPLICATE", check("4-2-1-3"));
        picks.put("0-0", 1);
        jdbc.update("DELETE FROM player_eligible_positions WHERE player_id=1");
        hasCode("POSITION_MISSING", check("4-2-1-3"));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=NULL WHERE player_id=2");
        hasCode("OVR_MISSING", check("4-2-1-3"));
        jdbc.update("UPDATE manual_player_memberships SET end_date='2026-10-02' WHERE player_id=3");
        hasCode("ROSTER", check("4-2-1-3"));
    }

    @Test
    void clubLimitUsesCurrentDatabaseMembership() {
        jdbc.update("INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position) VALUES (39, 2026, 2, 1, 'FORWARD')");
        jdbc.update("DELETE FROM manual_player_memberships WHERE player_id=2");
        jdbc.update("INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date) VALUES (39, 2026, 2, 1, '2026-07-01')");
        jdbc.update("INSERT INTO player_season_profiles (league_id, season_year, player_id, club_id, fc27_overall) VALUES (39, 2026, 2, 1, 78)");
        hasCode("CLUB_LIMIT", check("4-2-1-3"));
    }

    @Test
    void formationsRequireExactSlotKeysAndElevenChoices() {
        for (String formation : List.of("4-3-3", "4-4-2", "3-5-2")) {
            hasCode("EMPTY_SLOT", check(formation));
            hasCode("POSITION", check(formation));
        }
        hasCode("FORMATION", check("5-4-1"));
        picks.put("9-9", 99);
        hasCode("SLOT", check("4-2-1-3"));
        hasCode("COUNT", check("4-2-1-3"));
    }
}
