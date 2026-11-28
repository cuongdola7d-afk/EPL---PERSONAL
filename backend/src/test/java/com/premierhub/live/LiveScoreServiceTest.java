package com.premierhub.live;

import com.premierhub.fpl.FplMappingReader;
import com.premierhub.live.LiveMatchState.GoalEvent;
import com.premierhub.live.LiveMatchState.PlayerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:live-score-service-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "premierhub.live.scheduling.enabled=false"
})
@Transactional
class LiveScoreServiceTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.premierhub.service.FootballQueries footballQueries;

    private MutableClock clock;
    private StubHttpClient stubHttp;
    private FplLiveClient fplClient;
    private LiveStreamService streamService;
    private BroadcastRecorder recorder;
    private Map<Integer, FplMappingReader.TeamMapping> teamMap;
    private Map<Integer, FplMappingReader.PlayerMapping> playerMap;

    private static final int CHE_LEE_DB_ID = 1000560642;
    private static final int MCI_FUL_DB_ID = 1000560643;
    private static final int AVL_SUN_DB_ID = 1000560644;
    private static final int LIV_MUN_DB_ID = 1000560645;
    private static final int HULL_BHA_DB_ID = 1000560646;
    private static final int BOU_NFO_DB_ID = 1000560647;
    private static final int TOT_IPS_DB_ID = 1000560648;
    private static final int BRE_EVE_DB_ID = 1000560649;
    private static final int COV_CRY_DB_ID = 1000560650;
    private static final int NEW_ARS_DB_ID = 1000560651;

    @BeforeEach
    void setUp() throws Exception {
        clock = new MutableClock(Instant.parse("2026-11-21T14:58:00Z"));
        stubHttp = new StubHttpClient();
        fplClient = new FplLiveClient(stubHttp, "https://fpl.test/api/", clock);
        streamService = new LiveStreamService(20, 1800_000L, false, 3600_000L, clock);
        recorder = new BroadcastRecorder();
        streamService.subscribe(LiveStreamService.CHANNEL_MAIN, recorder);

        FplMappingReader reader = new FplMappingReader();
        teamMap = reader.readBundledTeams();
        playerMap = reader.readBundledPlayers();

        seedGw11Fixtures();
    }

    private void seedGw11Fixtures() {
        jdbc.update("MERGE INTO seasons (league_id, season_year) KEY(league_id, season_year) VALUES (39, 2026)");
        for (FplMappingReader.TeamMapping tm : teamMap.values()) {
            jdbc.update("MERGE INTO clubs (id, name, city) KEY(id) VALUES (?, ?, 'England')",
                    tm.clubId(), tm.clubName());
            jdbc.update("MERGE INTO season_clubs (league_id, season_year, club_id) KEY(league_id, season_year, club_id) VALUES (39, 2026, ?)",
                    tm.clubId());
        }

        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES
                  (?, 39, 2026, 1000000061, 1000000341, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h1', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000065, 1000000063, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h2', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000058, 1000000071, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h3', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000064, 1000000066, 11, DATE '2026-11-22', 'SCHEDULED', 'TIMED', NULL, NULL, 'h4', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000322, 1000000397, 11, DATE '2026-11-22', 'SCHEDULED', 'TIMED', NULL, NULL, 'h5', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000001044, 1000000351, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h6', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000073, 1000000349, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h7', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000402, 1000000062, 11, DATE '2026-11-23', 'SCHEDULED', 'TIMED', NULL, NULL, 'h8', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000001076, 1000000354, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h9', CURRENT_TIMESTAMP),
                  (?, 39, 2026, 1000000067, 1000000057, 11, DATE '2026-11-21', 'SCHEDULED', 'TIMED', NULL, NULL, 'h10', CURRENT_TIMESTAMP)
                """,
                CHE_LEE_DB_ID, MCI_FUL_DB_ID, AVL_SUN_DB_ID, LIV_MUN_DB_ID, HULL_BHA_DB_ID,
                BOU_NFO_DB_ID, TOT_IPS_DB_ID, BRE_EVE_DB_ID, COV_CRY_DB_ID, NEW_ARS_DB_ID);

        jdbc.update("""
                INSERT INTO football_data_fixtures (provider_id, fixture_id, kickoff_utc, provider_status)
                VALUES
                  (500104, ?, '2026-11-21T15:00:00Z', 'TIMED'),
                  (500108, ?, '2026-11-21T12:30:00Z', 'TIMED'),
                  (500101, ?, '2026-11-21T15:00:00Z', 'TIMED'),
                  (500107, ?, '2026-11-22T16:30:00Z', 'TIMED'),
                  (500106, ?, '2026-11-22T14:00:00Z', 'TIMED'),
                  (500102, ?, '2026-11-21T15:00:00Z', 'TIMED'),
                  (500110, ?, '2026-11-21T15:00:00Z', 'TIMED'),
                  (500103, ?, '2026-11-23T20:00:00Z', 'TIMED'),
                  (500105, ?, '2026-11-21T15:00:00Z', 'TIMED'),
                  (500109, ?, '2026-11-21T17:30:00Z', 'TIMED')
                """,
                CHE_LEE_DB_ID, MCI_FUL_DB_ID, AVL_SUN_DB_ID, LIV_MUN_DB_ID, HULL_BHA_DB_ID,
                BOU_NFO_DB_ID, TOT_IPS_DB_ID, BRE_EVE_DB_ID, COV_CRY_DB_ID, NEW_ARS_DB_ID);
    }

    @Test
    void activeWindowRespectsExact5MinutesBeforeAnd150MinutesAfterKickoff() {
        Instant kickoff = Instant.parse("2026-11-23T20:00:00Z"); // BRE vs EVE
        assertFalse(LiveScoreService.isKickoffInWindow(kickoff, Instant.parse("2026-11-23T19:54:59Z")));
        assertTrue(LiveScoreService.isKickoffInWindow(kickoff, Instant.parse("2026-11-23T19:55:00Z")));
        assertTrue(LiveScoreService.isKickoffInWindow(kickoff, Instant.parse("2026-11-23T22:30:00Z"))); // +150m
        assertFalse(LiveScoreService.isKickoffInWindow(kickoff, Instant.parse("2026-11-23T22:30:01Z"))); // +150m 1s

        LiveScoreService service = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());

        // 6 minutes before earliest Saturday kickoff (12:30 UTC) -> outside [kickoff - 5m, kickoff + 150m]
        clock.setInstant(Instant.parse("2026-11-21T12:24:00Z"));
        assertFalse(service.pollOnce());
        assertEquals(0L, fplClient.totalRequests());

        // Exactly 5 minutes before 12:30 UTC kickoff -> inside window -> calls FPL once
        clock.setInstant(Instant.parse("2026-11-21T12:25:00Z"));
        stubHttp.enqueueResponse(200, "[]", Map.of());
        assertTrue(service.pollOnce());
        assertEquals(1L, fplClient.totalRequests());

        // 151 minutes after Monday 20:00 UTC kickoff (22:31 UTC) -> outside window -> no FPL call
        clock.setInstant(Instant.parse("2026-11-23T22:31:00Z"));
        assertFalse(service.pollOnce());
        assertEquals(1L, fplClient.totalRequests());
    }

    @Test
    void ignoresYesterdayFinishedMatchesAndNeverRecreatesEvictedFtMatchesWhileKeepingOverrunningLiveMatch() throws Exception {
        Path realFile = Path.of("src/test/resources/fpl-live/real/fixtures-gw11-20261122.json");
        String realJson = Files.readString(realFile, StandardCharsets.UTF_8);

        // 1. On Sunday 2026-11-22 at 14:02 UTC (during Hull vs Brighton kickoff=14:00 UTC),
        //    FPL returns all 10 GW11 matches, including 7 finished_provisional matches from Saturday 2026-11-21.
        //    ONLY Hull vs Brighton (in [kickoff - 5m, kickoff + 150m]) must be tracked and broadcast!
        clock.setInstant(Instant.parse("2026-11-22T14:02:00Z"));
        LiveScoreService service = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());

        stubHttp.enqueueResponse(200, realJson, Map.of());
        assertTrue(service.pollOnce());

        List<LiveMatchState> matches = service.currentMatches();
        assertEquals(1, matches.size(), "Yesterday's 7 finished matches must be ignored completely");
        assertEquals(HULL_BHA_DB_ID, matches.getFirst().fixtureId());
        assertEquals("LIVE", matches.getFirst().status());
        assertEquals(1, recorder.scoreEvents().size(), "Only the 1 active match in window should be broadcast");

        // 2. Suppose Hull vs Brighton runs over 150 minutes (e.g. long stoppage/delay, reaches FT at kickoff + 160m = 16:40 UTC).
        //    Because it is currently LIVE in memory, it must still be updated to FT at 16:40 UTC!
        clock.setInstant(Instant.parse("2026-11-22T16:40:00Z"));
        String hullFinishedLateJson = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": true, "minutes": 90,
                  "team_h": 11, "team_a": 5, "team_h_score": 1, "team_a_score": 2,
                  "stats": []
                }]
                """;
        stubHttp.enqueueResponse(200, hullFinishedLateJson, Map.of());
        assertTrue(service.pollOnce());
        assertEquals("FT", service.currentMatches().getFirst().status());

        // 3. After 3 hours in FT (at 19:40 UTC), Hull vs Brighton is evicted from memory.
        //    Even if another poll runs and FPL still returns Hull vs Brighton as finished_provisional=true
        //    (and even if clock were moved back inside its kickoff window), it must NEVER be recreated!
        clock.advance(Duration.ofHours(3));
        assertTrue(service.currentMatches().isEmpty(), "FT match must be evicted after 3 hours");

        int broadcastsBefore = recorder.scoreEvents().size();
        clock.setInstant(Instant.parse("2026-11-22T15:30:00Z")); // Inside Hull kickoff window
        stubHttp.enqueueResponse(200, hullFinishedLateJson, Map.of());
        assertTrue(service.pollOnce());
        assertTrue(service.currentMatches().isEmpty(), "Evicted FT match must never be recreated");
        assertEquals(broadcastsBefore, recorder.scoreEvents().size(), "Evicted FT match must not broadcast again");
    }

    @Test
    void eventsAlreadyPresentOnInitialSightingHaveNullApproxMinuteWhileSubsequentEventsGetMinute() {
        clock.setInstant(Instant.parse("2026-11-22T15:15:00Z"));
        LiveScoreService service = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());

        // 1. Backend starts tracking mid-match at 60' when Hull 0-1 Brighton already has 1 goal (Welbeck 136),
        //    1 assist (Mitoma 121), and 1 red card (Millar 287).
        //    Since this is the first sighting of the match, approxMinute MUST be null for all pre-existing events!
        String payloadAt60 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 60,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 1,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 1, "element": 136}]},
                    {"identifier": "assists", "h": [], "a": [{"value": 1, "element": 121}]},
                    {"identifier": "own_goals", "h": [], "a": []},
                    {"identifier": "red_cards", "h": [{"value": 1, "element": 287}], "a": []}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, payloadAt60, Map.of());
        assertTrue(service.pollOnce());

        LiveMatchState initial = service.currentMatches().getFirst();
        assertEquals(1, initial.goals().size());
        assertEquals("Danny Welbeck", initial.goals().getFirst().playerName());
        assertNull(initial.goals().getFirst().approxMinute(), "Pre-existing goal on first sighting must have approxMinute = null");
        assertNull(initial.assists().getFirst().approxMinute(), "Pre-existing assist on first sighting must have approxMinute = null");
        assertNull(initial.redCards().getFirst().approxMinute(), "Pre-existing red card on first sighting must have approxMinute = null");

        // 2. Next poll at 74': Welbeck scores his 2nd goal (value=2) assisted by Joao Pedro (165),
        //    and another red card appears for Brighton (116 Lewis Dunk).
        //    Pre-existing events keep approxMinute = null; newly appeared events get approxMinute = 74!
        String payloadAt74 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 74,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 2,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 2, "element": 136}]},
                    {"identifier": "assists", "h": [], "a": [{"value": 1, "element": 121}, {"value": 1, "element": 165}]},
                    {"identifier": "own_goals", "h": [], "a": []},
                    {"identifier": "red_cards", "h": [{"value": 1, "element": 287}], "a": [{"value": 1, "element": 116}]}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, payloadAt74, Map.of());
        assertTrue(service.pollOnce());

        LiveMatchState updated = service.currentMatches().getFirst();
        assertEquals(2, updated.goals().size());
        assertNull(updated.goals().get(0).approxMinute(), "Welbeck's 1st goal (from initial sighting) must keep approxMinute = null");
        assertEquals(74, updated.goals().get(1).approxMinute(), "Welbeck's 2nd goal (scored after tracking started) must get approxMinute = 74");

        PlayerEvent mitomaAssist = updated.assists().stream().filter(a -> a.fplElementId() == 121).findFirst().orElseThrow();
        PlayerEvent pedroAssist = updated.assists().stream().filter(a -> a.fplElementId() == 165).findFirst().orElseThrow();
        assertNull(mitomaAssist.approxMinute(), "Pre-existing assist must keep approxMinute = null");
        assertEquals(74, pedroAssist.approxMinute(), "New assist must get approxMinute = 74");

        PlayerEvent millarRed = updated.redCards().stream().filter(r -> r.fplElementId() == 287).findFirst().orElseThrow();
        PlayerEvent dunkRed = updated.redCards().stream().filter(r -> r.fplElementId() == 116).findFirst().orElseThrow();
        assertNull(millarRed.approxMinute(), "Pre-existing red card must keep approxMinute = null");
        assertEquals(74, dunkRed.approxMinute(), "New red card must get approxMinute = 74");
    }

    @Test
    void realFplGw11PayloadMapsAllSevenFinishedMatchesAndMatchesChelseaLeedsCsvScorers() throws Exception {
        Path realFile = Path.of("src/test/resources/fpl-live/real/fixtures-gw11-20261122.json");
        assertTrue(Files.exists(realFile), "Real FPL GW11 snapshot must exist");
        String realJson = Files.readString(realFile, StandardCharsets.UTF_8);

        // 1. Verify team-map.csv maps team_h and team_a for all 7 finished_provisional matches in the real FPL file
        JsonNode root = objectMapper.readTree(realJson);
        int finishedCount = 0;
        for (int i = 0; i < root.size(); i++) {
            JsonNode fx = root.get(i);
            if (fx.path("finished_provisional").asBoolean(false)) {
                finishedCount++;
                int teamH = fx.path("team_h").asInt();
                int teamA = fx.path("team_a").asInt();
                FplMappingReader.TeamMapping homeMapped = teamMap.get(teamH);
                FplMappingReader.TeamMapping awayMapped = teamMap.get(teamA);
                assertNotNull(homeMapped, "Missing team_h mapping for FPL team " + teamH);
                assertNotNull(awayMapped, "Missing team_a mapping for FPL team " + teamA);
                Integer dbFixtureId = jdbc.queryForObject(
                        "SELECT id FROM fixtures WHERE season_year = 2026 AND gameweek = 11 AND home_club_id = ? AND away_club_id = ?",
                        Integer.class, homeMapped.clubId(), awayMapped.clubId());
                assertNotNull(dbFixtureId, "No GW11 DB fixture found for " + homeMapped.clubName() + " vs " + awayMapped.clubName());
            }
        }
        assertEquals(7, finishedCount, "Real GW11 snapshot captured on 2026-11-22 must have 7 finished_provisional matches");

        // 2. Process real FPL payload at 2026-11-21T17:00:00Z (120m after 15:00 UTC kickoff -> inside 150m window)
        //    and verify Chelsea 1-4 Leeds (fixtureId=1000560642)
        clock.setInstant(Instant.parse("2026-11-21T17:00:00Z"));
        LiveScoreService service = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());
        service.processFixturesJson(realJson, false);

        LiveMatchState cheLee = service.currentMatches().stream()
                .filter(m -> m.fixtureId() == CHE_LEE_DB_ID)
                .findFirst()
                .orElseThrow();

        assertEquals(CHE_LEE_DB_ID, cheLee.fixtureId());
        assertEquals("FT", cheLee.status());
        assertEquals(90, cheLee.minute());
        assertEquals(1, cheLee.homeScore());
        assertEquals(4, cheLee.awayScore());
        assertEquals(5, cheLee.goals().size());

        // Since all 5 goals were already present on first sighting of this snapshot, approxMinute is null
        for (GoalEvent g : cheLee.goals()) {
            assertNull(g.approxMinute(), "Goals already present on first sighting must have approxMinute = null");
        }

        // Verify internal web playerIds (2000...) and names for Chelsea 1-4 Leeds scorers
        List<GoalEvent> homeGoals = cheLee.goals().stream().filter(g -> "home".equals(g.side())).toList();
        assertEquals(1, homeGoals.size());
        assertEquals(2000005015, homeGoals.getFirst().playerId());
        assertEquals(170, homeGoals.getFirst().fplElementId());
        assertEquals("Emmanuel Emegha", homeGoals.getFirst().playerName());
        assertFalse(homeGoals.getFirst().ownGoal());

        List<GoalEvent> awayGoals = cheLee.goals().stream().filter(g -> "away".equals(g.side())).toList();
        assertEquals(4, awayGoals.size());

        List<GoalEvent> okaforGoals = awayGoals.stream().filter(g -> "Noah Okafor".equals(g.playerName())).toList();
        assertEquals(2, okaforGoals.size());
        assertEquals(2000020071, okaforGoals.get(0).playerId());
        assertEquals(336, okaforGoals.get(0).fplElementId());

        GoalEvent aaronsonGoal = awayGoals.stream().filter(g -> "Brenden Aaronson".equals(g.playerName())).findFirst().orElseThrow();
        assertEquals(2000020069, aaronsonGoal.playerId());
        assertEquals(337, aaronsonGoal.fplElementId());

        GoalEvent dclGoal = awayGoals.stream().filter(g -> "Dominic Calvert-Lewin".equals(g.playerName())).findFirst().orElseThrow();
        assertEquals(2000020067, dclGoal.playerId());
        assertEquals(346, dclGoal.fplElementId());

        // 3. Cross-check against imported CSV data/gw11-2026/manual-match-stats-2026-GW11.csv
        Path csvPath = Path.of("data/gw11-2026/manual-match-stats-2026-GW11.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Path.of("backend/data/gw11-2026/manual-match-stats-2026-GW11.csv");
        }
        Map<Integer, Integer> expectedGoalsByPlayerId = new LinkedHashMap<>();
        Map<Integer, Integer> expectedAssistsByPlayerId = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", -1);
            int fixtureId = Integer.parseInt(parts[1]);
            int playerId = Integer.parseInt(parts[2]);
            int goals = Integer.parseInt(parts[6]);
            int assists = Integer.parseInt(parts[7]);
            if (fixtureId == CHE_LEE_DB_ID) {
                if (goals > 0) expectedGoalsByPlayerId.put(playerId, goals);
                if (assists > 0) expectedAssistsByPlayerId.put(playerId, assists);
            }
        }

        Map<Integer, Integer> actualGoalsByPlayerId = new LinkedHashMap<>();
        for (GoalEvent g : cheLee.goals()) {
            assertNotNull(g.playerId(), "All Chelsea-Leeds scorers must have internal playerId");
            assertTrue(g.playerId() >= 2000000000, "Chelsea-Leeds scorer playerId must be internal 2000... ID, got " + g.playerId());
            actualGoalsByPlayerId.merge(g.playerId(), 1, Integer::sum);
        }
        assertEquals(expectedGoalsByPlayerId, actualGoalsByPlayerId,
                "LiveMatchState scorers for Chelsea-Leeds must match manual-match-stats-2026-GW11.csv");

        Map<Integer, Integer> actualAssistsByPlayerId = new LinkedHashMap<>();
        for (PlayerEvent a : cheLee.assists()) {
            assertNotNull(a.playerId());
            actualAssistsByPlayerId.put(a.playerId(), a.count());
        }
        assertEquals(expectedAssistsByPlayerId, actualAssistsByPlayerId,
                "LiveMatchState assists for Chelsea-Leeds must match manual-match-stats-2026-GW11.csv");
    }

    @Test
    void broadcastsOnlyOnChangeTracksApproxMinuteRescindedGoalOwnGoalUnmappedPlayerAndFtRetention() {
        clock.setInstant(Instant.parse("2026-11-22T14:02:00Z"));
        LiveScoreService service = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());

        // 0. Initial sighting at 2' (0-0, no goals yet)
        String payload02 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 2,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 0,
                  "stats": []
                }]
                """;
        stubHttp.enqueueResponse(200, payload02, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(1, recorder.scoreEvents().size());

        // 1. Poll 1 at 23': Hull 0-1 Brighton (Danny Welbeck element=136 -> internal playerId=2000005013)
        clock.setInstant(Instant.parse("2026-11-22T14:23:00Z"));
        String payload23 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 23,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 1,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 1, "element": 136}]},
                    {"identifier": "assists", "h": [], "a": [{"value": 1, "element": 121}]},
                    {"identifier": "own_goals", "h": [], "a": []},
                    {"identifier": "red_cards", "h": [], "a": []}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, payload23, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(2L, fplClient.totalRequests());
        assertEquals(2, recorder.scoreEvents().size());

        LiveMatchState s1 = recorder.scoreEvents().getLast();
        assertEquals(HULL_BHA_DB_ID, s1.fixtureId());
        assertEquals("LIVE", s1.status());
        assertEquals(23, s1.minute());
        assertEquals(0, s1.homeScore());
        assertEquals(1, s1.awayScore());
        assertEquals(1, s1.goals().size());
        assertEquals(2000005013, s1.goals().getFirst().playerId());
        assertEquals(136, s1.goals().getFirst().fplElementId());
        assertEquals("Danny Welbeck", s1.goals().getFirst().playerName());
        assertEquals("away", s1.goals().getFirst().side());
        assertFalse(s1.goals().getFirst().ownGoal());
        assertEquals(23, s1.goals().getFirst().approxMinute());

        // 2. Poll 2 with identical payload -> must NOT broadcast again
        stubHttp.enqueueResponse(200, payload23, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(3L, fplClient.totalRequests());
        assertEquals(2, recorder.scoreEvents().size());

        // 3. Poll 3 at 51':
        //    - Welbeck scores a 2nd goal (brace -> 1st keeps ~23', 2nd gets ~51')
        //    - Unmapped player element=999999 scores for Hull (home) -> playerId=null, fplElementId=999999, playerName="Không rõ"
        //    - Away player Mitoma (121 -> playerId=106835) scores an own goal (credited to "home" with ownGoal=true)
        String payload51 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 51,
                  "team_h": 11, "team_a": 5, "team_h_score": 2, "team_a_score": 2,
                  "stats": [
                    {"identifier": "goals_scored", "h": [{"value": 1, "element": 999999}], "a": [{"value": 2, "element": 136}]},
                    {"identifier": "assists", "h": [], "a": [{"value": 1, "element": 121}]},
                    {"identifier": "own_goals", "h": [], "a": [{"value": 1, "element": 121}]},
                    {"identifier": "red_cards", "h": [{"value": 1, "element": 287}], "a": []}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, payload51, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(3, recorder.scoreEvents().size());

        LiveMatchState s2 = recorder.scoreEvents().getLast();
        assertEquals(2, s2.homeScore());
        assertEquals(2, s2.awayScore());
        assertEquals(4, s2.goals().size());
        List<GoalEvent> welbeckGoals = s2.goals().stream()
                .filter(g -> "Danny Welbeck".equals(g.playerName()))
                .toList();
        assertEquals(2, welbeckGoals.size());
        assertEquals(23, welbeckGoals.get(0).approxMinute());
        assertEquals(51, welbeckGoals.get(1).approxMinute());

        // Unmapped player goal keeps fplElementId=999999 and playerId=null
        GoalEvent unknownGoal = s2.goals().stream()
                .filter(g -> "Không rõ".equals(g.playerName()))
                .findFirst().orElseThrow();
        assertNull(unknownGoal.playerId());
        assertEquals(999999, unknownGoal.fplElementId());
        assertEquals("home", unknownGoal.side());
        assertEquals(51, unknownGoal.approxMinute());

        // Own goal by away player (121 -> 106835) credited to "home"
        GoalEvent ownGoal = s2.goals().stream()
                .filter(GoalEvent::ownGoal)
                .findFirst().orElseThrow();
        assertEquals(106835, ownGoal.playerId());
        assertEquals(121, ownGoal.fplElementId());
        assertEquals("Kaoru Mitoma", ownGoal.playerName());
        assertEquals("home", ownGoal.side());
        assertEquals(51, ownGoal.approxMinute());

        // Red card tracked with internal playerId and fplElementId
        assertEquals(1, s2.redCards().size());
        assertEquals(2000020013, s2.redCards().getFirst().playerId());
        assertEquals(287, s2.redCards().getFirst().fplElementId());
        assertEquals("Liam Millar", s2.redCards().getFirst().playerName());

        // 4. Poll 4: VAR rescinds Welbeck's 2nd goal (value drops from 2 back to 1) & finished_provisional=true -> FT
        String payloadFt = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": true, "minutes": 90,
                  "team_h": 11, "team_a": 5, "team_h_score": 2, "team_a_score": 1,
                  "stats": [
                    {"identifier": "goals_scored", "h": [{"value": 1, "element": 999999}], "a": [{"value": 1, "element": 136}]},
                    {"identifier": "assists", "h": [], "a": [{"value": 1, "element": 121}]},
                    {"identifier": "own_goals", "h": [], "a": [{"value": 1, "element": 121}]},
                    {"identifier": "red_cards", "h": [{"value": 1, "element": 287}], "a": []}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, payloadFt, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(4, recorder.scoreEvents().size());

        LiveMatchState s3 = recorder.scoreEvents().getLast();
        assertEquals("FT", s3.status());
        assertEquals(2, s3.homeScore());
        assertEquals(1, s3.awayScore());
        assertEquals(3, s3.goals().size());

        // Retained in memory at +2 hours 59 minutes, evicted at +3 hours
        clock.advance(Duration.ofMinutes(179));
        assertEquals(1, service.currentMatches().size());

        clock.advance(Duration.ofMinutes(2));
        assertTrue(service.currentMatches().isEmpty());
    }

    @Test
    void fplLiveClientBacksOffOn5xxAnd429AndResetsOnSuccess() {
        stubHttp.enqueueResponse(503, "Service Unavailable", Map.of());
        assertTrue(fplClient.fetchEventFixtures(11).isEmpty());
        assertEquals(60L, fplClient.currentBackoffSeconds());
        assertTrue(fplClient.isBackingOff());

        assertEquals(1L, fplClient.totalRequests());
        assertTrue(fplClient.fetchEventFixtures(11).isEmpty());
        assertEquals(1L, fplClient.totalRequests());

        clock.advance(Duration.ofSeconds(60));
        stubHttp.enqueueResponse(502, "Bad Gateway", Map.of());
        assertTrue(fplClient.fetchEventFixtures(11).isEmpty());
        assertEquals(120L, fplClient.currentBackoffSeconds());

        clock.advance(Duration.ofSeconds(120));
        stubHttp.enqueueIoException(new IOException("Connection reset"));
        assertTrue(fplClient.fetchEventFixtures(11).isEmpty());
        assertEquals(240L, fplClient.currentBackoffSeconds());

        clock.advance(Duration.ofSeconds(240));
        stubHttp.enqueueResponse(500, "Server Error", Map.of());
        assertTrue(fplClient.fetchEventFixtures(11).isEmpty());
        assertEquals(300L, fplClient.currentBackoffSeconds());

        clock.advance(Duration.ofSeconds(300));
        stubHttp.enqueueResponse(429, "Too Many Requests", Map.of("Retry-After", List.of("95")));
        assertTrue(fplClient.fetchEventFixtures(11).isEmpty());
        assertEquals(95L, fplClient.currentBackoffSeconds());

        clock.advance(Duration.ofSeconds(95));
        stubHttp.enqueueResponse(200, "[]", Map.of());
        assertEquals(Optional.of("[]"), fplClient.fetchEventFixtures(11));
        assertEquals(0L, fplClient.currentBackoffSeconds());
        assertFalse(fplClient.isBackingOff());
    }

    @Test
    void replayModeReadsSyntheticTimeSeriesDerivedFromRealFileWithoutCallingFplAndBlocksInProdProfile() {
        String replayPath = Path.of("src/test/resources/fpl-live/synthetic").toAbsolutePath().toString();

        // Blocked when profile 'prod' is active
        LiveScoreService prodService = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, replayPath, List.of("prod", "render"));
        assertFalse(prodService.isReplayEnabled());
        assertFalse(prodService.replayNextStep());

        // Allowed in non-prod: reads 5 synthetic time-series files in chronological order without touching FplLiveClient
        clock.setInstant(Instant.parse("2026-11-25T03:00:00Z")); // Outside match window
        LiveScoreService replayService = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, replayPath, List.of("default"));
        assertTrue(replayService.isReplayEnabled());

        int steps = 0;
        while (replayService.replayNextStep()) {
            steps++;
        }
        assertEquals(5, steps);
        assertEquals(0L, fplClient.totalRequests());

        LiveMatchState cheLee = replayService.currentMatches().stream()
                .filter(m -> m.fixtureId() == CHE_LEE_DB_ID)
                .findFirst()
                .orElseThrow();
        assertEquals("FT", cheLee.status());
        assertEquals(1, cheLee.homeScore());
        assertEquals(4, cheLee.awayScore());

        // Emegha's goal was already present in step 1 (initial sighting) -> approxMinute = null
        GoalEvent emeghaGoal = cheLee.goals().stream()
                .filter(g -> "Emmanuel Emegha".equals(g.playerName()))
                .findFirst().orElseThrow();
        assertNull(emeghaGoal.approxMinute());

        // Noah Okafor scored at step 3 (55') and step 4 (71') -> approxMinute = 55 and 71
        List<GoalEvent> okaforGoals = cheLee.goals().stream()
                .filter(g -> "Noah Okafor".equals(g.playerName()))
                .toList();
        assertEquals(2, okaforGoals.size());
        assertEquals(2000020071, okaforGoals.get(0).playerId());
        assertEquals(55, okaforGoals.get(0).approxMinute());
        assertEquals(71, okaforGoals.get(1).approxMinute());
    }

    @Test
    void replayRealCaptureGw11CuratedSetVerifiesFinalScoresInternalPlayerIdsYoroRedCardFtTransitionsAndExactBroadcastCount() {
        String realCapturePath = Path.of("src/test/resources/fpl-live/real-capture-gw11").toAbsolutePath().toString();
        LiveScoreService replayService = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, realCapturePath, List.of("default"));
        assertTrue(replayService.isReplayEnabled());

        String hullFtStatusAtStep14 = null;
        String hullFtStatusAtStep15 = null;
        String livFtStatusAtStep31 = null;
        String livFtStatusAtStep32 = null;

        int step = 0;
        while (replayService.replayNextStep()) {
            step++;
            if (step == 14) {
                hullFtStatusAtStep14 = replayService.currentMatches().stream()
                        .filter(m -> m.fixtureId() == HULL_BHA_DB_ID)
                        .map(LiveMatchState::status).findFirst().orElse(null);
            } else if (step == 15) {
                hullFtStatusAtStep15 = replayService.currentMatches().stream()
                        .filter(m -> m.fixtureId() == HULL_BHA_DB_ID)
                        .map(LiveMatchState::status).findFirst().orElse(null);
            } else if (step == 31) {
                livFtStatusAtStep31 = replayService.currentMatches().stream()
                        .filter(m -> m.fixtureId() == LIV_MUN_DB_ID)
                        .map(LiveMatchState::status).findFirst().orElse(null);
            } else if (step == 32) {
                livFtStatusAtStep32 = replayService.currentMatches().stream()
                        .filter(m -> m.fixtureId() == LIV_MUN_DB_ID)
                        .map(LiveMatchState::status).findFirst().orElse(null);
            }
        }

        assertEquals(32, step, "Curated real capture directory has 32 files");
        assertEquals(0L, fplClient.totalRequests(), "Replay mode must never call FPL over network");

        // Out of 32 files: 6 files have no state change for active matches (205501 pre-match,
        // 210135 unchanged min=0, 215803 halftime min=45, 225702 stoppage 1-0, 231611 between matches,
        // 002652 halftime min=45). Only the 26 actual state transitions broadcast!
        assertEquals(26, recorder.scoreEvents().size(),
                "Number of broadcasts must equal the exact number of actual match state changes (26)");

        // FT transitions happen on the exact step where finished_provisional becomes true
        assertEquals("LIVE", hullFtStatusAtStep14);
        assertEquals("FT", hullFtStatusAtStep15);
        assertEquals("LIVE", livFtStatusAtStep31);
        assertEquals("FT", livFtStatusAtStep32);

        List<LiveMatchState> tracked = replayService.currentMatches();
        assertEquals(2, tracked.size(), "Only the 2 Sunday matches (Hull-Brighton and Liverpool-MU) were LIVE during capture");

        // 1. Verify Hull 2-0 Brighton (fixtureId = 1000560646)
        LiveMatchState hullBrighton = tracked.stream()
                .filter(m -> m.fixtureId() == HULL_BHA_DB_ID)
                .findFirst().orElseThrow();
        assertEquals("FT", hullBrighton.status());
        assertEquals(2, hullBrighton.homeScore());
        assertEquals(0, hullBrighton.awayScore());
        assertEquals(2, hullBrighton.goals().size());
        for (GoalEvent g : hullBrighton.goals()) {
            assertEquals(2000020008, g.playerId());
            assertEquals(295, g.fplElementId());
            assertEquals("Oli McBurnie", g.playerName());
            assertEquals("home", g.side());
            assertFalse(g.ownGoal());
            assertEquals(90, g.approxMinute());
        }

        // 2. Verify Liverpool 4-0 Manchester United (fixtureId = 1000560645)
        LiveMatchState livMun = tracked.stream()
                .filter(m -> m.fixtureId() == LIV_MUN_DB_ID)
                .findFirst().orElseThrow();
        assertEquals("FT", livMun.status());
        assertEquals(4, livMun.homeScore());
        assertEquals(0, livMun.awayScore());
        assertEquals(4, livMun.goals().size());

        // Goal 1: Dominik Szoboszlai (element 368 -> internal playerId 2000003008, approxMinute 10)
        assertEquals(2000003008, livMun.goals().get(0).playerId());
        assertEquals(368, livMun.goals().get(0).fplElementId());
        assertEquals("Dominik Szoboszlai", livMun.goals().get(0).playerName());
        assertEquals(10, livMun.goals().get(0).approxMinute());

        // Goal 2: Florian Wirtz (element 366 -> internal playerId 2000003007, approxMinute 13)
        assertEquals(2000003007, livMun.goals().get(1).playerId());
        assertEquals(366, livMun.goals().get(1).fplElementId());
        assertEquals("Florian Wirtz", livMun.goals().get(1).playerName());
        assertEquals(13, livMun.goals().get(1).approxMinute());

        // Goal 3: Rio Ngumoha (element 369 -> internal playerId 2000003024, approxMinute 66)
        assertEquals(2000003024, livMun.goals().get(2).playerId());
        assertEquals(369, livMun.goals().get(2).fplElementId());
        assertEquals("Rio Ngumoha", livMun.goals().get(2).playerName());
        assertEquals(66, livMun.goals().get(2).approxMinute());

        // Goal 4: Victor Munoz (element 377 -> internal playerId 2000003014, approxMinute 82)
        assertEquals(2000003014, livMun.goals().get(3).playerId());
        assertEquals(377, livMun.goals().get(3).fplElementId());
        assertEquals("Victor Munoz", livMun.goals().get(3).playerName());
        assertEquals(82, livMun.goals().get(3).approxMinute());

        // Red card: Leny Yoro (element 420 -> internal playerId 2000006014, away, approxMinute 8)
        assertEquals(1, livMun.redCards().size());
        PlayerEvent yoroRed = livMun.redCards().getFirst();
        assertEquals(2000006014, yoroRed.playerId());
        assertEquals(420, yoroRed.fplElementId());
        assertEquals("Leny Yoro", yoroRed.playerName());
        assertEquals("away", yoroRed.side());
        assertEquals(8, yoroRed.approxMinute());
    }

    @Test
    void mcBurnieStoppageTimeGoalsHaveApproxMinute90WhenTrackedLiveAndNullWhenFirstSeenAtFt() throws Exception {
        Path dir = Path.of("src/test/resources/fpl-live/real-capture-gw11/fixtures");
        String jsonLive00At89 = Files.readString(dir.resolve("20261122-225300.json"), StandardCharsets.UTF_8);
        String jsonLive10At90 = Files.readString(dir.resolve("20261122-225601.json"), StandardCharsets.UTF_8);
        String jsonLive20At90 = Files.readString(dir.resolve("20261122-225903.json"), StandardCharsets.UTF_8);
        String jsonFt20At90 = Files.readString(dir.resolve("20261122-230104.json"), StandardCharsets.UTF_8);

        // Case A: Backend is already tracking the match while LIVE (0-0 at 89'),
        // then McBurnie scores at 22:56 (90') and 22:59 (90') in stoppage time -> approxMinute = 90 is accurate!
        clock.setInstant(Instant.parse("2026-11-22T15:53:00Z"));
        LiveScoreService liveTrackedService = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());
        liveTrackedService.processFixturesJson(jsonLive00At89, false);
        liveTrackedService.processFixturesJson(jsonLive10At90, false);
        liveTrackedService.processFixturesJson(jsonLive20At90, false);
        liveTrackedService.processFixturesJson(jsonFt20At90, false);

        LiveMatchState trackedState = liveTrackedService.currentMatches().stream()
                .filter(m -> m.fixtureId() == HULL_BHA_DB_ID)
                .findFirst().orElseThrow();
        assertEquals(2, trackedState.goals().size());
        assertEquals(90, trackedState.goals().get(0).approxMinute());
        assertEquals(90, trackedState.goals().get(1).approxMinute());

        // Case B: Backend sees the match for the FIRST time when it is already FT (20261122-230104.json)
        // -> both McBurnie goals were pre-existing on initial sighting, so approxMinute MUST be null!
        clock.setInstant(Instant.parse("2026-11-22T16:01:04Z"));
        LiveScoreService lateStartService = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());
        lateStartService.processFixturesJson(jsonFt20At90, false);

        LiveMatchState lateState = lateStartService.currentMatches().stream()
                .filter(m -> m.fixtureId() == HULL_BHA_DB_ID)
                .findFirst().orElseThrow();
        assertEquals("FT", lateState.status());
        assertEquals(2, lateState.goals().size());
        assertNull(lateState.goals().get(0).approxMinute());
        assertNull(lateState.goals().get(1).approxMinute());
    }

    @Test
    void persistsGoalEventsIdempotentlySurvivesRestartHandlesVarOwnGoalsAndScoreReconciliation() {
        // Seed players used in Hull (home=1000000322) vs Brighton (away=1000000397)
        int welbeckId = playerMap.get(136).playerId();
        int millarId = playerMap.get(287).playerId();
        jdbc.update("MERGE INTO players (id, name) KEY(id) VALUES (?, ?), (?, ?)",
                welbeckId, playerMap.get(136).playerName(),
                millarId, playerMap.get(287).playerName());

        clock.setInstant(Instant.parse("2026-11-22T14:01:00Z"));
        LiveScoreService service = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());

        // 1. Kickoff 0-0 -> starts tracking
        stubHttp.enqueueResponse(200, """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 1,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 0,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": []},
                    {"identifier": "own_goals", "h": [], "a": []}
                  ]
                }]
                """, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM match_goal_events WHERE fixture_id = ?", Integer.class, HULL_BHA_DB_ID));

        // 2. Minute 12: Welbeck scores (away)
        clock.setInstant(Instant.parse("2026-11-22T14:12:00Z"));
        String at12 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 12,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 1,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 1, "element": 136}]},
                    {"identifier": "own_goals", "h": [], "a": []}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, at12, Map.of());
        assertTrue(service.pollOnce());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM match_goal_events WHERE fixture_id = ?", Integer.class, HULL_BHA_DB_ID));

        // 3. Idempotent repeat at minute 13 -> no duplicate row, keeps minute 12
        clock.setInstant(Instant.parse("2026-11-22T14:13:00Z"));
        stubHttp.enqueueResponse(200, at12.replace("\"minutes\": 12", "\"minutes\": 13"), Map.of());
        assertTrue(service.pollOnce());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM match_goal_events WHERE fixture_id = ?", Integer.class, HULL_BHA_DB_ID));
        assertEquals(12, jdbc.queryForObject("SELECT \"minute\" FROM match_goal_events WHERE fixture_id = ? AND player_id = ?",
                Integer.class, HULL_BHA_DB_ID, welbeckId));

        // 4. Minute 30: Home player Millar scores OWN GOAL (in "h" -> benefits away club Brighton 1000000397!)
        //    and Minute 67: Welbeck scores his 2nd goal
        clock.setInstant(Instant.parse("2026-11-22T14:30:00Z"));
        stubHttp.enqueueResponse(200, """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 30,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 2,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 1, "element": 136}]},
                    {"identifier": "own_goals", "h": [{"value": 1, "element": 287}], "a": []}
                  ]
                }]
                """, Map.of());
        assertTrue(service.pollOnce());

        clock.setInstant(Instant.parse("2026-11-22T15:22:00Z"));
        String at67 = """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 67,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 3,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 2, "element": 136}]},
                    {"identifier": "own_goals", "h": [{"value": 1, "element": 287}], "a": []}
                  ]
                }]
                """;
        stubHttp.enqueueResponse(200, at67, Map.of());
        assertTrue(service.pollOnce());

        MatchGoalEventRepository repo = new MatchGoalEventRepository(jdbc);
        List<MatchGoalEventRepository.StoredGoalEvent> saved = repo.findByFixture(HULL_BHA_DB_ID);
        assertEquals(3, saved.size());
        assertEquals(12, saved.get(0).minute());
        assertEquals("GOAL", saved.get(0).goalType());
        assertEquals(1000000397, saved.get(0).clubId());
        assertEquals(30, saved.get(1).minute());
        assertEquals("OWN_GOAL", saved.get(1).goalType());
        assertEquals(1000000397, saved.get(1).clubId(), "Own goal by home player must credit benefiting away club");
        assertEquals(67, saved.get(2).minute());
        assertEquals(2, saved.get(2).seq());

        // 5. Simulated backend restart mid-match: brand-new LiveScoreService polls at minute 70 -> keeps 12, 30, 67!
        clock.setInstant(Instant.parse("2026-11-22T15:25:00Z"));
        LiveScoreService restarted = new LiveScoreService(
                jdbc, fplClient, streamService, objectMapper, clock, teamMap, playerMap, true, "", List.of());
        stubHttp.enqueueResponse(200, at67.replace("\"minutes\": 67", "\"minutes\": 70"), Map.of());
        assertTrue(restarted.pollOnce());

        LiveMatchState afterRestart = restarted.currentMatches().getFirst();
        assertEquals(3, afterRestart.goals().size());
        assertEquals(12, afterRestart.goals().get(0).approxMinute());
        assertEquals(30, afterRestart.goals().get(1).approxMinute());
        assertEquals(67, afterRestart.goals().get(2).approxMinute());
        assertEquals(List.of(12, 30, 67), repo.findByFixture(HULL_BHA_DB_ID).stream()
                .map(MatchGoalEventRepository.StoredGoalEvent::minute).toList());

        // 6. VAR disallow: Welbeck's 2nd goal (seq=2, minute=67) is revoked -> drops from 2 to 1
        clock.setInstant(Instant.parse("2026-11-22T15:27:00Z"));
        stubHttp.enqueueResponse(200, """
                [{
                  "id": 106, "event": 11, "kickoff_time": "2026-11-22T14:00:00Z",
                  "started": true, "finished": false, "finished_provisional": false, "minutes": 72,
                  "team_h": 11, "team_a": 5, "team_h_score": 0, "team_a_score": 2,
                  "stats": [
                    {"identifier": "goals_scored", "h": [], "a": [{"value": 1, "element": 136}]},
                    {"identifier": "own_goals", "h": [{"value": 1, "element": 287}], "a": []}
                  ]
                }]
                """, Map.of());
        assertTrue(restarted.pollOnce());
        List<MatchGoalEventRepository.StoredGoalEvent> afterVar = repo.findByFixture(HULL_BHA_DB_ID);
        assertEquals(2, afterVar.size());
        assertEquals(List.of(12, 30), afterVar.stream().map(MatchGoalEventRepository.StoredGoalEvent::minute).toList());

        // 7. Match finishes with matching official score 0-2 -> matchDetail returns goalEvents
        jdbc.update("UPDATE fixtures SET status = 'FINISHED', home_goals = 0, away_goals = 2 WHERE id = ?", HULL_BHA_DB_ID);
        var detailMatch = footballQueries.matchDetail(HULL_BHA_DB_ID, 2026).orElseThrow();
        assertEquals(2, detailMatch.goalEvents().size());
        assertEquals(12, detailMatch.goalEvents().get(0).minute());
        assertEquals("GOAL", detailMatch.goalEvents().get(0).goalType());
        assertEquals(30, detailMatch.goalEvents().get(1).minute());
        assertEquals("OWN_GOAL", detailMatch.goalEvents().get(1).goalType());

        // 8. Official score mismatch (0-3 vs 2 events) -> falls back to empty goalEvents list
        jdbc.update("UPDATE fixtures SET home_goals = 0, away_goals = 3 WHERE id = ?", HULL_BHA_DB_ID);
        var detailMismatch = footballQueries.matchDetail(HULL_BHA_DB_ID, 2026).orElseThrow();
        assertTrue(detailMismatch.goalEvents().isEmpty(), "Mismatched goalEvents must fall back to legacy scorers");

        // 9. Null-minute event sorts last when score matches
        jdbc.update("""
                INSERT INTO match_goal_events (fixture_id, club_id, player_id, "minute", goal_type, source, detected_at, seq)
                VALUES (?, 1000000397, ?, NULL, 'GOAL', 'FPL_LIVE', TIMESTAMP '2026-11-22 15:30:00', 2)
                """, HULL_BHA_DB_ID, welbeckId);
        var detailWithNullMin = footballQueries.matchDetail(HULL_BHA_DB_ID, 2026).orElseThrow();
        assertEquals(3, detailWithNullMin.goalEvents().size());
        assertEquals(12, detailWithNullMin.goalEvents().get(0).minute());
        assertEquals(30, detailWithNullMin.goalEvents().get(1).minute());
        assertNull(detailWithNullMin.goalEvents().get(2).minute(), "Goal with null minute must sort last");

        // Manual stats must stay completely untouched
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        MutableClock(Instant initial) {
            this.current = initial;
        }

        void setInstant(Instant next) {
            this.current = next;
        }

        void advance(Duration delta) {
            this.current = this.current.plus(delta);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }

    private static final class BroadcastRecorder extends SseEmitter {
        private final List<LiveMatchState> scores = new ArrayList<>();

        BroadcastRecorder() {
            super(60_000L);
        }

        @Override
        public synchronized void send(SseEventBuilder builder) {
            Set<DataWithMediaType> items = builder.build();
            for (DataWithMediaType item : items) {
                if (item.getData() instanceof LiveMatchState state && MediaType.APPLICATION_JSON.equals(item.getMediaType())) {
                    scores.add(state);
                }
            }
        }

        List<LiveMatchState> scoreEvents() {
            return scores;
        }
    }

    private static final class StubHttpClient extends HttpClient {
        private record ScriptedResponse(int status, String body, Map<String, List<String>> headers, IOException error) {}
        private final List<ScriptedResponse> queue = new ArrayList<>();

        void enqueueResponse(int status, String body, Map<String, List<String>> headers) {
            queue.add(new ScriptedResponse(status, body, headers, null));
        }

        void enqueueIoException(IOException error) {
            queue.add(new ScriptedResponse(0, null, Map.of(), error));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            if (queue.isEmpty()) {
                throw new IllegalStateException("No scripted response queued in StubHttpClient");
            }
            ScriptedResponse next = queue.removeFirst();
            if (next.error() != null) {
                throw next.error();
            }
            return (HttpResponse<T>) new SimpleHttpResponse(request, next.status(), next.body(),
                    HttpHeaders.of(next.headers(), (k, v) -> true));
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.of(Duration.ofSeconds(10)); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private record SimpleHttpResponse(HttpRequest request, int statusCode, String body, HttpHeaders headers)
            implements HttpResponse<String> {
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
    }
}
