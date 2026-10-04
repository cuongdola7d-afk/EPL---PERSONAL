package com.premierhub.fantasy;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:fantasy-results;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=","PREMIERHUB_GOOGLE_CLIENT_SECRET=","premierhub.auth-proxy.enabled=false",
        "logging.level.root=WARN","debug=false"
})
@AutoConfigureMockMvc
class FantasyResultIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FantasyResultService service;
    @Autowired FantasyEvidenceImporter evidence;
    @Autowired MockMvc mvc;
    @MockitoBean(name="fantasyGameweekClock") Clock clock;
    @MockitoSpyBean FantasyResultRepository repository;
    @TempDir Path directory;
    final AtomicReference<Instant> now=new AtomicReference<>();
    Path stats,unrated,sources;
    static final BigDecimal EXPECTED=new BigDecimal("66.77");
    @BeforeEach void setup() throws Exception {
        for(String table:List.of("fantasy_team_results","fantasy_result_publications","fantasy_fixture_confirmations","fantasy_unrated_confirmations",
                "fantasy_submitted_picks","fantasy_draft_picks","fantasy_entries","fantasy_deadline_changes","fantasy_gameweeks",
                "fixture_lineup_players","fixture_lineups","manual_fixture_player_stats","football_data_fixtures","fixtures",
                "manual_player_memberships","player_season_profiles","player_eligible_positions","player_specific_positions",
                "player_season_stats","season_clubs","players","accounts","clubs","seasons")) jdbc.update("DELETE FROM "+table);
        now.set(Instant.parse("2026-10-20T00:00:00Z"));when(clock.instant()).thenAnswer(i->now.get());
        jdbc.update("INSERT INTO seasons VALUES (39,2026)");
        for(int club=1;club<=4;club++) {
            jdbc.update("INSERT INTO clubs (id,name) VALUES (?,?)",club,"Club "+club);
            jdbc.update("INSERT INTO season_clubs VALUES (39,2026,?)",club);
        }
        jdbc.update("INSERT INTO accounts VALUES (101,'admin@example.test','Admin',NULL,'ADMIN',CURRENT_TIMESTAMP),(102,'a@example.test','Player A',NULL,'USER',CURRENT_TIMESTAMP),(103,'b@example.test','Player B',NULL,'USER',CURRENT_TIMESTAMP),(104,'draft@example.test','Draft only',NULL,'USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO fantasy_gameweeks VALUES (2026,6,'2026-10-08 17:00:00','2026-10-05 00:00:00',601,'2026-10-10 11:30:00','OPEN',NULL,1,'2026-10-05 00:00:00','2026-10-05')");
        for(int id=1;id<=11;id++) player(id,(id-1)%4+1);
        fixture(601,1,2);fixture(602,3,4);
        List<String> slots=List.of("0-0","0-1","0-2","1-0","2-0","2-1","3-0","3-1","3-2","3-3","4-0");
        List<String> positions=List.of("LW","ST","RW","CAM","CM","CM","LB","CB","CB","RB","GK");
        for(long account:List.of(102L,103L)) {
            jdbc.update("INSERT INTO fantasy_entries VALUES (?,2026,6,1,'4-2-1-3','2026-10-06 00:00:00','4-2-1-3','2026-10-06 00:00:00',1,858)",account);
            for(int i=0;i<11;i++) jdbc.update("INSERT INTO fantasy_submitted_picks VALUES (?,2026,6,?,?,?,? ,?,?,78,?,?)",
                    account,slots.get(i),i+1,positions.get(i),i%4+1,"Player "+(i+1),"Club "+(i%4+1),positions.get(i),positions.get(i));
        }
        jdbc.update("INSERT INTO fantasy_entries VALUES (104,2026,6,1,'4-2-1-3','2026-10-06 00:00:00',NULL,NULL,NULL,NULL)");
        stats=directory.resolve("stats.csv");unrated=directory.resolve("confirmed-unrated.csv");sources=directory.resolve("sources.md");
        Files.writeString(sources,"Synthetic verified fixture data, no production imports.");
        files();evidence.importFiles(stats,unrated,sources);
    }
    private void player(int id,int club) {
        jdbc.update("INSERT INTO players VALUES (?,?)",id,"Player "+id);
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,?,?,'FORWARD')",id,club);
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?,'2026-07-01',NULL)",id,club);
    }
    private void fixture(int fixture,int home,int away) {
        jdbc.update("INSERT INTO fixtures VALUES (?,39,2026,?,?,6,'2026-10-10','FINISHED','FINISHED',1,0,?,CURRENT_TIMESTAMP)",fixture,home,away,"a".repeat(64));
        for(int club:List.of(home,away)) {
            jdbc.update("INSERT INTO fixture_lineups VALUES (?,?,39,2026,'4-3-3','2026-10-10','synthetic lineups','2026-10-10','synthetic roles')",fixture,club);
            int starters=0;
            for(int id=1;id<=11;id++) if((id-1)%4+1==club) {
                String role=id==1?"SUB_UNUSED":id==2?"SUB_USED":"STARTER";
                if(role.equals("STARTER")) starters++;
                match(fixture,id,club,role);
            }
            for(int i=0;starters<11;i++,starters++) {
                int id=club*100+i;
                if(jdbc.queryForObject("SELECT COUNT(*) FROM players WHERE id=?",Integer.class,id)==0) player(id,club);
                match(fixture,id,club,"STARTER");
            }
        }
    }
    private void match(int fixture,int id,int club,String role) {
        jdbc.update("INSERT INTO fixture_lineup_players (fixture_id,club_id,player_id,role) VALUES (?,?,?,?)",fixture,club,id,role);
        List<String> ratings=Arrays.asList(null,null,"6.15","7.01","8.23","7.17","6.44","8.01","9.11","7.99","6.66");
        BigDecimal rating=id<=11 ? (ratings.get(id-1)==null?null:new BigDecimal(ratings.get(id-1))) : new BigDecimal("7.00");
        boolean dnp=role.equals("SUB_UNUSED");
        jdbc.update("INSERT INTO manual_fixture_player_stats VALUES (?,?,39,2026,?,?,?,?,?,0,0,0,0)",fixture,id,club,
                dnp?"DID_NOT_PLAY":"PLAYED",rating,dnp?BigDecimal.ZERO:rating,dnp?0:role.equals("SUB_USED")?3:90);
    }
    private void files() throws Exception {
        StringBuilder csv=new StringBuilder(com.premierhub.manualstats.ManualMatchStatsCsvReader.HEADER+"\n");
        StringBuilder absent=new StringBuilder("fixture_id,player_id,name,minutes,rating,fantasy_points\n");
        for(int fixture:jdbc.queryForList("SELECT id FROM fixtures ORDER BY id",Integer.class))
            for(var row:repository.stats(fixture,false)) {
                csv.append("2026,").append(fixture).append(",").append(row.playerId()).append(",").append(row.status()).append(",")
                        .append(row.rating()==null?"":row.rating().toPlainString()).append(",").append(row.minutes()).append(",")
                        .append(row.goals()).append(",").append(row.assists()).append(",").append(row.yellowCards()).append(",").append(row.redCards()).append("\n");
                if(row.playerId()==2 && row.rating()==null) absent.append(fixture).append(",2,Player 2,3,,\n");
            }
        Files.writeString(stats,csv);Files.writeString(unrated,absent);
    }
    private FantasyResultService.Published publish() { return service.publish(6,0,101,"Publish verified local results",false); }
    @Test void decimalRatingsDnpAndConfirmedUnratedPublishOnlyStoredSubmittedTeams() {
        assertTrue(service.readiness(6).ready());
        assertEquals("AWAITING_RESULTS",service.mine(102,6).status());assertNull(service.mine(102,6).result());
        publish();
        var result=service.mine(102,6);
        assertEquals(EXPECTED,result.result().totalPoints());assertEquals(11,result.result().players().size());
        assertEquals("0.00",result.result().players().get(0).points().toPlainString());
        assertEquals("DID_NOT_PLAY",result.result().players().get(0).matches().get(0).reason());
        assertEquals("SOFASCORE_UNRATED_CONFIRMED",result.result().players().get(1).matches().get(0).reason());
        assertNull(jdbc.queryForObject("SELECT rating FROM manual_fixture_player_stats WHERE fixture_id=601 AND player_id=2",BigDecimal.class));
        assertNull(jdbc.queryForObject("SELECT fantasy_points FROM manual_fixture_player_stats WHERE fixture_id=601 AND player_id=2",BigDecimal.class));
        assertEquals("NOT_PARTICIPATING",service.mine(104,6).status());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_team_results",Integer.class));
    }
    @Test void unconfirmedNullAndMissingRowsBlockWithoutTemporaryPoints() {
        jdbc.update("UPDATE manual_fixture_player_stats SET rating=NULL WHERE fixture_id=602 AND player_id=3");
        var readiness=service.readiness(6);assertFalse(readiness.ready());
        assertTrue(readiness.issues().stream().anyMatch(i->Objects.equals(i.fixtureId(),602) && Objects.equals(i.playerId(),3) && i.code().equals("RATING_PENDING")));
        assertThrows(FantasyResultService.NotReady.class,this::publish);
        jdbc.update("DELETE FROM manual_fixture_player_stats WHERE fixture_id=601 AND player_id=5");
        assertTrue(service.readiness(6).issues().stream().anyMatch(i->Objects.equals(i.fixtureId(),601) && Objects.equals(i.playerId(),5) && i.code().equals("SELECTED_STAT_MISSING")));
        assertEquals("AWAITING_RESULTS",service.mine(102,6).status());assertNull(service.mine(102,6).result());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_result_publications",Integer.class));
    }
    @Test void deadlinePostponedFixtureAndUnconfirmedScopeRemainBlocked() {
        now.set(Instant.parse("2026-10-08T16:59:59Z"));
        assertTrue(service.readiness(6).issues().stream().anyMatch(i->i.code().equals("DEADLINE_PENDING")));
        now.set(Instant.parse("2026-10-20T00:00:00Z"));
        jdbc.update("UPDATE fixtures SET status='POSTPONED' WHERE id=602");
        assertThrows(FantasyResultService.NotReady.class,this::publish);
        jdbc.update("UPDATE fixtures SET status='FINISHED' WHERE id=602");
        jdbc.update("DELETE FROM fantasy_fixture_confirmations WHERE fixture_id=602");
        assertTrue(service.readiness(6).issues().stream().anyMatch(i->Objects.equals(i.fixtureId(),602) && i.code().equals("FIXTURE_UNCONFIRMED")));
        assertThrows(FantasyResultService.NotReady.class,this::publish);
    }
    @Test void repeatedPublicationIsIdempotentAndRecalculationUpdatesWholeGwWithHistory() throws Exception {
        publish();var repeat=publish();assertTrue(repeat.unchanged());assertEquals(1,repeat.version());
        jdbc.update("UPDATE manual_fixture_player_stats SET rating=9.23 WHERE fixture_id=601 AND player_id=5");
        assertThrows(FantasyResultService.NotReady.class,()->service.publish(6,1,101,"Correct rating source",true));
        files();evidence.importFiles(stats,unrated,sources);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,this::publish);
        assertEquals(2,service.publish(6,1,101,"Corrected verified SofaScore rating",true).version());
        for(long account:List.of(102L,103L)) {
            assertEquals(2,service.mine(account,6).version());
            assertEquals(EXPECTED.add(BigDecimal.ONE),service.mine(account,6).result().totalPoints());
        }
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_team_results",Integer.class));
        assertEquals(EXPECTED,repository.team(102,6,1).orElseThrow().totalPoints());
        var history=service.readiness(6).history();assertEquals(2,history.size());
        assertEquals(2,history.getFirst().version());assertEquals(101,history.getFirst().publishedBy());
        assertEquals("Corrected verified SofaScore rating",history.getFirst().reason());
        assertTrue(service.publish(6,1,101,"Repeat request after success",true).unchanged());
    }
    @Test void correctedRatedPlayerRetiresUnratedEvidenceWithoutChangingOldResults() throws Exception {
        publish();
        jdbc.update("UPDATE manual_fixture_player_stats SET rating=6.75 WHERE fixture_id=601 AND player_id=2");
        assertTrue(service.readiness(6).issues().stream().anyMatch(i->i.code().equals("UNRATED_CONFLICT")));
        files();evidence.importFiles(stats,unrated,sources);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_unrated_confirmations",Integer.class));
        assertTrue(service.readiness(6).ready());
        service.publish(6,1,101,"Verified rating replaces unrated confirmation",true);
        assertEquals(new BigDecimal("73.52"),service.mine(102,6).result().totalPoints());
        assertEquals(EXPECTED,repository.team(102,6,1).orElseThrow().totalPoints());
        var player=service.mine(102,6).result().players().stream().filter(p->p.playerId()==2).findFirst().orElseThrow();
        assertEquals("SOFASCORE_RATING",player.matches().getFirst().reason());
        assertNull(jdbc.queryForObject("SELECT fantasy_points FROM manual_fixture_player_stats WHERE fixture_id=601 AND player_id=2",BigDecimal.class));
    }
    @Test void failingOneTeamWriteRollsBackTheEntireNewVersion() throws Exception {
        publish();jdbc.update("UPDATE manual_fixture_player_stats SET rating=9.23 WHERE fixture_id=601 AND player_id=5");
        files();evidence.importFiles(stats,unrated,sources);
        doAnswer(invocation->{var participant=invocation.getArgument(2,FantasyResultRepository.Participant.class);
            if(participant.accountId()==103) throw new IllegalStateException("Synthetic second-team write failure");
            return invocation.callRealMethod();}).when(repository).team(eq(6),eq(2),any(FantasyResultRepository.Participant.class),any());
        assertThrows(IllegalStateException.class,()->service.publish(6,1,101,"Verified correction local",true));
        assertEquals(1,repository.latest(6).orElseThrow().version());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_team_results",Integer.class));
        for(long account:List.of(102L,103L)) assertEquals(EXPECTED,service.mine(account,6).result().totalPoints());
    }
    @Test void multipleMatchesAreAddedOncePerPlayerFixtureKey() throws Exception {
        fixture(603,1,2);files();evidence.importFiles(stats,unrated,sources);publish();
        var result=service.mine(102,6).result();
        assertEquals(new BigDecimal("99.27"),result.totalPoints());
        var player=result.players().stream().filter(p->p.playerId()==9).findFirst().orElseThrow();
        assertEquals(2,player.matches().size());assertEquals(new BigDecimal("18.22"),player.points());
    }
    @Test void evidenceReuseAndConflictsNeverRewriteRawStatistics() throws Exception {
        var repeat=evidence.importFiles(stats,unrated,sources);assertEquals(0,repeat.fixtureChanges());assertEquals(0,repeat.unratedChanges());
        String old=Files.readString(stats);Files.writeString(stats,old.replace("8.23","9.23"));
        assertThrows(IllegalArgumentException.class,()->evidence.importFiles(stats,unrated,sources));
        assertEquals(new BigDecimal("8.23"),jdbc.queryForObject("SELECT rating FROM manual_fixture_player_stats WHERE fixture_id=601 AND player_id=5",BigDecimal.class));
        assertTrue(service.readiness(6).ready());
    }
    @Test void existingUnratedHeaderVariantsRequireAnExplicitConfirmationFile() throws Exception {
        jdbc.update("DELETE FROM fantasy_unrated_confirmations");
        Files.writeString(unrated,"fixture_id,club,player_id,name,status,minutes\n601,Club 2,2,Player 2,PLAYED,3\n");
        assertEquals(1,evidence.importFiles(null,unrated,sources).unratedChanges());
        jdbc.update("DELETE FROM fantasy_unrated_confirmations");
        Files.writeString(unrated,"fixture_id,club,player_id,name,status,field,reason\n601,Club 2,2,Player 2,PLAYED,rating,User confirmed SofaScore unrated\n");
        assertEquals(1,evidence.importFiles(stats,unrated,sources).unratedChanges());
        assertTrue(service.readiness(6).ready());
        var pending=directory.resolve("ratings-needed.csv");Files.copy(unrated,pending);
        assertThrows(IllegalArgumentException.class,()->evidence.importFiles(null,pending,sources));
    }
    @Test void adminCsrfAndSessionOwnershipProtectPublicationAndPrivateResults() throws Exception {
        String admin="/api/fantasy/2026/admin/gameweeks/6",mine="/api/fantasy/2026/me/gameweeks/6/result";
        mvc.perform(get(admin+"/readiness")).andExpect(status().isUnauthorized());
        mvc.perform(get(admin+"/readiness").with(user("a@example.test"))).andExpect(status().isForbidden());
        mvc.perform(get(admin+"/readiness").with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ready").value(true));
        mvc.perform(get(mine).with(user("a@example.test")).header("X-PrismaXI-Account-ID",102))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AWAITING_RESULTS")).andExpect(jsonPath("$.result").isEmpty());
        String body="{\"expectedVersion\":0,\"reason\":\"Publish verified local\",\"totalPoints\":999,\"user_id\":103}";
        mvc.perform(post(admin+"/publish-results").with(user("admin@example.test").roles("ADMIN")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(admin+"/publish-results").with(user("a@example.test")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(admin+"/publish-results").with(user("admin@example.test").roles("ADMIN")).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(get(mine).with(user("a@example.test")).header("X-PrismaXI-Account-ID",102))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.result.totalPoints").value(66.77)).andExpect(jsonPath("$.result.players.length()").value(11)).andExpect(jsonPath("$.email").doesNotExist());
        mvc.perform(get(mine).with(user("a@example.test")).header("X-PrismaXI-Account-ID",103)).andExpect(status().isConflict());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.readiness(1));
    }
}
