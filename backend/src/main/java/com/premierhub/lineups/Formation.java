package com.premierhub.lineups;

import java.util.Arrays;
import java.util.List;

/** Match formations are independent of Fantasy's formation and eligibility rules. */
public final class Formation {
    private Formation() { }

    public static List<Integer> lines(String value) {
        if (value == null || !value.matches("[1-5](?:-[1-5]){1,4}")) {
            throw new IllegalArgumentException("Invalid formation: " + value);
        }
        var lines = Arrays.stream(value.split("-")).map(Integer::parseInt).toList();
        if (lines.stream().mapToInt(Integer::intValue).sum() != 10) {
            throw new IllegalArgumentException("Formation must contain 10 outfield players: " + value);
        }
        var withGoalkeeper = new java.util.ArrayList<Integer>();
        withGoalkeeper.add(1);
        withGoalkeeper.addAll(lines);
        return List.copyOf(withGoalkeeper);
    }
}
