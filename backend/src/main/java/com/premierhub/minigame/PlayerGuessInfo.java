package com.premierhub.minigame;

import java.time.Instant;
import java.util.List;

public record PlayerGuessInfo(int season, String timezone, Instant serverTime, Instant nextDailyAt,
                               boolean loginRequired, int initialScore, int maxGuesses, int initiallyRevealedHints,
                               int revealPenalty, int wrongGuessPenalty, List<PlayerGuessData.Hint> hintOrder) { }
