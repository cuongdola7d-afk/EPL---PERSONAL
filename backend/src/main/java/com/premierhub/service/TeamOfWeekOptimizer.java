package com.premierhub.service;

import com.premierhub.repository.TeamOfWeekRepository.Appearance;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public final class TeamOfWeekOptimizer {
    public static final List<String> SLOTS = List.of(
            "GK", "LB", "LCB", "RCB", "RB", "LCM", "CM", "RCM", "LW", "ST", "RW");
    private static final int FULL = (1 << SLOTS.size()) - 1;

    public record Selection(List<Appearance> players, int filledSlots, int ratingCents) { }
    private record State(Appearance[] players, int ratingCents) { }

    private TeamOfWeekOptimizer() { }

    public static String permission(String slot) {
        return switch (slot) {
            case "LCB", "RCB" -> "CB";
            case "LCM", "RCM" -> "CM";
            default -> slot;
        };
    }

    public static Selection select(List<Appearance> candidates) {
        State[] best = new State[FULL + 1];
        best[0] = new State(new Appearance[SLOTS.size()], 0);
        var seen = new HashSet<Integer>();
        for (var player : candidates.stream().sorted(Comparator.comparingInt(Appearance::playerId)).toList()) {
            if (!seen.add(player.playerId())) throw new IllegalArgumentException("Repeated player_id in candidates");
            if (player.rating() == null) continue;
            int ratingCents = player.rating().movePointRight(2).intValueExact();
            // Descending masks ensure this player is used at most once, even with multiple permissions.
            for (int mask = FULL; mask >= 0; mask--) {
                State current = best[mask];
                if (current == null) continue;
                for (int slot = 0; slot < SLOTS.size(); slot++) {
                    int bit = 1 << slot;
                    if ((mask & bit) != 0 || !player.eligiblePositions().contains(permission(SLOTS.get(slot)))) continue;
                    int next = mask | bit;
                    int score = current.ratingCents() + ratingCents;
                    if (best[next] != null && score < best[next].ratingCents()) continue;
                    var assignment = current.players().clone();
                    assignment[slot] = player;
                    var alternative = new State(assignment, score);
                    if (better(alternative, best[next])) best[next] = alternative;
                }
            }
        }
        int chosen = FULL;
        if (best[FULL] == null) {
            chosen = 0;
            for (int mask = 1; mask <= FULL; mask++) {
                if (best[mask] == null) continue;
                if (Integer.bitCount(mask) > Integer.bitCount(chosen)
                        || Integer.bitCount(mask) == Integer.bitCount(chosen) && better(best[mask], best[chosen])) {
                    chosen = mask;
                }
            }
        }
        // Arrays.asList retains NULL for unfilled slots; List.copyOf would reject them.
        return new Selection(Arrays.asList(best[chosen].players()), Integer.bitCount(chosen), best[chosen].ratingCents());
    }

    private static boolean better(State candidate, State previous) {
        if (previous == null || candidate.ratingCents() != previous.ratingCents()) {
            return previous == null || candidate.ratingCents() > previous.ratingCents();
        }
        // Exact hundredths, then lexicographically smallest player IDs in the declared slot order.
        for (int slot = 0; slot < SLOTS.size(); slot++) {
            int first = candidate.players()[slot] == null ? Integer.MAX_VALUE : candidate.players()[slot].playerId();
            int second = previous.players()[slot] == null ? Integer.MAX_VALUE : previous.players()[slot].playerId();
            if (first != second) return first < second;
        }
        return false;
    }
}
