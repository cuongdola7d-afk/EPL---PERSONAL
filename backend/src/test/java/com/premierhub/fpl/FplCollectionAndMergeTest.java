package com.premierhub.fpl;

import com.premierhub.manualstats.ManualMatchStatsCsvReader;
import com.premierhub.manualstats.ManualMatchStatsImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:fpl-collect-merge-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class FplCollectionAndMergeTest {
    private static final Path FIXTURES_DIR = Path.of("src/test/resources/fpl");

    @Autowired JdbcTemplate jdbc;
    @Autowired FplCollectorService collector;
    @Autowired FplRatingMergeService merger;
    @Autowired ManualMatchStatsImporter matchStatsImporter;
    @TempDir Path temp;

    private void seedBase() {
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO clubs VALUES (1000000057, 'Arsenal FC', NULL), "
                + "(1000000061, 'Chelsea FC', NULL), (1000000064, 'Liverpool FC', NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, 1000000057), "
                + "(39, 2026, 1000000061), (39, 2026, 1000000064)");
        jdbc.update("INSERT INTO players VALUES (2000001001, 'Alex Alpha'), "
                + "(2000001002, 'Ben Beta'), (2000002001, 'Greg Gamma'), (2000002002, 'Dan Delta')");
        jdbc.update("""
                INSERT INTO player_season_stats (league_id, season_year, player_id, club_id, position)
                VALUES (39, 2026, 2000001001, 1000000057, 'FORWARD'),
                       (39, 2026, 2000001002, 1000000057, 'MIDFIELDER'),
                       (39, 2026, 2000002001, 1000000061, 'DEFENDER'),
                       (39, 2026, 2000002002, 1000000061, 'GOALKEEPER')
                """);
        jdbc.update("""
                INSERT INTO manual_player_memberships (league_id, season_year, player_id, club_id, start_date, end_date)
                VALUES (39, 2026, 2000001001, 1000000057, DATE '2026-08-01', NULL),
                       (39, 2026, 2000001002, 1000000057, DATE '2026-08-01', NULL),
                       (39, 2026, 2000002001, 1000000061, DATE '2026-08-01', NULL),
                       (39, 2026, 2000002002, 1000000061, DATE '2026-08-01', NULL)
                """);
        jdbc.update("""
                INSERT INTO player_season_profiles
                (league_id, season_year, player_id, club_id, shirt_number)
                VALUES (39, 2026, 2000001001, 1000000057, 9),
                       (39, 2026, 2000001002, 1000000057, 17),
                       (39, 2026, 2000002001, 1000000061, 4),
                       (39, 2026, 2000002002, 1000000061, 1)
                """);
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560650, 39, 2026, 1000000057, 1000000061, 11, DATE '2026-11-21',
                        'FINISHED', 'FINISHED', 2, 1, 'hash11', CURRENT_TIMESTAMP)
                """);
    }

    private Path writeMappings(boolean includeBeta) throws Exception {
        Path mapDir = temp.resolve("maps-" + (includeBeta ? "full" : "partial"));
        Files.createDirectories(mapDir);
        Files.writeString(mapDir.resolve("team-map.csv"),
                FplMappingReader.TEAM_HEADER + "\n"
                + "1,1000000057,Arsenal,Arsenal FC\n"
                + "6,1000000061,Chelsea,Chelsea FC\n"
                + "14,1000000064,Liverpool,Liverpool FC\n", StandardCharsets.UTF_8);
        StringBuilder players = new StringBuilder(FplMappingReader.PLAYER_HEADER).append('\n')
                .append("101,2000001001,Alex Alpha,Alex Alpha,test,2026-11-21\n")
                .append("103,2000002001,Greg Gamma,Greg Gamma,test,2026-11-21\n")
                .append("104,2000002002,Dan Delta,Dan Delta,test,2026-11-21\n");
        if (includeBeta) {
            players.append("102,2000001002,Ben Beta,Ben Beta,test,2026-11-21\n");
        }
        Files.writeString(mapDir.resolve("player-map.csv"), players.toString(), StandardCharsets.UTF_8);
        return mapDir;
    }

    @Test
    void stopsWhenAnyFixtureIsNotFinishedEvenIfProvisionalIsTrue() throws Exception {
        seedBase();
        Path mapDir = writeMappings(true);
        Path srcDir = temp.resolve("unfinished-src");
        Files.createDirectories(srcDir);
        Files.copy(FIXTURES_DIR.resolve("bootstrap-static.json"), srcDir.resolve("bootstrap-static.json"));
        Files.copy(FIXTURES_DIR.resolve("event-gw11-live.json"), srcDir.resolve("event-gw11-live.json"));
        String provisional = Files.readString(FIXTURES_DIR.resolve("fixtures-gw11.json"), StandardCharsets.UTF_8)
                .replace("\"finished\": true", "\"finished\": false");
        Files.writeString(srcDir.resolve("fixtures-gw11.json"), provisional, StandardCharsets.UTF_8);

        IllegalStateException err = assertThrows(IllegalStateException.class,
                () -> collector.collect(2026, 11, mapDir, temp.resolve("out-unfinished"), FplSource.fromDirectory(srcDir)));
        assertTrue(err.getMessage().contains("fpl_fixture=101"));
    }

    @Test
    void failsIfFixtureCannotMatchOneToOneByTeamsAndUtcDate() throws Exception {
        seedBase();
        // Change DB date so UTC date does not match FPL kickoff_time 2026-11-21
        jdbc.update("UPDATE fixtures SET match_date = DATE '2026-11-22' WHERE id = 1000560650");
        Path mapDir = writeMappings(true);

        IllegalStateException err = assertThrows(IllegalStateException.class,
                () -> collector.collect(2026, 11, mapDir, temp.resolve("out-bad-date"),
                        FplSource.fromDirectory(FIXTURES_DIR)));
        assertTrue(err.getMessage().contains("1-to-1"));
    }

    @Test
    void writesUnmappedAndOmitsFinalCsvWhenPlayedPlayerIsUnmapped() throws Exception {
        seedBase();
        Path mapDir = writeMappings(false);
        Path outDir = temp.resolve("out-unmapped");

        FplCollectorService.Result result = collector.collect(
                2026, 11, mapDir, outDir, FplSource.fromDirectory(FIXTURES_DIR));
        assertEquals(1, result.fixturesMatched());
        assertEquals(1, result.unmappedCount());
        assertFalse(result.finalCsvWritten());
        assertTrue(Files.exists(outDir.resolve("raw/bootstrap-static.json")));
        assertTrue(Files.exists(outDir.resolve("fixture-map.csv")));
        assertTrue(Files.exists(outDir.resolve("unmapped.csv")));
        assertTrue(Files.readString(outDir.resolve("unmapped.csv")).contains("102,Ben Beta"));
        assertFalse(Files.exists(outDir.resolve("fpl-match-stats-2026-GW11.csv")));
        assertFalse(Files.exists(outDir.resolve("fpl-extra-stats-2026-GW11.csv")));
    }

    @Test
    void splitsDoubleGameweekOnlyForMultiFixtureElementViaElementSummary() throws Exception {
        seedBase();
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560651, 39, 2026, 1000000057, 1000000064, 11, DATE '2026-11-24',
                        'FINISHED', 'FINISHED', 1, 0, 'hash11b', CURRENT_TIMESTAMP)
                """);
        Path mapDir = writeMappings(true);
        Path dgwSrc = temp.resolve("dgw-src");
        Files.createDirectories(dgwSrc);
        Files.copy(FIXTURES_DIR.resolve("bootstrap-static.json"), dgwSrc.resolve("bootstrap-static.json"));
        Files.copy(FIXTURES_DIR.resolve("element-summary-101.json"), dgwSrc.resolve("element-summary-101.json"));
        Files.writeString(dgwSrc.resolve("fixtures-gw11.json"), """
                [
                  {"id":101,"event":11,"kickoff_time":"2026-11-21T15:00:00Z","team_h":1,"team_a":6,"team_h_score":2,"team_a_score":1,"finished":true,"finished_provisional":true},
                  {"id":102,"event":11,"kickoff_time":"2026-11-24T20:00:00Z","team_h":1,"team_a":14,"team_h_score":1,"team_a_score":0,"finished":true,"finished_provisional":true}
                ]
                """, StandardCharsets.UTF_8);
        String live = Files.readString(FIXTURES_DIR.resolve("event-gw11-live.json"), StandardCharsets.UTF_8)
                .replace(
                        "{\"fixture\": 101, \"stats\": [{\"identifier\": \"minutes\", \"points\": 2, \"value\": 90}]}",
                        "{\"fixture\": 101, \"stats\": [{\"identifier\": \"minutes\", \"points\": 2, \"value\": 90}]},"
                        + "{\"fixture\": 102, \"stats\": [{\"identifier\": \"minutes\", \"points\": 2, \"value\": 65}]}");
        Files.writeString(dgwSrc.resolve("event-gw11-live.json"), live, StandardCharsets.UTF_8);

        AtomicInteger summaryCalls = new AtomicInteger();
        FplSource delegate = FplSource.fromDirectory(dgwSrc);
        FplSource counting = new FplSource() {
            @Override public String fetchBootstrapStatic() throws java.io.IOException, InterruptedException { return delegate.fetchBootstrapStatic(); }
            @Override public String fetchFixtures(int gw) throws java.io.IOException, InterruptedException { return delegate.fetchFixtures(gw); }
            @Override public String fetchEventLive(int gw) throws java.io.IOException, InterruptedException { return delegate.fetchEventLive(gw); }
            @Override public String fetchElementSummary(int id) throws java.io.IOException, InterruptedException {
                summaryCalls.incrementAndGet();
                return delegate.fetchElementSummary(id);
            }
        };

        Path outDir = temp.resolve("out-dgw");
        FplCollectorService.Result result = collector.collect(2026, 11, mapDir, outDir, counting);
        assertTrue(result.finalCsvWritten());
        assertEquals(2, result.fixturesMatched());
        assertEquals(1, summaryCalls.get());
        assertEquals(1, result.elementSummaryCalls());
        assertEquals(3, result.playedRows()); // element 101 x2 fixtures + element 102 x1 fixture
        assertTrue(Files.exists(outDir.resolve("raw/element-summary-101.json")));
        String statsCsv = Files.readString(outDir.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8);
        assertTrue(statsCsv.contains("1000560650,2000001001,90,1,0,1,0"));
        assertTrue(statsCsv.contains("1000560651,2000001001,65,0,1,0,0"));
        assertTrue(statsCsv.contains("1000560650,2000001002,2,0,0,0,0"));
    }

    @Test
    void mergesFourRulesAndProducesImportableGw10FormatAndDetectsAllThreeAnomalies() throws Exception {
        seedBase();
        Path mapDir = writeMappings(true);
        Path fplOut = temp.resolve("fpl-gw11");
        FplCollectorService.Result collected = collector.collect(
                2026, 11, mapDir, fplOut, FplSource.fromDirectory(FIXTURES_DIR));
        assertTrue(collected.finalCsvWritten());

        // Case A: Happy path covering PLAYED+rating, PLAYED+NO_RATING_SHOWN, DID_NOT_PLAY (via GW10 screenshot-evidence format!)
        Path ratingsClean = temp.resolve("screenshot-evidence-clean.csv");
        Files.writeString(ratingsClean,
                "season,gameweek,fixture_id,club_id,club,side,pitch_image,substitutes_image,shirt_number,display_name,role,rating,substitution_in_minute,substitution_out_minute,replaced_shirt_number,pitch_row_index,pitch_slot_index,visible_symbols\n"
                + "2026,11,1000560650,1000000057,Arsenal FC,HOME,1,2,9,A. Alpha,STARTER,8.3,,,,4,0,\n"
                + "2026,11,1000560650,1000000057,Arsenal FC,HOME,1,2,17,Ben Beta,SUB_USED,,88,,9,,,NO_RATING_SHOWN\n"
                + "2026,11,1000560650,1000000061,Chelsea FC,AWAY,1,2,4,Greg Gamma,SUB_UNUSED,,,,,,,\n",
                StandardCharsets.UTF_8);

        Path mergeCleanDir = temp.resolve("merge-clean");
        FplRatingMergeService.Result merged = merger.merge(2026, 11, fplOut, ratingsClean, null, mergeCleanDir);
        assertEquals(1, merged.fixturesIncluded());
        assertEquals(0, merged.fixturesBlocked());
        assertEquals(3, merged.statRowsWritten());
        assertEquals(1, merged.unratedCount());
        assertEquals(0, merged.missingCount());
        assertEquals(0, merged.anomalyCount());

        Path manualStatsCsv = mergeCleanDir.resolve("manual-match-stats-2026-GW11.csv");
        List<ManualMatchStatsCsvReader.Row> parsed = new ManualMatchStatsCsvReader().read(manualStatsCsv);
        assertEquals(3, parsed.size());
        assertEquals("PLAYED", parsed.get(0).status());
        assertEquals(new BigDecimal("8.30"), parsed.get(0).rating());
        assertEquals(90, parsed.get(0).minutes());
        assertEquals(1, parsed.get(0).goals());
        assertEquals(1, parsed.get(0).assists());
        assertEquals("PLAYED", parsed.get(1).status());
        assertNull(parsed.get(1).rating());
        assertEquals(2, parsed.get(1).minutes());
        assertEquals("DID_NOT_PLAY", parsed.get(2).status());
        assertNull(parsed.get(2).rating());
        assertEquals(0, parsed.get(2).minutes());
        assertTrue(Files.readString(mergeCleanDir.resolve("confirmed-unrated.csv"))
                .contains("1000560650,2000001002,Ben Beta,2,,"));
        // Verify merged output imports cleanly via existing ManualMatchStatsImporter
        assertEquals(3, matchStatsImporter.importFile(manualStatsCsv).inserted());

        // Case B: Anomalies — (1) FPL played not in squad (Ben Beta missing), (2) Rating with 0 minutes (Greg Gamma has rating 6.5)
        Path ratingsAnomalies = temp.resolve("ratings-anomalies.csv");
        Files.writeString(ratingsAnomalies,
                "fixture_id,player_id,name,club,rating,visible_symbols\n"
                + "1000560650,2000001001,Alex Alpha,Arsenal FC,8.3,\n"
                + "1000560650,2000002001,Greg Gamma,Chelsea FC,6.5,\n",
                StandardCharsets.UTF_8);
        Path mergeBad1 = temp.resolve("merge-bad-1");
        FplRatingMergeService.Result bad1 = merger.merge(2026, 11, fplOut, ratingsAnomalies, null, mergeBad1);
        assertEquals(0, bad1.fixturesIncluded());
        assertEquals(1, bad1.fixturesBlocked());
        assertEquals(2, bad1.anomalyCount());
        assertFalse(Files.exists(mergeBad1.resolve("manual-match-stats-2026-GW11.csv")));
        String anomCsv1 = Files.readString(mergeBad1.resolve("anomalies.csv"), StandardCharsets.UTF_8);
        assertTrue(anomCsv1.contains("FPL_PLAYED_NOT_IN_SQUAD"));
        assertTrue(anomCsv1.contains("RATING_WITH_ZERO_MINUTES"));

        // Case C: Anomaly (3) — FPL score differs from DB fixtures score
        jdbc.update("UPDATE fixtures SET home_goals = 3 WHERE id = 1000560650");
        Path mergeBad2 = temp.resolve("merge-bad-2");
        FplRatingMergeService.Result bad2 = merger.merge(2026, 11, fplOut, ratingsClean, null, mergeBad2);
        assertEquals(0, bad2.fixturesIncluded());
        assertEquals(1, bad2.fixturesBlocked());
        assertFalse(Files.exists(mergeBad2.resolve("manual-match-stats-2026-GW11.csv")));
        assertTrue(Files.readString(mergeBad2.resolve("anomalies.csv")).contains("SCORE_MISMATCH"));
    }

    @Test
    void flagsBenchCardAnomalyAndOmitsFixtureFromFinalCsvForYellowOrRedCardWithZeroMinutes() throws Exception {
        seedBase();
        Path mapDir = writeMappings(true);

        Path ratingsClean = temp.resolve("ratings-bench-card.csv");
        Files.writeString(ratingsClean,
                "fixture_id,player_id,name,club,rating,visible_symbols\n"
                + "1000560650,2000001001,Alex Alpha,Arsenal FC,8.3,\n"
                + "1000560650,2000001002,Ben Beta,Arsenal FC,,NO_RATING_SHOWN\n"
                + "1000560650,2000002001,Greg Gamma,Chelsea FC,,\n",
                StandardCharsets.UTF_8);

        // Case 1: unused sub receives yellow card on the bench (minutes=0, yellow_cards=1)
        Path yellowSrc = temp.resolve("bench-yellow-src");
        Files.createDirectories(yellowSrc);
        Files.copy(FIXTURES_DIR.resolve("bootstrap-static.json"), yellowSrc.resolve("bootstrap-static.json"));
        Files.copy(FIXTURES_DIR.resolve("fixtures-gw11.json"), yellowSrc.resolve("fixtures-gw11.json"));
        String baseLive = Files.readString(FIXTURES_DIR.resolve("event-gw11-live.json"), StandardCharsets.UTF_8);
        int idx103 = baseLive.indexOf("\"id\": 103");
        String yellowLive = baseLive.substring(0, idx103)
                + baseLive.substring(idx103).replaceFirst("\"yellow_cards\": 0", "\"yellow_cards\": 1");
        Files.writeString(yellowSrc.resolve("event-gw11-live.json"), yellowLive, StandardCharsets.UTF_8);

        Path fplYellow = temp.resolve("fpl-bench-yellow");
        FplCollectorService.Result collectedYellow = collector.collect(
                2026, 11, mapDir, fplYellow, FplSource.fromDirectory(yellowSrc));
        assertTrue(collectedYellow.finalCsvWritten());
        assertEquals(2, collectedYellow.playedRows());
        assertTrue(Files.readString(fplYellow.resolve("bench-cards.csv"), StandardCharsets.UTF_8)
                .contains("1000560650,2000002001,1,0"));
        assertFalse(Files.readString(fplYellow.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8)
                .contains("2000002001"));

        Path mergeYellow = temp.resolve("merge-bench-yellow");
        FplRatingMergeService.Result mergedYellow = merger.merge(2026, 11, fplYellow, ratingsClean, null, mergeYellow);
        assertEquals(0, mergedYellow.fixturesIncluded());
        assertEquals(1, mergedYellow.fixturesBlocked());
        assertEquals(0, mergedYellow.statRowsWritten());
        assertEquals(1, mergedYellow.anomalyCount());
        assertFalse(Files.exists(mergeYellow.resolve("manual-match-stats-2026-GW11.csv")));
        String yellowAnom = Files.readString(mergeYellow.resolve("anomalies.csv"), StandardCharsets.UTF_8);
        assertTrue(yellowAnom.contains("1000560650,2000002001,BENCH_CARD,"));
        assertTrue(yellowAnom.contains("yellow_cards=1"));

        // Case 2: unused sub receives straight red card on the bench (minutes=0, red_cards=1)
        Path redSrc = temp.resolve("bench-red-src");
        Files.createDirectories(redSrc);
        Files.copy(FIXTURES_DIR.resolve("bootstrap-static.json"), redSrc.resolve("bootstrap-static.json"));
        Files.copy(FIXTURES_DIR.resolve("fixtures-gw11.json"), redSrc.resolve("fixtures-gw11.json"));
        String redLive = baseLive.substring(0, idx103)
                + baseLive.substring(idx103).replaceFirst("\"red_cards\": 0", "\"red_cards\": 1");
        Files.writeString(redSrc.resolve("event-gw11-live.json"), redLive, StandardCharsets.UTF_8);

        Path fplRed = temp.resolve("fpl-bench-red");
        collector.collect(2026, 11, mapDir, fplRed, FplSource.fromDirectory(redSrc));
        assertTrue(Files.readString(fplRed.resolve("bench-cards.csv"), StandardCharsets.UTF_8)
                .contains("1000560650,2000002001,0,1"));

        Path mergeRed = temp.resolve("merge-bench-red");
        FplRatingMergeService.Result mergedRed = merger.merge(2026, 11, fplRed, ratingsClean, null, mergeRed);
        assertEquals(0, mergedRed.fixturesIncluded());
        assertEquals(1, mergedRed.fixturesBlocked());
        assertEquals(1, mergedRed.anomalyCount());
        assertFalse(Files.exists(mergeRed.resolve("manual-match-stats-2026-GW11.csv")));
        String redAnom = Files.readString(mergeRed.resolve("anomalies.csv"), StandardCharsets.UTF_8);
        assertTrue(redAnom.contains("1000560650,2000002001,BENCH_CARD,"));
        assertTrue(redAnom.contains("red_cards=1"));
    }

    @Test
    void matchesFixtureAcrossWholeSeasonEvenWhenDbGameweekDiffersFromFplEvent() throws Exception {
        seedBase();
        // Simulate a fixture placed in GW10 in DB but moved to FPL event 11 (same clubs & match date)
        jdbc.update("UPDATE fixtures SET gameweek = 10 WHERE id = 1000560650");
        Path mapDir = writeMappings(true);
        Path outDir = temp.resolve("out-cross-event");

        FplCollectorService.Result result = collector.collect(
                2026, 11, mapDir, outDir, FplSource.fromDirectory(FIXTURES_DIR));
        assertTrue(result.finalCsvWritten());
        assertEquals(1, result.fixturesMatched());
        assertTrue(Files.readString(outDir.resolve("fixture-map.csv"), StandardCharsets.UTF_8)
                .contains("101,1000560650,2026-11-21T15:00:00Z,1000000057,1000000061"));
    }

    @Test
    void blocksRatingMergeForGameweekBelow11ToProtectStatMuseStats() throws Exception {
        seedBase();
        Path dummyRatings = temp.resolve("dummy-ratings.csv");
        Files.writeString(dummyRatings, "fixture_id,player_id,rating\n", StandardCharsets.UTF_8);

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> merger.merge(2026, 10, temp, dummyRatings, null, temp.resolve("merge-gw10")));
        assertTrue(err.getMessage().contains("GW11"));
        assertFalse(Files.exists(temp.resolve("merge-gw10/manual-match-stats-2026-GW10.csv")));
    }

    @Test
    void backfillConsistencyExcludesMismatchesAndUnmappedIntoConsistencyCsv() throws Exception {
        seedBase();
        // Seed DB manual_fixture_player_stats for fixture 1000560650:
        // - 2000001001 (Alex Alpha, FPL 101, 90m): PLAYED -> eligible!
        // - 2000001002 (Ben Beta, FPL 102, 2m): NOT in player-map -> UNMAPPED_PLAYER!
        // - 2000002001 (Greg Gamma, FPL 103, 0m): PLAYED in DB -> DB_PLAYED_FPL_ZERO!
        // - 2000002002 (Dan Delta, FPL 104, added with 15m in live): DID_NOT_PLAY in DB -> FPL_PLAYED_DB_NOT_PLAYED!
        jdbc.update("""
                INSERT INTO manual_fixture_player_stats
                (fixture_id, player_id, league_id, season_year, club_id, participation_status,
                 rating, fantasy_points, minutes, goals, assists, yellow_cards, red_cards)
                VALUES (1000560650, 2000001001, 39, 2026, 1000000057, 'PLAYED', 8.00, 8.00, 90, 1, 1, 1, 0),
                       (1000560650, 2000002001, 39, 2026, 1000000061, 'PLAYED', 6.50, 6.50, 10, 0, 0, 0, 0),
                       (1000560650, 2000002002, 39, 2026, 1000000061, 'DID_NOT_PLAY', NULL, 0.00, 0, 0, 0, 0, 0)
                """);

        Path mapDir = writeMappings(false); // Ben Beta (102) unmapped; 101, 103, 104 mapped
        Path srcDir = temp.resolve("backfill-consistency-src");
        Files.createDirectories(srcDir);
        Files.copy(FIXTURES_DIR.resolve("bootstrap-static.json"), srcDir.resolve("bootstrap-static.json"));
        Files.copy(FIXTURES_DIR.resolve("fixtures-gw11.json"), srcDir.resolve("fixtures-gw11.json"));
        String baseLive = Files.readString(FIXTURES_DIR.resolve("event-gw11-live.json"), StandardCharsets.UTF_8);
        String extraEl104 = """
                ,
                    {
                      "id": 104,
                      "stats": {
                        "minutes": 15, "goals_scored": 0, "assists": 0, "clean_sheets": 0, "goals_conceded": 0,
                        "own_goals": 0, "penalties_saved": 0, "penalties_missed": 0, "yellow_cards": 0, "red_cards": 0,
                        "saves": 1, "bonus": 0, "bps": 5, "influence": "2.0", "creativity": "0.0", "threat": "0.0",
                        "ict_index": "0.2", "clearances_blocks_interceptions": 0, "recoveries": 1, "tackles": 0,
                        "defensive_contribution": 0, "starts": 0, "expected_goals": "0.00", "expected_assists": "0.00",
                        "expected_goal_involvements": "0.00", "expected_goals_conceded": "0.10"
                      },
                      "explain": [{"fixture": 101, "stats": [{"identifier": "minutes", "points": 1, "value": 15}]}]
                    }
                  ]
                }
                """;
        int lastBracket = baseLive.lastIndexOf(']');
        String modifiedLive = baseLive.substring(0, lastBracket) + extraEl104;
        Files.writeString(srcDir.resolve("event-gw11-live.json"), modifiedLive, StandardCharsets.UTF_8);

        Path outDir = temp.resolve("out-backfill-consistency");
        FplCollectorService.Result result = collector.collectBackfill(
                2026, 11, mapDir, outDir, FplSource.fromDirectory(srcDir));
        assertTrue(result.finalCsvWritten());
        assertEquals(1, result.playedRows()); // Only 2000001001 eligible for extra-stats import!
        assertEquals(1, result.unmappedCount());

        String extraCsv = Files.readString(outDir.resolve("fpl-extra-stats-2026-GW11.csv"), StandardCharsets.UTF_8);
        assertTrue(extraCsv.contains("2026,1000560650,2000001001,"));
        assertFalse(extraCsv.contains("2000001002"));
        assertFalse(extraCsv.contains("2000002001"));
        assertFalse(extraCsv.contains("2000002002"));

        String consistencyCsv = Files.readString(outDir.resolve("consistency.csv"), StandardCharsets.UTF_8);
        assertTrue(consistencyCsv.contains(",102,Ben Beta,UNMAPPED_PLAYER,2,,"));
        assertTrue(consistencyCsv.contains("1000560650,2000002002,104,Dan Delta,FPL_PLAYED_DB_NOT_PLAYED,15,DID_NOT_PLAY,0"));
        assertTrue(consistencyCsv.contains("1000560650,2000002001,103,Greg Gamma,DB_PLAYED_FPL_ZERO,0,PLAYED,10"));
    }

    private Path createTwoFixtureGw11Source(
            String dirName,
            boolean f101Finished, boolean f101Provisional,
            boolean f102Finished, boolean f102Provisional,
            int el101Assists
    ) throws Exception {
        Path src = temp.resolve(dirName);
        Files.createDirectories(src);
        Files.writeString(src.resolve("bootstrap-static.json"), """
                {
                  "teams": [
                    {"id": 1, "name": "Arsenal", "short_name": "ARS"},
                    {"id": 6, "name": "Chelsea", "short_name": "CHE"},
                    {"id": 14, "name": "Liverpool", "short_name": "LIV"}
                  ],
                  "elements": [
                    {"id": 101, "code": 1001, "web_name": "Alpha", "first_name": "Alex", "second_name": "Alpha", "team": 1},
                    {"id": 102, "code": 1002, "web_name": "Beta", "first_name": "Ben", "second_name": "Beta", "team": 1},
                    {"id": 103, "code": 1003, "web_name": "Gamma", "first_name": "Greg", "second_name": "Gamma", "team": 6},
                    {"id": 104, "code": 1004, "web_name": "Delta", "first_name": "Dan", "second_name": "Delta", "team": 6}
                  ],
                  "events": [{"id": 11, "name": "Gameweek 11", "finished": false}]
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(src.resolve("fixtures-gw11.json"), String.format("""
                [
                  {"id":101,"event":11,"kickoff_time":"2026-11-21T15:00:00Z","team_h":1,"team_a":6,"team_h_score":2,"team_a_score":1,"finished":%s,"finished_provisional":%s},
                  {"id":102,"event":11,"kickoff_time":"2026-11-22T16:30:00Z","team_h":14,"team_a":6,"team_h_score":1,"team_a_score":0,"finished":%s,"finished_provisional":%s}
                ]
                """, f101Finished, f101Provisional, f102Finished, f102Provisional), StandardCharsets.UTF_8);
        Files.writeString(src.resolve("event-gw11-live.json"), String.format("""
                {
                  "elements": [
                    {
                      "id": 101,
                      "stats": {
                        "minutes": 90, "goals_scored": 1, "assists": %d, "clean_sheets": 0, "goals_conceded": 1,
                        "own_goals": 0, "penalties_saved": 0, "penalties_missed": 0, "yellow_cards": 0, "red_cards": 0,
                        "saves": 0, "bonus": 2, "bps": 30, "influence": "25.0", "creativity": "15.0", "threat": "30.0",
                        "ict_index": "7.0", "clearances_blocks_interceptions": 1, "recoveries": 3, "tackles": 1,
                        "defensive_contribution": 2, "starts": 1, "expected_goals": "0.55", "expected_assists": "0.20",
                        "expected_goal_involvements": "0.75", "expected_goals_conceded": "0.90"
                      },
                      "explain": [{"fixture": 101, "stats": [{"identifier": "minutes", "points": 2, "value": 90}]}]
                    },
                    {
                      "id": 103,
                      "stats": {
                        "minutes": 80, "goals_scored": 0, "assists": 0, "clean_sheets": 0, "goals_conceded": 1,
                        "own_goals": 0, "penalties_saved": 0, "penalties_missed": 0, "yellow_cards": 0, "red_cards": 0,
                        "saves": 0, "bonus": 0, "bps": 12, "influence": "10.0", "creativity": "5.0", "threat": "4.0",
                        "ict_index": "1.9", "clearances_blocks_interceptions": 3, "recoveries": 4, "tackles": 2,
                        "defensive_contribution": 5, "starts": 1, "expected_goals": "0.05", "expected_assists": "0.02",
                        "expected_goal_involvements": "0.07", "expected_goals_conceded": "1.10"
                      },
                      "explain": [{"fixture": 102, "stats": [{"identifier": "minutes", "points": 2, "value": 80}]}]
                    }
                  ]
                }
                """, el101Assists), StandardCharsets.UTF_8);
        return src;
    }

    private void seedSecondFixture() {
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560651, 39, 2026, 1000000064, 1000000061, 11, DATE '2026-11-22',
                        'FINISHED', 'FINISHED', 1, 0, 'hash11b', CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void fixtureIdsCollectsOnlySelectedFinishedFixtureWhileOtherGwFixtureIsUnfinished() throws Exception {
        seedBase();
        seedSecondFixture();
        Path mapDir = writeMappings(true);
        Path src = createTwoFixtureGw11Source("src-partial-fin", true, true, false, false, 1);
        Path out = temp.resolve("out-partial-fin");

        // Without --fixture-ids: still rejects because fixture 102 is unfinished
        IllegalStateException fullErr = assertThrows(IllegalStateException.class,
                () -> collector.collect(2026, 11, mapDir, out, FplSource.fromDirectory(src)));
        assertTrue(fullErr.getMessage().contains("fpl_fixture=102"));

        // With --fixture-ids=1000560650: succeeds and exports ONLY fixture 1000560650
        FplCollectorService.Result result = collector.collect(
                2026, 11, mapDir, out, FplSource.fromDirectory(src), Set.of(1000560650), false);
        assertTrue(result.finalCsvWritten());
        assertEquals(1, result.fixturesMatched());
        assertEquals(0, result.provisionalFixtures());
        assertEquals(1, result.playedRows());

        String fixtureMap = Files.readString(out.resolve("fixture-map.csv"), StandardCharsets.UTF_8);
        assertTrue(fixtureMap.contains("101,1000560650,"));
        assertTrue(fixtureMap.contains(",FINISHED"));
        assertFalse(fixtureMap.contains("1000560651"));

        String statsCsv = Files.readString(out.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8);
        assertTrue(statsCsv.contains("1000560650,2000001001,90,1,1,0,0"));
        assertFalse(statsCsv.contains("1000560651"));
        assertFalse(statsCsv.contains("2000002001"));
    }

    @Test
    void fixtureIdsProvisionalHandlingStopsWithoutFlagWarnsWithFlagAndStopsIfNotProvisional() throws Exception {
        seedBase();
        seedSecondFixture();
        Path mapDir = writeMappings(true);
        Path src = createTwoFixtureGw11Source("src-provisional", false, true, false, false, 1);

        // 1. finished_provisional=true without allowProvisional -> stops clearly
        IllegalStateException noFlagErr = assertThrows(IllegalStateException.class,
                () -> collector.collect(2026, 11, mapDir, temp.resolve("out-prov-deny"),
                        FplSource.fromDirectory(src), Set.of(1000560650), false));
        assertTrue(noFlagErr.getMessage().contains("1000560650"));
        assertTrue(noFlagErr.getMessage().contains("allow-provisional"));

        // 2. finished_provisional=true with allowProvisional=true -> succeeds and marks PROVISIONAL
        Path outProv = temp.resolve("out-prov-allow");
        FplCollectorService.Result ok = collector.collect(
                2026, 11, mapDir, outProv, FplSource.fromDirectory(src), Set.of(1000560650), true);
        assertTrue(ok.finalCsvWritten());
        assertEquals(1, ok.fixturesMatched());
        assertEquals(1, ok.provisionalFixtures());
        String mapCsv = Files.readString(outProv.resolve("fixture-map.csv"), StandardCharsets.UTF_8);
        assertTrue(mapCsv.contains("fpl_status"));
        assertTrue(mapCsv.contains("101,1000560650,"));
        assertTrue(mapCsv.contains(",PROVISIONAL"));

        // 3. Not even finished_provisional=true (fixture 1000560651) with allowProvisional=true -> STILL stops!
        IllegalStateException notProvErr = assertThrows(IllegalStateException.class,
                () -> collector.collect(2026, 11, mapDir, temp.resolve("out-not-prov"),
                        FplSource.fromDirectory(src), Set.of(1000560651), true));
        assertTrue(notProvErr.getMessage().contains("1000560651"));
    }

    @Test
    void fixtureIdsRejectsFixtureNotBelongingToGameweek() throws Exception {
        seedBase();
        // Add a fixture in GW12 that is not in FPL GW11 JSON
        jdbc.update("""
                INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                  gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                VALUES (1000560699, 39, 2026, 1000000064, 1000000057, 12, DATE '2026-11-28',
                        'FINISHED', 'FINISHED', 2, 2, 'hash12', CURRENT_TIMESTAMP)
                """);
        Path mapDir = writeMappings(true);
        Path src = createTwoFixtureGw11Source("src-bad-id", true, true, true, true, 1);

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> collector.collect(2026, 11, mapDir, temp.resolve("out-bad-id"),
                        FplSource.fromDirectory(src), Set.of(1000560699), false));
        assertTrue(err.getMessage().contains("1000560699"));
        assertTrue(err.getMessage().contains("GW11"));
    }

    @Test
    void fixtureIdsIncrementalRunsMergeIntoExistingFilesDeduplicateRepeatsAndRejectConflicts() throws Exception {
        seedBase();
        seedSecondFixture();
        Path mapDir = writeMappings(true);
        Path src = createTwoFixtureGw11Source("src-inc", true, true, true, true, 1);
        Path fplOut = temp.resolve("out-inc-fpl");
        Path mergeOut = temp.resolve("out-inc-merge");

        // Run 1: collect fixture 1000560650
        collector.collect(2026, 11, mapDir, fplOut, FplSource.fromDirectory(src), Set.of(1000560650), false);
        // Run 2: collect fixture 1000560651 -> merges both fixtures into the same GW11 files
        collector.collect(2026, 11, mapDir, fplOut, FplSource.fromDirectory(src), Set.of(1000560651), false);
        // Run 3: repeat fixture 1000560650 with identical values -> no duplicate rows!
        collector.collect(2026, 11, mapDir, fplOut, FplSource.fromDirectory(src), Set.of(1000560650), false);

        List<String> mapLines = Files.readAllLines(fplOut.resolve("fixture-map.csv"), StandardCharsets.UTF_8);
        assertEquals(3, mapLines.size()); // header + 2 fixtures
        List<String> matchLines = Files.readAllLines(fplOut.resolve("fpl-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8);
        assertEquals(3, matchLines.size()); // header + 2 players across 2 fixtures
        List<String> extraLines = Files.readAllLines(fplOut.resolve("fpl-extra-stats-2026-GW11.csv"), StandardCharsets.UTF_8);
        assertEquals(3, extraLines.size()); // header + 2 players

        // Conflict on same (fixture_id, player_id) with different assists -> throws!
        Path srcConflict = createTwoFixtureGw11Source("src-inc-conflict", true, true, true, true, 2);
        IllegalStateException conflictErr = assertThrows(IllegalStateException.class,
                () -> collector.collect(2026, 11, mapDir, fplOut, FplSource.fromDirectory(srcConflict), Set.of(1000560650), false));
        assertTrue(conflictErr.getMessage().contains("Conflict"));
        assertTrue(conflictErr.getMessage().contains("1000560650"));

        // Incremental rating merge across two fixtures + repeat + conflict detection
        Path ratings1 = temp.resolve("ratings-f1.csv");
        Files.writeString(ratings1,
                "fixture_id,player_id,name,club,rating,visible_symbols\n"
                + "1000560650,2000001001,Alex Alpha,Arsenal FC,8.1,\n"
                + "1000560650,2000001002,Ben Beta,Arsenal FC,,\n", StandardCharsets.UTF_8);
        Path ratings2 = temp.resolve("ratings-f2.csv");
        Files.writeString(ratings2,
                "fixture_id,player_id,name,club,rating,visible_symbols\n"
                + "1000560651,2000002001,Greg Gamma,Chelsea FC,7.4,\n"
                + "1000560651,2000002002,Dan Delta,Chelsea FC,,\n", StandardCharsets.UTF_8);

        merger.merge(2026, 11, fplOut, ratings1, null, mergeOut, Set.of(1000560650));
        merger.merge(2026, 11, fplOut, ratings2, null, mergeOut, Set.of(1000560651));
        merger.merge(2026, 11, fplOut, ratings1, null, mergeOut, Set.of(1000560650)); // idempotent repeat

        List<String> manualLines = Files.readAllLines(mergeOut.resolve("manual-match-stats-2026-GW11.csv"), StandardCharsets.UTF_8);
        assertEquals(5, manualLines.size()); // header + 4 rows (2 per fixture)

        Path ratingsConflict = temp.resolve("ratings-f1-conflict.csv");
        Files.writeString(ratingsConflict,
                "fixture_id,player_id,name,club,rating,visible_symbols\n"
                + "1000560650,2000001001,Alex Alpha,Arsenal FC,8.9,\n"
                + "1000560650,2000001002,Ben Beta,Arsenal FC,,\n", StandardCharsets.UTF_8);
        IllegalStateException mergeConflict = assertThrows(IllegalStateException.class,
                () -> merger.merge(2026, 11, fplOut, ratingsConflict, null, mergeOut, Set.of(1000560650)));
        assertTrue(mergeConflict.getMessage().contains("Conflict"));
        assertTrue(mergeConflict.getMessage().contains("2000001001"));
    }
}
