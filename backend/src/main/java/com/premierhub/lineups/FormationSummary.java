package com.premierhub.lineups;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record FormationSummary(String defaultFormation, int verifiedMatches,
                               String formationCounts, String fixtureIds) {
    public record Observation(int fixtureId, LocalDate matchDate, String formation) { }

    public static FormationSummary from(List<Observation> observations) {
        var counts = new TreeMap<String, Integer>();
        var latest = new TreeMap<String, Observation>();
        var chronology = Comparator.comparing(Observation::matchDate).thenComparingInt(Observation::fixtureId);
        var seen = new java.util.HashSet<Integer>();
        for (var row : observations) {
            Formation.lines(row.formation());
            if (!seen.add(row.fixtureId())) throw new IllegalArgumentException("Repeated fixture in formation sample");
            counts.merge(row.formation(), 1, Integer::sum);
            latest.merge(row.formation(), row, (first, second) -> chronology.compare(first, second) >= 0 ? first : second);
        }
        String chosen = counts.keySet().stream().max(Comparator.<String>comparingInt(counts::get)
                .thenComparing(formation -> latest.get(formation), chronology).thenComparing(Comparator.naturalOrder())).orElse(null);
        String frequencies = counts.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
        String ids = observations.stream().sorted(chronology).map(row -> Integer.toString(row.fixtureId()))
                .collect(Collectors.joining(";"));
        return new FormationSummary(chosen, observations.size(), frequencies, ids);
    }
}
