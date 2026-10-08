package com.premierhub.minigame;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import static com.premierhub.minigame.PlayerGuessRules.*;

public final class PlayerGuessData {
    public enum Mode { DAILY, PRACTICE }
    public record PlayerChoice(int playerId, String name) { }
    public record Candidate(int playerId, String name, int clubId, String club, String nationality,
                            LocalDate birthDate, Integer heightCm, String preferredFoot, Integer shirtNumber,
                            Integer fc27Overall, String primaryPosition, int membershipCount) {
        public boolean eligible(LocalDate date) {
            return membershipCount == 1 && text(name) && text(club) && text(nationality)
                    && birthDate != null && !birthDate.isAfter(date)
                    && heightCm != null && heightCm >= 100 && heightCm <= 250
                    && List.of("LEFT", "RIGHT", "BOTH").contains(preferredFoot == null ? "" : preferredFoot)
                    && shirtNumber != null && shirtNumber >= 1 && shirtNumber <= 99
                    && fc27Overall != null && fc27Overall >= 75 && fc27Overall <= 99
                    && List.of("GK", "LB", "CB", "RB", "CM", "CAM", "LM", "RM", "LW", "ST", "RW")
                            .contains(primaryPosition == null ? "" : primaryPosition);
        }
        public Snapshot snapshot(LocalDate date, List<PlayerChoice> choices) {
            return new Snapshot(1, date, playerId, name, clubId, club, heightCm, preferredFoot,
                    Period.between(birthDate, date).getYears(), fc27Overall, nationality, primaryPosition,
                    shirtNumber, List.copyOf(choices));
        }
    }
    // Internal records are NEVER controller response bodies.
    public record Snapshot(int rulesVersion, LocalDate questionDate, int playerId, String name, int clubId,
                           String club, int heightCm, String preferredFoot, int age, int fc27Overall,
                           String nationality, String primaryPosition, int shirtNumber, List<PlayerChoice> choices) { }
    public record Question(String id, Mode mode, LocalDate date, Instant expiresAt, Snapshot snapshot) { }
    public record Game(String id, long accountId, String questionId, Mode mode, LocalDate dailyDate,
                       State state, long version, Instant createdAt, Instant finishedAt) { }
    public record Guess(int number, int playerId, String name, boolean correct, int penalty, String autoRevealedHint) { }
    public record Cycle(long number, List<Integer> remaining, Integer lastPlayerId) { }
    public record Action(String fingerprint, String gameId) { }
    public record Hint(String key, String label, boolean revealed, String value) { }
    public record Answer(int playerId, String name, int clubId, String club, String primaryPosition, int shirtNumber) { }
    public record GameView(String gameId, long accountId, int season, Mode mode, LocalDate questionDate,
                           Status status, long version, int currentScore, Integer finalScore, int guessesUsed,
                           int guessesRemaining, int revealedHintCount, int totalHints, boolean canGuess,
                           boolean canRevealHint, String nextHintKey, Instant serverTime, Instant expiresAt,
                           Instant nextDailyAt, List<Hint> hints, List<Guess> guesses, Answer answer) { }
    public record Current(String status, LocalDate date, Instant serverTime, Instant nextDailyAt, GameView game) { }
    public record Effect(String type, int scoreChange, String revealedHintKey, boolean replayed) { }
    public record Mutation(String code, String message, GameView game, Effect effect) { }
    public record Ranked(long rank, long accountId, String displayName, long totalPoints, long dailyGames, boolean tied) { }
    public record Leaderboard(int season, Instant serverTime, List<Ranked> players, int offset, int limit) { }
    public record History(List<GameView> games, int offset, int limit) { }
    private static boolean text(String value) { return value != null && !value.isBlank(); }
    private PlayerGuessData() { }
}
