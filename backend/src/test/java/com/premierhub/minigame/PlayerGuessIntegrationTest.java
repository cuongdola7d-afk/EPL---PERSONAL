package com.premierhub.minigame;

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
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static com.premierhub.minigame.PlayerGuessData.*;
import static com.premierhub.minigame.PlayerGuessRules.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:player-guess;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql,classpath:auth-schema.sql,classpath:fantasy-gameweek-schema.sql,classpath:fantasy-entry-schema.sql,classpath:fantasy-result-schema.sql,classpath:player-guess-schema.sql",
        "premierhub.minigame.enabled=true", "premierhub.auth-proxy.enabled=false",
        "PREMIERHUB_GOOGLE_CLIENT_ID=", "PREMIERHUB_GOOGLE_CLIENT_SECRET=", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class PlayerGuessIntegrationTest {
    static final String API = "/api/minigame/2026/player-guess";
    static final Instant NOON = Instant.parse("2026-10-08T05:00:00Z");
    static final Instant MIDNIGHT = Instant.parse("2026-10-08T17:00:00Z");
    @Autowired JdbcTemplate jdbc;
    @Autowired PlayerGuessService service;
    @Autowired MockMvc mvc;
    @MockitoBean(name = "playerGuessClock") Clock clock;
    @MockitoSpyBean PlayerGuessRepository repository;
    final AtomicReference<Instant> time = new AtomicReference<>();
    final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void setup() {
        for (String table : List.of("player_guess_daily_results", "player_guess_actions", "player_guess_guesses", "player_guess_practice_state",
                "player_guess_games", "player_guess_questions", "player_guess_selector", "player_eligible_positions", "player_specific_positions",
                "manual_player_memberships", "player_season_profiles", "player_season_stats", "season_clubs", "players", "clubs", "accounts", "seasons"))
            jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO player_guess_selector VALUES (2026,'{\"number\":0,\"remaining\":[],\"lastPlayerId\":null}')");
        time.set(NOON);
        when(clock.instant()).thenAnswer(invocation -> time.get());
        jdbc.update("INSERT INTO accounts VALUES (101,'a@example.test','Test A',NULL,'USER',CURRENT_TIMESTAMP),(102,'b@example.test','Test B',NULL,'USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO seasons VALUES (39,2026),(39,2024)");
        jdbc.update("INSERT INTO clubs VALUES (1,'Test club',NULL),(2,'Outside season',NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,1)");
        for (int id = 1; id <= 7; id++) {
            jdbc.update("INSERT INTO players VALUES (?,?)", id, "Cầu thủ Ánh " + id);
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id) VALUES (39,2026,?,?)", id, id == 7 ? 2 : 1);
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?, '2026-07-01',NULL)", id, id == 7 ? 2 : 1);
            jdbc.update("INSERT INTO player_season_profiles VALUES (39,2026,?,?,'Vietnam','2000-10-08',?,'BOTH',9,?)",
                    id, id == 7 ? 2 : 1, id == 5 ? null : 180, id == 6 ? 74 : 75 + id);
            jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,'RW')", id);
        }
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,goals) VALUES (39,2024,1,1,10)");
    }

    static String key() { return UUID.randomUUID().toString(); }
    GameView start(long owner, Mode mode) { return service.start(owner, mode, key(), null, 0).game(); }
    int answer(GameView game) { return repository.question(repository.game(game.accountId(), game.gameId(), false).orElseThrow().questionId()).snapshot().playerId(); }
    int wrong(GameView game) { return answer(game) == 1 ? 2 : 1; }
    Mutation guess(GameView game, int player) { return service.change(game.accountId(), game.gameId(), key(), game.version(), player); }
    Mutation hint(GameView game) { return service.change(game.accountId(), game.gameId(), key(), game.version(), null); }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test void readsNeverStartGamesAndPracticeResumesUntilExplicitNextGame() {
        assertEquals("NOT_STARTED", service.current(101, Mode.DAILY).status());
        assertEquals("NOT_STARTED", service.current(101, Mode.PRACTICE).status());
        assertEquals(0, count("player_guess_questions"));
        var game = start(101, Mode.PRACTICE);
        var progress = hint(game).game();
        assertEquals(progress.gameId(), service.current(101, Mode.PRACTICE).game().gameId());
        assertEquals(progress.version(), service.read(101, game.gameId()).version());
        assertEquals(game.gameId(), start(101, Mode.PRACTICE).gameId());
        var won = guess(progress, answer(progress)).game();
        assertEquals(won, service.current(101, Mode.PRACTICE).game());
        assertThrows(ResponseStatusException.class, () -> service.start(101, Mode.PRACTICE, key(), null, 0));
        var next = service.start(101, Mode.PRACTICE, key(), won.gameId(), won.version()).game();
        assertNotEquals(game.gameId(), next.gameId());
        assertEquals(100, next.currentScore());
        assertEquals(0, count("player_guess_daily_results"));
        assertEquals(10, jdbc.queryForObject("SELECT goals FROM player_season_stats WHERE season_year=2024", Integer.class));
    }

    @Test void sharedDailySnapshotAndVietnameseDateStayStableAfterProfileChanges() {
        time.set(Instant.parse("2026-10-07T17:00:00Z"));
        var first = start(101, Mode.DAILY);
        var second = start(102, Mode.DAILY);
        assertEquals(LocalDate.of(2026, 10, 8), first.questionDate());
        assertEquals(first.hints(), second.hints());
        assertEquals(answer(first), answer(second));
        assertEquals(MIDNIGHT, first.expiresAt());
        int id = answer(first);
        jdbc.update("UPDATE player_season_profiles SET height_cm=190,fc27_overall=99,shirt_number=20 WHERE player_id=?", id);
        jdbc.update("UPDATE manual_player_memberships SET end_date='2026-10-08' WHERE player_id=?", id);
        var resumed = service.read(101, first.gameId());
        assertEquals("180 cm", resumed.hints().getFirst().value());
        assertEquals("Hai chân", resumed.hints().get(1).value());
        var age = hint(resumed).game();
        assertEquals("26 tuổi", age.hints().get(2).value());
        assertEquals("RW", guess(age, id).game().answer().primaryPosition());
        assertEquals(1, count("player_guess_questions"));
    }

    @Test void emptyPoolAndPracticeOnlyDailyCandidateHaveClearStates() {
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=74");
        var failure = assertThrows(ResponseStatusException.class, () -> start(101, Mode.DAILY));
        assertEquals("INSUFFICIENT_DATA", failure.getReason());
        assertEquals(0, count("player_guess_questions"));
        jdbc.update("UPDATE player_season_profiles SET fc27_overall=80 WHERE player_id=1");
        start(101, Mode.DAILY);
        failure = assertThrows(ResponseStatusException.class, () -> start(101, Mode.PRACTICE));
        assertEquals("PRACTICE_POOL_TOO_SMALL", failure.getReason());
        assertEquals(1, count("player_guess_games"));
    }

    @Test void poolUsesHalfOpenMembershipAndRejectsAmbiguousMembershipOrOutsideClub() {
        var date = LocalDate.of(2026, 10, 8);
        jdbc.update("UPDATE manual_player_memberships SET end_date='2026-10-08' WHERE player_id=1");
        jdbc.update("UPDATE manual_player_memberships SET start_date='2026-10-09' WHERE player_id=2");
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,3,1,'2026-10-08',NULL)");
        assertEquals(List.of(4), repository.candidates(date).stream().filter(c -> c.eligible(date)).map(Candidate::playerId).toList());
        assertEquals(4, answer(start(101, Mode.DAILY)));
    }

    @Test void guessingLowOvrIsAllowedButInvalidAndRepeatedIdsAreFree() {
        var game = start(101, Mode.DAILY);
        var bad = guess(game, 999);
        assertEquals("INVALID_PLAYER", bad.code());
        assertEquals(game, bad.game());
        var wrong = guess(game, 6).game();
        assertEquals(80, wrong.currentScore());
        assertEquals(2, wrong.guessesRemaining());
        assertEquals(3, wrong.revealedHintCount());
        assertEquals("age", wrong.guesses().getFirst().autoRevealedHint());
        var repeated = guess(wrong, 6);
        assertEquals("PLAYER_ALREADY_GUESSED", repeated.code());
        assertEquals(wrong, repeated.game());
        assertEquals(1, count("player_guess_guesses"));
        assertEquals(1, service.players(101, game.gameId(), "cau THU anh 6").size());
        assertTrue(service.players(101, game.gameId(), "7").isEmpty());
    }

    @Test void correctAtZeroIsWinAndPracticeNeverAddsLeaderboardPoints() {
        var game = start(101, Mode.PRACTICE);
        for (int i = 0; i < 6; i++) game = hint(game).game();
        var all = hint(game);
        assertEquals("ALL_HINTS_REVEALED", all.code());
        assertEquals(game, all.game());
        game = guess(game, 5).game();
        game = guess(game, 6).game();
        game = guess(game, answer(game)).game();
        assertEquals(Status.WON, game.status());
        assertEquals(0, game.finalScore());
        assertEquals(3, game.guessesUsed());
        assertFalse(game.canGuess());
        assertTrue(game.hints().stream().allMatch(Hint::revealed));
        assertTrue(service.leaderboard(0, 20).players().isEmpty());
        assertEquals(0, count("player_guess_daily_results"));
    }

    @Test void lossRevealsAnswerAndFinalScoreIsZeroRatherThanTemporaryForty() {
        var game = start(101, Mode.DAILY);
        for (int id : List.of(5, 6, wrong(game))) game = guess(game, id).game();
        assertEquals(Status.LOST, game.status());
        assertEquals(40, game.currentScore());
        assertEquals(0, game.finalScore());
        assertNotNull(game.answer());
        assertTrue(game.hints().stream().allMatch(Hint::revealed));
        var ended = hint(game);
        assertEquals("GAME_FINISHED", ended.code());
        assertEquals(game, ended.game());
        assertEquals(0, service.leaderboard(0, 20).players().getFirst().totalPoints());
    }

    @Test void retriesAndStaleVersionsDoNotChargeTwiceAndReplayReturnsLatestState() {
        var game = start(101, Mode.DAILY);
        String action = key();
        var changed = service.change(101, game.gameId(), action, 0, null).game();
        var newer = guess(changed, wrong(game)).game();
        var replay = service.change(101, game.gameId(), action, 0, null);
        assertTrue(replay.effect().replayed());
        assertEquals(newer, replay.game());
        assertEquals("VERSION_CONFLICT", service.change(101, game.gameId(), key(), 0, null).code());
        var failure = assertThrows(ResponseStatusException.class, () -> service.change(101, game.gameId(), action, 0, wrong(game)));
        assertEquals("ACTION_KEY_REUSED", failure.getReason());
        String winAction = key();
        var won = service.change(101, game.gameId(), winAction, newer.version(), answer(game));
        for (int i = 0; i < 3; i++) service.change(101, game.gameId(), winAction, newer.version(), answer(game));
        assertEquals(1, count("player_guess_daily_results"));
        assertEquals(won.game().finalScore().longValue(), service.leaderboard(0, 20).players().getFirst().totalPoints());
    }

    @Test void dailyExpiresAtMidnightWhilePracticeKeepsItsSnapshotAndProgress() {
        var daily = start(101, Mode.DAILY);
        var practice = hint(start(101, Mode.PRACTICE)).game();
        time.set(MIDNIGHT.minusNanos(1));
        assertEquals(Status.IN_PROGRESS, service.read(101, daily.gameId()).status());
        time.set(MIDNIGHT);
        var expired = service.change(101, daily.gameId(), key(), daily.version(), answer(daily));
        assertEquals("GAME_EXPIRED", expired.code());
        assertEquals(Status.EXPIRED, expired.game().status());
        assertEquals(0, expired.game().finalScore());
        assertNotNull(expired.game().answer());
        assertEquals(Status.EXPIRED, repository.game(101, daily.gameId(), false).orElseThrow().state().status());
        assertEquals(1, count("player_guess_daily_results"));
        assertEquals(practice.version(), service.current(101, Mode.PRACTICE).game().version());
        assertEquals(90, service.read(101, practice.gameId()).currentScore());
        assertEquals("NOT_STARTED", service.current(101, Mode.DAILY).status());
        var newDaily = start(101, Mode.DAILY);
        assertNotEquals(daily.gameId(), newDaily.gameId());
        assertEquals(100, newDaily.currentScore());
        assertEquals(3, newDaily.guessesRemaining());
        assertEquals(2, service.history(101, 0, 20).games().size());
    }

    @Test void cyclesPersistAcrossServiceRecreationAndPracticeAlwaysExcludesTodayDaily() {
        var seen = new java.util.HashSet<Integer>();
        int previous = -1;
        var restarted = new PlayerGuessService(new PlayerGuessRepository(jdbc), clock);
        for (int day = 0; day < 9; day++) {
            time.set(NOON.plusSeconds(day * 86400L));
            var daily = restarted.start(101, Mode.DAILY, key(), null, 0).game();
            int selected = answer(daily);
            if (day % 4 == 0) seen.clear();
            assertTrue(seen.add(selected));
            assertNotEquals(previous, selected);
            var current = service.current(101, Mode.PRACTICE).game();
            if (current != null && current.status() == Status.IN_PROGRESS) guess(current, answer(current));
            current = service.current(101, Mode.PRACTICE).game();
            var practice = service.start(101, Mode.PRACTICE, key(), current == null ? null : current.gameId(), current == null ? 0 : current.version()).game();
            assertNotEquals(selected, answer(practice));
            previous = selected;
        }
    }

    @Test void practiceAvoidsRecentAnswersUntilSmallPoolRequiresRelaxation() {
        int daily = answer(start(101, Mode.DAILY));
        var seen = new java.util.HashSet<Integer>();
        GameView game = start(101, Mode.PRACTICE);
        for (int i = 0; i < 5; i++) {
            if (i < 3) assertTrue(seen.add(answer(game)));
            assertNotEquals(daily, answer(game));
            var won = guess(game, answer(game)).game();
            game = service.start(101, Mode.PRACTICE, key(), won.gameId(), won.version()).game();
        }
    }

    @Test void leaderboardRanksTiesAcrossPagesWithoutUsingCompletionTime() {
        var first = start(101, Mode.DAILY);
        var second = start(102, Mode.DAILY);
        guess(first, answer(first));
        time.set(NOON.plusSeconds(200));
        guess(second, answer(second));
        var standings = service.leaderboard(0, 20).players();
        assertEquals(2, standings.size());
        assertEquals(1, standings.getFirst().rank());
        assertEquals(1, standings.getLast().rank());
        assertTrue(standings.getLast().tied());
        assertEquals(1, service.leaderboard(1, 1).players().getFirst().rank());
        time.set(NOON.plusSeconds(86400));
        var third = start(101, Mode.DAILY);
        guess(third, answer(third));
        assertEquals(200, service.leaderboard(0, 20).players().getFirst().totalPoints());
        assertEquals(2, service.leaderboard(0, 20).players().getLast().rank());
    }

    @Test void twoTabsAndIdenticalRequestsHaveOneEffect() throws Exception {
        var game = start(101, Mode.DAILY);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Mutation> command = () -> { gate.await(); return service.change(101, game.gameId(), key(), 0, null); };
            var a = executor.submit(command); var b = executor.submit(command); gate.countDown();
            assertEquals(1, List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)).stream().filter(r -> "OK".equals(r.code())).count());
            assertEquals(90, service.read(101, game.gameId()).currentScore());
        }
        var progress = service.read(101, game.gameId());
        String action = key();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Mutation> command = () -> service.change(101, game.gameId(), action, progress.version(), answer(game));
            var a = executor.submit(command); var b = executor.submit(command);
            assertEquals(Status.WON, a.get(15, TimeUnit.SECONDS).game().status());
            assertEquals(Status.WON, b.get(15, TimeUnit.SECONDS).game().status());
        }
        assertEquals(1, count("player_guess_guesses"));
        assertEquals(1, count("player_guess_daily_results"));
    }

    @Test void simultaneousStartsCreateOneQuestionOneDailyPerAccountAndOneActivePractice() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var a = executor.submit(() -> start(101, Mode.DAILY));
            var b = executor.submit(() -> start(101, Mode.DAILY));
            var c = executor.submit(() -> start(102, Mode.DAILY));
            assertEquals(a.get(15, TimeUnit.SECONDS).gameId(), b.get(15, TimeUnit.SECONDS).gameId());
            c.get(15, TimeUnit.SECONDS);
            assertEquals(1, count("player_guess_questions"));
            assertEquals(2, count("player_guess_games"));
            var d = executor.submit(() -> start(101, Mode.PRACTICE));
            var e = executor.submit(() -> start(101, Mode.PRACTICE));
            assertEquals(d.get(15, TimeUnit.SECONDS).gameId(), e.get(15, TimeUnit.SECONDS).gameId());
        }
    }

    @Test void rollbackBetweenCompletionAndLedgerDoesNotPartiallySaveWin() {
        var game = start(101, Mode.DAILY);
        doThrow(new IllegalStateException("simulated storage failure")).when(repository).recordResult(any());
        assertThrows(IllegalStateException.class, () -> guess(game, answer(game)));
        assertEquals(0, repository.game(101, game.gameId(), false).orElseThrow().version());
        assertEquals(0, count("player_guess_guesses"));
        assertEquals(0, count("player_guess_daily_results"));
        assertEquals(1, count("player_guess_actions"));
    }

    @Test void queuedRequestChecksClockAfterLockAndLateWriteRollsBack() {
        var game = start(101, Mode.DAILY);
        doAnswer(invocation -> { var result = invocation.callRealMethod(); time.set(MIDNIGHT); return result; })
                .when(repository).game(101, game.gameId(), true);
        assertEquals("GAME_EXPIRED", guess(game, answer(game)).code());
        assertEquals(0, count("player_guess_guesses"));
        assertEquals(1, count("player_guess_daily_results"));
    }

    @Test void lateSqlCannotCommitWinAfterMidnightAndNextReadExpiresIt() {
        var game = start(101, Mode.DAILY);
        doAnswer(invocation -> { invocation.callRealMethod(); time.set(MIDNIGHT); return null; })
                .when(repository).recordResult(any());
        var failure = assertThrows(ResponseStatusException.class, () -> guess(game, answer(game)));
        assertEquals("DAY_CHANGED_RETRY", failure.getReason());
        assertEquals(Status.IN_PROGRESS, repository.game(101, game.gameId(), false).orElseThrow().state().status());
        assertEquals(0, count("player_guess_guesses"));
        assertEquals(0, count("player_guess_daily_results"));
        assertEquals(Status.EXPIRED, service.read(101, game.gameId()).status());
        assertEquals(1, count("player_guess_daily_results"));
    }

    @Test void httpEnforcesLoginOwnershipCsrfAndOnlySendsRevealedData() throws Exception {
        mvc.perform(get(API + "/info")).andExpect(status().isOk()).andExpect(jsonPath("$.nextDailyAt").exists())
                .andExpect(jsonPath("$.hintOrder[0].value").isEmpty());
        assertEquals(0, count("player_guess_questions"));
        mvc.perform(get(API + "/practice/current")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/daily/current")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/leaderboard")).andExpect(status().isOk());
        String body = json.writeValueAsString(Map.of("actionId", key(), "expectedVersion", 0));
        mvc.perform(post(API + "/daily/start").with(user("a@example.test")).header("X-PrismaXI-Account-ID", 101)
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        var started = mvc.perform(post(API + "/daily/start").with(user("a@example.test")).with(csrf())
                .header("X-PrismaXI-Account-ID", 101).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var data = json.readTree(started.getResponse().getContentAsString());
        var state = data.path("game");
        assertTrue(state.path("answer").isNull());
        assertFalse(started.getResponse().getContentAsString().contains("snapshot"));
        assertFalse(started.getResponse().getContentAsString().contains("questionId"));
        assertFalse(started.getResponse().getContentAsString().contains("birthDate"));
        assertFalse(started.getResponse().getContentAsString().contains("fc27Overall"));
        for (int i = 2; i < 8; i++) assertTrue(state.path("hints").get(i).path("value").isNull());
        String gameId = state.path("gameId").asText();
        mvc.perform(get(API + "/games/" + gameId).with(user("b@example.test")).header("X-PrismaXI-Account-ID", 102))
                .andExpect(status().isNotFound());
        mvc.perform(get(API + "/games/" + gameId).with(user("b@example.test")).header("X-PrismaXI-Account-ID", 101))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_CHANGED"));
        mvc.perform(post(API + "/games/" + gameId + "/guesses").with(user("a@example.test")).with(csrf())
                .header("X-PrismaXI-Account-ID", 101).contentType("application/json")
                .content(json.writeValueAsString(Map.of("actionId", key(), "expectedVersion", 0, "name", "free text"))))
                .andExpect(status().isBadRequest());
        assertEquals(0, count("player_guess_guesses"));
        mvc.perform(get(API + "/players").param("gameId", gameId).with(user("a@example.test")).header("X-PrismaXI-Account-ID", 101))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].playerId").exists()).andExpect(jsonPath("$[0].club").doesNotExist());
    }

    @Test void startKeysAlsoRemainIdempotentAfterPracticeEndsAndNewGameStarts() {
        String action = key();
        var first = service.start(101, Mode.PRACTICE, action, null, 0).game();
        var won = guess(first, answer(first)).game();
        var next = service.start(101, Mode.PRACTICE, key(), won.gameId(), won.version()).game();
        var retried = service.start(101, Mode.PRACTICE, action, null, 0);
        assertTrue(retried.effect().replayed());
        assertEquals(won.gameId(), retried.game().gameId());
        assertEquals(next.gameId(), service.current(101, Mode.PRACTICE).game().gameId());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM player_guess_games WHERE mode='PRACTICE'", Integer.class));
    }

    @Test void dailyReplayAfterMidnightReturnsExpiredStateAndLeaderboardNormalizesUntouchedGames() {
        String action = key();
        var first = service.start(101, Mode.DAILY, action, null, 0).game();
        start(102, Mode.DAILY);
        time.set(MIDNIGHT);
        var standings = service.leaderboard(0, 20).players();
        assertEquals(2, standings.size());
        assertTrue(standings.stream().allMatch(row -> row.totalPoints() == 0));
        var replay = service.start(101, Mode.DAILY, action, null, 0);
        assertEquals(first.gameId(), replay.game().gameId());
        assertEquals(Status.EXPIRED, replay.game().status());
        assertNotNull(replay.game().answer());
        assertEquals(2, count("player_guess_daily_results"));
        assertEquals(1, count("player_guess_questions"));
    }

    @Test void newlyEligiblePlayersWaitUntilNextCycleAndIneligibleQueueMembersAreSkipped() {
        start(101, Mode.DAILY);
        jdbc.update("UPDATE player_season_profiles SET height_cm=180 WHERE player_id=5");
        for (int day = 1; day < 4; day++) {
            time.set(NOON.plusSeconds(86400L * day));
            assertNotEquals(5, answer(start(101, Mode.DAILY)));
        }
        // At cycle exhaustion, player 5 now belongs to the shuffled pool.
        time.set(NOON.plusSeconds(4 * 86400L));
        var fifth = start(101, Mode.DAILY);
        var storedCycle = json.readValue(jdbc.queryForObject("SELECT cycle_json FROM player_guess_selector", String.class), Cycle.class);
        assertEquals(2, storedCycle.number());
        assertEquals(5, storedCycle.remaining().size() + 1);
        assertTrue(answer(fifth) == 5 || storedCycle.remaining().contains(5));
        int removed = storedCycle.remaining().getFirst();
        jdbc.update("UPDATE player_season_profiles SET nationality=NULL WHERE player_id=?", removed);
        time.set(NOON.plusSeconds(5 * 86400L));
        assertNotEquals(removed, answer(start(101, Mode.DAILY)));
    }

    @Test void startingDuringMidnightWriteRollsBackQuestionCycleAndGame() {
        doAnswer(invocation -> { invocation.callRealMethod(); time.set(MIDNIGHT); return null; })
                .when(repository).insertGame(any());
        var failure = assertThrows(ResponseStatusException.class, () -> start(101, Mode.DAILY));
        assertEquals("DAY_CHANGED_RETRY", failure.getReason());
        assertEquals(0, count("player_guess_questions"));
        assertEquals(0, count("player_guess_games"));
        assertEquals(0, count("player_guess_actions"));
        assertEquals(0, json.readValue(jdbc.queryForObject("SELECT cycle_json FROM player_guess_selector", String.class), Cycle.class).number());
    }

    @Test void missingHeadersMalformedBodyAndStaleVersionDoNotMutateGame() throws Exception {
        var game = start(101, Mode.DAILY);
        mvc.perform(get(API + "/games/" + game.gameId()).with(user("a@example.test"))).andExpect(status().isBadRequest());
        mvc.perform(post(API + "/games/" + game.gameId() + "/guesses").with(user("a@example.test")).with(csrf())
                .header("X-PrismaXI-Account-ID", 101).contentType("application/json")
                .content(json.writeValueAsString(Map.of("actionId", key(), "expectedVersion", 0, "player_id", 0))))
                .andExpect(status().isBadRequest());
        mvc.perform(post(API + "/games/" + game.gameId() + "/hints/next").with(user("a@example.test")).with(csrf())
                .header("X-PrismaXI-Account-ID", 101).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        hint(game);
        mvc.perform(post(API + "/games/" + game.gameId() + "/hints/next").with(user("a@example.test")).with(csrf())
                .header("X-PrismaXI-Account-ID", 101).contentType("application/json")
                .content(json.writeValueAsString(Map.of("actionId", key(), "expectedVersion", 0))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        assertEquals(90, service.read(101, game.gameId()).currentScore());
        assertEquals(0, count("player_guess_guesses"));
    }

    @Test void cookieSessionAndCsrfCanStartAndReloadTheSameGame() throws Exception {
        String password = "Local-test-password-2026";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", "session@example.test", "displayName", "Session Test", "password", password))))
                .andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", "session@example.test", "password", password))))
                .andExpect(status().isOk()).andReturn();
        long owner = json.readTree(login.getResponse().getContentAsString()).path("id").asLong();
        var cookie = login.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        var csrfResponse = mvc.perform(get("/api/auth/csrf").cookie(cookie)).andExpect(status().isOk()).andReturn();
        var token = json.readTree(csrfResponse.getResponse().getContentAsString());
        var started = mvc.perform(post(API + "/daily/start").cookie(cookie)
                .header(token.path("headerName").asText(), token.path("token").asText()).header("X-PrismaXI-Account-ID", owner)
                .contentType("application/json").content(json.writeValueAsString(Map.of("actionId", key(), "expectedVersion", 0))))
                .andExpect(status().isOk()).andReturn();
        String gameId = json.readTree(started.getResponse().getContentAsString()).path("game").path("gameId").asText();
        mvc.perform(get(API + "/daily/current").cookie(cookie).header("X-PrismaXI-Account-ID", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.game.gameId").value(gameId));
    }

    @Test void leaderboardAndExpiredGuessShareTheSameLockOrderAndWriteOneZeroResult() throws Exception {
        var game = start(101, Mode.DAILY);
        start(102, Mode.DAILY);
        time.set(MIDNIGHT);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> service.leaderboard(0, 20));
            var b = executor.submit(() -> service.change(101, game.gameId(), key(), 0, answer(game)));
            assertEquals(2, a.get(15, TimeUnit.SECONDS).players().size());
            assertEquals("GAME_EXPIRED", b.get(15, TimeUnit.SECONDS).code());
        }
        assertEquals(2, count("player_guess_daily_results"));
        assertEquals(0, count("player_guess_guesses"));
    }
}
