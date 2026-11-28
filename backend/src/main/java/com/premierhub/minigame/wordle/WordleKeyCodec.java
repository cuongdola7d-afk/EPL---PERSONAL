package com.premierhub.minigame.wordle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Lightweight obfuscation for answer letters sent to the browser for instant client-side scoring.
 * Uses a 4-byte mask derived from puzzleId, XORed with each letter byte and Base64-encoded.
 * Not intended as cryptographic security — prevents casual plaintext reading in JSON responses.
 */
public final class WordleKeyCodec {
    private static final String DOMAIN = ":prismaxi-wordle-key-v1";
    private static final int MASK_LEN = 4;

    private WordleKeyCodec() { }

    public static String encode(String answerLetters, String puzzleId) {
        if (answerLetters == null || answerLetters.isEmpty()) {
            throw new IllegalArgumentException("answerLetters must not be empty");
        }
        if (puzzleId == null || puzzleId.isEmpty()) {
            throw new IllegalArgumentException("puzzleId must not be empty");
        }
        byte[] mask = maskFor(puzzleId);
        byte[] plain = answerLetters.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[MASK_LEN + plain.length];
        System.arraycopy(mask, 0, out, 0, MASK_LEN);
        for (int i = 0; i < plain.length; i++) {
            int mix = (mask[i % MASK_LEN] & 0xFF) ^ ((i * 31 + 17) & 0xFF);
            out[MASK_LEN + i] = (byte) ((plain[i] & 0xFF) ^ mix);
        }
        return Base64.getEncoder().encodeToString(out);
    }

    public static String decode(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        byte[] raw = Base64.getDecoder().decode(key);
        if (raw.length <= MASK_LEN) {
            throw new IllegalArgumentException("key too short");
        }
        int len = raw.length - MASK_LEN;
        byte[] plain = new byte[len];
        for (int i = 0; i < len; i++) {
            int mix = (raw[i % MASK_LEN] & 0xFF) ^ ((i * 31 + 17) & 0xFF);
            plain[i] = (byte) ((raw[MASK_LEN + i] & 0xFF) ^ mix);
        }
        return new String(plain, StandardCharsets.US_ASCII);
    }

    private static byte[] maskFor(String puzzleId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((puzzleId + DOMAIN).getBytes(StandardCharsets.UTF_8));
            byte[] mask = new byte[MASK_LEN];
            System.arraycopy(digest, 0, mask, 0, MASK_LEN);
            return mask;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
