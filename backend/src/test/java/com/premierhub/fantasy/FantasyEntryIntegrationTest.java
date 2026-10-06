package com.premierhub.fantasy;

import com.premierhub.service.FantasyLineupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:fantasy-entry;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa","spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=","PREMIERHUB_GOOGLE_CLIENT_SECRET=","premierhub.auth-proxy.enabled=false",
        "logging.level.root=WARN","debug=false"
})
@AutoConfigureMockMvc
class FantasyEntryIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FantasyEntryService service;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean(name="fantasyGameweekClock") Clock clock;
    @MockitoSpyBean FantasyEntryRepository entries;
    @MockitoSpyBean GameweekRepository gameweeks;
    @MockitoSpyBean FantasyLineupService validator;
    final AtomicReference<Instant> time=new AtomicReference<>();
    static final Instant DEADLINE=Instant.parse("2026-10-08T17:00:00Z");
    final Map<String,Integer> picks=new LinkedHashMap<>();

    @BeforeEach void setup() {
        for(String table:List.of("fantasy_submitted_picks","fantasy_draft_picks","fantasy_entries","fantasy_deadline_changes",
                "fantasy_gameweeks","player_eligible_positions","player_specific_positions","manual_player_memberships",
                "player_season_profiles","player_season_stats","players","accounts","clubs","seasons")) jdbc.update("DELETE FROM "+table);
        time.set(Instant.parse("2026-10-05T00:00:00Z"));when(clock.instant()).thenAnswer(invocation -> time.get());
        jdbc.update("INSERT INTO accounts VALUES (101,'a@example.test','Player A',NULL,'USER',CURRENT_TIMESTAMP),(102,'b@example.test','Player B',NULL,'USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO seasons VALUES (39,2026),(39,2024)");
        for(int club=1;club<=4;club++) jdbc.update("INSERT INTO clubs (id,name) VALUES (?,?)",club,"Club "+club);
        List<String> positions=List.of("LW","ST","RW","CAM","CM","CM","LB","CB","CB","RB","GK","LW");
        List<String> keys=List.of("0-0","0-1","0-2","1-0","2-0","2-1","3-0","3-1","3-2","3-3","4-0");
        picks.clear();
        for(int i=0;i<positions.size();i++) {
            int id=i+1,club=i==11?1:i%4+1;String position=positions.get(i);
            jdbc.update("INSERT INTO players VALUES (?,?)",id,"Player "+id);
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,?,?,'FORWARD')",id,club);
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?, '2026-07-01',NULL)",id,club);
            jdbc.update("INSERT INTO player_season_profiles (league_id,season_year,player_id,club_id,fc27_overall) VALUES (39,2026,?,?,78)",id,club);
            jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,?)",id,position);
            jdbc.update("INSERT INTO player_eligible_positions VALUES (39,2026,?,?)",id,position);
            if(i<11)picks.put(keys.get(i),id);
        }
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,goals) VALUES (39,2024,1,1,10)");
        for(int gw:List.of(6,7)) jdbc.update("""
                INSERT INTO fantasy_gameweeks VALUES (2026,?,?,'2026-10-05 00:00:00',901,'2026-10-10 11:30:00','OPEN',NULL,1,'2026-10-05 00:00:00','2026-10-05')
                """,gw,GameweekRepository.timestamp(DEADLINE));
    }
    private FantasyEntryService.Mine save(Map<String,Integer> choices,long version,boolean submit) {
        return service.save(101,6,"4-2-1-3",choices,version,submit);
    }
    private Map<String,Integer> alternate() {var copy=new LinkedHashMap<>(picks);copy.put("0-0",12);return copy;}

    @Test void incompleteDraftSubmitEditAndResubmitAreSeparateAndVersioned() {
        assertNull(service.read(101,6).submitted());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_entries",Integer.class));
        var draft=save(Map.of("0-0",1),0,false);
        assertEquals(1,draft.version());assertNull(draft.submitted());assertEquals(1,draft.draft().picks().size());
        var submitted=save(picks,1,true);
        assertEquals(858,submitted.submitted().totalOvr());assertEquals(11,submitted.submitted().players().size());
        var edited=save(alternate(),2,false);
        assertEquals(3,edited.version());assertEquals(2,edited.submitted().version());
        assertEquals(1,edited.submitted().picks().get("0-0"));assertEquals(12,edited.draft().picks().get("0-0"));
        var resubmitted=save(alternate(),3,true);
        assertEquals(4,resubmitted.submitted().version());assertEquals(12,resubmitted.submitted().picks().get("0-0"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_entries",Integer.class));
        assertEquals(11,jdbc.queryForObject("SELECT COUNT(*) FROM fantasy_submitted_picks",Integer.class));
        assertEquals(10,jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2024",Integer.class));
        assertNull(service.read(101,7).draft());assertNull(service.read(102,6).submitted());
    }
    @Test void invalidResubmissionsKeepPreviousTeamAndVersion() {
        save(picks,0,true);
        var duplicate=new LinkedHashMap<>(picks);duplicate.put("0-0",2);
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(duplicate,1,true));
        var wrongSlot=new LinkedHashMap<>(picks);wrongSlot.remove("0-0");wrongSlot.put("9-9",1);
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(wrongSlot,1,true));
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(Map.of("0-0",1),1,true));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=NULL WHERE player_id=1");
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(picks,1,true));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=99");
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(picks,1,true));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=78");
        jdbc.update("DELETE FROM player_eligible_positions WHERE player_id=1");
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(picks,1,true));
        assertEquals(1,service.read(101,6).version());assertEquals(picks,service.read(101,6).submitted().picks());
    }
    @Test void savesExactly910AndRejects911WithoutReplacingSnapshot() {
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=82 WHERE season_year=2026");
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=90 WHERE player_id=1 AND season_year=2026");
        var original=save(picks,0,true).submitted();
        assertEquals(910,original.totalOvr());
        assertEquals(910,jdbc.queryForObject("SELECT submitted_total_ovr FROM fantasy_entries WHERE account_id=101",Integer.class));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=91 WHERE player_id=1 AND season_year=2026");
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(picks,1,true));
        assertEquals(original,service.read(101,6).submitted());
        assertEquals(1,service.read(101,6).version());
    }
    @Test void draftStillRejectsWrongPermissionMissingOvrAndClubLimitButAllowsEmpty() {
        assertNotNull(save(Map.of(),0,false).draft());
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(Map.of("0-0",2),1,false));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=NULL WHERE player_id=1");
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(Map.of("0-0",1),1,false));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=78 WHERE player_id=1");
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,2,1,'FORWARD')");
        jdbc.update("DELETE FROM manual_player_memberships WHERE player_id=2");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,2,1,'2026-07-01',NULL)");
        jdbc.update("INSERT INTO player_season_profiles (league_id,season_year,player_id,club_id,fc27_overall) VALUES (39,2026,2,1,78)");
        assertThrows(FantasyEntryService.InvalidLineup.class,()->save(picks,1,true));
        assertNull(service.read(101,6).submitted());
    }
    @Test void snapshotIsIndependentOfLaterProfilesPositionsMembershipAndNames() {
        var original=save(picks,0,true).submitted();
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=50");
        jdbc.update("DELETE FROM player_eligible_positions");
        jdbc.update("UPDATE players SET name='Changed name'");
        jdbc.update("UPDATE clubs SET name='Changed club'");
        jdbc.update("UPDATE manual_player_memberships SET end_date='2026-10-02'");
        assertEquals(original,service.read(101,6).submitted());
        assertEquals(78,original.players().get(0).ovr());
        assertTrue(original.players().stream().anyMatch(p -> p.requiredPosition().equals("CB") && p.eligiblePositions().contains("CB")));
    }
    @Test void oldVersionNeverOverwritesNewerDraft() {
        save(picks,0,false);
        var failure=assertThrows(ResponseStatusException.class,()->save(Map.of(),0,false));
        assertEquals(409,failure.getStatusCode().value());assertEquals(picks,service.read(101,6).draft().picks());
    }
    @Test void concurrentFirstSubmissionsYieldOneSuccessOneConflict() throws Exception {
        var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Integer> submit=()->{start.await();try{save(picks,0,true);return 200;}catch(ResponseStatusException failure){return failure.getStatusCode().value();}};
        try {
            var a=executor.submit(submit);var b=executor.submit(submit);start.countDown();
            assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)).sorted().toList());
            assertEquals(1,service.read(101,6).version());assertEquals(11,service.read(101,6).submitted().players().size());
        } finally {executor.shutdownNow();}
    }
    @Test void exactDeadlineAndSlowValidationRejectWithoutChangingCommittedTeam() {
        time.set(DEADLINE.minusNanos(1));save(picks,0,true);
        time.set(DEADLINE);
        assertThrows(ResponseStatusException.class,()->save(alternate(),1,true));
        assertThrows(ResponseStatusException.class,()->save(Map.of(),1,false));
        time.set(DEADLINE.minusSeconds(1));
        doAnswer(invocation -> {var answer=invocation.callRealMethod();time.set(DEADLINE);return answer;})
                .when(validator).inspect(any(),eq(true),any(java.time.LocalDate.class));
        assertThrows(ResponseStatusException.class,()->save(alternate(),1,true));
        assertEquals(picks,service.read(101,6).submitted().picks());
    }
    @Test void writesCrossingDeadlineRollbackReplacedSnapshotAndDraft() {
        save(picks,0,true);
        time.set(DEADLINE.minusSeconds(1));
        doAnswer(invocation -> {invocation.callRealMethod();time.set(DEADLINE);return null;})
                .when(entries).replaceSubmitted(eq(101L),eq(6),anyList());
        assertThrows(ResponseStatusException.class,()->save(alternate(),1,true));
        var retained=service.read(101,6);assertEquals(1,retained.version());
        assertEquals(picks,retained.draft().picks());assertEquals(picks,retained.submitted().picks());
    }
    @Test void queuedRequestChecksClockAfterAcquiringLocks() throws Exception {
        save(picks,0,true);time.set(DEADLINE.minusSeconds(1));
        var executor=Executors.newFixedThreadPool(2);
        var held=new CountDownLatch(1);var release=new CountDownLatch(1);var attempted=new CountDownLatch(1);
        try {
            var holder=executor.submit(()->new TransactionTemplate(transactions).execute(status->{
                jdbc.queryForList("SELECT gameweek FROM fantasy_gameweeks WHERE season=2026 AND gameweek=6 FOR UPDATE");
                held.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException failure){Thread.currentThread().interrupt();throw new RuntimeException(failure);}return null;}));
            assertTrue(held.await(5,TimeUnit.SECONDS));
            doAnswer(invocation->{attempted.countDown();return invocation.callRealMethod();}).when(gameweeks).lock(6);
            var pending=executor.submit(()->save(alternate(),1,true));
            assertTrue(attempted.await(5,TimeUnit.SECONDS));assertFalse(pending.isDone());
            time.set(DEADLINE);release.countDown();holder.get(5,TimeUnit.SECONDS);
            var failure=assertThrows(ExecutionException.class,()->pending.get(5,TimeUnit.SECONDS));
            assertInstanceOf(ResponseStatusException.class,failure.getCause());
            assertEquals(picks,service.read(101,6).submitted().picks());
        } finally {release.countDown();executor.shutdownNow();}
    }
    @Test void privateApiUsesSessionOwnerAndCsrfWithNoOtherUsersDraftOrEmail() throws Exception {
        service.save(102,6,"4-2-1-3",Map.of("0-0",12),0,false);
        String path="/api/fantasy/2026/me/gameweeks/6";
        mvc.perform(get(path).header("X-PrismaXI-Account-ID",101)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user("a@example.test")).header("X-PrismaXI-Account-ID",101).param("user_id","102"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.accountId").value(101)).andExpect(jsonPath("$.draft").isEmpty()).andExpect(jsonPath("$.email").doesNotExist());
        mvc.perform(get(path).with(user("a@example.test")).header("X-PrismaXI-Account-ID",102))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_CHANGED"));
        mvc.perform(post(path+"/draft").with(user("a@example.test")).header("X-PrismaXI-Account-ID",101)
                .contentType("application/json").content("{\"formation\":\"4-2-1-3\",\"picks\":{},\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path+"/draft").with(user("a@example.test")).with(csrf()).header("X-PrismaXI-Account-ID",101)
                .contentType("application/json").content("{\"formation\":\"4-2-1-3\",\"picks\":{},\"expectedVersion\":0,\"user_id\":102,\"ovr\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value(101));
        assertEquals(Map.of("0-0",12),service.read(102,6).draft().picks());
        mvc.perform(post(path+"/submit").with(user("a@example.test")).with(csrf()).header("X-PrismaXI-Account-ID",101)
                .contentType("application/json").content("{\"formation\":\"4-2-1-3\",\"picks\":{},\"expectedVersion\":1}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.code").value("LINEUP_INVALID"));
        mvc.perform(get(path).with(user("a@example.test"))).andExpect(status().isBadRequest());
        assertThrows(ResponseStatusException.class,()->service.save(101,1,"4-2-1-3",picks,0,true));
    }
}
