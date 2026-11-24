package com.premierhub.fpl;

import com.premierhub.service.PlayerSeasonProfileService;
import com.premierhub.web.dto.PlayerSeasonProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fpl-provisional-sync-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "premierhub.fpl.provisional-sync.enabled=false"
})
class FplProvisionalSyncTest {
    private static final Path REAL_GW11_RAW = Path.of("data/gw11-2026/fpl/raw");
    private static final Path REAL_GW11_DIR = Path.of("data/gw11-2026/fpl");
    private static final Path REAL_MAP_DIR = Path.of("data/fpl-2026");
    private static final Path TEST_FPL_DIR = Path.of("src/test/resources/fpl");

    private static final int LIV_MUN_FIXTURE = 1000560645;
    private static final int LIV_CLUB = 1000000064;
    private static final int MUN_CLUB = 1000000066;

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired FplCollectorService collector;
    @Autowired FplJsonParser parser;
    @Autowired PlayerSeasonProfileService profileService;
    @Autowired WebApplicationContext wac;
    @TempDir Path temp;

    private MockMvc mvc;
    private final FplMappingReader mappingReader = new FplMappingReader();

    @BeforeEach
    void resetDb() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jdbc.update("DELETE FROM fpl_provisional_unmapped");
        jdbc.update("DELETE FROM fpl_provisional_player_stats");
        jdbc.update("DELETE FROM manual_fixture_player_extra_stats");
        jdbc.update("DELETE FROM manual_fixture_player_stats");
        jdbc.update("DELETE FROM fixture_lineup_players");
        jdbc.update("DELETE FROM fixture_lineups");
        jdbc.update("DELETE FROM football_data_fixtures");
        jdbc.update("DELETE FROM fixtures");
        jdbc.update("DELETE FROM manual_player_memberships");
        jdbc.update("DELETE FROM player_season_profiles");
        jdbc.update("DELETE FROM player_season_stats");
        jdbc.update("DELETE FROM season_clubs");
        jdbc.update("DELETE FROM players");
        jdbc.update("DELETE FROM clubs");
        jdbc.update("DELETE FROM seasons");
    }

    private void seedRealGw11ClubsAndPlayers(Set<Integer> fixtureIdsToSeed) throws Exception {
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        Map<Integer, FplMappingReader.TeamMapping> teamMap = mappingReader.readTeams(REAL_MAP_DIR.resolve("team-map.csv"));
        for (FplMappingReader.TeamMapping team : teamMap.values()) {
            jdbc.update("INSERT INTO clubs (id, name, city) VALUES (?, ?, NULL)",
                    team.clubId(), team.clubName());
            jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, ?)", team.clubId());
        }

        // Seed all fixtures listed in real gw11 fixture-map.csv that are in fixtureIdsToSeed
        List<String> fixtureLines = Files.readAllLines(REAL_GW11_DIR.resolve("fixture-map.csv"), StandardCharsets.UTF_8);
        for (int i = 1; i < fixtureLines.size(); i++) {
            String line = fixtureLines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] p = line.split(",", -1);
            int fixtureId = Integer.parseInt(p[1]);
            if (!fixtureIdsToSeed.isEmpty() && !fixtureIdsToSeed.contains(fixtureId)) continue;
            String kickoffUtc = p[2];
            int homeClub = Integer.parseInt(p[3]);
            int awayClub = Integer.parseInt(p[4]);
            int homeGoals = Integer.parseInt(p[7]);
            int awayGoals = Integer.parseInt(p[8]);
            String matchDate = kickoffUtc.substring(0, 10);
            jdbc.update("""
                    INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                      gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                    VALUES (?, 39, 2026, ?, ?, 11, ?, 'FINISHED', 'FINISHED', ?, ?, 'hash', CURRENT_TIMESTAMP)
                    """, fixtureId, homeClub, awayClub, java.sql.Date.valueOf(matchDate), homeGoals, awayGoals);
            jdbc.update("""
                    INSERT INTO football_data_fixtures (provider_id, fixture_id, kickoff_utc, provider_status)
                    VALUES (?, ?, ?, 'FINISHED')
                    """, fixtureId, fixtureId, kickoffUtc);
        }

        // Seed all players from player-map.csv using their FPL team from real bootstrap-static.json
        FplModels.Bootstrap bootstrap = parser.parseBootstrap(
                Files.readString(REAL_GW11_RAW.resolve("bootstrap-static.json"), StandardCharsets.UTF_8));
        Map<Integer, FplModels.Element> elementsById = new LinkedHashMap<>();
        for (FplModels.Element el : bootstrap.elements()) {
            elementsById.put(el.id(), el);
        }
        Map<Integer, FplMappingReader.PlayerMapping> playerMap = mappingReader.readPlayers(REAL_MAP_DIR.resolve("player-map.csv"));
        Set<Integer> seededPlayers = new LinkedHashSet<>();
        for (FplMappingReader.PlayerMapping pm : playerMap.values()) {
            if (!seededPlayers.add(pm.playerId())) continue;
            FplModels.Element el = elementsById.get(pm.fplElementId());
            int clubId = (el != null && teamMap.containsKey(el.teamId()))
                    ? teamMap.get(el.teamId()).clubId()
                    : LIV_CLUB;
            jdbc.update("INSERT INTO players (id, name) VALUES (?, ?)", pm.playerId(), pm.playerName());
            jdbc.update("""
                    INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                    VALUES (39, 2026, ?, ?, 'MIDFIELDER')
                    """, pm.playerId(), clubId);
            jdbc.update("""
                    INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                    VALUES (39, 2026, ?, ?, DATE '2026-08-01', NULL)
                    """, pm.playerId(), clubId);
        }
    }

    private FplProvisionalSync createSync(FplSource source, Clock clock, boolean enabled) {
        return new FplProvisionalSync(
                jdbc,
                transactions,
                collector,
                parser,
                source,
                clock,
                enabled,
                Duration.ofMinutes(100),
                Duration.ofHours(72),
                Duration.ofHours(6),
                10,
                60);
    }

    @Test
    void matchesRealGw11LiverpoolManUtdAndMatchesSavedFplCsvExactly() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        // 3 hours after Liverpool-Man Utd kickoff (2026-11-22T16:30:00Z -> 19:30:00Z)
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);
        FplSource realRaw = FplSource.fromDirectory(REAL_GW11_RAW);
        FplProvisionalSync sync = createSync(realRaw, clock, true);

        FplProvisionalSync.SyncSummary first = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, first.outcome());
        assertEquals(1, first.fixturesWritten());
        assertEquals(32, first.playerRowsWritten());
        assertEquals(0, first.unmappedWritten());
        assertEquals(0, first.skippedScoreMismatch());

        // Compare every row against saved backend/data/gw11-2026/fpl/fpl-match-stats-2026-GW11.csv
        Map<Integer, String[]> expectedBasic = new LinkedHashMap<>();
        for (String line : Files.readAllLines(REAL_GW11_DIR.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8)) {
            if (line.startsWith(LIV_MUN_FIXTURE + ",")) {
                String[] cols = line.split(",", -1);
                expectedBasic.put(Integer.parseInt(cols[1]), cols);
            }
        }
        assertEquals(32, expectedBasic.size());

        List<Map<String, Object>> actualRows = jdbc.queryForList(
                "SELECT * FROM fpl_provisional_player_stats WHERE fixture_id = ? ORDER BY player_id",
                LIV_MUN_FIXTURE);
        assertEquals(32, actualRows.size());
        for (Map<String, Object> row : actualRows) {
            int playerId = ((Number) row.get("player_id")).intValue();
            String[] exp = expectedBasic.get(playerId);
            assertEquals(Integer.parseInt(exp[2]), ((Number) row.get("minutes")).intValue(), "minutes for " + playerId);
            assertEquals(Integer.parseInt(exp[3]), ((Number) row.get("goals")).intValue(), "goals for " + playerId);
            assertEquals(Integer.parseInt(exp[4]), ((Number) row.get("assists")).intValue(), "assists for " + playerId);
            assertEquals(Integer.parseInt(exp[5]), ((Number) row.get("yellow_cards")).intValue(), "yc for " + playerId);
            assertEquals(Integer.parseInt(exp[6]), ((Number) row.get("red_cards")).intValue(), "rc for " + playerId);
            assertEquals("PROVISIONAL", row.get("fpl_status"));
        }

        // Verify extended stats (bps, bonus, xG, xA) match fpl-extra-stats-2026-GW11.csv
        Map<Integer, String[]> expectedExtra = new LinkedHashMap<>();
        for (String line : Files.readAllLines(REAL_GW11_DIR.resolve("fpl-extra-stats-2026-GW11.csv"), StandardCharsets.UTF_8)) {
            if (line.startsWith("2026," + LIV_MUN_FIXTURE + ",")) {
                String[] cols = line.split(",", -1);
                expectedExtra.put(Integer.parseInt(cols[2]), cols);
            }
        }
        assertEquals(32, expectedExtra.size());
        for (Map<String, Object> row : actualRows) {
            int playerId = ((Number) row.get("player_id")).intValue();
            String[] exp = expectedExtra.get(playerId);
            assertEquals(Integer.parseInt(exp[10]), ((Number) row.get("bonus")).intValue(), "bonus for " + playerId);
            assertEquals(Integer.parseInt(exp[11]), ((Number) row.get("bps")).intValue(), "bps for " + playerId);
            assertEquals(0, new BigDecimal(exp[16]).compareTo((BigDecimal) row.get("expected_goals")), "xG for " + playerId);
            assertEquals(0, new BigDecimal(exp[17]).compareTo((BigDecimal) row.get("expected_assists")), "xA for " + playerId);
        }

        // Running a second time is idempotent
        FplProvisionalSync.SyncSummary second = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, second.outcome());
        assertEquals(32, second.playerRowsWritten());
        Integer countAfterSecond = jdbc.queryForObject(
                "SELECT COUNT(*) FROM fpl_provisional_player_stats WHERE fixture_id = ?",
                Integer.class, LIV_MUN_FIXTURE);
        assertEquals(32, countAfterSecond);

        // Never writes to manual tables
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_stats", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM manual_fixture_player_extra_stats", Integer.class));
    }

    @Test
    void skipsConfirmedFixtureCompletelyWithoutCallingFpl() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        Integer anyLivPlayer = jdbc.queryForObject(
                "SELECT player_id FROM player_season_stats WHERE club_id = ? LIMIT 1", Integer.class, LIV_CLUB);
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 minutes, goals, assists, yellow_cards, red_cards, rating)
                VALUES (?, ?, 39, 2026, ?, 'PLAYED', 90, 0, 0, 0, 0, 7.5)
                """, LIV_MUN_FIXTURE, anyLivPlayer, LIV_CLUB);

        AtomicInteger calls = new AtomicInteger();
        FplSource countingSource = countingWrapper(FplSource.fromDirectory(REAL_GW11_RAW), calls);
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);

        FplProvisionalSync.SyncSummary summary = createSync(countingSource, clock, true).runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.NO_CANDIDATES, summary.outcome());
        assertEquals(0, calls.get());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fpl_provisional_player_stats", Integer.class));
    }

    @Test
    void doesNotWriteOnScoreMismatchAndRetriesOnceDbScoreMatches() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        jdbc.update("UPDATE fixtures SET home_goals = 3, away_goals = 0 WHERE id = ?", LIV_MUN_FIXTURE);

        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);
        FplProvisionalSync sync = createSync(FplSource.fromDirectory(REAL_GW11_RAW), clock, true);

        FplProvisionalSync.SyncSummary mismatch = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, mismatch.outcome());
        assertEquals(0, mismatch.fixturesWritten());
        assertEquals(1, mismatch.skippedScoreMismatch());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fpl_provisional_player_stats", Integer.class));

        // Once football-data syncs final 4-0 score, next tick succeeds
        jdbc.update("UPDATE fixtures SET home_goals = 4, away_goals = 0 WHERE id = ?", LIV_MUN_FIXTURE);
        FplProvisionalSync.SyncSummary ok = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, ok.outcome());
        assertEquals(1, ok.fixturesWritten());
        assertEquals(32, ok.playerRowsWritten());
    }

    @Test
    void recordsUnmappedPlayersInUnmappedTableAndSkipsThemFromStats() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        Map<Integer, FplMappingReader.TeamMapping> teamMap = mappingReader.readTeams(REAL_MAP_DIR.resolve("team-map.csv"));
        Map<Integer, FplMappingReader.PlayerMapping> fullMap = mappingReader.readPlayers(REAL_MAP_DIR.resolve("player-map.csv"));

        // Drop Florian Wirtz's FPL mapping (one known played player in Liverpool-Man Utd)
        Integer wirtzElementId = fullMap.values().stream()
                .filter(pm -> pm.playerName().contains("Wirtz"))
                .map(FplMappingReader.PlayerMapping::fplElementId)
                .findFirst()
                .orElseThrow();
        Map<Integer, FplMappingReader.PlayerMapping> partialMap = new LinkedHashMap<>(fullMap);
        partialMap.remove(wirtzElementId);

        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);
        FplProvisionalSync sync = createSync(FplSource.fromDirectory(REAL_GW11_RAW), clock, true);

        FplProvisionalSync.SyncSummary summary = sync.runIfDue(FplSource.fromDirectory(REAL_GW11_RAW), teamMap, partialMap);
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, summary.outcome());
        assertEquals(1, summary.fixturesWritten());
        assertEquals(31, summary.playerRowsWritten());
        assertEquals(1, summary.unmappedWritten());

        List<Map<String, Object>> unmapped = jdbc.queryForList(
                "SELECT * FROM fpl_provisional_unmapped WHERE fixture_id = ?", LIV_MUN_FIXTURE);
        assertEquals(1, unmapped.size());
        assertEquals(wirtzElementId, ((Number) unmapped.get(0).get("fpl_element_id")).intValue());
        assertEquals(LIV_CLUB, ((Number) unmapped.get(0).get("club_id")).intValue());
        assertTrue(((Number) unmapped.get(0).get("minutes")).intValue() > 0);
    }

    @Test
    void transitionsProvisionalToFinalFplAndStopsPolling() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        String bootstrapJson = Files.readString(REAL_GW11_RAW.resolve("bootstrap-static.json"), StandardCharsets.UTF_8);
        String fixturesProvisional = Files.readString(REAL_GW11_RAW.resolve("fixtures-gw11.json"), StandardCharsets.UTF_8);
        String fixturesFinal = fixturesProvisional
                .replace("\"finished\":false", "\"finished\":true")
                .replace("\"finished\": false", "\"finished\": true");
        String liveJson = Files.readString(REAL_GW11_RAW.resolve("event-gw11-live.json"), StandardCharsets.UTF_8);

        AtomicReference<String> fixturesPayload = new AtomicReference<>(fixturesProvisional);
        AtomicInteger fetchCalls = new AtomicInteger();
        FplSource dynamicSource = new FplSource() {
            @Override public String fetchBootstrapStatic() { fetchCalls.incrementAndGet(); return bootstrapJson; }
            @Override public String fetchFixtures(int gw) { fetchCalls.incrementAndGet(); return fixturesPayload.get(); }
            @Override public String fetchEventLive(int gw) { fetchCalls.incrementAndGet(); return liveJson; }
            @Override public String fetchElementSummary(int id) { throw new UnsupportedOperationException(); }
        };

        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);
        FplProvisionalSync sync = createSync(dynamicSource, clock, true);

        // Run 1: PROVISIONAL (bootstrap + fixtures + live = 3 calls)
        FplProvisionalSync.SyncSummary run1 = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, run1.outcome());
        assertEquals(3, fetchCalls.get());
        assertEquals("PROVISIONAL", jdbc.queryForObject(
                "SELECT DISTINCT fpl_status FROM fpl_provisional_player_stats WHERE fixture_id = ?",
                String.class, LIV_MUN_FIXTURE));

        // Run 2: FPL marks finished=true -> becomes FINAL_FPL (bootstrap cached, only fixtures + live = +2 calls)
        fixturesPayload.set(fixturesFinal);
        FplProvisionalSync.SyncSummary run2 = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, run2.outcome());
        assertEquals(5, fetchCalls.get());
        assertEquals("FINAL_FPL", jdbc.queryForObject(
                "SELECT DISTINCT fpl_status FROM fpl_provisional_player_stats WHERE fixture_id = ?",
                String.class, LIV_MUN_FIXTURE));

        // Run 3: Already FINAL_FPL -> NO_CANDIDATES, 0 additional FPL calls
        FplProvisionalSync.SyncSummary run3 = sync.runIfDue();
        assertEquals(FplProvisionalSync.TickOutcome.NO_CANDIDATES, run3.outcome());
        assertEquals(5, fetchCalls.get());
    }

    @Test
    void splitsDoubleGameweekViaElementSummaryInMemory() throws Exception {
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO clubs VALUES (1000000057, 'Arsenal FC', 'Arsenal'), "
                + "(1000000061, 'Chelsea FC', 'Chelsea'), (1000000064, 'Liverpool FC', 'Liverpool')");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, 1000000057), (39, 2026, 1000000061), (39, 2026, 1000000064)");
        jdbc.update("INSERT INTO players VALUES (2000001001, 'Alex Alpha'), (2000001002, 'Ben Beta'), "
                + "(2000002001, 'Greg Gamma'), (2000002002, 'Dan Delta')");
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560650, 39, 2026, 1000000057, 1000000061, 11, DATE '2026-11-21', 'FINISHED', 'FINISHED', 2, 1, 'h1', CURRENT_TIMESTAMP),
                       (1000560651, 39, 2026, 1000000057, 1000000064, 11, DATE '2026-11-22', 'FINISHED', 'FINISHED', 1, 0, 'h2', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO football_data_fixtures (provider_id, fixture_id, kickoff_utc, provider_status)
                VALUES (1000560650, 1000560650, '2026-11-21T15:00:00Z', 'FINISHED'),
                       (1000560651, 1000560651, '2026-11-22T15:00:00Z', 'FINISHED')
                """);

        Path dgwSrc = temp.resolve("dgw-prov-src");
        Files.createDirectories(dgwSrc);
        Files.copy(TEST_FPL_DIR.resolve("bootstrap-static.json"), dgwSrc.resolve("bootstrap-static.json"));
        Files.copy(TEST_FPL_DIR.resolve("element-summary-101.json"), dgwSrc.resolve("element-summary-101.json"));
        Files.writeString(dgwSrc.resolve("fixtures-gw11.json"), """
                [
                  {"id":101,"event":11,"kickoff_time":"2026-11-21T15:00:00Z","team_h":1,"team_a":6,"team_h_score":2,"team_a_score":1,"finished":true,"finished_provisional":true},
                  {"id":102,"event":11,"kickoff_time":"2026-11-22T15:00:00Z","team_h":1,"team_a":14,"team_h_score":1,"team_a_score":0,"finished":true,"finished_provisional":true}
                ]
                """, StandardCharsets.UTF_8);
        String live = Files.readString(TEST_FPL_DIR.resolve("event-gw11-live.json"), StandardCharsets.UTF_8)
                .replace(
                        "{\"fixture\": 101, \"stats\": [{\"identifier\": \"minutes\", \"points\": 2, \"value\": 90}]}",
                        "{\"fixture\": 101, \"stats\": [{\"identifier\": \"minutes\", \"points\": 2, \"value\": 90}]},"
                        + "{\"fixture\": 102, \"stats\": [{\"identifier\": \"minutes\", \"points\": 2, \"value\": 65}]}");
        Files.writeString(dgwSrc.resolve("event-gw11-live.json"), live, StandardCharsets.UTF_8);

        Map<Integer, FplMappingReader.TeamMapping> teamMap = Map.of(
                1, new FplMappingReader.TeamMapping(1, 1000000057, "Arsenal", "Arsenal FC"),
                6, new FplMappingReader.TeamMapping(6, 1000000061, "Chelsea", "Chelsea FC"),
                14, new FplMappingReader.TeamMapping(14, 1000000064, "Liverpool", "Liverpool FC"));
        Map<Integer, FplMappingReader.PlayerMapping> playerMap = Map.of(
                101, new FplMappingReader.PlayerMapping(101, 2000001001, "Alex Alpha", "Alex Alpha", "test", java.time.LocalDate.of(2026, 11, 21)),
                102, new FplMappingReader.PlayerMapping(102, 2000001002, "Ben Beta", "Ben Beta", "test", java.time.LocalDate.of(2026, 11, 21)));

        Clock clock = Clock.fixed(Instant.parse("2026-11-22T18:00:00Z"), ZoneOffset.UTC);
        FplProvisionalSync sync = createSync(FplSource.fromDirectory(dgwSrc), clock, true);
        FplProvisionalSync.SyncSummary summary = sync.runIfDue(FplSource.fromDirectory(dgwSrc), teamMap, playerMap);

        assertEquals(FplProvisionalSync.TickOutcome.SYNCED, summary.outcome());
        assertEquals(2, summary.fixturesWritten());
        assertEquals(3, summary.playerRowsWritten());
        assertEquals(1, summary.elementSummaryCalls());
        assertEquals(90, jdbc.queryForObject(
                "SELECT minutes FROM fpl_provisional_player_stats WHERE fixture_id = 1000560650 AND player_id = 2000001001",
                Integer.class));
        assertEquals(65, jdbc.queryForObject(
                "SELECT minutes FROM fpl_provisional_player_stats WHERE fixture_id = 1000560651 AND player_id = 2000001001",
                Integer.class));
    }

    @Test
    void seasonTotalsExcludeProvisionalAndApiReturnsAllThreeDataStatuses() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        Map<Integer, FplMappingReader.PlayerMapping> playerMap = mappingReader.readPlayers(REAL_MAP_DIR.resolve("player-map.csv"));
        int wirtzId = playerMap.values().stream()
                .filter(pm -> pm.playerName().contains("Wirtz"))
                .map(FplMappingReader.PlayerMapping::playerId)
                .findFirst()
                .orElseThrow();

        // 1) NONE state before any stats exist
        mvc.perform(get("/api/matches/" + LIV_MUN_FIXTURE + "/details").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus", is("NONE")))
                .andExpect(jsonPath("$.homePlayers", hasSize(0)))
                .andExpect(jsonPath("$.awayPlayers", hasSize(0)));

        mvc.perform(get("/api/players/" + wirtzId + "/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataStatus", is("NONE")))
                .andExpect(jsonPath("$[0].stats", nullValue()));

        // 2) PROVISIONAL_FPL state after sync
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);
        createSync(FplSource.fromDirectory(REAL_GW11_RAW), clock, true).runIfDue();

        mvc.perform(get("/api/matches/" + LIV_MUN_FIXTURE + "/details").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus", is("PROVISIONAL_FPL")))
                .andExpect(jsonPath("$.provisionalFetchedAt", is("2026-11-22T19:30:00Z")))
                .andExpect(jsonPath("$.homeLineup").doesNotExist())
                .andExpect(jsonPath("$.awayLineup").doesNotExist())
                .andExpect(jsonPath("$.homePlayers.length()", greaterThan(10)))
                .andExpect(jsonPath("$.homePlayers[0].rating", nullValue()))
                .andExpect(jsonPath("$.homePlayers[0].participationStatus", is("PLAYED")))
                .andExpect(jsonPath("$.homePlayers[0].extra.bps", notNullValue()));

        mvc.perform(get("/api/players/" + wirtzId + "/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataStatus", is("PROVISIONAL_FPL")))
                .andExpect(jsonPath("$[0].stats.rating", nullValue()))
                .andExpect(jsonPath("$[0].stats.participationStatus", is("PLAYED")))
                .andExpect(jsonPath("$[0].stats.minutes", greaterThan(0)))
                .andExpect(jsonPath("$[0].stats.extra.bps", notNullValue()));

        // Season profile totals MUST NOT include provisional match!
        PlayerSeasonProfileResponse profile = profileService.seasonProfile(wirtzId, 2026);
        assertEquals(0, profile.totals().appearances());
        assertEquals(0, profile.totals().clubFinishedMatches());
        assertEquals(0, profile.totals().minutes());
        assertEquals(0, profile.totals().goals());
        assertNull(profile.totals().averageRating());

        // 3) CONFIRMED state once manual stats are imported
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 minutes, goals, assists, yellow_cards, red_cards, rating)
                VALUES (?, ?, 39, 2026, ?, 'PLAYED', 90, 2, 1, 0, 0, 10.0)
                """, LIV_MUN_FIXTURE, wirtzId, LIV_CLUB);

        mvc.perform(get("/api/matches/" + LIV_MUN_FIXTURE + "/details").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus", is("CONFIRMED")))
                .andExpect(jsonPath("$.provisionalFetchedAt").doesNotExist())
                .andExpect(jsonPath("$.homePlayers", hasSize(1)))
                .andExpect(jsonPath("$.homePlayers[0].rating", is("10.00")));

        mvc.perform(get("/api/players/" + wirtzId + "/matches").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataStatus", is("CONFIRMED")))
                .andExpect(jsonPath("$[0].stats.rating", is("10.00")));

        PlayerSeasonProfileResponse confirmedProfile = profileService.seasonProfile(wirtzId, 2026);
        assertEquals(1, confirmedProfile.totals().appearances());
        assertEquals(1, confirmedProfile.totals().clubFinishedMatches());
        assertEquals(2, confirmedProfile.totals().goals());
    }

    @Test
    void respectsDisabledFlagAndAppliesBackoffOnNetworkFailure() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        AtomicInteger calls = new AtomicInteger();
        FplSource failingSource = new FplSource() {
            @Override public String fetchBootstrapStatic() throws IOException {
                calls.incrementAndGet();
                throw new IOException("FPL timeout");
            }
            @Override public String fetchFixtures(int gw) throws IOException { throw new IOException("FPL timeout"); }
            @Override public String fetchEventLive(int gw) throws IOException { throw new IOException("FPL timeout"); }
            @Override public String fetchElementSummary(int id) throws IOException { throw new IOException("FPL timeout"); }
        };

        Instant base = Instant.parse("2026-11-22T19:30:00Z");
        FplProvisionalSync disabledSync = createSync(failingSource, Clock.fixed(base, ZoneOffset.UTC), false);
        assertEquals(FplProvisionalSync.TickOutcome.DISABLED, disabledSync.runIfDue().outcome());
        assertEquals(0, calls.get());

        AtomicReference<Instant> now = new AtomicReference<>(base);
        Clock mutableClock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        FplProvisionalSync failingSync = createSync(failingSource, mutableClock, true);
        assertEquals(FplProvisionalSync.TickOutcome.FAILED, failingSync.runIfDue().outcome());
        assertEquals(1, calls.get());

        // 5 minutes later -> still in 10-min backoff
        now.set(base.plus(Duration.ofMinutes(5)));
        assertEquals(FplProvisionalSync.TickOutcome.BACKOFF, failingSync.runIfDue().outcome());
        assertEquals(1, calls.get());

        // 11 minutes later -> retries and fails again (backoff becomes 20 min)
        now.set(base.plus(Duration.ofMinutes(11)));
        assertEquals(FplProvisionalSync.TickOutcome.FAILED, failingSync.runIfDue().outcome());
        assertEquals(2, calls.get());
    }

    @Test
    void disablesJobGracefullyWhenBundledMappingsCannotBeLoaded() throws Exception {
        seedRealGw11ClubsAndPlayers(Set.of(LIV_MUN_FIXTURE));
        AtomicInteger calls = new AtomicInteger();
        FplSource countingSource = countingWrapper(FplSource.fromDirectory(REAL_GW11_RAW), calls);
        FplMappingReader brokenReader = new FplMappingReader(
                "data/fpl-2026/does-not-exist-team-map.csv",
                "data/fpl-2026/does-not-exist-player-map.csv",
                List.of(temp.resolve("missing-team.csv")),
                List.of(temp.resolve("missing-player.csv")));

        Clock clock = Clock.fixed(Instant.parse("2026-11-22T19:30:00Z"), ZoneOffset.UTC);
        FplProvisionalSync sync = new FplProvisionalSync(
                jdbc, transactions, collector, parser, countingSource, brokenReader, clock,
                true, Duration.ofMinutes(100), Duration.ofHours(72), Duration.ofHours(6), 10, 60);

        // Must not throw on startup initialization
        sync.initializeMappings();
        assertTrue(sync.isDisabledByMappingError());

        // Subsequent ticks return DISABLED without calling FPL
        assertEquals(FplProvisionalSync.TickOutcome.DISABLED, sync.runIfDue().outcome());
        assertEquals(0, calls.get());
    }

    @Test
    void regressionFplCollectorProducesIdenticalGw11CsvsAfterRefactor() throws Exception {
        // Seed all 9 finished GW11 fixtures present in real backend/data/gw11-2026/fpl/fixture-map.csv
        Set<Integer> gw11Fixtures = new LinkedHashSet<>();
        List<String> mapLines = Files.readAllLines(REAL_GW11_DIR.resolve("fixture-map.csv"), StandardCharsets.UTF_8);
        for (int i = 1; i < mapLines.size(); i++) {
            String line = mapLines.get(i).trim();
            if (!line.isEmpty()) {
                gw11Fixtures.add(Integer.parseInt(line.split(",", -1)[1]));
            }
        }
        seedRealGw11ClubsAndPlayers(gw11Fixtures);

        Path outDir = temp.resolve("gw11-regression");
        FplCollectorService.Result result = collector.collect(
                2026, 11, REAL_MAP_DIR, outDir, FplSource.fromDirectory(REAL_GW11_RAW), gw11Fixtures, true);
        assertTrue(result.finalCsvWritten());
        assertEquals(9, result.fixturesMatched());
        assertEquals(0, result.unmappedCount());

        // Basic match stats CSV must be byte-for-byte identical
        String expectedBasic = Files.readString(REAL_GW11_DIR.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n").trim();
        String actualBasic = Files.readString(outDir.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n").trim();
        assertEquals(expectedBasic, actualBasic, "fpl-match-stats-2026-GW11.csv must match exactly");

        // Extra stats CSV must match identically on all columns except fetched_at for the 8 Batch-2 fixtures
        // whose raw JSON matches backend/data/gw11-2026/fpl/raw/ (Chelsea-Leeds 1000560642 in the saved CSV
        // kept its earlier Batch-1 snapshot before FPL revised Opta xG/xGC).
        List<String> expectedExtra = stripLastColumn(
                Files.readAllLines(REAL_GW11_DIR.resolve("fpl-extra-stats-2026-GW11.csv"), StandardCharsets.UTF_8))
                .stream().filter(l -> !l.startsWith("2026,1000560642,")).toList();
        List<String> actualExtra = stripLastColumn(
                Files.readAllLines(outDir.resolve("fpl-extra-stats-2026-GW11.csv"), StandardCharsets.UTF_8))
                .stream().filter(l -> !l.startsWith("2026,1000560642,")).toList();
        assertEquals(expectedExtra, actualExtra, "fpl-extra-stats-2026-GW11.csv (excluding fetched_at and Batch-1 1000560642) must match exactly");

        // Fixture map CSV must match identically
        String expectedMap = Files.readString(REAL_GW11_DIR.resolve("fixture-map.csv"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n").trim();
        String actualMap = Files.readString(outDir.resolve("fixture-map.csv"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n").trim();
        assertEquals(expectedMap, actualMap, "fixture-map.csv must match exactly");
    }

    private static List<String> stripLastColumn(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            int idx = line.lastIndexOf(',');
            out.add(idx >= 0 ? line.substring(0, idx) : line);
        }
        return out;
    }

    private static FplSource countingWrapper(FplSource delegate, AtomicInteger calls) {
        return new FplSource() {
            @Override public String fetchBootstrapStatic() throws IOException, InterruptedException {
                calls.incrementAndGet();
                return delegate.fetchBootstrapStatic();
            }
            @Override public String fetchFixtures(int gw) throws IOException, InterruptedException {
                calls.incrementAndGet();
                return delegate.fetchFixtures(gw);
            }
            @Override public String fetchEventLive(int gw) throws IOException, InterruptedException {
                calls.incrementAndGet();
                return delegate.fetchEventLive(gw);
            }
            @Override public String fetchElementSummary(int id) throws IOException, InterruptedException {
                calls.incrementAndGet();
                return delegate.fetchElementSummary(id);
            }
        };
    }
}
