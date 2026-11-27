package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleData.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wordle-it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql,classpath:auth-schema.sql,classpath:fantasy-gameweek-schema.sql,classpath:fantasy-entry-schema.sql,classpath:fantasy-result-schema.sql,classpath:wordle-schema.sql",
        "premierhub.wordle.enabled=true",
        "premierhub.minigame.enabled=false",
        "premierhub.auth-proxy.enabled=false",
        "PREMIERHUB_GOOGLE_CLIENT_ID=",
        "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "logging.level.root=WARN",
        "debug=false"
})
@AutoConfigureMockMvc
class WordleIntegrationTest {
    private static final String API = "/api/minigame/2026/wordle";
    private static final Instant DAY1_NOON_VN = Instant.parse("2026-11-27T05:00:00Z"); // 12:00 VN
    private static final Instant DAY1_2359_VN = Instant.parse("2026-11-27T16:59:59Z"); // 23:59:59 VN
    private static final Instant DAY2_0000_VN = Instant.parse("2026-11-27T17:00:00Z"); // 00:00:00 VN (28/11)

    @Autowired JdbcTemplate jdbc;
    @Autowired WordleService service;
    @Autowired WordleRepository repository;
    @Autowired MockMvc mvc;
    @MockitoBean(name = "wordleClock") Clock clock;

    private final AtomicReference<Instant> time = new AtomicReference<>();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        service.evictDictionaryCache();
        for (String table : List.of(
                "wordle_guesses", "wordle_games", "wordle_puzzles", "wordle_selector", "wordle_name_overrides",
                "player_eligible_positions", "player_specific_positions", "manual_player_memberships",
                "player_season_profiles", "player_season_stats", "season_clubs", "players", "clubs", "accounts", "seasons")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("INSERT INTO wordle_selector VALUES (2026, '{\"number\":0,\"remaining\":[],\"lastPlayerId\":null}')");
        time.set(DAY1_NOON_VN);
        when(clock.instant()).thenAnswer(inv -> time.get());

        jdbc.update("INSERT INTO accounts VALUES (101,'a@example.test','Player A',NULL,'USER',CURRENT_TIMESTAMP),"
                + "(102,'b@example.test','Player B',NULL,'USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
        jdbc.update("INSERT INTO clubs VALUES (1,'Liverpool FC','Liverpool'),(2,'Manchester City FC','Manchester'),(3,'Arsenal FC','London')");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,1),(39,2026,2),(39,2026,3)");

        // Seven 7-letter players:
        // 1: Virgil van Dijk (OVR 88, eligible answer) -> VAN DIJK / VANDIJK
        // 2: Erling Haaland (OVR 90, eligible answer)  -> HAALAND / HAALAND
        // 3: Bradley Barcola (OVR 85, eligible answer) -> BARCOLA / BARCOLA
        // 4: Estêvão (OVR 74, LOW OVR -> dictionary ONLY, not eligible answer!) -> ESTEVAO
        // 5: Conor Bradley (OVR 73, LOW OVR -> dictionary ONLY) -> BRADLEY
        // 6: Hugo Ekitike (OVR 72, LOW OVR -> dictionary ONLY) -> EKITIKE
        // 7: Danny Welbeck (OVR 71, LOW OVR -> dictionary ONLY) -> WELBECK
        // 8: Dominic Solanke (OVR 70, LOW OVR -> dictionary ONLY) -> SOLANKE
        // 9: Eddie Nketiah (OVR 69, LOW OVR -> dictionary ONLY) -> NKETIAH
        // 10: Duplicate surname "Junior Barcola" (OVR 62, shares BARCOLA -> higher OVR Bradley Barcola must win!)
        // 11: 5-letter "Daniel Muñoz" (OVR 74) -> MUNOZ (included in 4..7 dictionary!)
        // 12: 8-letter "Martin Odegaard" (OVR 74) -> ODEGAARD (excluded from 4..7 dictionary because length 8 > 7!)
        addPlayer(1, "Virgil van Dijk", 1, "Netherlands", "1991-07-08", 4, 88, "CB");
        addPlayer(2, "Erling Haaland", 2, "Norway", "2000-07-21", 9, 90, "ST");
        addPlayer(3, "Bradley Barcola", 1, "France", "2002-09-02", 29, 85, "LW");
        addPlayer(4, "Estêvão", 3, "Brazil", "2007-04-24", 41, 74, "RW");
        addPlayer(5, "Conor Bradley", 1, "Northern Ireland", "2003-07-09", 12, 73, "RB");
        addPlayer(6, "Hugo Ekitike", 1, "France", "2002-06-20", 22, 72, "ST");
        addPlayer(7, "Danny Welbeck", 3, "England", "1990-11-26", 18, 71, "ST");
        addPlayer(8, "Dominic Solanke", 3, "England", "1997-09-14", 19, 70, "ST");
        addPlayer(9, "Eddie Nketiah", 3, "England", "1999-05-30", 14, 69, "ST");
        addPlayer(10, "Junior Barcola", 2, "France", "2006-01-01", 40, 62, "LW");
        addPlayer(11, "Daniel Muñoz", 3, "Colombia", "1996-05-26", 12, 74, "RB");
        addPlayer(12, "Martin Odegaard", 3, "Norway", "1998-12-17", 8, 74, "CAM");
    }

