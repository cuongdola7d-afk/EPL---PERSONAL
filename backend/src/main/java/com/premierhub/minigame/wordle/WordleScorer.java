package com.premierhub.minigame.wordle;

import java.util.Arrays;

public final class WordleScorer {
    private WordleScorer() { }

    /**
     * Two-pass Wordle scoring supporting guesses of length 1..N against an answer of length N.
     * Pass 1 marks exact hits at indices 0..L-1 and counts all remaining answer letters (including L..N-1).
     * Pass 2 marks near hits for unmatched guess letters using the remaining counts.
     * Returns a string of 'H' (hit), 'N' (near), 'M' (miss) of exact length L.
     */
    public static String score(String guessLetters, String answerLetters) {
        if (guessLetters == null || answerLetters == null) {
            throw new IllegalArgumentException("Guess and answer must not be null");
        }
        int l = guessLetters.length();
        int n = answerLetters.length();
        if (l < 1 || l > n) {
            throw new IllegalArgumentException("Guess length must be between 1 and answer length");
        }
        char[] result = new char[l];
        Arrays.fill(result, 'M');
        int[] remaining = new int[26];

        // Pass 1: exact hits at 0..L-1, count all unmatched answer letters including L..N-1.
        for (int i = 0; i < l; i++) {
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
        for (int i = l; i < n; i++) {
            char a = answerLetters.charAt(i);
            if (a < 'A' || a > 'Z') {
                throw new IllegalArgumentException("Letters must be uppercase A-Z");
            }
            remaining[a - 'A']++;
        }

        // Pass 2: near hits for unmatched guess positions using remaining answer letter counts.
        for (int i = 0; i < l; i++) {
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
