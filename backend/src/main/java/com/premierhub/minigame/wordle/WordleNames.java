package com.premierhub.minigame.wordle;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class WordleNames {
    private static final Pattern OVERRIDE_PATTERN = Pattern.compile("^[A-Z]+( [A-Z]+)*$");

    public record NormalizedName(String word, String letters) { }

    private WordleNames() { }

    /**
     * Normalizes a player's full name into the Wordle target representation:
     * drops the first token (given name) when multi-word, folds diacritics and
     * special letters, turns hyphens into spaces, removes apostrophes/dots,
     * and uppercases to A-Z with single spaces.
     */
    public static NormalizedName normalize(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new NormalizedName("", "");
        }
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int firstSpace = trimmed.indexOf(' ');
        String surnamePart = firstSpace >= 0 ? trimmed.substring(firstSpace + 1).trim() : trimmed;
        return cleanWord(surnamePart);
    }

    /**
     * Validates that a curated override word consists solely of ASCII A-Z words
     * separated by at most a single space (no leading/trailing/double spaces or accents).
     */
    public static boolean isValidOverrideWord(String overrideWord) {
        return overrideWord != null && OVERRIDE_PATTERN.matcher(overrideWord).matches();
    }

    /**
     * Parses an explicit override word after enforcing the strict A-Z + single-space format.
     */
    public static NormalizedName normalizeOverride(String overrideWord) {
        if (!isValidOverrideWord(overrideWord)) {
            throw new IllegalArgumentException(
                    "Invalid wordle_name_overrides.word (expected A-Z with single spaces): " + overrideWord);
        }
        return new NormalizedName(overrideWord, overrideWord.replace(" ", ""));
    }

    public static NormalizedName resolve(String fullName, String overrideWord) {
        if (overrideWord != null) {
            return normalizeOverride(overrideWord);
        }
        return normalize(fullName);
    }

    private static NormalizedName cleanWord(String raw) {
        String replaced = replaceSpecialCharacters(raw);
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        StringBuilder out = new StringBuilder(decomposed.length());
        boolean lastWasSpace = true;
        for (int i = 0; i < decomposed.length(); i++) {
            char ch = decomposed.charAt(i);
            if (ch == '\'' || ch == '\u2019' || ch == '\u2018' || ch == '`' || ch == '\u02BC' || ch == '.') {
                continue;
            }
            if (ch == '-' || ch == '\u2013' || ch == '\u2014' || Character.isWhitespace(ch)) {
                if (!lastWasSpace) {
                    out.append(' ');
                    lastWasSpace = true;
                }
                continue;
            }
            char upper = Character.toUpperCase(ch);
            if (upper >= 'A' && upper <= 'Z') {
                out.append(upper);
                lastWasSpace = false;
            }
        }
        String word = out.toString().trim();
        String letters = word.replace(" ", "");
        return new NormalizedName(word, letters);
    }

    private static String replaceSpecialCharacters(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case 'Ø', 'ø' -> sb.append('O');
                case 'Æ', 'æ' -> sb.append("AE");
                case 'ß', '\u1E9E' -> sb.append("SS");
                case 'Ł', 'ł' -> sb.append('L');
                case 'Đ', 'đ', 'Ð', 'ð' -> sb.append('D');
                case '\u0130', '\u0131' -> sb.append('I');
                case 'Œ', 'œ' -> sb.append("OE");
                default -> sb.append(String.valueOf(ch).toUpperCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}
