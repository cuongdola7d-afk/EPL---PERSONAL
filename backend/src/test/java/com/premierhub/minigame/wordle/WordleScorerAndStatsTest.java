package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleData.Status;
import com.premierhub.minigame.wordle.WordleStatsCalculator.GameOutcome;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordleScorerAndStatsTest {
    @Test
    void matchesAllSharedScoreCasesAndKeyCodecRoundTrips() throws Exception {
        Path casesPath = Path.of("..", "docs", "wordle-score-cases.json");
        assertTrue(Files.exists(casesPath), "Shared score cases file must exist: " + casesPath.toAbsolutePath());
        JsonNode cases = JsonMapper.builder().build().readTree(Files.readString(casesPath));
        assertTrue(cases.isArray() && cases.size() >= 30, "Expected at least 30 shared scoring cases");
        for (JsonNode entry : cases) {
            String name = entry.get("name").asText();
            String guess = entry.get("guess").asText();
            String answer = entry.get("answer").asText();
            String expected = entry.get("expected").asText();
            assertEquals(expected, WordleScorer.score(guess, answer),
                    "Mismatch in shared case " + name + " (" + guess + " vs " + answer + ")");
        }

        String key1 = WordleKeyCodec.encode("VANDIJK", "550e8400-e29b-41d4-a716-446655440000");
        String key2 = WordleKeyCodec.encode("VANDIJK", "660e8400-e29b-41d4-a716-446655440001");
        assertNotEquals(key1, key2);
        assertFalse(key1.contains("VANDIJK"));
        assertEquals("VANDIJK", WordleKeyCodec.decode(key1));
        assertEquals("VANDIJK", WordleKeyCodec.decode(key2));
        assertEquals("MACALLISTER", WordleKeyCodec.decode(WordleKeyCodec.encode("MACALLISTER", "p-123")));
    }

    @Test
    void scoresRepeatedLettersInGuessAndAnswerAndMultiWordAgainstSingleWord() {
        // Prompt example: HAALAND vs VANDIJK -> A at index 1 is H, second/third A are M, N & D are N.
        assertEquals("MHMMMNN", WordleScorer.score("HAALAND", "VANDIJK"));
        // Exact match
        assertEquals("HHHHHHH", WordleScorer.score("VANDIJK", "VANDIJK"));
        // Multi-word answer VAN DIJK (VANDIJK) guessed with 1-word BARCOLA -> A at 1 is H, rest M
        assertEquals("MHMMMMM", WordleScorer.score("BARCOLA", "VANDIJK"));
        // Repeated letter in guess when answer has 1 occurrence in wrong position -> only first L gets N, second L gets M
        assertEquals("NMMMM", WordleScorer.score("LLXYZ", "ABCLD"));
        // Exact hits in middle + swapped edges
        assertEquals("NHHHN", WordleScorer.score("LLAMA", "ALAML"));
        // Repeated letter in answer (3 A's in ABACA), guess has 2 A's in wrong places + 1 exact -> H at 0, N at 1 and 3
        assertEquals("HNMMM", WordleScorer.score("AAXYZ", "ABACA"));
        assertEquals("MNMNM", WordleScorer.score("XAXAX", "ABACA"));
        // Guess has 3 E's, answer XEAE has 2 E's (index 1 exact H, index 3 unmatched -> index 0 gets N, index 2 gets M)
        assertEquals("NHMM", WordleScorer.score("EEEY", "XEAE"));
        // Guess EEEX vs EXEA: both E's in answer match at indices 0 & 2 -> middle E is M, X at 3 is N
        assertEquals("HMHN", WordleScorer.score("EEEX", "EXEA"));
    }

    @Test
    void scoresShorterGuessesUsingAllRemainingAnswerPositions() {
        // Prompt examples against VANDIJK (7 letters)
        assertEquals("MHMMM", WordleScorer.score("SALAH", "VANDIJK"));
        assertEquals("NMNN", WordleScorer.score("ISAK", "VANDIJK"));
        // Repeated letter at positions >= L in answer (both K's at indices 5 & 6)
        assertEquals("NNMM", WordleScorer.score("KKXX", "VANDIKK"));
        assertEquals("NNMM", WordleScorer.score("KKKX", "VANDIKK"));
        // Exact hit at index 0 plus two repeated K's at positions >= L
        assertEquals("HNNM", WordleScorer.score("KKKX", "KANDIKK"));
        // Guess longer than answer must be rejected
        assertThrows(IllegalArgumentException.class, () -> WordleScorer.score("ODEGAARD", "VANDIJK"));
    }

    @Test
    void computesPersonalStatsStreaksAndIgnoresUnfinishedPlayingGames() {
        LocalDate d1 = LocalDate.of(2026, 11, 20);
        LocalDate d2 = LocalDate.of(2026, 11, 21);
        LocalDate d3 = LocalDate.of(2026, 11, 22);
        // Skip d4 (2026-11-23) -> streak breaks!
        LocalDate d5 = LocalDate.of(2026, 11, 24);
        LocalDate d6 = LocalDate.of(2026, 11, 25);
        LocalDate d7 = LocalDate.of(2026, 11, 26); // unfinished PLAYING from yesterday -> ignored!
        LocalDate d8 = LocalDate.of(2026, 11, 27); // today

        // Won d1, d2, d3 (maxStreak=3), skipped d4, won d5, LOST d6, abandoned d7, won d8 -> currentStreak=1
        var stats = WordleStatsCalculator.compute(List.of(
                new GameOutcome(d1, Status.WON, 2),
                new GameOutcome(d2, Status.WON, 4),
                new GameOutcome(d3, Status.WON, 4),
                new GameOutcome(d5, Status.WON, 1),
                new GameOutcome(d6, Status.LOST, 6),
                new GameOutcome(d7, Status.PLAYING, 3),
                new GameOutcome(d8, Status.WON, 6)
        ), d8);

        assertEquals(6, stats.played()); // 5 wins + 1 loss; abandoned d7 NOT counted!
        assertEquals(5, stats.wins());
        assertEquals(83, stats.winPercentage());
        assertEquals(1, stats.currentStreak());
        assertEquals(3, stats.maxStreak());
        assertEquals(List.of(1, 1, 0, 2, 0, 1), stats.distribution());

        // If today is still PLAYING (not lost yet) and user won yesterday + day before -> streak stays alive at 2
        var aliveStreak = WordleStatsCalculator.compute(List.of(
                new GameOutcome(d6, Status.WON, 3),
                new GameOutcome(d7, Status.WON, 2),
                new GameOutcome(d8, Status.PLAYING, 1)
        ), d8);
        assertEquals(2, aliveStreak.played());
        assertEquals(2, aliveStreak.currentStreak());
        assertEquals(2, aliveStreak.maxStreak());

        // Losing today breaks an active streak immediately
        var brokenByLoss = WordleStatsCalculator.compute(List.of(
                new GameOutcome(d6, Status.WON, 3),
                new GameOutcome(d7, Status.WON, 2),
                new GameOutcome(d8, Status.LOST, 6)
        ), d8);
        assertEquals(3, brokenByLoss.played());
        assertEquals(0, brokenByLoss.currentStreak());
        assertEquals(2, brokenByLoss.maxStreak());

        // Skipping yesterday breaks current streak even if today hasn't been played yet
        var brokenBySkip = WordleStatsCalculator.compute(List.of(
                new GameOutcome(d6, Status.WON, 3)
        ), d8);
        assertEquals(0, brokenBySkip.currentStreak());
        assertEquals(1, brokenBySkip.maxStreak());
    }
}
