package com.premierhub.fpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FplCollectorService {
    public static final int LEAGUE_ID = 39;
    public static final int SEASON_YEAR = 2026;
    public static final String FIXTURE_MAP_HEADER =
            "fpl_fixture_id,fixture_id,kickoff_utc,home_club_id,away_club_id,"
            + "fpl_home_goals,fpl_away_goals,db_home_goals,db_away_goals";
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
        return collectInternal(season, gameweek, mappingDir, outputDir, source, false);
    }

    public Result collectBackfill(int season, int gameweek, Path mappingDir, Path outputDir, FplSource source)
            throws IOException, InterruptedException {
        return collectInternal(season, gameweek, mappingDir, outputDir, source, true);
    }

    private Result collectInternal(int season, int gameweek, Path mappingDir, Path outputDir,
                                   FplSource source, boolean backfillConsistency)
            throws IOException, InterruptedException {
        if (season != SEASON_YEAR) {
            throw new IllegalArgumentException("Only season 2026 is supported, got " + season);
        }
        if (gameweek < 1 || gameweek > 38) {
            throw new IllegalArgumentException("gameweek must be between 1 and 38, got " + gameweek);
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

        Map<Integer, FplMappingReader.TeamMapping> teamMap = mappingReader.readTeams(mappingDir.resolve("team-map.csv"));
        Map<Integer, FplMappingReader.PlayerMapping> playerMap = mappingReader.readPlayers(mappingDir.resolve("player-map.csv"));

        Map<Integer, FplModels.Element> elementsById = new HashMap<>();
        for (FplModels.Element el : bootstrap.elements()) {
            elementsById.put(el.id(), el);
        }

        List<DbFixture> dbFixtures = loadSeasonFixtures();
        Map<Integer, MatchedFixture> matchedFixtures = matchFixturesOneToOne(gameweek, fplFixtures, dbFixtures, teamMap);

        writeFixtureMap(outputDir.resolve("fixture-map.csv"), matchedFixtures);

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
                    gameweek, live, elementsById.get(live.elementId()), teamMap, matchedFixtures, rawDir, source);
            if (resolved.usedElementSummary()) {
                elementSummaryCalls++;
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

        unmappedRows.sort(Comparator.comparingInt(UnmappedRow::fplElementId)
                .thenComparingInt(UnmappedRow::fixtureId));
        writeUnmapped(outputDir.resolve("unmapped.csv"), unmappedRows);

        benchCardRows.sort(Comparator.comparingInt(BenchCardRow::fixtureId)
                .thenComparingInt(BenchCardRow::playerId));
        writeBenchCards(outputDir.resolve("bench-cards.csv"), benchCardRows);

        Path matchStatsFile = outputDir.resolve("fpl-match-stats-2026-" + gwUpper + ".csv");
        Path extraStatsFile = outputDir.resolve("fpl-extra-stats-2026-" + gwUpper + ".csv");

        if (backfillConsistency) {
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
                    unmappedRows.size(), elementSummaryCalls, true);
        }

        if (!unmappedRows.isEmpty()) {
            Files.deleteIfExists(matchStatsFile);
            Files.deleteIfExists(extraStatsFile);
            return new Result(gameweek, matchedFixtures.size(), matchRows.size(),
                    unmappedRows.size(), elementSummaryCalls, false);
        }

        matchRows.sort(Comparator.comparingInt(MatchStatRow::fixtureId)
                .thenComparingInt(MatchStatRow::playerId));
        extraRows.sort(Comparator.comparingInt(ExtraStatRow::fixtureId)
                .thenComparingInt(ExtraStatRow::playerId));

        writeMatchStats(matchStatsFile, matchRows);
        writeExtraStats(extraStatsFile, extraRows);

        return new Result(gameweek, matchedFixtures.size(), matchRows.size(),
                0, elementSummaryCalls, true);
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
            Path rawDir,
            FplSource source
    ) throws IOException, InterruptedException {
        List<Integer> fixtureIds = live.explainFixtureIds();
        if (fixtureIds.size() == 1) {
            int fplFixtureId = fixtureIds.getFirst();
            MatchedFixture matched = matchedFixtures.get(fplFixtureId);
            if (matched == null) {
                throw new IllegalStateException("Element " + live.elementId()
                        + " references FPL fixture " + fplFixtureId + " which is not in GW" + gameweek);
            }
            return new ResolvedElementStats(List.of(new PerFixtureStats(matched, live.stats())), false);
        }
        if (fixtureIds.isEmpty() && live.stats().minutes() == 0 && hasCard(live.stats())) {
            List<MatchedFixture> clubFixtures = findClubFixtures(element, teamMap, matchedFixtures);
            if (clubFixtures.size() == 1) {
                return new ResolvedElementStats(List.of(new PerFixtureStats(clubFixtures.getFirst(), live.stats())), false);
            }
            if (clubFixtures.size() > 1) {
                return resolveViaElementSummary(gameweek, live, matchedFixtures, rawDir, source);
            }
        }
        if (fixtureIds.size() > 1) {
            return resolveViaElementSummary(gameweek, live, matchedFixtures, rawDir, source);
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
                throw new IllegalStateException("element-summary(" + live.elementId()
                        + ") references FPL fixture " + match.fixtureId()
                        + " in round " + gameweek + " not found in fixture-map");
            }
            split.add(new PerFixtureStats(matched, match.stats()));
        }
        if (split.isEmpty()) {
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
                    fpl.teamHScore(), fpl.teamAScore(), db.homeGoals(), db.awayGoals()));
        }
        return matched;
    }

    private static void writeFixtureMap(Path file, Map<Integer, MatchedFixture> matched) throws IOException {
        StringBuilder out = new StringBuilder(FIXTURE_MAP_HEADER).append('\n');
        for (MatchedFixture m : matched.values()) {
            out.append(m.fplFixtureId()).append(',')
                    .append(m.internalFixtureId()).append(',')
                    .append(m.kickoffUtc()).append(',')
                    .append(m.homeClubId()).append(',')
                    .append(m.awayClubId()).append(',')
                    .append(nullable(m.fplHomeGoals())).append(',')
                    .append(nullable(m.fplAwayGoals())).append(',')
                    .append(nullable(m.dbHomeGoals())).append(',')
                    .append(nullable(m.dbAwayGoals())).append('\n');
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
        StringBuilder out = new StringBuilder(ManualExtraStatsCsvReader.HEADER).append('\n');
        for (ExtraStatRow r : rows) {
            FplModels.PlayerStats s = r.stats();
            out.append(r.season()).append(',')
                    .append(r.fixtureId()).append(',')
                    .append(r.playerId()).append(',')
                    .append(s.starts()).append(',')
                    .append(s.cleanSheets()).append(',')
                    .append(s.goalsConceded()).append(',')
                    .append(s.ownGoals()).append(',')
                    .append(s.penaltiesSaved()).append(',')
                    .append(s.penaltiesMissed()).append(',')
                    .append(s.saves()).append(',')
                    .append(s.bonus()).append(',')
                    .append(s.bps()).append(',')
                    .append(s.clearancesBlocksInterceptions()).append(',')
                    .append(s.recoveries()).append(',')
                    .append(s.tackles()).append(',')
                    .append(s.defensiveContribution()).append(',')
                    .append(decimal(s.expectedGoals())).append(',')
                    .append(decimal(s.expectedAssists())).append(',')
                    .append(decimal(s.expectedGoalInvolvements())).append(',')
                    .append(decimal(s.expectedGoalsConceded())).append(',')
                    .append(decimal(s.influence())).append(',')
                    .append(decimal(s.creativity())).append(',')
                    .append(decimal(s.threat())).append(',')
                    .append(decimal(s.ictIndex())).append(',')
                    .append(r.source()).append(',')
                    .append(r.fetchedAt()).append('\n');
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
                                  Integer dbHomeGoals, Integer dbAwayGoals) {
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
                         int elementSummaryCalls, boolean finalCsvWritten) {
    }
}
