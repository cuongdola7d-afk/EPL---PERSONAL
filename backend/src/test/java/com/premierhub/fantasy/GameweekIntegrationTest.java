package com.premierhub.fantasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fantasy-gameweek;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=", "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "premierhub.auth-proxy.enabled=false"
})
@AutoConfigureMockMvc
class GameweekIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired GameweekService service;
    @Autowired MockMvc mvc;
    @MockitoSpyBean GameweekRepository repository;
    @MockitoBean(name = "fantasyGameweekClock") Clock clock;
    private static final Instant FIRST = Instant.parse("2026-10-10T11:30:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-10-08T17:00:00Z");

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM fantasy_deadline_changes");
        jdbc.update("DELETE FROM fantasy_gameweeks");
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("MERGE INTO seasons KEY(league_id,season_year) VALUES (39,2026),(39,2024)");
        jdbc.update("MERGE INTO clubs KEY(id) VALUES (991,'Test Home',''),(992,'Test Away','')");
        jdbc.update("INSERT INTO accounts VALUES (91,'admin@example.test','Admin',NULL,'ADMIN',CURRENT_TIMESTAMP)");
        jdbc.update("MERGE INTO players KEY(id) VALUES (99001,'Roster test player')");
        jdbc.update("MERGE INTO player_season_stats (league_id,season_year,player_id,club_id,position) KEY(league_id,season_year,player_id,club_id) VALUES (39,2026,99001,991,'FORWARD')");
        jdbc.update("DELETE FROM manual_player_memberships WHERE player_id=99001");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,99001,991,'2026-07-01',NULL)");
        for (int i = 0; i < 10; i++) {
            jdbc.update("""
                    INSERT INTO fixtures VALUES (?,39,2026,991,992,6,'2026-10-10','SCHEDULED','TIMED',NULL,NULL,?,CURRENT_TIMESTAMP)
                    """, 900 + i, "a".repeat(64));
            jdbc.update("INSERT INTO football_data_fixtures VALUES (?,?,?,'TIMED')", 900 + i, 900 + i, FIRST.plusSeconds(i * 3600).toString());
        }
        jdbc.update("INSERT INTO fixtures VALUES (9900,39,2024,991,992,6,'2024-10-01','FINISHED','FINISHED',2,1,?,CURRENT_TIMESTAMP)", "b".repeat(64));
        at(Instant.parse("2026-10-05T00:00:00Z"));
    }

    private void at(Instant value) { when(clock.instant()).thenReturn(value); }
    private void publish() { service.publishDeadline(6, 91, "Công bố deadline thử local"); }

    @Test void usesVietnamCalendarRatherThanTwentyFourHoursBeforeKickoff() {
        assertEquals(DEADLINE, GameweekService.calculateDeadline(FIRST));
        assertEquals("2026-10-09T00:00+07:00[Asia/Ho_Chi_Minh]", DEADLINE.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString());
        // Friday UTC evening is Saturday in Vietnam; the previous local date is Friday.
        assertEquals(DEADLINE, GameweekService.calculateDeadline(Instant.parse("2026-10-09T19:00:00Z")));
        assertEquals(Instant.parse("2026-10-07T17:00:00Z"),
                GameweekService.calculateDeadline(Instant.parse("2026-10-09T10:00:00Z")));
        assertNotEquals(FIRST.minusSeconds(86400), DEADLINE);
    }

    @Test void locksBeforeAtAndAfterDeadlineWithoutUpdatingStoredOpenRow() {
        publish();
        at(DEADLINE.minusNanos(1));
        assertEquals(GameweekService.Status.OPEN, service.info(6).status());
        assertDoesNotThrow(() -> service.requireOpen(6));
        at(DEADLINE);
        assertEquals(GameweekService.Status.LOCKED, service.info(6).status());
        assertFalse(service.info(6).canEdit());
        assertThrows(ResponseStatusException.class, () -> service.requireOpen(6));
        at(DEADLINE.plusSeconds(1));
        assertEquals(GameweekService.Status.LOCKED, service.info(6).status());
        assertEquals("OPEN", jdbc.queryForObject("SELECT workflow_status FROM fantasy_gameweeks", String.class));
    }

    @Test void finishedFixturesNeverPublishFantasyResults() {
        publish();
        at(FIRST);
        assertEquals(GameweekService.Status.AWAITING_RESULTS, service.info(6).status());
        jdbc.update("UPDATE fixtures SET status='FINISHED',home_goals=1,away_goals=0");
        at(FIRST.plusSeconds(86400));
        assertEquals(GameweekService.Status.AWAITING_RESULTS, service.info(6).status());
        assertNull(jdbc.queryForObject("SELECT results_published_at FROM fantasy_gameweeks", java.sql.Timestamp.class));
        jdbc.update("UPDATE fantasy_gameweeks SET workflow_status='PUBLISHED',results_published_at=?", GameweekRepository.timestamp(clock.instant()));
        assertEquals(GameweekService.Status.PUBLISHED, service.info(6).status());
        assertFalse(service.info(6).canEdit());
    }

    @Test void databaseReadsCrossingDeadlineCannotReturnAnOldOpenDecision() {
        publish();
        at(DEADLINE.minusSeconds(1));
        doAnswer(invocation -> {
            at(DEADLINE);
            return invocation.callRealMethod();
        }).when(repository).fixtures();
        var overview = service.overview();
        assertEquals(DEADLINE, overview.serverTimeUtc());
        assertEquals(GameweekService.Status.LOCKED, overview.gameweeks().get(5).status());
        assertThrows(ResponseStatusException.class, () -> service.requireOpen(6));
    }

    @Test void scheduleChangeAfterPublicationDoesNotMoveDeadline() {
        publish();
        jdbc.update("UPDATE football_data_fixtures SET kickoff_utc='2026-10-17T11:30:00Z'");
        var info = service.info(6);
        assertEquals(DEADLINE, info.deadlineUtc());
        assertEquals(Instant.parse("2026-10-15T17:00:00Z"), info.candidateDeadlineUtc());
        assertEquals(1, info.revision());
        assertEquals(1, info.deadlineChanges().size());
        assertThrows(ResponseStatusException.class, this::publish);
    }

    @Test void adminAdjustmentIsAuditedVersionedAndCannotReopenLockedRound() {
        publish();
        Instant publication = service.info(6).deadlinePublishedAt();
        Instant revised = DEADLINE.minusSeconds(86400);
        assertThrows(ResponseStatusException.class, () -> service.adjustDeadline(6, revised, 1, 91, " "));
        var view = service.adjustDeadline(6, revised, 1, 91, "Điều chỉnh theo thông báo quản trị");
        assertEquals(publication, view.deadlinePublishedAt());
        assertEquals(2, view.revision());
        assertEquals(DEADLINE, view.deadlineChanges().get(1).oldDeadlineUtc());
        assertEquals(revised, view.deadlineChanges().get(1).newDeadlineUtc());
        assertEquals(91L, jdbc.queryForObject("SELECT changed_by FROM fantasy_deadline_changes WHERE revision=2", Long.class));
        assertThrows(ResponseStatusException.class, () -> service.adjustDeadline(6, DEADLINE, 1, 91, "Phiên bản cũ"));
        at(revised);
        assertThrows(ResponseStatusException.class, () -> service.adjustDeadline(6, DEADLINE, 2, 91, "Muốn mở lại"));
        assertEquals(revised, service.info(6).deadlineUtc());
    }

    @Test void unconfiguredMissingKickoffAndReplayCannotParticipate() {
        var candidate = service.info(6);
        assertFalse(candidate.configured());
        assertNull(candidate.deadlineUtc());
        assertEquals(DEADLINE, candidate.candidateDeadlineUtc());
        assertFalse(candidate.canEdit());
        assertThrows(ResponseStatusException.class, () -> service.requireOpen(6));
        assertThrows(ResponseStatusException.class, () -> service.publishDeadline(1, 91, "Replay"));
        jdbc.update("UPDATE football_data_fixtures SET kickoff_utc='2026-10-10' WHERE fixture_id=900");
        assertFalse(service.info(6).scheduleComplete());
        assertNull(service.info(6).candidateDeadlineUtc());
        assertThrows(ResponseStatusException.class, this::publish);
        jdbc.update("DELETE FROM football_data_fixtures WHERE fixture_id=909");
        jdbc.update("DELETE FROM fixtures WHERE id=909");
        assertThrows(ResponseStatusException.class, this::publish);
    }

    @Test void initialPublicationOfExpiredRoundDoesNotOpenIt() {
        at(DEADLINE.plusSeconds(1));
        publish();
        assertEquals(GameweekService.Status.LOCKED, service.info(6).status());
        assertFalse(service.info(6).canEdit());
        assertThrows(ResponseStatusException.class, () -> service.requireOpen(6));
    }

    @Test void publicApiAndAdminRequireRoleCsrfAndRejectStateInjection() throws Exception {
        mvc.perform(get("/api/fantasy/2026/gameweeks")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.serverTimeUtc").value("2026-10-05T00:00:00Z"))
                .andExpect(jsonPath("$.recommendedGameweek").value(6));
        String path = "/api/fantasy/2026/admin/gameweeks/6/publish-deadline";
        mvc.perform(post(path).with(csrf()).contentType("application/json").content("{\"reason\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(user("user@example.test").roles("USER")).with(csrf())
                .contentType("application/json").content("{\"reason\":\"Test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("admin@example.test").roles("ADMIN"))
                .contentType("application/json").content("{\"reason\":\"Test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("admin@example.test").roles("ADMIN")).with(csrf())
                .contentType("application/json").content("{\"reason\":\"Test local\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/api/fantasy/2026/gameweeks/6")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameweeks[0].deadlineUtc").value(DEADLINE.toString()));
        mvc.perform(get("/api/fantasy/2026/gameweeks/39")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fantasy/2024/gameweeks")).andExpect(status().isUnauthorized());
    }

    @Test void transactionsRollbackWhenAuditFailsAndMigrationIsIdempotent() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.publishDeadline(6, 9999, "Không có account local này"));
        assertFalse(service.info(6).configured());
        publish();
        var datasource = jdbc.getDataSource();
        assertNotNull(datasource);
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("fantasy-gameweek-schema.sql")).execute(datasource);
        assertEquals(1, service.info(6).revision());
        assertEquals(1, service.info(6).deadlineChanges().size());
        assertEquals(11, jdbc.queryForObject("SELECT COUNT(*) FROM fixtures", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT home_goals FROM fixtures WHERE season_year=2024", Integer.class));
    }
}
