package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleNames.NormalizedName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class WordleData {
    public static final int MAX_TURNS = 6;
    public static final int HINT_UNLOCK_TURNS = 4;
    public static final int MIN_ANSWER_LENGTH = 4;
    public static final int MAX_ANSWER_LENGTH = 11;
    public static final int MIN_ANSWER_OVR = 75;

    public enum Status { PLAYING, WON, LOST }

    // Internal records are never serialized directly as API responses.
    public record Candidate(
            int playerId,
            String name,
            int clubId,
            String club,
            String nationality,
            LocalDate birthDate,
            Integer shirtNumber,
            Integer fc27Overall,
            String primaryPosition,
            String broadPosition,
            String overrideWord,
            int membershipCount
    ) {
        public NormalizedName normalized() {
            return WordleNames.resolve(name, overrideWord);
        }

        public boolean activeSingleClub() {
            return membershipCount == 1 && name != null && !name.isBlank() && club != null && !club.isBlank();
        }

        public boolean eligibleAnswer() {
            if (!activeSingleClub()) {
                return false;
            }
            if (fc27Overall == null || fc27Overall < MIN_ANSWER_OVR) {
                return false;
            }
            int len = normalized().letters().length();
            return len >= MIN_ANSWER_LENGTH && len <= MAX_ANSWER_LENGTH;
        }
    }

    public record Cycle(long number, List<Integer> remaining, Integer lastPlayerId) { }

    public record Puzzle(
            String id,
            LocalDate puzzleDate,
            int puzzleNumber,
            int answerPlayerId,
            String answerWord,
            String answerLetters,
            Instant createdAt
    ) { }

    public record Game(
            String id,
            long accountId,
            String puzzleId,
            Status status,
            boolean hintUsed,
            Instant startedAt,
            Instant finishedAt
    ) { }

    public record GuessRow(
            int turn,
            String word,
            String letters,
            int playerId,
            String feedback,
            String playerName,
            String club
    ) { }

    public record DictionaryItem(
            int playerId,
            String letters,
            String word,
            String name,
            String club,
            Integer fc27Overall
    ) {
        public DictionaryEntry toPublicEntry() {
            return new DictionaryEntry(letters, word, name, club);
        }
    }

    // Public API response DTOs.
    public record DictionaryEntry(String letters, String word, String name, String club) { }

    public record GuessView(
            int turn,
            String word,
            String letters,
            String feedback,
            String playerName,
            String club
    ) { }

    public record AnswerView(
            int playerId,
            String name,
            int clubId,
            String club,
            Integer shirtNumber,
            String position,
            String nationality,
            Integer age,
            Integer overall,
            String word
    ) { }

    public record StatsView(
            int played,
            int wins,
            int winPercentage,
            int currentStreak,
            int maxStreak,
            List<Integer> distribution
    ) { }

    public record InfoView(
            boolean enabled,
            Integer puzzleNumber,
            LocalDate date,
            Integer length,
            List<Integer> shape,
            Instant nextAt,
            Instant serverTime
    ) { }

    public record TodayView(
            int puzzleNumber,
            LocalDate date,
            int length,
            List<Integer> shape,
            Instant serverTime,
            Instant nextAt,
            Status status,
            boolean hintUsed,
            String hintClub,
            List<GuessView> guesses,
            AnswerView answer,
            StatsView stats
    ) { }

    private WordleData() { }
}
