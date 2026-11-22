package com.premierhub.live;

import com.premierhub.fpl.FplMappingReader;
import com.premierhub.fpl.FplMappingReader.PlayerMapping;
import com.premierhub.fpl.FplMappingReader.TeamMapping;
import com.premierhub.live.LiveMatchState.GoalEvent;
import com.premierhub.live.LiveMatchState.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class LiveScoreService {
    public static final int LEAGUE_ID = 39;
    public static final int SEASON_YEAR = 2026;
    public static final String UNKNOWN_PLAYER_NAME = "Không rõ";
    public static final Duration LEAD_BEFORE_KICKOFF = Duration.ofMinutes(5);
    public static final Duration WINDOW_AFTER_KICKOFF = Duration.ofMinutes(150);
    public static final Duration FT_RETENTION = Duration.ofHours(3);

    private static final Logger LOG = LoggerFactory.getLogger(LiveScoreService.class);

    private final JdbcTemplate jdbc;
    private final FplLiveClient fplClient;
    private final LiveStreamService streamService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final Map<Integer, TeamMapping> teamMap;
    private final Map<Integer, PlayerMapping> playerMap;
    private final Set<Integer> warnedUnmappedElements = ConcurrentHashMap.newKeySet();
    private final Set<Integer> evictedFtFixtureIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Integer, TrackedFixture> trackedFixtures = new ConcurrentHashMap<>();

    private final boolean enabled;
    private final boolean replayEnabled;
    private final List<Path> replayFiles;
    private final AtomicInteger replayIndex = new AtomicInteger(0);

    @Autowired
    public LiveScoreService(
            JdbcTemplate jdbc,
            FplLiveClient fplClient,
            LiveStreamService streamService,
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${PREMIERHUB_LIVE_SCORES_ENABLED:}") String rawEnvEnabled,
            @Value("${premierhub.live.scores.enabled:true}") boolean configuredEnabled,
            @Value("${premierhub.live.replay-dir:}") String replayDir) {
        this(jdbc, fplClient, streamService, objectMapper, Clock.systemUTC(),
                loadBundledTeams(), loadBundledPlayers(),
                resolveEnabled(rawEnvEnabled, configuredEnabled, replayDir),
                replayDir,
                Arrays.asList(environment.getActiveProfiles()));
    }

    private static boolean resolveEnabled(String rawEnvEnabled, boolean configuredEnabled, String replayDir) {
        if (rawEnvEnabled != null && !rawEnvEnabled.isBlank()) {
            return Boolean.parseBoolean(rawEnvEnabled.strip());
        }
        if (replayDir != null && !replayDir.isBlank()) {
            return true;
        }
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().startsWith("org.junit.")) {
                return false;
            }
        }
        return configuredEnabled;
    }

    public LiveScoreService(
            JdbcTemplate jdbc,
            FplLiveClient fplClient,
            LiveStreamService streamService,
            ObjectMapper objectMapper,
            Clock clock,
            Map<Integer, TeamMapping> teamMap,
            Map<Integer, PlayerMapping> playerMap,
            boolean enabled,
            String replayDir,
            List<String> activeProfiles) {
        this.jdbc = jdbc;
        this.fplClient = fplClient;
        this.streamService = streamService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.teamMap = Map.copyOf(teamMap);
        this.playerMap = Map.copyOf(playerMap);
        this.enabled = enabled;

        boolean isProd = activeProfiles != null && activeProfiles.stream()
                .anyMatch(p -> "prod".equalsIgnoreCase(p));
        if (replayDir != null && !replayDir.isBlank()) {
            if (isProd) {
                LOG.error("PREMIERHUB_LIVE_REPLAY_DIR ({}) cannot be enabled when profile 'prod' is active; ignoring replay.",
                        replayDir);
                this.replayEnabled = false;
                this.replayFiles = List.of();
            } else {
                this.replayFiles = discoverReplayFiles(Path.of(replayDir.strip()));
                this.replayEnabled = !this.replayFiles.isEmpty();
                if (this.replayEnabled) {
                    LOG.info("Live score replay mode enabled with {} files from {}",
                            this.replayFiles.size(), replayDir);
                } else {
                    LOG.warn("PREMIERHUB_LIVE_REPLAY_DIR ({}) contains no fixture JSON files.", replayDir);
                }
            }
        } else {
            this.replayEnabled = false;
            this.replayFiles = List.of();
        }
    }

    public boolean isReplayEnabled() {
        return replayEnabled;
    }

    public List<LiveMatchState> currentMatches() {
        evictExpiredFullTimeMatches(Instant.now(clock));
        List<LiveMatchState> list = new ArrayList<>();
        for (TrackedFixture tracked : trackedFixtures.values()) {
            list.add(tracked.state());
        }
        list.sort(Comparator.comparingInt(LiveMatchState::fixtureId));
        return List.copyOf(list);
    }

    @Scheduled(fixedDelayString = "${premierhub.live.poll-interval-ms:60000}",
               initialDelayString = "${premierhub.live.poll-initial-delay-ms:5000}")
    public void scheduledPoll() {
        if (!enabled || replayEnabled) {
            return;
        }
        pollOnce();
    }

    @Scheduled(fixedDelayString = "${premierhub.live.replay-interval-ms:3000}",
               initialDelayString = "${premierhub.live.replay-initial-delay-ms:2000}")
    public void scheduledReplayStep() {
        if (!enabled || !replayEnabled) {
            return;
        }
        replayNextStep();
    }

    public synchronized boolean pollOnce() {
        Instant now = Instant.now(clock);
        evictExpiredFullTimeMatches(now);

        List<DbFixture> dbFixtures = loadSeasonFixtures();
        if (dbFixtures.isEmpty()) {
            return false;
        }

        Integer activeGameweek = findActiveGameweek(dbFixtures, now);
        if (activeGameweek == null) {
            return false;
        }

        Optional<String> jsonOpt = fplClient.fetchEventFixtures(activeGameweek);
        if (jsonOpt.isEmpty()) {
            return false;
        }
        processFixturesJson(jsonOpt.get(), dbFixtures, now, false);
        return true;
    }

    public synchronized boolean replayNextStep() {
        if (!replayEnabled || replayFiles.isEmpty()) {
            return false;
        }
        int idx = replayIndex.get();
        if (idx >= replayFiles.size()) {
            return false;
        }
        Path file = replayFiles.get(idx);
        replayIndex.incrementAndGet();
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Instant now = Instant.now(clock);
            evictExpiredFullTimeMatches(now);
            List<DbFixture> dbFixtures = loadSeasonFixtures();
            LOG.info("Live score replay step {}/{}: {}", idx + 1, replayFiles.size(), file.getFileName());
            processFixturesJson(json, dbFixtures, now, true);
            return true;
        } catch (IOException ex) {
            LOG.warn("Failed to read replay file {}: {}", file, ex.getMessage());
            return false;
        }
    }

    public synchronized void processFixturesJson(String json, boolean isReplay) {
        Instant now = Instant.now(clock);
        evictExpiredFullTimeMatches(now);
        processFixturesJson(json, loadSeasonFixtures(), now, isReplay);
    }

    private void processFixturesJson(String json, List<DbFixture> dbFixtures, Instant now, boolean isReplay) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (RuntimeException ex) {
            LOG.warn("Invalid FPL fixtures JSON payload: {}", ex.getMessage());
            return;
        }
        if (root == null || !root.isArray()) {
            LOG.warn("Expected JSON array from FPL fixtures endpoint");
            return;
        }

        Map<ClubPair, DbFixture> dbByPair = new HashMap<>();
        for (DbFixture db : dbFixtures) {
            dbByPair.put(new ClubPair(db.homeClubId(), db.awayClubId()), db);
        }

        for (int i = 0; i < root.size(); i++) {
            JsonNode node = root.get(i);
            if (node == null || !node.isObject()) {
                continue;
            }
            int teamH = node.path("team_h").asInt(-1);
            int teamA = node.path("team_a").asInt(-1);
            TeamMapping homeTeam = teamMap.get(teamH);
            TeamMapping awayTeam = teamMap.get(teamA);
            if (homeTeam == null || awayTeam == null) {
                continue;
            }
            DbFixture dbFixture = dbByPair.get(new ClubPair(homeTeam.clubId(), awayTeam.clubId()));
            if (dbFixture == null) {
                continue;
            }

            boolean started = node.path("started").asBoolean(false);
            boolean finishedProvisional = node.path("finished_provisional").asBoolean(false);
            boolean finished = node.path("finished").asBoolean(false);
            int minute = Math.max(0, node.path("minutes").asInt(0));
            Integer homeScore = nullableInt(node.get("team_h_score"));
            Integer awayScore = nullableInt(node.get("team_a_score"));
            Instant kickoff = parseKickoff(node.get("kickoff_time"), dbFixture);

            String status = (finishedProvisional || finished)
                    ? LiveMatchState.STATUS_FT
                    : started ? LiveMatchState.STATUS_LIVE : LiveMatchState.STATUS_PRE;

            TrackedFixture existing = trackedFixtures.get(dbFixture.id());
            if (!shouldTrackFixture(status, existing, dbFixture, kickoff, now, isReplay)) {
                continue;
            }

            boolean isInitialSighting = (existing == null);
            List<TrackedGoal> updatedGoals = extractGoals(node.get("stats"), minute, isInitialSighting,
                    existing == null ? List.of() : existing.trackedGoals(), dbFixture.id());
            List<GoalEvent> goalEvents = updatedGoals.stream()
                    .map(TrackedGoal::toEvent)
                    .toList();

            List<PlayerEvent> assists = extractPlayerEvents(node.get("stats"), "assists", minute, isInitialSighting,
                    existing == null ? List.of() : existing.state().assists(), dbFixture.id());
            List<PlayerEvent> redCards = extractPlayerEvents(node.get("stats"), "red_cards", minute, isInitialSighting,
                    existing == null ? List.of() : existing.state().redCards(), dbFixture.id());

            LiveMatchState newState = new LiveMatchState(
                    dbFixture.id(),
                    status,
                    minute,
                    homeScore,
                    awayScore,
                    goalEvents,
                    assists,
                    redCards,
                    now,
                    LiveMatchState.SOURCE_FPL
            );

            Instant ftReachedAt = null;
            if (LiveMatchState.STATUS_FT.equals(status)) {
                ftReachedAt = (existing != null && existing.ftReachedAt() != null) ? existing.ftReachedAt() : now;
            }

            boolean changed = existing == null || !newState.sameContentAs(existing.state());
            LiveMatchState stateToStore = changed ? newState : existing.state();
            trackedFixtures.put(dbFixture.id(), new TrackedFixture(stateToStore, updatedGoals, ftReachedAt));

            if (changed) {
                streamService.broadcast("score", newState);
            }
        }
    }

    private boolean shouldTrackFixture(String status, TrackedFixture existing, DbFixture dbFixture,
                                       Instant kickoff, Instant now, boolean isReplay) {
        if (isReplay) {
            return existing != null
                    || LiveMatchState.STATUS_LIVE.equals(status)
                    || LiveMatchState.STATUS_FT.equals(status);
        }
        if (evictedFtFixtureIds.contains(dbFixture.id())) {
            return false;
        }
        boolean currentlyLiveInMemory = existing != null
                && LiveMatchState.STATUS_LIVE.equals(existing.state().status());
        boolean inKickoffWindow = kickoff != null && isKickoffInWindow(kickoff, now);
        return currentlyLiveInMemory || inKickoffWindow;
    }

    private Integer findActiveGameweek(List<DbFixture> dbFixtures, Instant now) {
        // 1. Any currently LIVE tracked fixture takes priority
        for (TrackedFixture tracked : trackedFixtures.values()) {
            if (LiveMatchState.STATUS_LIVE.equals(tracked.state().status())) {
                for (DbFixture db : dbFixtures) {
                    if (db.id() == tracked.state().fixtureId()) {
                        return db.gameweek();
                    }
                }
            }
        }

        // 2. Otherwise check DB fixtures with now in [kickoff - 5m, kickoff + 150m] that are not yet FT
        Integer candidateGw = null;
        for (DbFixture db : dbFixtures) {
            if ("FINISHED".equalsIgnoreCase(db.status())) {
                continue;
            }
            TrackedFixture tracked = trackedFixtures.get(db.id());
            if (tracked != null && LiveMatchState.STATUS_FT.equals(tracked.state().status())) {
                continue;
            }
            if (db.kickoffUtc() != null && isKickoffInWindow(db.kickoffUtc(), now)) {
                if (candidateGw == null || db.gameweek() < candidateGw) {
                    candidateGw = db.gameweek();
                }
            }
        }
        return candidateGw;
    }

    /**
     * Checks whether {@code now} falls within the active live polling window of a fixture:
     * from 5 minutes before kickoff ({@code kickoff - 5m}) up to 150 minutes after kickoff ({@code kickoff + 150m}).
     */
    public static boolean isKickoffInWindow(Instant kickoff, Instant now) {
        Instant windowStart = kickoff.minus(LEAD_BEFORE_KICKOFF);
        Instant windowEnd = kickoff.plus(WINDOW_AFTER_KICKOFF);
        return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
    }

    private void evictExpiredFullTimeMatches(Instant now) {
        for (Map.Entry<Integer, TrackedFixture> entry : trackedFixtures.entrySet()) {
            TrackedFixture tracked = entry.getValue();
            if (LiveMatchState.STATUS_FT.equals(tracked.state().status()) && tracked.ftReachedAt() != null) {
                if (Duration.between(tracked.ftReachedAt(), now).compareTo(FT_RETENTION) >= 0) {
                    if (trackedFixtures.remove(entry.getKey(), tracked)) {
                        evictedFtFixtureIds.add(entry.getKey());
                    }
                }
            }
        }
    }

    private List<TrackedGoal> extractGoals(JsonNode statsNode, int currentMinute, boolean isInitialSighting,
                                           List<TrackedGoal> previousGoals, int fixtureId) {
        if (statsNode == null || !statsNode.isArray()) {
            return List.of();
        }
        Map<GoalKey, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < statsNode.size(); i++) {
            JsonNode stat = statsNode.get(i);
            String identifier = stat.path("identifier").asText("");
            if ("goals_scored".equals(identifier)) {
                collectGoalCounts(stat.get("h"), "home", false, counts);
                collectGoalCounts(stat.get("a"), "away", false, counts);
            } else if ("own_goals".equals(identifier)) {
                // Own goal by home team ("h") awards a goal to "away"; own goal by away ("a") awards to "home"
                collectGoalCounts(stat.get("h"), "away", true, counts);
                collectGoalCounts(stat.get("a"), "home", true, counts);
            }
        }
        if (counts.isEmpty()) {
            return List.of();
        }

        List<TrackedGoal> retained = new ArrayList<>();
        Map<GoalKey, Integer> keptSoFar = new HashMap<>();

        // Keep existing goal occurrences in their original order up to the current count
        for (TrackedGoal prev : previousGoals) {
            GoalKey key = new GoalKey(prev.fplElementId(), prev.side(), prev.ownGoal());
            int targetCount = counts.getOrDefault(key, 0);
            int seen = keptSoFar.getOrDefault(key, 0);
            if (seen < targetCount) {
                keptSoFar.put(key, seen + 1);
                retained.add(new TrackedGoal(prev.fplElementId(), prev.playerId(), prev.playerName(),
                        prev.side(), prev.ownGoal(), seen + 1, prev.approxMinute()));
            }
        }

        // Goals already present at first sighting have unknown minute (null); goals appearing later get currentMinute
        Integer newGoalMinute = isInitialSighting ? null : currentMinute;
        for (Map.Entry<GoalKey, Integer> entry : counts.entrySet()) {
            GoalKey key = entry.getKey();
            int targetCount = entry.getValue();
            int alreadyKept = keptSoFar.getOrDefault(key, 0);
            if (alreadyKept < targetCount) {
                ResolvedPlayer player = resolvePlayer(key.fplElementId(), fixtureId);
                for (int occ = alreadyKept + 1; occ <= targetCount; occ++) {
                    retained.add(new TrackedGoal(key.fplElementId(), player.playerId(), player.playerName(),
                            key.side(), key.ownGoal(), occ, newGoalMinute));
                }
            }
        }
        return List.copyOf(retained);
    }

    private static void collectGoalCounts(JsonNode arrayNode, String benefitingSide,
                                          boolean ownGoal, Map<GoalKey, Integer> counts) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode item = arrayNode.get(i);
            int element = item.path("element").asInt(-1);
            int value = item.path("value").asInt(0);
            if (element > 0 && value > 0) {
                GoalKey key = new GoalKey(element, benefitingSide, ownGoal);
                counts.merge(key, value, Integer::sum);
            }
        }
    }

    private List<PlayerEvent> extractPlayerEvents(JsonNode statsNode, String targetIdentifier,
                                                  int currentMinute, boolean isInitialSighting,
                                                  List<PlayerEvent> previousEvents, int fixtureId) {
        if (statsNode == null || !statsNode.isArray()) {
            return List.of();
        }
        Map<String, Integer> previousMinutes = new HashMap<>();
        for (PlayerEvent prev : previousEvents) {
            String key = prev.fplElementId() + ":" + prev.side();
            if (!previousMinutes.containsKey(key)) {
                previousMinutes.put(key, prev.approxMinute());
            }
        }

        List<PlayerEvent> events = new ArrayList<>();
        for (int i = 0; i < statsNode.size(); i++) {
            JsonNode stat = statsNode.get(i);
            if (!targetIdentifier.equals(stat.path("identifier").asText(""))) {
                continue;
            }
            collectSidePlayerEvents(stat.get("h"), "home", currentMinute, isInitialSighting, previousMinutes, fixtureId, events);
            collectSidePlayerEvents(stat.get("a"), "away", currentMinute, isInitialSighting, previousMinutes, fixtureId, events);
        }
        return List.copyOf(events);
    }

    private void collectSidePlayerEvents(JsonNode arrayNode, String side, int currentMinute,
                                         boolean isInitialSighting, Map<String, Integer> previousMinutes,
                                         int fixtureId, List<PlayerEvent> out) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode item = arrayNode.get(i);
            int element = item.path("element").asInt(-1);
            int count = item.path("value").asInt(0);
            if (element <= 0 || count <= 0) {
                continue;
            }
            ResolvedPlayer player = resolvePlayer(element, fixtureId);
            String key = element + ":" + side;
            Integer approxMin = previousMinutes.containsKey(key)
                    ? previousMinutes.get(key)
                    : (isInitialSighting ? null : currentMinute);
            out.add(new PlayerEvent(player.playerId(), element, player.playerName(), side, count, approxMin));
        }
    }

    private ResolvedPlayer resolvePlayer(int fplElementId, int fixtureId) {
        PlayerMapping mapped = playerMap.get(fplElementId);
        if (mapped != null) {
            return new ResolvedPlayer(mapped.playerId(), mapped.playerName());
        }
        if (warnedUnmappedElements.add(fplElementId)) {
            LOG.warn("Unmapped FPL element {} in live fixture {}; displaying as '{}'",
                    fplElementId, fixtureId, UNKNOWN_PLAYER_NAME);
        }
        return new ResolvedPlayer(null, UNKNOWN_PLAYER_NAME);
    }

    private List<DbFixture> loadSeasonFixtures() {
        return jdbc.query("""
                SELECT f.id, f.gameweek, f.home_club_id, f.away_club_id, f.match_date, f.status, p.kickoff_utc
                FROM fixtures f
                LEFT JOIN football_data_fixtures p ON p.fixture_id = f.id
                WHERE f.league_id = ? AND f.season_year = ?
                ORDER BY f.id
                """, (rs, row) -> {
            String kickoffRaw = rs.getString("kickoff_utc");
            Instant kickoffUtc = null;
            if (kickoffRaw != null && !kickoffRaw.isBlank()) {
                try {
                    kickoffUtc = Instant.parse(kickoffRaw.strip());
                } catch (RuntimeException ignored) {
                }
            }
            return new DbFixture(
                    rs.getInt("id"),
                    rs.getInt("gameweek"),
                    rs.getInt("home_club_id"),
                    rs.getInt("away_club_id"),
                    rs.getDate("match_date").toLocalDate(),
                    rs.getString("status"),
                    kickoffUtc
            );
        }, LEAGUE_ID, SEASON_YEAR);
    }

    private static Instant parseKickoff(JsonNode kickoffNode, DbFixture dbFixture) {
        if (kickoffNode != null && kickoffNode.isString() && !kickoffNode.stringValue().isBlank()) {
            try {
                return Instant.parse(kickoffNode.stringValue().strip());
            } catch (RuntimeException ignored) {
            }
        }
        return dbFixture.kickoffUtc();
    }

    private static Integer nullableInt(JsonNode node) {
        if (node == null || node.isNull() || !node.isIntegralNumber()) {
            return null;
        }
        return node.intValue();
    }

    private static List<Path> discoverReplayFiles(Path dir) {
        Path target = Files.isDirectory(dir.resolve("fixtures")) ? dir.resolve("fixtures") : dir;
        if (!Files.isDirectory(target)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(target)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException ex) {
            LOG.warn("Cannot list replay directory {}: {}", target, ex.getMessage());
            return List.of();
        }
    }

    private static Map<Integer, TeamMapping> loadBundledTeams() {
        try {
            return new FplMappingReader().readBundledTeams();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load bundled FPL team-map.csv", ex);
        }
    }

    private static Map<Integer, PlayerMapping> loadBundledPlayers() {
        try {
            return new FplMappingReader().readBundledPlayers();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load bundled FPL player-map.csv", ex);
        }
    }

    private record DbFixture(int id, int gameweek, int homeClubId, int awayClubId,
                             LocalDate matchDate, String status, Instant kickoffUtc) {
    }

    private record ClubPair(int homeClubId, int awayClubId) {
    }

    private record GoalKey(int fplElementId, String side, boolean ownGoal) {
    }

    private record ResolvedPlayer(Integer playerId, String playerName) {
    }

    private record TrackedGoal(int fplElementId, Integer playerId, String playerName,
                               String side, boolean ownGoal, int occurrence, Integer approxMinute) {
        GoalEvent toEvent() {
            return new GoalEvent(playerId, fplElementId, playerName, side, ownGoal, approxMinute);
        }
    }

    private record TrackedFixture(LiveMatchState state, List<TrackedGoal> trackedGoals, Instant ftReachedAt) {
    }
}
