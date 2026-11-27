package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleData.StatsView;
import com.premierhub.minigame.wordle.WordleData.Status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WordleStatsCalculator {
    public record GameOutcome(LocalDate puzzleDate, Status status, int turnsUsed) { }

    private WordleStatsCalculator() { }

    /**
     * Computes personal Wordle statistics from finished games only (WON or LOST).
     * Unfinished PLAYING games (including past-midnight abandoned games) are never
     * counted as losses, and any missed or lost day breaks the streak.
     */
    public static StatsView compute(List<GameOutcome> games, LocalDate today) {
        int played = 0;
        int wins = 0;
        int[] dist = new int[6];
        Set<LocalDate> wonDates = new HashSet<>();
        boolean lostToday = false;

        for (GameOutcome game : games) {
            if (game.status() == Status.WON) {
                played++;
                wins++;
                if (game.turnsUsed() >= 1 && game.turnsUsed() <= 6) {
                    dist[game.turnsUsed() - 1]++;
                }
                wonDates.add(game.puzzleDate());
            } else if (game.status() == Status.LOST) {
                played++;
                if (today.equals(game.puzzleDate())) {
                    lostToday = true;
                }
            }
        }

        int winPercentage = played == 0 ? 0 : (int) Math.round(wins * 100.0 / played);

        List<LocalDate> sortedWins = new ArrayList<>(wonDates);
        sortedWins.sort(Comparator.naturalOrder());

        int maxStreak = 0;
        int run = 0;
        LocalDate prev = null;
        for (LocalDate date : sortedWins) {
            if (prev != null && date.equals(prev.plusDays(1))) {
                run++;
            } else {
                run = 1;
            }
            if (run > maxStreak) {
                maxStreak = run;
            }
            prev = date;
        }

        int currentStreak = 0;
        if (!lostToday && !sortedWins.isEmpty()) {
            LocalDate cursor = wonDates.contains(today) ? today
                    : wonDates.contains(today.minusDays(1)) ? today.minusDays(1) : null;
            while (cursor != null && wonDates.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            }
        }

        List<Integer> distribution = List.of(dist[0], dist[1], dist[2], dist[3], dist[4], dist[5]);
        return new StatsView(played, wins, winPercentage, currentStreak, maxStreak, distribution);
    }
}
