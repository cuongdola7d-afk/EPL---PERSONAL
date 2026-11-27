package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleData.Status;
import com.premierhub.minigame.wordle.WordleStatsCalculator.GameOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordleScorerAndStatsTest {
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