    private void addPlayer(int id, String name, int clubId, String nat, String birthDate,
                           Integer shirt, Integer ovr, String pos) {
        jdbc.update("INSERT INTO players VALUES (?, ?)", id, name);
        jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,?,?,'DEFENDER')", id, clubId);
        jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?,'2026-07-01',NULL)", id, clubId);
        jdbc.update("INSERT INTO player_season_profiles (league_id,season_year,player_id,club_id,nationality,birth_date,shirt_number,fc27_overall) VALUES (39,2026,?,?,?,?,?,?)",
                id, clubId, nat, birthDate, shirt, ovr);
        jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,?)", id, pos);
    }

    private WordleData.Puzzle forceOnlyAnswer(int playerId) {
        // Make only playerId eligible (OVR >= 75) so test has deterministic answer
        jdbc.update("UPDATE player_season_profiles SET fc27_overall = 70 WHERE player_id <> ?", playerId);
        jdbc.update("UPDATE player_season_profiles SET fc27_overall = 88 WHERE player_id = ?", playerId);
        service.evictDictionaryCache();
        service.today(101);
        return repository.findPuzzleByDate(LocalDate.of(2026, 11, 27)).orElseThrow();
    }

    @Test
    void rejectsUnauthenticatedAndMismatchedAccountHeader() throws Exception {
        mvc.perform(get(API + "/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mvc.perform(get(API + "/today")
                        .with(user("a@example.test").roles("USER"))
                        .header("X-PrismaXI-Account-ID", "102"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CHANGED"));
    }

    @Test
    void doesNotLeakAnswerBeforeFinishAndValidatesAllErrorCodesAndHintFlow() throws Exception {
        var puzzle = forceOnlyAnswer(1); // Virgil van Dijk (VAN DIJK / VANDIJK, length 7)
        assertEquals("VAN DIJK", puzzle.answerWord());
        assertEquals("VANDIJK", puzzle.answerLetters());

        // 1. /today while PLAYING must NOT leak answer id, name, word, letters, or club
        String todayJson = mvc.perform(get(API + "/today")
                        .with(user("a@example.test").roles("USER"))
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puzzleNumber").value(1))
                .andExpect(jsonPath("$.length").value(7))
                .andExpect(jsonPath("$.shape[0]").value(3))
                .andExpect(jsonPath("$.shape[1]").value(4))
                .andExpect(jsonPath("$.shape.length()").value(2))
                .andExpect(jsonPath("$.status").value("PLAYING"))
                .andExpect(jsonPath("$.hintUsed").value(false))
                .andExpect(jsonPath("$.hintClub").doesNotExist())
                .andExpect(jsonPath("$.answer").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertFalse(todayJson.contains("Virgil"));
        assertFalse(todayJson.contains("van Dijk"));
        assertFalse(todayJson.contains("VANDIJK"));
        assertFalse(todayJson.contains("Liverpool"));
        assertFalse(todayJson.contains("\"answerPlayerId\""));

        // 2. /dictionary has 4..N players including low-OVR, excludes >N, deduplicates BARCOLA to higher OVR, never exposes playerId or answer flags
        String dictJson = mvc.perform(get(API + "/dictionary")
                        .with(user("a@example.test").roles("USER"))
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dictJson.contains("playerId"));
        assertFalse(dictJson.contains("answer"));
        assertTrue(dictJson.contains("MUNOZ")); // 5 letters (4..7) included
        assertFalse(dictJson.contains("ODEGAARD")); // 8 letters (>7) excluded
        assertTrue(dictJson.contains("ESTEVAO")); // low OVR included
        assertTrue(dictJson.contains("Bradley Barcola"));
        assertFalse(dictJson.contains("Junior Barcola")); // lower OVR duplicate omitted

        // 3. Error codes: TOO_SHORT (<4), TOO_LONG (>N), INVALID_CHARACTERS, NOT_A_PLAYER
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"ABC\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TOO_SHORT"));

        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"ODEGAARD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TOO_LONG"));

        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"VAN DIJK\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CHARACTERS"));

        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"ZZZZZ\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_A_PLAYER"));

        // 4. Hint locked before turn 4
        mvc.perform(post(API + "/today/hint")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HINT_LOCKED"));

        // Turn 1: HAALAND (tests repeated A scoring)
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"HAALAND\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guesses[0].feedback").value("MHMMMNN"))
                .andExpect(jsonPath("$.guesses[0].playerName").value("Erling Haaland"))
                .andExpect(jsonPath("$.answer").doesNotExist());

        // Duplicate guess -> ALREADY_GUESSED
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"HAALAND\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALREADY_GUESSED"));

        // Turns 2, 3, 4 -> still PLAYING; hint still locked after turn 3, unlocks after turn 4
        for (String w : List.of("BARCOLA", "ESTEVAO")) {
            mvc.perform(post(API + "/today/guesses")
                            .with(user("a@example.test").roles("USER")).with(csrf())
                            .header("X-PrismaXI-Account-ID", "101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"letters\":\"" + w + "\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(post(API + "/today/hint")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HINT_LOCKED"));

        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"BRADLEY\"}"))
                .andExpect(status().isOk());

        // Hint succeeds after turn 4
        mvc.perform(post(API + "/today/hint")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hintUsed").value(true))
                .andExpect(jsonPath("$.hintClub").value("Liverpool FC"))
                .andExpect(jsonPath("$.answer").doesNotExist());

        // Second hint -> HINT_USED
        mvc.perform(post(API + "/today/hint")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HINT_USED"));

        // Turn 5: Win! Answer revealed with all metadata & stats updated
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"VANDIJK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.guesses[4].word").value("VAN DIJK"))
                .andExpect(jsonPath("$.guesses[4].feedback").value("HHHHHHH"))
                .andExpect(jsonPath("$.answer.name").value("Virgil van Dijk"))
                .andExpect(jsonPath("$.answer.club").value("Liverpool FC"))
                .andExpect(jsonPath("$.answer.shirtNumber").value(4))
                .andExpect(jsonPath("$.answer.position").value("CB"))
                .andExpect(jsonPath("$.answer.nationality").value("Netherlands"))
                .andExpect(jsonPath("$.answer.age").value(35))
                .andExpect(jsonPath("$.answer.overall").value(88))
                .andExpect(jsonPath("$.answer.word").value("VAN DIJK"))
                .andExpect(jsonPath("$.stats.played").value(1))
                .andExpect(jsonPath("$.stats.wins").value(1))
                .andExpect(jsonPath("$.stats.currentStreak").value(1))
                .andExpect(jsonPath("$.stats.distribution[4]").value(1));

        // Guess after win -> GAME_FINISHED
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"EKITIKE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FINISHED"));
    }

    @Test
    void losesAfterSixWrongGuessesAndRevealsAnswer() throws Exception {
        forceOnlyAnswer(1);
        List<String> sixWrong = List.of("HAALAND", "BARCOLA", "ESTEVAO", "BRADLEY", "EKITIKE", "WELBECK");
        for (int i = 0; i < sixWrong.size(); i++) {
            var res = mvc.perform(post(API + "/today/guesses")
                            .with(user("a@example.test").roles("USER")).with(csrf())
                            .header("X-PrismaXI-Account-ID", "101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"letters\":\"" + sixWrong.get(i) + "\"}"))
                    .andExpect(status().isOk());
            if (i < 5) {
                res.andExpect(jsonPath("$.status").value("PLAYING"))
                        .andExpect(jsonPath("$.answer").doesNotExist());
            } else {
                res.andExpect(jsonPath("$.status").value("LOST"))
                        .andExpect(jsonPath("$.answer.name").value("Virgil van Dijk"))
                        .andExpect(jsonPath("$.stats.played").value(1))
                        .andExpect(jsonPath("$.stats.wins").value(0))
                        .andExpect(jsonPath("$.stats.winPercentage").value(0))
                        .andExpect(jsonPath("$.stats.currentStreak").value(0));
            }
        }
        // Further guess rejected
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"SOLANKE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FINISHED"));
    }

    @Test
    void dailyCycleDoesNotRepeatAndSwitchesAtVietnamMidnight() {
        // 3 eligible answers (players 1, 2, 3)
        time.set(DAY1_2359_VN);
        var d1a = service.today(101);
        var d1b = service.today(102);
        assertEquals(LocalDate.of(2026, 11, 27), d1a.date());
        assertEquals(1, d1a.puzzleNumber());
        assertEquals(d1a.puzzleNumber(), d1b.puzzleNumber());

        Set<Integer> firstCycle = new HashSet<>();
        firstCycle.add(repository.findPuzzleByDate(LocalDate.of(2026, 11, 27)).orElseThrow().answerPlayerId());

        // Cross 00:00 Vietnam time -> new day (28/11)
        time.set(DAY2_0000_VN);
        var d2 = service.today(101);
        assertEquals(LocalDate.of(2026, 11, 28), d2.date());
        assertEquals(2, d2.puzzleNumber());
        firstCycle.add(repository.findPuzzleByDate(LocalDate.of(2026, 11, 28)).orElseThrow().answerPlayerId());

        // Day 3 (29/11) finishes cycle 1 -> all 3 distinct
        time.set(DAY2_0000_VN.plusSeconds(86400));
        var d3 = service.today(101);
        assertEquals(3, d3.puzzleNumber());
        int lastOfCycle1 = repository.findPuzzleByDate(LocalDate.of(2026, 11, 29)).orElseThrow().answerPlayerId();
        firstCycle.add(lastOfCycle1);
        assertEquals(3, firstCycle.size());

        // Day 4 (30/11) starts cycle 2 -> must NOT repeat lastOfCycle1
        time.set(DAY2_0000_VN.plusSeconds(2 * 86400));
        var d4 = service.today(101);
        assertEquals(4, d4.puzzleNumber());
        int firstOfCycle2 = repository.findPuzzleByDate(LocalDate.of(2026, 11, 30)).orElseThrow().answerPlayerId();
        assertNotEquals(lastOfCycle1, firstOfCycle2);
    }

    @Test
    void pastMidnightUnfinishedGameIsLockedAndNotCountedAsLoss() throws Exception {
        forceOnlyAnswer(1);
        service.submitGuess(101, "HAALAND");
        String day1GameId = repository.findGame(101,
                repository.findPuzzleByDate(LocalDate.of(2026, 11, 27)).orElseThrow().id(), false)
                .orElseThrow().id();

        // Advance past Vietnam midnight to Day 2
        time.set(DAY2_0000_VN);

        // Attempting to continue Day 1 game is rejected with GAME_FINISHED
        var staleErr = assertThrows(ResponseStatusException.class,
                () -> service.submitGuessForGame(101, day1GameId, "BARCOLA"));
        assertEquals("GAME_FINISHED", staleErr.getReason());

        // Attempting via REST with stale Day 1 date is also rejected with GAME_FINISHED
        mvc.perform(post(API + "/today/guesses")
                        .with(user("a@example.test").roles("USER")).with(csrf())
                        .header("X-PrismaXI-Account-ID", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"letters\":\"BARCOLA\",\"date\":\"2026-11-27\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FINISHED"));

        // Day 1 abandoned PLAYING game is NOT counted as a loss in stats
        var stats = service.stats(101);
        assertEquals(0, stats.played());
        assertEquals(0, stats.wins());
        assertEquals(0, stats.currentStreak());

        // /today on Day 2 returns fresh puzzle #2 in PLAYING state
        var day2 = service.today(101);
        assertEquals(2, day2.puzzleNumber());
        assertEquals(Status.PLAYING, day2.status());
        assertTrue(day2.guesses().isEmpty());
    }

    @Test
    void concurrentDuplicateGuessesRecordOnlyOnceAndNeverDuplicateTurn() throws Exception {
        forceOnlyAnswer(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<String> f1 = pool.submit(() -> {
                try {
                    service.submitGuess(101, "HAALAND");
                    return "OK";
                } catch (ResponseStatusException e) {
                    return e.getReason();
                }
            });
            Future<String> f2 = pool.submit(() -> {
                try {
                    service.submitGuess(101, "HAALAND");
                    return "OK";
                } catch (ResponseStatusException e) {
                    return e.getReason();
                }
            });
            List<String> outcomes = List.of(f1.get(10, TimeUnit.SECONDS), f2.get(10, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter("OK"::equals).count());
            assertEquals(1, outcomes.stream().filter("ALREADY_GUESSED"::equals).count());
            Integer guessCount = jdbc.queryForObject("SELECT COUNT(*) FROM wordle_guesses", Integer.class);
            assertEquals(1, guessCount);
        }
    }

    @Test
    void publicInfoDoesNotRequireAuthDoesNotCreatePuzzleAndNeverLeaksAnswer() throws Exception {
        // Restrict eligible answers to player 1 (Virgil van Dijk) without creating today's puzzle yet.
        jdbc.update("UPDATE player_season_profiles SET fc27_overall = 70 WHERE player_id <> 1");
        jdbc.update("UPDATE player_season_profiles SET fc27_overall = 88 WHERE player_id = 1");
        service.evictDictionaryCache();

        Integer beforeCount = jdbc.queryForObject("SELECT COUNT(*) FROM wordle_puzzles", Integer.class);
        assertEquals(0, beforeCount);

        String bodyBefore = mvc.perform(get(API + "/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.puzzleNumber").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.length").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.shape").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.date").value("2026-11-27"))
                .andExpect(jsonPath("$.nextAt").isNotEmpty())
                .andExpect(jsonPath("$.serverTime").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Integer afterCount = jdbc.queryForObject("SELECT COUNT(*) FROM wordle_puzzles", Integer.class);
        assertEquals(beforeCount, afterCount, "/info must not create a puzzle when none exists yet");
        assertFalse(bodyBefore.contains("van Dijk"));
        assertFalse(bodyBefore.contains("VANDIJK"));
        assertFalse(bodyBefore.contains("Liverpool"));
        assertFalse(bodyBefore.contains("answer"));
        assertFalse(bodyBefore.contains("playerId"));

        // Once today's puzzle exists, /info exposes puzzleNumber, length, and shape while still hiding answer details.
        service.today(101);
        String bodyAfter = mvc.perform(get(API + "/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.puzzleNumber").value(1))
                .andExpect(jsonPath("$.length").value(7))
                .andExpect(jsonPath("$.shape[0]").value(3))
                .andExpect(jsonPath("$.shape[1]").value(4))
                .andExpect(jsonPath("$.shape.length()").value(2))
                .andExpect(jsonPath("$.date").value("2026-11-27"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(bodyAfter.contains("van Dijk"));
        assertFalse(bodyAfter.contains("VANDIJK"));
        assertFalse(bodyAfter.contains("Liverpool"));
        assertFalse(bodyAfter.contains("answer"));
        assertFalse(bodyAfter.contains("playerId"));
    }

    @Test
    void unauthenticatedTodayStillReturns401() throws Exception {
        mvc.perform(get(API + "/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedOverrideWordWhenReadingCandidatesAndAppliesValidOverride() {
        jdbc.update("INSERT INTO wordle_name_overrides VALUES (1, 'VAN  DIJK', 'bad double space')");
        service.evictDictionaryCache();
        assertThrows(IllegalStateException.class, () -> repository.candidates(LocalDate.of(2026, 11, 27)));

        jdbc.update("UPDATE wordle_name_overrides SET word = 'ALISSON' WHERE player_id = 1");
        service.evictDictionaryCache();
        forceOnlyAnswer(1);
        var dict = service.dictionary();
        assertTrue(dict.stream().anyMatch(e -> "ALISSON".equals(e.word()) && "ALISSON".equals(e.letters())));
        assertTrue(dict.stream().noneMatch(e -> "VANDIJK".equals(e.letters())));
    }

    @Test
    void allowsFiveShortGuessesWithoutWinningAndWinsOnTurnSixWithExactAnswer() throws Exception {
        // Add 4..5 letter players (including SALAH and ISAK from prompt examples + a prefix player VAND)
        addPlayer(21, "Mohamed Salah", 1, "Egypt", "1992-06-15", 11, 74, "RW");
        addPlayer(22, "Alexander Isak", 1, "Sweden", "1999-09-21", 14, 74, "ST");
        addPlayer(23, "Bukayo Saka", 3, "England", "2001-09-05", 7, 74, "RW");
        addPlayer(24, "Test Vand", 3, "England", "2000-01-01", 99, 74, "CB"); // exact 4-letter prefix of VANDIJK -> HHHH but still PLAYING!
        forceOnlyAnswer(1); // Virgil van Dijk (VANDIJK, 7 letters)

        var g1 = service.submitGuess(101, "SALAH");
        assertEquals(Status.PLAYING, g1.status());
        assertEquals("MHMMM", g1.guesses().get(0).feedback());

        var g2 = service.submitGuess(101, "ISAK");
        assertEquals(Status.PLAYING, g2.status());
        assertEquals("NMNN", g2.guesses().get(1).feedback());

        // Even an all-H shorter prefix ("VAND" -> "HHHH") MUST NOT win!
        var g3 = service.submitGuess(101, "VAND");
        assertEquals(Status.PLAYING, g3.status());
        assertEquals("HHHH", g3.guesses().get(2).feedback());
        assertNull(g3.answer());

        var g4 = service.submitGuess(101, "SAKA");
        assertEquals(Status.PLAYING, g4.status());
        var g5 = service.submitGuess(101, "MUNOZ");
        assertEquals(Status.PLAYING, g5.status());
        assertEquals(5, g5.guesses().size());

        // Turn 6: exact answer wins!
        var g6 = service.submitGuess(101, "VANDIJK");
        assertEquals(Status.WON, g6.status());
        assertEquals(6, g6.guesses().size());
        assertEquals("HHHHHHH", g6.guesses().get(5).feedback());
        assertNotNull(g6.answer());
        assertEquals("Virgil van Dijk", g6.answer().name());
    }
}
