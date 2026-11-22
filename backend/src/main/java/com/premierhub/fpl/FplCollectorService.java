package com.premierhub.fpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class FplCollectorService {
    private static final Logger log = LoggerFactory.getLogger(FplCollectorService.class);
    public static final int LEAGUE_ID = 39;
    public static final int SEASON_YEAR = 2026;
    public static final String FIXTURE_MAP_LEGACY_HEADER =
            "fpl_fixture_id,fixture_id,kickoff_utc,home_club_id,away_club_id,"
            + "fpl_home_goals,fpl_away_goals,db_home_goals,db_away_goals";
    public static final String FIXTURE_MAP_HEADER =
            FIXTURE_MAP_LEGACY_HEADER + ",fpl_status";
    public static final String MATCH_STATS_HEADER =
            "fixture_id,player_id,minutes,goals,assists,yellow_cards,red_cards";
    public static final String UNMAPPED_HEADER =
            "fpl_element_id,fpl_name,fpl_web_name,fpl_team_id,club_id,fixture_id,minutes";
    public static final String BENCH_CARDS_HEADER =
            "fixture_id,player_id,yellow_cards,red_cards";
    public static final String CONSISTENCY_HEADER =
            "fixture_id,player_id,fpl_element_id,player_name,reason,fpl_minutes,db_status,db_minutes";

    private final JdbcTemplate jdbc;
    private final FplJsonParser parser;
    private final FplMappingReader mappingReader = new FplMappingReader();
    private final Clock clock;

    @Autowired
    public FplCollectorService(JdbcTemplate jdbc, FplJsonParser parser) {
        this(jdbc, parser, Clock.systemUTC());
    }

    FplCollectorService(JdbcTemplate jdbc, FplJsonParser parser, Clock clock) {
        this.jdbc = jdbc;
        this.parser = parser;
        this.clock = clock;
    }

    public Result collect(int season, int gameweek, Path mappingDir, Path outputDir, FplSource source)
            throws IOException, InterruptedException {
        return collectInternal(season, gameweek, mappingDir, outputDir, source, Set.of(), false, false);
    }

    public Result collect(int season, int gameweek, Path mappingDir, Path outputDir, FplSource source,
                          Set<Integer> fixtureIds, boolean allowProvisional)
            throws IOException, InterruptedException {
        return collectInternal(season, gameweek, mappingDir, outputDir, source,
                fixtureIds == null ? Set.of() : fixtureIds, allowProvisional, false);
    }

    public Result collectBackfill(int season, int gameweek, Path mappingDir, Path outputDir, FplSource source)
            throws IOException, InterruptedException {
        return collectInternal(season, gameweek, mappingDir, outputDir, source, Set.of(), false, true);
    }

    private Result collectInternal(int season, int gameweek, Path mappingDir, Path outputDir,
                                   FplSource source, Set<Integer> requestedFixtureIds,
                                   boolean allowProvisional, boolean backfillConsistency)
            throws IOException, InterruptedException {
        if (season != SEASON_YEAR) {
            throw new IllegalArgumentException("Only season 2026 is supported, got " + season);
        }
        if (gameweek < 1 || gameweek > 38) {
            throw new IllegalArgumentException("gameweek must be between 1 and 38, got " + gameweek);
        }
        boolean filteredMode = !requestedFixtureIds.isEmpty();
        if (allowProvisional && !filteredMode) {
            throw new IllegalArgumentException("--allow-provisional=true requires --fixture-ids=<id1,id2,...>");
        }

        Path rawDir = outputDir.resolve("raw");
        Files.createDirectories(rawDir);

        String gwTag = String.format("gw%02d", gameweek);
        String gwUpper = String.format("GW%02d", gameweek);

        String bootstrapJson = source.fetchBootstrapStatic();
        Files.writeString(rawDir.resolve("bootstrap-static.json"), bootstrapJson, StandardCharsets.UTF_8);

        String fixturesJson = source.fetchFixtures(gameweek);
        Files.writeString(rawDir.resolve("fixtures-" + gwTag + ".json"), fixturesJson, StandardCharsets.UTF_8);

        String liveJson = source.fetchEventLive(gameweek);
        Files.writeString(rawDir.resolve("event-" + gwTag + "-live.json"), liveJson, StandardCharsets.UTF_8);

        FplModels.Bootstrap bootstrap = parser.parseBootstrap(bootstrapJson);
        List<FplModels.FplFixture> fplFixtures = parser.parseFixtures(fixturesJson, gameweek);
        List<FplModels.LiveElement> liveElements = parser.parseEventLive(liveJson);

        if (fplFixtures.isEmpty()) {
            throw new IllegalStateException("FPL returned 0 fixtures for GW" + gameweek);
        }

        Set<Integer> allGwFplFixtureIds = new HashSet<>();
        for (FplModels.FplFixture f : fplFixtures) {
            allGwFplFixtureIds.add(f.id());
        }

        Map<Integer, FplMappingReader.TeamMapping> teamMap = mappingReader.readTeams(mappingDir.resolve("team-map.csv"));
        Map<Integer, FplMappingReader.PlayerMapping> playerMap = mappingReader.readPlayers(mappingDir.resolve("player-map.csv"));

        Map<Integer, FplModels.Element> elementsById = new HashMap<>();
        for (FplModels.Element el : bootstrap.elements()) {
            elementsById.put(el.id(), el);
        }

        List<DbFixture> dbFixtures = loadSeasonFixtures();
        Map<Integer, MatchedFixture> matchedFixtures;
        if (!filteredMode) {
            List<String> unfinished = new ArrayList<>();
            for (FplModels.FplFixture f : fplFixtures) {
                if (!f.finished()) {
                    unfinished.add("fpl_fixture=" + f.id() + " (team_h=" + f.teamH() + ", team_a=" + f.teamA()
                            + ", kickoff=" + f.kickoffTime() + ", finished=" + f.finished()
                            + ", finished_provisional=" + f.finishedProvisional() + ")");
                }
            }
            if (!unfinished.isEmpty()) {
                throw new IllegalStateException("GW" + gameweek + " has unfinished FPL fixtures (finished != true): "
                        + String.join("; ", unfinished));
            }
            matchedFixtures = matchFixturesOneToOne(gameweek, fplFixtures, dbFixtures, teamMap);
        } else {
            matchedFixtures = matchSelectedFixturesOneToOne(
                    gameweek, fplFixtures, dbFixtures, teamMap, requestedFixtureIds, allowProvisional);
        }

        int provisionalCount = 0;
        for (MatchedFixture mf : matchedFixtures.values()) {
            if ("PROVISIONAL".equals(mf.fplStatus())) {
                provisionalCount++;
                String warning = "WARNING: FPL fixture " + mf.fplFixtureId()
                        + " (internal fixture_id=" + mf.internalFixtureId()
                        + ") is only finished_provisional=true (finished=false); collecting with --allow-provisional=true"
                        + " and marking fpl_status=PROVISIONAL in fixture-map.csv";
                log.warn(warning);
                System.out.println(warning);
            }
        }

        Instant fetchedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        List<MatchStatRow> matchRows = new ArrayList<>();
        List<ExtraStatRow> extraRows = new ArrayList<>();
        List<BenchCardRow> benchCardRows = new ArrayList<>();
        List<UnmappedRow> unmappedRows = new ArrayList<>();
        int elementSummaryCalls = 0;

        for (FplModels.LiveElement live : liveElements) {
            boolean played = live.stats().minutes() > 0;
            boolean hasCard = hasCard(live.stats());
            if (!played && !hasCard) {
                continue;
            }
            ResolvedElementStats resolved = resolvePerFixtureStats(
                    gameweek, live, elementsById.get(live.elementId()), teamMap,
                    matchedFixtures, allGwFplFixtureIds, filteredMode, rawDir, source);
            if (resolved.usedElementSummary()) {
                elementSummaryCalls++;
            }
            if (resolved.entries().isEmpty()) {
                continue;
            }

            FplMappingReader.PlayerMapping mapped = playerMap.get(live.elementId());
            if (mapped == null) {
                FplModels.Element el = elementsById.get(live.elementId());
                String fplName = el != null ? el.fullName() : ("UNKNOWN_" + live.elementId());
                String webName = el != null ? el.webName() : "";
                int fplTeamId = el != null ? el.teamId() : 0;
                Integer clubId = el != null && teamMap.containsKey(el.teamId())
                        ? teamMap.get(el.teamId()).clubId() : null;
                for (PerFixtureStats pf : resolved.entries()) {
                    unmappedRows.add(new UnmappedRow(
                            live.elementId(), fplName, webName, fplTeamId, clubId,
                            pf.fixture().internalFixtureId(), pf.stats().minutes()));
                }
                continue;
            }

            for (PerFixtureStats pf : resolved.entries()) {
                FplModels.PlayerStats s = pf.stats();
                int internalFixtureId = pf.fixture().internalFixtureId();
                if (s.minutes() > 0) {
                    matchRows.add(new MatchStatRow(
                            internalFixtureId, mapped.playerId(),
                            s.minutes(), s.goalsScored(), s.assists(), s.yellowCards(), s.redCards()));
                    extraRows.add(new ExtraStatRow(
                            SEASON_YEAR, internalFixtureId, mapped.playerId(), s, "FPL", fetchedAt));
                } else if (hasCard(s)) {
                    benchCardRows.add(new BenchCardRow(
                            internalFixtureId, mapped.playerId(), s.yellowCards(), s.redCards()));
                }
            }
        }

        Path fixtureMapFile = outputDir.resolve("fixture-map.csv");
        Path unmappedFile = outputDir.resolve("unmapped.csv");
        Path benchCardsFile = outputDir.resolve("bench-cards.csv");
        Path matchStatsFile = outputDir.resolve("fpl-match-stats-2026-" + gwUpper + ".csv");
        Path extraStatsFile = outputDir.resolve("fpl-extra-stats-2026-" + gwUpper + ".csv");

        if (backfillConsistency) {
            writeFixtureMap(fixtureMapFile, matchedFixtures.values());
            unmappedRows.sort(Comparator.comparingInt(UnmappedRow::fplElementId)
                    .thenComparingInt(UnmappedRow::fixtureId));
            writeUnmapped(unmappedFile, unmappedRows);
            benchCardRows.sort(Comparator.comparingInt(BenchCardRow::fixtureId)
                    .thenComparingInt(BenchCardRow::playerId));
            writeBenchCards(benchCardsFile, benchCardRows);

            List<ConsistencyRow> consistencyRows = new ArrayList<>();
            Map<Integer, Integer> playerIdToFplId = new HashMap<>();
            for (var mapping : playerMap.values()) {
                playerIdToFplId.put(mapping.playerId(), mapping.fplElementId());
            }
            for (UnmappedRow u : unmappedRows) {
                consistencyRows.add(new ConsistencyRow(
                        u.fixtureId(), null, u.fplElementId(), u.fplName(),
                        "UNMAPPED_PLAYER", u.minutes(), "", null));
            }

            Map<FixturePlayerKey, DbManualStat> dbStats = loadDbManualStats(matchedFixtures);
            Map<FixturePlayerKey, MatchStatRow> mappedFplByKey = new HashMap<>();
            for (MatchStatRow mr : matchRows) {
                mappedFplByKey.put(new FixturePlayerKey(mr.fixtureId(), mr.playerId()), mr);
            }

            List<ExtraStatRow> eligibleExtraRows = new ArrayList<>();
            for (ExtraStatRow er : extraRows) {
                FixturePlayerKey key = new FixturePlayerKey(er.fixtureId(), er.playerId());
                DbManualStat db = dbStats.get(key);
                if (db != null && "PLAYED".equals(db.status())) {
                    eligibleExtraRows.add(er);
                } else {
                    String dbStatus = db == null ? "MISSING" : db.status();
                    Integer dbMinutes = db == null ? null : db.minutes();
                    String name = db != null ? db.playerName() : lookupPlayerName(er.playerId());
                    Integer fplId = playerIdToFplId.get(er.playerId());
                    consistencyRows.add(new ConsistencyRow(
                            er.fixtureId(), er.playerId(), fplId, name,
                            "FPL_PLAYED_DB_NOT_PLAYED", er.stats().minutes(), dbStatus, dbMinutes));
                }
            }

            for (var entry : dbStats.entrySet()) {
                DbManualStat db = entry.getValue();
                if (!"PLAYED".equals(db.status())) {
                    continue;
                }
                if (mappedFplByKey.containsKey(entry.getKey())) {
                    continue;
                }
                Integer fplId = playerIdToFplId.get(db.playerId());
                if (fplId != null) {
                    consistencyRows.add(new ConsistencyRow(
                            db.fixtureId(), db.playerId(), fplId, db.playerName(),
                            "DB_PLAYED_FPL_ZERO", 0, db.status(), db.minutes()));
                } else {
                    consistencyRows.add(new ConsistencyRow(
                            db.fixtureId(), db.playerId(), null, db.playerName(),
                            "DB_PLAYED_UNMAPPED_PLAYER", null, db.status(), db.minutes()));
                }
            }

            consistencyRows.sort(Comparator.comparingInt(ConsistencyRow::fixtureId)
                    .thenComparing(ConsistencyRow::reason)
                    .thenComparingInt(r -> r.playerId() == null ? 0 : r.playerId())
                    .thenComparingInt(r -> r.fplElementId() == null ? 0 : r.fplElementId()));
            writeConsistency(outputDir.resolve("consistency.csv"), consistencyRows);

            matchRows.sort(Comparator.comparingInt(MatchStatRow::fixtureId)
                    .thenComparingInt(MatchStatRow::playerId));
            eligibleExtraRows.sort(Comparator.comparingInt(ExtraStatRow::fixtureId)
                    .thenComparingInt(ExtraStatRow::playerId));

            writeMatchStats(matchStatsFile, matchRows);
            writeExtraStats(extraStatsFile, eligibleExtraRows);

            return new Result(gameweek, matchedFixtures.size(), eligibleExtraRows.size(),
                    unmappedRows.size(), elementSummaryCalls, true, provisionalCount);
        }

        // Validate/merge with existing CSV files before writing anything when --fixture-ids is active
        List<MatchedFixture> finalFixtureMap = filteredMode
                ? mergeFixtureMap(fixtureMapFile, matchedFixtures.values())
                : new ArrayList<>(matchedFixtures.values());
        List<UnmappedRow> finalUnmapped = filteredMode
                ? mergeUnmapped(unmappedFile, unmappedRows, requestedFixtureIds)
                : new ArrayList<>(unmappedRows);
        List<BenchCardRow> finalBenchCards = filteredMode
                ? mergeBenchCards(benchCardsFile, benchCardRows)
                : new ArrayList<>(benchCardRows);

        List<MatchStatRow> finalMatchRows = null;
        List<String> finalExtraCsvLines = null;
        if (unmappedRows.isEmpty()) {
            finalMatchRows = filteredMode
                    ? mergeMatchStats(matchStatsFile, matchRows)
                    : new ArrayList<>(matchRows);
            finalExtraCsvLines = filteredMode
                    ? mergeExtraStats(extraStatsFile, extraRows)
                    : formatExtraStatsRows(extraRows);
        }

        // All conflict checks passed; now write outputs
        finalFixtureMap.sort(Comparator.comparing(MatchedFixture::kickoffUtc)
                .thenComparingInt(MatchedFixture::fplFixtureId));
        writeFixtureMap(fixtureMapFile, finalFixtureMap);

        finalUnmapped.sort(Comparator.comparingInt(UnmappedRow::fplElementId)
                .thenComparingInt(UnmappedRow::fixtureId));
        writeUnmapped(unmappedFile, finalUnmapped);

        finalBenchCards.sort(Comparator.comparingInt(BenchCardRow::fixtureId)
                .thenComparingInt(BenchCardRow::playerId));
        writeBenchCards(benchCardsFile, finalBenchCards);

        if (!unmappedRows.isEmpty()) {
            if (!filteredMode) {
                Files.deleteIfExists(matchStatsFile);
                Files.deleteIfExists(extraStatsFile);
            }
            return new Result(gameweek, matchedFixtures.size(), matchRows.size(),
                    unmappedRows.size(), elementSummaryCalls, false, provisionalCount);
        }

        finalMatchRows.sort(Comparator.comparingInt(MatchStatRow::fixtureId)
                .thenComparingInt(MatchStatRow::playerId));
        writeMatchStats(matchStatsFile, finalMatchRows);
        writeExtraStatsLines(extraStatsFile, finalExtraCsvLines);

        return new Result(gameweek, matchedFixtures.size(), matchRows.size(),
                0, elementSummaryCalls, true, provisionalCount);
    }

    private static boolean hasCard(FplModels.PlayerStats stats) {
        return stats.yellowCards() > 0 || stats.redCards() > 0;
    }

    private ResolvedElementStats resolvePerFixtureStats(
            int gameweek,
            FplModels.LiveElement live,
            FplModels.Element element,
            Map<Integer, FplMappingReader.TeamMapping> teamMap,
            Map<Integer, MatchedFixture> matchedFixtures,
            Set<Integer> allGwFplFixtureIds,
            boolean filteredMode,
            Path rawDir,
            FplSource source
    ) throws IOException, InterruptedException {
        List<Integer> fixtureIds = live.explainFixtureIds();
        if (fixtureIds.size() == 1) {
            int fplFixtureId = fixtureIds.getFirst();
            MatchedFixture matched = matchedFixtures.get(fplFixtureId);
            if (matched == null) {
                if (filteredMode && allGwFplFixtureIds.contains(fplFixtureId)) {
                    return new ResolvedElementStats(List.of(), false);
                }
                throw new IllegalStateException("Element " + live.elementId()
                        + " references FPL fixture " + fplFixtureId + " which is not in GW" + gameweek);
            }
            return new ResolvedElementStats(List.of(new PerFixtureStats(matched, live.stats())), false);
        }
        if (fixtureIds.isEmpty() && live.stats().minutes() == 0 && hasCard(live.stats())) {
            List<MatchedFixture> clubFixtures = findClubFixtures(element, teamMap, matchedFixtures);
            if (clubFixtures.isEmpty() && filteredMode) {
                return new ResolvedElementStats(List.of(), false);
            }
            if (clubFixtures.size() == 1) {
                return new ResolvedElementStats(List.of(new PerFixtureStats(clubFixtures.getFirst(), live.stats())), false);
            }
            if (clubFixtures.size() > 1) {
                return resolveViaElementSummary(gameweek, live, matchedFixtures, allGwFplFixtureIds, filteredMode, rawDir, source);
            }
        }
        if (fixtureIds.size() > 1) {
            if (filteredMode) {
                boolean anySelected = false;
                for (int id : fixtureIds) {
                    if (matchedFixtures.containsKey(id)) {
                        anySelected = true;
                        break;
                    }
                    if (!allGwFplFixtureIds.contains(id)) {
                        throw new IllegalStateException("Element " + live.elementId()
                                + " references FPL fixture " + id + " which is not in GW" + gameweek);
                    }
                }
                if (!anySelected) {
                    return new ResolvedElementStats(List.of(), false);
                }
            }
            return resolveViaElementSummary(gameweek, live, matchedFixtures, allGwFplFixtureIds, filteredMode, rawDir, source);
        }
        if (filteredMode) {
            List<MatchedFixture> clubFixtures = findClubFixtures(element, teamMap, matchedFixtures);
            if (clubFixtures.isEmpty()) {
                return new ResolvedElementStats(List.of(), false);
            }
        }
        throw new IllegalStateException("Element " + live.elementId()
                + " has minutes=" + live.stats().minutes() + ", cards="
                + live.stats().yellowCards() + "/" + live.stats().redCards()
                + " but empty explain[] in GW" + gameweek);
    }

    private ResolvedElementStats resolveViaElementSummary(
            int gameweek,
            FplModels.LiveElement live,
            Map<Integer, MatchedFixture> matchedFixtures,
            Set<Integer> allGwFplFixtureIds,
            boolean filteredMode,
            Path rawDir,
            FplSource source
    ) throws IOException, InterruptedException {
        String summaryJson = source.fetchElementSummary(live.elementId());
        Files.writeString(rawDir.resolve("element-summary-" + live.elementId() + ".json"),
                summaryJson, StandardCharsets.UTF_8);
        List<FplModels.HistoryMatch> history = parser.parseElementSummary(summaryJson, live.elementId());
        List<PerFixtureStats> split = new ArrayList<>();
        for (FplModels.HistoryMatch match : history) {
            if (match.round() != gameweek) {
                continue;
            }
            if (match.stats().minutes() <= 0 && !hasCard(match.stats())) {
                continue;
            }
            MatchedFixture matched = matchedFixtures.get(match.fixtureId());
            if (matched == null) {
                if (filteredMode && allGwFplFixtureIds.contains(match.fixtureId())) {
                    continue;
                }
                throw new IllegalStateException("element-summary(" + live.elementId()
                        + ") references FPL fixture " + match.fixtureId()
                        + " in round " + gameweek + " not found in fixture-map");
            }
            split.add(new PerFixtureStats(matched, match.stats()));
        }
        if (split.isEmpty() && !filteredMode) {
            throw new IllegalStateException("Double-GW element " + live.elementId()
                    + " has minutes=" + live.stats().minutes()
                    + " in live API but 0 relevant matches in element-summary for GW" + gameweek);
        }
        return new ResolvedElementStats(split, true);
    }

    private static List<MatchedFixture> findClubFixtures(
            FplModels.Element element,
            Map<Integer, FplMappingReader.TeamMapping> teamMap,
            Map<Integer, MatchedFixture> matchedFixtures
    ) {
        if (element == null) {
            return List.of();
        }
        FplMappingReader.TeamMapping team = teamMap.get(element.teamId());
        if (team == null) {
            return List.of();
        }
        int clubId = team.clubId();
        List<MatchedFixture> found = new ArrayList<>();
        for (MatchedFixture mf : matchedFixtures.values()) {
            if (mf.homeClubId() == clubId || mf.awayClubId() == clubId) {
                found.add(mf);
            }
        }
        return found;
    }

    private List<DbFixture> loadSeasonFixtures() {
        return jdbc.query("""
                SELECT id, gameweek, home_club_id, away_club_id, match_date, home_goals, away_goals, status
                FROM fixtures
                WHERE league_id = ? AND season_year = ?
                ORDER BY id
                """, (rs, index) -> new DbFixture(
                rs.getInt("id"),
                rs.getInt("gameweek"),
                rs.getInt("home_club_id"),
                rs.getInt("away_club_id"),
                rs.getDate("match_date").toLocalDate(),
                rs.getObject("home_goals", Integer.class),
                rs.getObject("away_goals", Integer.class),
                rs.getString("status")
        ), LEAGUE_ID, SEASON_YEAR);
    }

    private Map<Integer, MatchedFixture> matchFixturesOneToOne(
            int gameweek,
            List<FplModels.FplFixture> fplFixtures,
            List<DbFixture> dbFixtures,
            Map<Integer, FplMappingReader.TeamMapping> teamMap
    ) {
        if (dbFixtures.isEmpty()) {
            throw new IllegalStateException("No DB fixtures found for season " + SEASON_YEAR + " when matching GW" + gameweek);
        }

        Map<TeamPairKey, List<DbFixture>> dbByPair = new HashMap<>();
        for (DbFixture db : dbFixtures) {
            TeamPairKey key = new TeamPairKey(db.homeClubId(), db.awayClubId());
            dbByPair.computeIfAbsent(key, ignored -> new ArrayList<>()).add(db);
        }

        Map<Integer, MatchedFixture> matched = new LinkedHashMap<>();
        Map<Integer, Integer> usedDbFixtures = new HashMap<>();

        List<FplModels.FplFixture> sortedFpl = new ArrayList<>(fplFixtures);
        sortedFpl.sort(Comparator.comparing(FplModels.FplFixture::kickoffTime)
                .thenComparingInt(FplModels.FplFixture::id));

        for (FplModels.FplFixture fpl : sortedFpl) {
            FplMappingReader.TeamMapping home = teamMap.get(fpl.teamH());
            FplMappingReader.TeamMapping away = teamMap.get(fpl.teamA());
            if (home == null || away == null) {
                throw new IllegalStateException("Missing team-map.csv entry for FPL fixture " + fpl.id()
                        + " (team_h=" + fpl.teamH() + ", team_a=" + fpl.teamA() + ")");
            }
            TeamPairKey pair = new TeamPairKey(home.clubId(), away.clubId());
            List<DbFixture> candidates = dbByPair.getOrDefault(pair, List.of());
            if (candidates.size() != 1) {
                throw new IllegalStateException("Expected unique 1-to-1 season DB fixture for FPL fixture " + fpl.id()
                        + " (" + home.clubName() + " vs " + away.clubName() + " on " + fpl.kickoffDateUtc()
                        + "), found " + candidates.size());
            }
            DbFixture db = candidates.getFirst();
            if (!db.matchDate().equals(fpl.kickoffDateUtc())) {
                throw new IllegalStateException("Expected 1-to-1 date match for FPL fixture " + fpl.id()
                        + " (" + home.clubName() + " vs " + away.clubName() + "): FPL UTC date="
                        + fpl.kickoffDateUtc() + " differs from DB match_date=" + db.matchDate()
                        + " (db_fixture=" + db.id() + ", db_gw=" + db.gameweek() + ", fpl_event=" + gameweek + ")");
            }
            Integer prevFpl = usedDbFixtures.putIfAbsent(db.id(), fpl.id());
            if (prevFpl != null) {
                throw new IllegalStateException("DB fixture " + db.id() + " matched multiple FPL fixtures: "
                        + prevFpl + " and " + fpl.id());
            }
            matched.put(fpl.id(), new MatchedFixture(
                    fpl.id(), db.id(), fpl.kickoffTime(), home.clubId(), away.clubId(),
                    fpl.teamHScore(), fpl.teamAScore(), db.homeGoals(), db.awayGoals(),
                    fpl.finished() ? "FINISHED" : "PROVISIONAL"));
        }
        return matched;
    }

    private Map<Integer, MatchedFixture> matchSelectedFixturesOneToOne(
            int gameweek,
            List<FplModels.FplFixture> fplFixtures,
            List<DbFixture> dbFixtures,
            Map<Integer, FplMappingReader.TeamMapping> teamMap,
            Set<Integer> requestedFixtureIds,
            boolean allowProvisional
    ) {
        if (dbFixtures.isEmpty()) {
            throw new IllegalStateException("No DB fixtures found for season " + SEASON_YEAR + " when matching GW" + gameweek);
        }

        Map<Integer, DbFixture> dbById = new HashMap<>();
        Map<TeamPairKey, List<DbFixture>> dbByPair = new HashMap<>();
        for (DbFixture db : dbFixtures) {
            dbById.put(db.id(), db);
            dbByPair.computeIfAbsent(new TeamPairKey(db.homeClubId(), db.awayClubId()), ignored -> new ArrayList<>()).add(db);
        }

        Map<TeamPairKey, List<FplModels.FplFixture>> fplByPair = new HashMap<>();
        for (FplModels.FplFixture fpl : fplFixtures) {
            FplMappingReader.TeamMapping home = teamMap.get(fpl.teamH());
            FplMappingReader.TeamMapping away = teamMap.get(fpl.teamA());
            if (home == null || away == null) {
                continue;
            }
            fplByPair.computeIfAbsent(new TeamPairKey(home.clubId(), away.clubId()), ignored -> new ArrayList<>()).add(fpl);
        }

        List<MatchedFixture> selected = new ArrayList<>();
        List<String> unfinished = new ArrayList<>();
        Set<Integer> usedFplIds = new HashSet<>();

        for (int fixtureId : requestedFixtureIds) {
            DbFixture db = dbById.get(fixtureId);
            if (db == null) {
                throw new IllegalArgumentException("Requested fixture_id=" + fixtureId
                        + " does not exist in DB fixtures for season " + SEASON_YEAR);
            }
            TeamPairKey pair = new TeamPairKey(db.homeClubId(), db.awayClubId());
            List<DbFixture> dbCandidates = dbByPair.getOrDefault(pair, List.of());
            if (dbCandidates.size() != 1) {
                throw new IllegalStateException("Expected unique 1-to-1 season DB fixture for requested fixture_id="
                        + fixtureId + ", found " + dbCandidates.size());
            }
            List<FplModels.FplFixture> fplCandidates = fplByPair.getOrDefault(pair, List.of());
            if (fplCandidates.isEmpty()) {
                throw new IllegalArgumentException("Requested fixture_id=" + fixtureId
                        + " (db_gw=" + db.gameweek() + ", match_date=" + db.matchDate()
                        + ") does not belong to FPL GW" + gameweek);
            }
            if (fplCandidates.size() != 1) {
                throw new IllegalStateException("Expected unique 1-to-1 FPL fixture in GW" + gameweek
                        + " for requested fixture_id=" + fixtureId + ", found " + fplCandidates.size());
            }
            FplModels.FplFixture fpl = fplCandidates.getFirst();
            if (!db.matchDate().equals(fpl.kickoffDateUtc())) {
                throw new IllegalStateException("Expected 1-to-1 date match for requested fixture_id=" + fixtureId
                        + " (fpl_fixture=" + fpl.id() + "): FPL UTC date=" + fpl.kickoffDateUtc()
                        + " differs from DB match_date=" + db.matchDate());
            }
            if (!usedFplIds.add(fpl.id())) {
                throw new IllegalStateException("FPL fixture " + fpl.id() + " matched multiple requested fixture IDs");
            }
            if (!fpl.finished()) {
                if (allowProvisional && fpl.finishedProvisional()) {
                    // Accepted as provisional; warning emitted by caller
                } else {
                    unfinished.add("fixture_id=" + db.id() + " (fpl_fixture=" + fpl.id()
                            + ", kickoff=" + fpl.kickoffTime()
                            + ", finished=" + fpl.finished()
                            + ", finished_provisional=" + fpl.finishedProvisional() + ")");
                }
            }
            selected.add(new MatchedFixture(
                    fpl.id(), db.id(), fpl.kickoffTime(), db.homeClubId(), db.awayClubId(),
                    fpl.teamHScore(), fpl.teamAScore(), db.homeGoals(), db.awayGoals(),
                    fpl.finished() ? "FINISHED" : "PROVISIONAL"));
        }

        if (!unfinished.isEmpty()) {
            String requirement = allowProvisional
                    ? "finished=true or finished_provisional=true"
                    : "finished=true (pass --allow-provisional=true if finished_provisional=true)";
            throw new IllegalStateException("Selected GW" + gameweek + " fixtures do not satisfy "
                    + requirement + ": " + String.join("; ", unfinished));
        }

        selected.sort(Comparator.comparing(MatchedFixture::kickoffUtc)
                .thenComparingInt(MatchedFixture::fplFixtureId));
        Map<Integer, MatchedFixture> matched = new LinkedHashMap<>();
        for (MatchedFixture mf : selected) {
            matched.put(mf.fplFixtureId(), mf);
        }
        return matched;
    }

    private static List<MatchedFixture> mergeFixtureMap(Path file, java.util.Collection<MatchedFixture> incoming)
            throws IOException {
        Map<Integer, MatchedFixture> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null) {
                    throw new IllegalArgumentException("Existing fixture-map.csv is empty: " + file);
                }
                String cleanHeader = header.replaceFirst("^\uFEFF", "").strip();
                boolean hasStatus = cleanHeader.equals(FIXTURE_MAP_HEADER);
                if (!hasStatus && !cleanHeader.equals(FIXTURE_MAP_LEGACY_HEADER)) {
                    throw new IllegalArgumentException("Existing fixture-map.csv has unexpected header: " + cleanHeader);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    MatchedFixture row = new MatchedFixture(
                            Integer.parseInt(f[0].strip()),
                            Integer.parseInt(f[1].strip()),
                            Instant.parse(f[2].strip()),
                            Integer.parseInt(f[3].strip()),
                            Integer.parseInt(f[4].strip()),
                            parseNullableInt(f[5]),
                            parseNullableInt(f[6]),
                            parseNullableInt(f[7]),
                            parseNullableInt(f[8]),
                            hasStatus && f.length > 9 && !f[9].isBlank() ? f[9].strip() : "FINISHED");
                    merged.put(row.internalFixtureId(), row);
                }
            }
        }
        for (MatchedFixture row : incoming) {
            MatchedFixture prev = merged.get(row.internalFixtureId());
            if (prev != null && !prev.equals(row)) {
                throw new IllegalStateException("Conflict in fixture-map.csv for fixture_id=" + row.internalFixtureId()
                        + ": existing=" + prev + " vs new=" + row);
            }
            merged.put(row.internalFixtureId(), row);
        }
        return new ArrayList<>(merged.values());
    }

    private static List<UnmappedRow> mergeUnmapped(Path file, List<UnmappedRow> incoming, Set<Integer> selectedFixtureIds)
            throws IOException {
        Map<String, UnmappedRow> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(UNMAPPED_HEADER)) {
                    throw new IllegalArgumentException("Existing unmapped.csv has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    int fixtureId = Integer.parseInt(f[5].strip());
                    if (selectedFixtureIds.contains(fixtureId)) {
                        continue; // refreshed for selected fixtures
                    }
                    UnmappedRow row = new UnmappedRow(
                            Integer.parseInt(f[0].strip()), f[1].strip(), f[2].strip(),
                            Integer.parseInt(f[3].strip()), parseNullableInt(f[4]),
                            fixtureId, Integer.parseInt(f[6].strip()));
                    merged.put(fixtureId + ":" + row.fplElementId(), row);
                }
            }
        }
        for (UnmappedRow row : incoming) {
            merged.put(row.fixtureId() + ":" + row.fplElementId(), row);
        }
        return new ArrayList<>(merged.values());
    }

    private static List<BenchCardRow> mergeBenchCards(Path file, List<BenchCardRow> incoming) throws IOException {
        Map<FixturePlayerKey, BenchCardRow> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(BENCH_CARDS_HEADER)) {
                    throw new IllegalArgumentException("Existing bench-cards.csv has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    BenchCardRow row = new BenchCardRow(
                            Integer.parseInt(f[0].strip()), Integer.parseInt(f[1].strip()),
                            Integer.parseInt(f[2].strip()), Integer.parseInt(f[3].strip()));
                    merged.put(new FixturePlayerKey(row.fixtureId(), row.playerId()), row);
                }
            }
        }
        for (BenchCardRow row : incoming) {
            FixturePlayerKey key = new FixturePlayerKey(row.fixtureId(), row.playerId());
            BenchCardRow prev = merged.get(key);
            if (prev != null && !prev.equals(row)) {
                throw new IllegalStateException("Conflict in bench-cards.csv for (fixture_id=" + row.fixtureId()
                        + ", player_id=" + row.playerId() + "): existing=" + prev + " vs new=" + row);
            }
            merged.put(key, row);
        }
        return new ArrayList<>(merged.values());
    }

    private static List<MatchStatRow> mergeMatchStats(Path file, List<MatchStatRow> incoming) throws IOException {
        Map<FixturePlayerKey, MatchStatRow> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(MATCH_STATS_HEADER)) {
                    throw new IllegalArgumentException("Existing fpl-match-stats CSV has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    MatchStatRow row = new MatchStatRow(
                            Integer.parseInt(f[0].strip()), Integer.parseInt(f[1].strip()),
                            Integer.parseInt(f[2].strip()), Integer.parseInt(f[3].strip()),
                            Integer.parseInt(f[4].strip()), Integer.parseInt(f[5].strip()),
                            Integer.parseInt(f[6].strip()));
                    merged.put(new FixturePlayerKey(row.fixtureId(), row.playerId()), row);
                }
            }
        }
        for (MatchStatRow row : incoming) {
            FixturePlayerKey key = new FixturePlayerKey(row.fixtureId(), row.playerId());
            MatchStatRow prev = merged.get(key);
            if (prev != null && !prev.equals(row)) {
                throw new IllegalStateException("Conflict in " + file.getFileName()
                        + " for (fixture_id=" + row.fixtureId() + ", player_id=" + row.playerId()
                        + "): existing=" + prev + " vs new=" + row);
            }
            merged.put(key, row);
        }
        return new ArrayList<>(merged.values());
    }

    private static List<String> mergeExtraStats(Path file, List<ExtraStatRow> incoming) throws IOException {
        Map<FixturePlayerKey, String> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(ManualExtraStatsCsvReader.HEADER)) {
                    throw new IllegalArgumentException("Existing fpl-extra-stats CSV has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    if (f.length != 26) {
                        throw new IllegalArgumentException("Expected 26 columns in existing extra stats CSV: " + file);
                    }
                    FixturePlayerKey key = new FixturePlayerKey(
                            Integer.parseInt(f[1].strip()), Integer.parseInt(f[2].strip()));
                    merged.put(key, line.strip());
                }
            }
        }
        for (ExtraStatRow row : incoming) {
            FixturePlayerKey key = new FixturePlayerKey(row.fixtureId(), row.playerId());
            String formatted = formatExtraStatLine(row);
            String prev = merged.get(key);
            if (prev != null) {
                if (!extraStatValuesEqual(prev, formatted)) {
                    throw new IllegalStateException("Conflict in " + file.getFileName()
                            + " for (fixture_id=" + row.fixtureId() + ", player_id=" + row.playerId()
                            + "): existing=" + prev + " vs new=" + formatted);
                }
                // Identical values (ignoring fetched_at); keep existing line to stay idempotent
            } else {
                merged.put(key, formatted);
            }
        }
        List<Map.Entry<FixturePlayerKey, String>> entries = new ArrayList<>(merged.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<FixturePlayerKey, String> e) -> e.getKey().fixtureId())
                .thenComparingInt(e -> e.getKey().playerId()));
        List<String> lines = new ArrayList<>(entries.size());
        for (var e : entries) {
            lines.add(e.getValue());
        }
        return lines;
    }

    private static boolean extraStatValuesEqual(String existingLine, String newLine) {
        String[] a = existingLine.split(",", -1);
        String[] b = newLine.split(",", -1);
        if (a.length != 26 || b.length != 26) {
            return false;
        }
        // Compare all columns except fetched_at (index 25)
        for (int i = 0; i < 25; i++) {
            if (!Objects.equals(a[i].strip(), b[i].strip())) {
                return false;
            }
        }
        return true;
    }

    private static Integer parseNullableInt(String raw) {
        String s = raw == null ? "" : raw.strip();
        return s.isEmpty() ? null : Integer.valueOf(s);
    }

    private static void writeFixtureMap(Path file, java.util.Collection<MatchedFixture> matched) throws IOException {
        StringBuilder out = new StringBuilder(FIXTURE_MAP_HEADER).append('\n');
        for (MatchedFixture m : matched) {
            out.append(m.fplFixtureId()).append(',')
                    .append(m.internalFixtureId()).append(',')
                    .append(m.kickoffUtc()).append(',')
                    .append(m.homeClubId()).append(',')
                    .append(m.awayClubId()).append(',')
                    .append(nullable(m.fplHomeGoals())).append(',')
                    .append(nullable(m.fplAwayGoals())).append(',')
                    .append(nullable(m.dbHomeGoals())).append(',')
                    .append(nullable(m.dbAwayGoals())).append(',')
                    .append(m.fplStatus()).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeUnmapped(Path file, List<UnmappedRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(UNMAPPED_HEADER).append('\n');
        for (UnmappedRow r : rows) {
            out.append(r.fplElementId()).append(',')
                    .append(sanitize(r.fplName())).append(',')
                    .append(sanitize(r.fplWebName())).append(',')
                    .append(r.fplTeamId()).append(',')
                    .append(nullable(r.clubId())).append(',')
                    .append(r.fixtureId()).append(',')
                    .append(r.minutes()).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeBenchCards(Path file, List<BenchCardRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(BENCH_CARDS_HEADER).append('\n');
        for (BenchCardRow r : rows) {
            out.append(r.fixtureId()).append(',')
                    .append(r.playerId()).append(',')
                    .append(r.yellowCards()).append(',')
                    .append(r.redCards()).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeConsistency(Path file, List<ConsistencyRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(CONSISTENCY_HEADER).append('\n');
        for (ConsistencyRow r : rows) {
            out.append(r.fixtureId()).append(',')
                    .append(nullable(r.playerId())).append(',')
                    .append(nullable(r.fplElementId())).append(',')
                    .append(sanitize(r.playerName())).append(',')
                    .append(r.reason()).append(',')
                    .append(nullable(r.fplMinutes())).append(',')
                    .append(sanitize(r.dbStatus())).append(',')
                    .append(nullable(r.dbMinutes())).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private Map<FixturePlayerKey, DbManualStat> loadDbManualStats(Map<Integer, MatchedFixture> matchedFixtures) {
        Map<FixturePlayerKey, DbManualStat> result = new LinkedHashMap<>();
        for (MatchedFixture mf : matchedFixtures.values()) {
            List<DbManualStat> rows = jdbc.query("""
                    SELECT s.fixture_id, s.player_id, p.name AS player_name,
                           s.participation_status, s.minutes
                    FROM manual_fixture_player_stats s
                    JOIN players p ON p.id = s.player_id
                    WHERE s.fixture_id = ? AND s.league_id = ? AND s.season_year = ?
                    """, (rs, idx) -> new DbManualStat(
                    rs.getInt("fixture_id"),
                    rs.getInt("player_id"),
                    rs.getString("player_name"),
                    rs.getString("participation_status"),
                    rs.getObject("minutes", Integer.class)
            ), mf.internalFixtureId(), LEAGUE_ID, SEASON_YEAR);
            for (DbManualStat row : rows) {
                result.put(new FixturePlayerKey(row.fixtureId(), row.playerId()), row);
            }
        }
        return result;
    }

    private String lookupPlayerName(int playerId) {
        List<String> names = jdbc.queryForList("SELECT name FROM players WHERE id = ?", String.class, playerId);
        return names.isEmpty() ? ("Player " + playerId) : names.getFirst();
    }

    private static void writeMatchStats(Path file, List<MatchStatRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(MATCH_STATS_HEADER).append('\n');
        for (MatchStatRow r : rows) {
            out.append(r.fixtureId()).append(',')
                    .append(r.playerId()).append(',')
                    .append(r.minutes()).append(',')
                    .append(r.goals()).append(',')
                    .append(r.assists()).append(',')
                    .append(r.yellowCards()).append(',')
                    .append(r.redCards()).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeExtraStats(Path file, List<ExtraStatRow> rows) throws IOException {
        writeExtraStatsLines(file, formatExtraStatsRows(rows));
    }

    private static List<String> formatExtraStatsRows(List<ExtraStatRow> rows) {
        List<ExtraStatRow> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt(ExtraStatRow::fixtureId)
                .thenComparingInt(ExtraStatRow::playerId));
        List<String> lines = new ArrayList<>(sorted.size());
        for (ExtraStatRow r : sorted) {
            lines.add(formatExtraStatLine(r));
        }
        return lines;
    }

    private static String formatExtraStatLine(ExtraStatRow r) {
        FplModels.PlayerStats s = r.stats();
        return r.season() + ","
                + r.fixtureId() + ","
                + r.playerId() + ","
                + s.starts() + ","
                + s.cleanSheets() + ","
                + s.goalsConceded() + ","
                + s.ownGoals() + ","
                + s.penaltiesSaved() + ","
                + s.penaltiesMissed() + ","
                + s.saves() + ","
                + s.bonus() + ","
                + s.bps() + ","
                + s.clearancesBlocksInterceptions() + ","
                + s.recoveries() + ","
                + s.tackles() + ","
                + s.defensiveContribution() + ","
                + decimal(s.expectedGoals()) + ","
                + decimal(s.expectedAssists()) + ","
                + decimal(s.expectedGoalInvolvements()) + ","
                + decimal(s.expectedGoalsConceded()) + ","
                + decimal(s.influence()) + ","
                + decimal(s.creativity()) + ","
                + decimal(s.threat()) + ","
                + decimal(s.ictIndex()) + ","
                + r.source() + ","
                + r.fetchedAt();
    }

    private static void writeExtraStatsLines(Path file, List<String> lines) throws IOException {
        StringBuilder out = new StringBuilder(ManualExtraStatsCsvReader.HEADER).append('\n');
        for (String line : lines) {
            out.append(line).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static String nullable(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace(',', ' ').replace('"', ' ').replace('\t', ' ').strip();
    }

    private record DbFixture(int id, int gameweek, int homeClubId, int awayClubId, LocalDate matchDate,
                             Integer homeGoals, Integer awayGoals, String status) {
    }

    private record TeamPairKey(int homeClubId, int awayClubId) {
    }

    private record MatchedFixture(int fplFixtureId, int internalFixtureId, Instant kickoffUtc,
                                  int homeClubId, int awayClubId, Integer fplHomeGoals, Integer fplAwayGoals,
                                  Integer dbHomeGoals, Integer dbAwayGoals, String fplStatus) {
    }

    private record PerFixtureStats(MatchedFixture fixture, FplModels.PlayerStats stats) {
    }

    private record ResolvedElementStats(List<PerFixtureStats> entries, boolean usedElementSummary) {
    }

    private record BenchCardRow(int fixtureId, int playerId, int yellowCards, int redCards) {
    }

    private record FixturePlayerKey(int fixtureId, int playerId) {
    }

    private record DbManualStat(int fixtureId, int playerId, String playerName, String status, Integer minutes) {
    }

    private record ConsistencyRow(int fixtureId, Integer playerId, Integer fplElementId, String playerName,
                                  String reason, Integer fplMinutes, String dbStatus, Integer dbMinutes) {
    }

    private record MatchStatRow(int fixtureId, int playerId, int minutes, int goals, int assists,
                                int yellowCards, int redCards) {
    }

    private record ExtraStatRow(int season, int fixtureId, int playerId, FplModels.PlayerStats stats,
                                String source, Instant fetchedAt) {
    }

    private record UnmappedRow(int fplElementId, String fplName, String fplWebName, int fplTeamId,
                               Integer clubId, int fixtureId, int minutes) {
    }

    public record Result(int gameweek, int fixturesMatched, int playedRows, int unmappedCount,
                         int elementSummaryCalls, boolean finalCsvWritten, int provisionalFixtures) {
    }
}
