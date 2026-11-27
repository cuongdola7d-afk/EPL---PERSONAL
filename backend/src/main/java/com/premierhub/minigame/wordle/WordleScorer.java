package com.premierhub.minigame.wordle;

import java.util.Arrays;

public final class WordleScorer {
    private WordleScorer() { }

    /**
     * Two-pass Wordle scoring returning a string of 'H' (hit), 'N' (near), 'M' (miss)
     * matching the exact length of guessLetters and answerLetters.
     */
    public static String score(String guessLetters, String answerLetters) {
        if (guessLetters == null || answerLetters == null) {
            throw new IllegalArgumentException("Guess and answer must not be null");
        }
        int n = answerLetters.length();
        if (guessLetters.length() != n) {
            throw new IllegalArgumentException("Guess length must equal answer length");
        }
        char[] result = new char[n];
        Arrays.fill(result, 'M');
        int[] remaining = new int[26];

        // Pass 1: exact hits first, count unmatched answer letters.
        for (int i = 0; i < n; i++) {
            char g = guessLetters.charAt(i);
            char a = answerLetters.charAt(i);
            if (g < 'A' || g > 'Z' || a < 'A' || a > 'Z') {
                throw new IllegalArgumentException("Letters must be uppercase A-Z");
            }
            if (g == a) {
                result[i] = 'H';
            } else {
                remaining[a - 'A']++;
            }
        }

        // Pass 2: near hits using remaining available letter counts.
        for (int i = 0; i < n; i++) {
            if (result[i] == 'H') {
                continue;
            }
            int idx = guessLetters.charAt(i) - 'A';
            if (remaining[idx] > 0) {
                result[i] = 'N';
                remaining[idx]--;
            }
        }
        return new String(result);
    }
}
