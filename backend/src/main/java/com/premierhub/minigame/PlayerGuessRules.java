package com.premierhub.minigame;

/** Pure game rules: persistence, authentication and the clock belong to the service. */
public final class PlayerGuessRules {
    public enum Status { IN_PROGRESS, WON, LOST, EXPIRED }
    public record State(Status status, int score, int guessesUsed, int revealedHints, Integer finalScore) { }

    private PlayerGuessRules() { }

    public static State initial() { return new State(Status.IN_PROGRESS, 100, 0, 3, null); }

    public static State reveal(State state) {
        requirePlaying(state);
        if (state.revealedHints() == 8) throw new IllegalArgumentException("ALL_HINTS_REVEALED");
        return new State(state.status(), Math.max(0, state.score() - 10), state.guessesUsed(),
                state.revealedHints() + 1, null);
    }

    public static State guess(State state, boolean correct) {
        requirePlaying(state);
        int used = state.guessesUsed() + 1;
        if (correct) return new State(Status.WON, state.score(), used, state.revealedHints(), state.score());
        int score = Math.max(0, state.score() - 20);
        return new State(used == 3 ? Status.LOST : Status.IN_PROGRESS, score, used,
                Math.min(8, state.revealedHints() + 1), used == 3 ? 0 : null);
    }

    public static State expire(State state) {
        return state.status() == Status.IN_PROGRESS
                ? new State(Status.EXPIRED, state.score(), state.guessesUsed(), state.revealedHints(), 0) : state;
    }

    private static void requirePlaying(State state) {
        if (state.status() != Status.IN_PROGRESS) throw new IllegalArgumentException("GAME_FINISHED");
    }
}
