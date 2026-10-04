package com.premierhub.fantasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:fantasy-roster;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=","PREMIERHUB_GOOGLE_CLIENT_SECRET=","premierhub.auth-proxy.enabled=false",
        "logging.level.root=WARN","debug=false"
})
@AutoConfigureMockMvc
class FantasyRosterReferenceTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FantasyEntryService service;
    @Autowired MockMvc mvc;
    @Autowired GameweekService deadlines;
    @MockitoBean(name="fantasyGameweekClock") Clock clock;
    final AtomicReference<Instant> time=new AtomicReference<>();
    final Map<String,Integer> picks=new LinkedHashMap<>();

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures");
        for(String table:List.of("fantasy_submitted_picks","fantasy_draft_picks","fantasy_entries","fantasy_deadline_changes",
                "fantasy_gameweeks","player_eligible_positions","player_specific_positions","manual_player_memberships",
                "player_season_profiles","player_season_stats","players","accounts","clubs","seasons")) jdbc.update("DELETE FROM "+table);
        time.set(Instant.parse("2026-10-05T00:00:00Z"));
        when(clock.instant()).thenAnswer(invocation -> time.get());
        jdbc.update("INSERT INTO accounts VALUES (101,'roster@example.test','Roster test',NULL,'USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO seasons VALUES (39,2026)");
        for(int club=1;club<=4;club++) jdbc.update("INSERT INTO clubs (id,name) VALUES (?,?)",club,"Club "+club);
        var positions=List.of("LW","ST","RW","CAM","CM","CM","LB","CB","CB","RB","GK","LW");
        var keys=List.of("0-0","0-1","0-2","1-0","2-0","2-1","3-0","3-1","3-2","3-3","4-0");
        picks.clear();
        for(int i=0;i<positions.size();i++) {
            int id=i+1,club=i==11?1:i%4+1;
            jdbc.update("INSERT INTO players VALUES (?,?)",id,"Player "+id);
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,?,?,'FORWARD')",id,club);
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?,'2026-07-01',NULL)",id,club);
            jdbc.update("INSERT INTO player_season_profiles (league_id,season_year,player_id,club_id,fc27_overall) VALUES (39,2026,?,?,78)",id,club);
            jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,?)",id,positions.get(i));
            jdbc.update("INSERT INTO player_eligible_positions VALUES (39,2026,?,?)",id,positions.get(i));
            if(i<11) picks.put(keys.get(i),id);
        }
        for(int gw:List.of(6,7)) jdbc.update("INSERT INTO fantasy_gameweeks VALUES (2026,?,'2026-10-08 17:00:00','2026-10-05 00:00:00',600,'2026-10-10 11:30:00','OPEN',NULL,1,'2026-10-05 00:00:00','2026-10-05')",gw);
        prepareRosterDates();
    }

    private void prepareRosterDates() {
        jdbc.update("UPDATE fantasy_gameweeks SET roster_as_of='2026-10-07' WHERE gameweek=7");
        jdbc.update("UPDATE manual_player_memberships SET end_date='2026-10-06' WHERE player_id=1");
        jdbc.update("UPDATE manual_player_memberships SET start_date='2026-10-06' WHERE player_id=12");
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures");
        for (int gw : java.util.List.of(8, 9)) {
            for (int i=0; i<10; i++) {
                int id=gw*100+i;
                jdbc.update("INSERT INTO fixtures VALUES (?,39,2026,1,2,?,'2026-10-24','SCHEDULED','TIMED',NULL,NULL,?,CURRENT_TIMESTAMP)", id, gw, "a".repeat(64));
                jdbc.update("INSERT INTO football_data_fixtures VALUES (?,?,?,'TIMED')", id, id,
                        Instant.parse("2026-10-24T11:30:00Z").plusSeconds(i*3600).toString());
            }
        }
    }

    @Test void rosterTwoGameweeksUseTheSameDatabaseDateForListAndValidator() throws Exception {
        mvc.perform(get("/api/fantasy/2026/gameweeks/6/players").param("asOf","2026-10-07"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.rosterAsOf").value("2026-10-05"))
                .andExpect(jsonPath("$.players[?(@.id == 1)]").isNotEmpty())
                .andExpect(jsonPath("$.players[?(@.id == 12)]").isEmpty());
        mvc.perform(get("/api/fantasy/2026/gameweeks/7/players"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rosterAsOf").value("2026-10-07"))
                .andExpect(jsonPath("$.players[?(@.id == 1)]").isEmpty())
                .andExpect(jsonPath("$.players[?(@.id == 12)]").isNotEmpty());
        assertEquals(858, service.save(101,6,"4-2-1-3",picks,0,true).submitted().totalOvr());
        assertThrows(FantasyEntryService.InvalidLineup.class, () -> service.save(101,7,"4-2-1-3",picks,0,true));
        var later=new LinkedHashMap<>(picks); later.put("0-0",12);
        assertEquals(12,service.save(101,7,"4-2-1-3",later,0,true).submitted().picks().get("0-0"));
        mvc.perform(post("/api/fantasy/2026/validate").param("gameweek","7")
                .contentType("application/json").content("{\"formation\":\"4-2-1-3\",\"picks\":{\"0-0\":1}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.issues[?(@.code == 'ROSTER')]").isNotEmpty());
    }

    @Test void rosterPublicationDefaultsToVietnamDayAndAllowsExplicitExistingDate() {
        time.set(Instant.parse("2026-10-05T18:00:00Z")); // Vietnam is already October 6.
        assertEquals(LocalDate.parse("2026-10-06"),deadlines.publishDeadline(8,101,"Open local GW").rosterAsOf());
        assertEquals(LocalDate.parse("2026-10-05"),deadlines.publishDeadline(9,101,"Choose existing roster",LocalDate.parse("2026-10-05")).rosterAsOf());
        time.set(Instant.parse("2026-10-07T00:00:00Z"));
        assertEquals(LocalDate.parse("2026-10-06"),deadlines.info(8).rosterAsOf());
        deadlines.adjustDeadline(8,Instant.parse("2026-10-22T16:00:00Z"),1,101,"Adjust only deadline");
        assertEquals(LocalDate.parse("2026-10-06"),deadlines.rosterAsOf(8));
    }

    @Test void rosterMissingDateOrRepublishingCannotReplaceTheFrozenReference() {
        assertThrows(ResponseStatusException.class, () -> deadlines.publishDeadline(8,101,"Unknown roster",LocalDate.parse("2025-01-01")));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_gameweeks WHERE gameweek=8",Integer.class));
        deadlines.publishDeadline(8,101,"Known roster",LocalDate.parse("2026-10-05"));
        assertThrows(ResponseStatusException.class, () -> deadlines.publishDeadline(8,101,"Change roster",LocalDate.parse("2026-10-07")));
        assertEquals(LocalDate.parse("2026-10-05"),deadlines.rosterAsOf(8));
    }

    @Test void rosterLaterDateAndProfileChangesNeverRevalidateAnExistingSnapshot() {
        var original=service.save(101,6,"4-2-1-3",picks,0,true).submitted();
        time.set(Instant.parse("2026-10-07T00:00:00Z"));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=40");
        jdbc.update("DELETE FROM player_eligible_positions");
        service.save(101,7,"4-2-1-3",Map.of(),0,false);
        assertEquals(original,service.read(101,6).submitted());
        assertEquals(LocalDate.parse("2026-10-05"),deadlines.rosterAsOf(6));
        assertEquals(858,service.read(101,6).submitted().totalOvr());
    }

    @Test void rosterLocalSchemaUpgradesLegacyGwsWithoutChangingExplicitDates() throws Exception {
        var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource(
                "jdbc:h2:mem:roster-upgrade;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","",true);
        var local=new org.springframework.jdbc.core.JdbcTemplate(source);
        local.execute("CREATE TABLE accounts (id BIGINT PRIMARY KEY)");
        var resource=new org.springframework.core.io.ClassPathResource("fantasy-gameweek-schema.sql");
        String schema=new String(resource.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        String original=schema.substring(0,schema.indexOf("-- Upgrade existing local H2"))
                .replace("    roster_as_of DATE NOT NULL,", "");
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ByteArrayResource(original.getBytes(java.nio.charset.StandardCharsets.UTF_8))).execute(source);
        local.update("INSERT INTO fantasy_gameweeks VALUES (2026,6,'2026-10-08 17:00:00','2026-10-04 18:00:00',600,'2026-10-10 11:30:00','OPEN',NULL,1,'2026-10-04 18:00:00')");
        var upgrade=new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(resource);
        upgrade.execute(source);
        assertEquals("2026-10-05",local.queryForObject("SELECT roster_as_of FROM fantasy_gameweeks",String.class));
        local.update("UPDATE fantasy_gameweeks SET roster_as_of='2026-10-03'");
        upgrade.execute(source);
        assertEquals("2026-10-03",local.queryForObject("SELECT roster_as_of FROM fantasy_gameweeks",String.class));
        source.destroy();
    }
}
