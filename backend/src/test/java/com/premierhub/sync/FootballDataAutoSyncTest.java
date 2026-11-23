package com.premierhub.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:football-data-auto-sync-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "premierhub.football-data.auto-sync.enabled=false"
})
class FootballDataAutoSyncTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FootballDataSync realSync;
    @Autowired FootballDataAutoSync springManagedAutoSync;
    @Autowired ObjectMapper mapper;
    @MockitoBean FootballDataClient client;

    private MutableClock clock;

    @BeforeEach
    void resetSeason2026Fixtures() {
        clock = new MutableClock(Instant.parse("2026-11-23T12:00:00Z"));
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures WHERE league_id=39 AND season_year=2026");
        jdbc.update("DELETE FROM standings WHERE league_id=39 AND season_year=2026");
        jdbc.update("DELETE FROM season_clubs WHERE league_id=39 AND season_year=2026");
        jdbc.update("DELETE FROM football_data_teams");
        jdbc.update("DELETE FROM clubs WHERE id >= 1000000000");
        jdbc.update("INSERT INTO seasons (league_id, season_year) SELECT 39, 2026 WHERE NOT EXISTS "
                + "(SELECT 1 FROM seasons WHERE league_id=39 AND season_year=2026)");
        jdbc.update("INSERT INTO clubs (id, name, city) VALUES (1000000057, 'Arsenal FC', NULL)");
        jdbc.update("INSERT INTO clubs (id, name, city) VALUES (1000000067, 'Newcastle United FC', NULL)");
        jdbc.update("INSERT INTO football_data_teams (provider_id, club_id) VALUES (57, 1000000057)");
        jdbc.update("INSERT INTO football_data_teams (provider_id, club_id) VALUES (67, 1000000067)");
        jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) VALUES (39, 2026, 1000000057)");
        jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) VALUES (39, 2026, 1000000067)");
    }

    @Test
    void usesSixHourIntervalWhenNoMatchIsInWindow() throws Exception {
        // Upcoming fixture is 2 days away; past fixture is already FINISHED 24 hours ago.
        insertFixture(560640, "FINISHED", "FINISHED", "2026-11-22T12:00:00Z");
        insertFixture(560641, "SCHEDULED", "TIMED", "2026-11-25T20:00:00Z");

        FootballDataSync mockSync = mock(FootballDataSync.class);
        when(mockSync.sync()).thenReturn(new FootballDataSync.Result(0, 0, 0, 20, 380, 20));
        FootballDataAutoSync autoSync = newAutoSync(mockSync, true);

        assertFalse(autoSync.isMatchdayWindow(clock.instant()));
        assertEquals(Duration.ofHours(6), autoSync.currentInterval(clock.instant()));

        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());
        verify(mockSync, times(1)).sync();

        clock.advance(Duration.ofHours(5).plusMinutes(59));
        assertEquals(FootballDataAutoSync.TickOutcome.NOT_DUE, autoSync.runIfDue());
        verify(mockSync, times(1)).sync();

        clock.advance(Duration.ofMinutes(1));
        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());
        verify(mockSync, times(2)).sync();
    }

    @Test
    void usesTenMinuteIntervalDuringKickoffWindowOrOverdueUnfinishedMatch() throws Exception {
        // Case A: kickoff 10 minutes from now (inside [now - 4h, now + 15m]).
        insertFixture(560650, "SCHEDULED", "TIMED", "2026-11-23T12:10:00Z");

        FootballDataSync mockSync = mock(FootballDataSync.class);
        when(mockSync.sync()).thenReturn(new FootballDataSync.Result(0, 0, 1, 20, 380, 20));
        FootballDataAutoSync autoSync = newAutoSync(mockSync, true);

        assertTrue(autoSync.isMatchdayWindow(clock.instant()));
        assertEquals(Duration.ofMinutes(10), autoSync.currentInterval(clock.instant()));
        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());

        clock.advance(Duration.ofMinutes(9));
        assertEquals(FootballDataAutoSync.TickOutcome.NOT_DUE, autoSync.runIfDue());

        clock.advance(Duration.ofMinutes(1));
        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());
        verify(mockSync, times(2)).sync();

        // Case B: kickoff was 6 hours ago (outside 4h window), still SCHEDULED/LIVE -> still 10-minute cadence.
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures WHERE league_id=39 AND season_year=2026");
        insertFixture(560651, "SCHEDULED", "TIMED", "2026-11-23T06:00:00Z");
        assertTrue(autoSync.isMatchdayWindow(clock.instant()));
        assertEquals(Duration.ofMinutes(10), autoSync.currentInterval(clock.instant()));

        jdbc.update("UPDATE fixtures SET status='LIVE' WHERE id=1000560651");
        jdbc.update("UPDATE football_data_fixtures SET provider_status='IN_PLAY' WHERE provider_id=560651");
        assertTrue(autoSync.isMatchdayWindow(clock.instant()));

        // Once FINISHED and outside the 4h post-kickoff window -> back to 6-hour cadence.
        jdbc.update("UPDATE fixtures SET status='FINISHED' WHERE id=1000560651");
        jdbc.update("UPDATE football_data_fixtures SET provider_status='FINISHED' WHERE provider_id=560651");
        assertFalse(autoSync.isMatchdayWindow(clock.instant()));
        assertEquals(Duration.ofHours(6), autoSync.currentInterval(clock.instant()));
    }

    @Test
    void skipsOverlappingRunWhileSyncIsInProgress() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();

        FootballDataSync mockSync = mock(FootballDataSync.class);
        when(mockSync.sync()).thenAnswer(inv -> {
            calls.incrementAndGet();
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return new FootballDataSync.Result(0, 0, 0, 20, 380, 20);
        });
        FootballDataAutoSync autoSync = newAutoSync(mockSync, true);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<FootballDataAutoSync.TickOutcome> firstRun = pool.submit(autoSync::runIfDue);
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            assertEquals(FootballDataAutoSync.TickOutcome.SKIPPED_RUNNING, autoSync.runIfDue());
            assertEquals(1, calls.get());

            release.countDown();
            assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, firstRun.get(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void appliesExponentialBackoffOn429AndNetworkErrorsAndResetsOnRecovery() throws Exception {
        insertFixture(560660, "SCHEDULED", "TIMED", "2026-11-23T12:05:00Z");

        FootballDataSync mockSync = mock(FootballDataSync.class);
        when(mockSync.sync())
                .thenThrow(new IOException("football-data.org matches returned HTTP 429; no data imported, no retry"))
                .thenThrow(new IOException("football-data.org matches returned HTTP 429; no data imported, no retry"))
                .thenThrow(new IOException("Connection reset"))
                .thenThrow(new IOException("football-data.org standings returned HTTP 429; no data imported, no retry"))
                .thenReturn(new FootballDataSync.Result(0, 0, 1, 20, 380, 20));

        FootballDataAutoSync autoSync = newAutoSync(mockSync, true);

        // Attempt 1 fails -> 10m backoff
        assertEquals(FootballDataAutoSync.TickOutcome.FAILED, autoSync.runIfDue());
        assertEquals(1, autoSync.consecutiveFailures());
        assertEquals(Duration.ofMinutes(10), autoSync.currentInterval(clock.instant()));

        clock.advance(Duration.ofMinutes(9));
        assertEquals(FootballDataAutoSync.TickOutcome.NOT_DUE, autoSync.runIfDue());
        clock.advance(Duration.ofMinutes(1));

        // Attempt 2 fails -> 20m backoff
        assertEquals(FootballDataAutoSync.TickOutcome.FAILED, autoSync.runIfDue());
        assertEquals(2, autoSync.consecutiveFailures());
        assertEquals(Duration.ofMinutes(20), autoSync.currentInterval(clock.instant()));

        clock.advance(Duration.ofMinutes(19));
        assertEquals(FootballDataAutoSync.TickOutcome.NOT_DUE, autoSync.runIfDue());
        clock.advance(Duration.ofMinutes(1));

        // Attempt 3 fails -> 40m backoff
        assertEquals(FootballDataAutoSync.TickOutcome.FAILED, autoSync.runIfDue());
        assertEquals(3, autoSync.consecutiveFailures());
        assertEquals(Duration.ofMinutes(40), autoSync.currentInterval(clock.instant()));

        clock.advance(Duration.ofMinutes(39));
        assertEquals(FootballDataAutoSync.TickOutcome.NOT_DUE, autoSync.runIfDue());
        clock.advance(Duration.ofMinutes(1));

        // Attempt 4 fails -> capped at 60m backoff
        assertEquals(FootballDataAutoSync.TickOutcome.FAILED, autoSync.runIfDue());
        assertEquals(4, autoSync.consecutiveFailures());
        assertEquals(Duration.ofMinutes(60), autoSync.currentInterval(clock.instant()));

        clock.advance(Duration.ofMinutes(59));
        assertEquals(FootballDataAutoSync.TickOutcome.NOT_DUE, autoSync.runIfDue());
        clock.advance(Duration.ofMinutes(1));

        // Attempt 5 succeeds -> resets consecutiveFailures to 0
        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());
        assertEquals(0, autoSync.consecutiveFailures());
        verify(mockSync, times(5)).sync();
    }

    @Test
    void doesNotRunWhenFeatureFlagIsDisabled() throws Exception {
        FootballDataSync mockSync = mock(FootballDataSync.class);
        FootballDataAutoSync disabled = newAutoSync(mockSync, false);

        assertEquals(FootballDataAutoSync.TickOutcome.DISABLED, disabled.runIfDue());
        assertEquals(FootballDataAutoSync.TickOutcome.DISABLED, springManagedAutoSync.runIfDue());
        verifyNoInteractions(mockSync);
    }

    @Test
    void autoSyncWithRealImporterProtectsFinishedFixtureFromRegressing() throws Exception {
        // Clear minimal clubs so realSync can import a clean 20-team / 380-fixture season.
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures WHERE league_id=39 AND season_year=2026");
        jdbc.update("DELETE FROM standings WHERE league_id=39 AND season_year=2026");
        jdbc.update("DELETE FROM season_clubs WHERE league_id=39 AND season_year=2026");
        jdbc.update("DELETE FROM football_data_teams");
        jdbc.update("DELETE FROM clubs WHERE id >= 1000000000");

        ObjectNode[] finishedResponses = fullSeasonResponses("FINISHED", 3, 1);
        ObjectNode[] regressedResponses = fullSeasonResponses("TIMED", null, null);
        when(client.download())
                .thenReturn(FootballDataBatch.parse(finishedResponses[0], finishedResponses[1], finishedResponses[2]))
                .thenReturn(FootballDataBatch.parse(regressedResponses[0], regressedResponses[1], regressedResponses[2]));

        FootballDataAutoSync autoSync = newAutoSync(realSync, true);
        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());

        clock.advance(Duration.ofHours(6));
        assertEquals(FootballDataAutoSync.TickOutcome.SYNCED, autoSync.runIfDue());

        String status = jdbc.queryForObject("SELECT status FROM fixtures WHERE id=1001208021", String.class);
        Integer homeGoals = jdbc.queryForObject("SELECT home_goals FROM fixtures WHERE id=1001208021", Integer.class);
        Integer awayGoals = jdbc.queryForObject("SELECT away_goals FROM fixtures WHERE id=1001208021", Integer.class);
        assertEquals("FINISHED", status);
        assertEquals(3, homeGoals);
        assertEquals(1, awayGoals);
    }

    private FootballDataAutoSync newAutoSync(FootballDataSync syncService, boolean enabled) {
        return new FootballDataAutoSync(
                syncService,
                jdbc,
                clock,
                enabled,
                Duration.ofMinutes(10),
                Duration.ofHours(6),
                Duration.ofMinutes(15),
                Duration.ofHours(4),
                10,
                60
        );
    }

    private void insertFixture(int providerId, String status, String providerStatus, String kickoffUtc) {
        int internalId = 1_000_000_000 + providerId;
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (?, 39, 2026, 1000000057, 1000000067, 11, DATE '2026-11-23', ?, ?, NULL, NULL, 'hash', CURRENT_TIMESTAMP)
                """, internalId, status, status);
        jdbc.update("""
                INSERT INTO football_data_fixtures (provider_id, fixture_id, kickoff_utc, provider_status)
                VALUES (?, ?, ?, ?)
                """, providerId, internalId, kickoffUtc, providerStatus);
    }

    private ObjectNode[] fullSeasonResponses(String firstFixtureStatus, Integer homeGoals, Integer awayGoals) {
        ObjectNode teams = envelope();
        ObjectNode matches = envelope();
        ObjectNode standings = envelope();
        var teamRows = teams.putArray("teams");
        var rows = standings.putArray("standings").addObject().put("type", "TOTAL").putArray("table");
        for (int i = 0; i < 20; i++) {
            teamRows.addObject().put("id", 33 + i).put("name", "Club " + i);
            var row = rows.addObject().put("position", i + 1).put("playedGames", 0).put("won", 0)
                    .put("draw", 0).put("lost", 0).put("goalsFor", 0).put("goalsAgainst", 0)
                    .put("goalDifference", 0).put("points", 0);
            row.putObject("team").put("id", 33 + i);
        }
        var fixtures = matches.putArray("matches");
        for (int i = 0; i < 380; i++) {
            var fixture = fixtures.addObject().put("id", 1208021 + i).put("matchday", i / 10 + 1)
                    .put("status", i == 0 ? firstFixtureStatus : "TIMED")
                    .put("utcDate", "2026-11-28T15:00:00Z");
            fixture.putObject("season").put("startDate", "2026-08-21").put("endDate", "2027-05-30");
            fixture.putObject("homeTeam").put("id", 33 + i % 20);
            fixture.putObject("awayTeam").put("id", 33 + (i + 1) % 20);
            var score = fixture.putObject("score").putObject("fullTime");
            if (i == 0 && homeGoals != null && awayGoals != null) {
                score.put("home", homeGoals).put("away", awayGoals);
            } else {
                score.putNull("home").putNull("away");
            }
        }
        return new ObjectNode[]{teams, matches, standings};
    }

    private ObjectNode envelope() {
        ObjectNode node = mapper.createObjectNode();
        node.putObject("competition").put("id", 2021).put("code", "PL");
        node.putObject("season").put("startDate", "2026-08-21").put("endDate", "2027-05-30");
        return node;
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advance(Duration delta) {
            current.updateAndGet(instant -> instant.plus(delta));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
