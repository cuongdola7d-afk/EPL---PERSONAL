package com.premierhub.fpl;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "premierhub.fpl-collect.enabled", havingValue = "true")
public class FplCollectCommand implements ApplicationRunner {
    private final FplCollectorService collector;
    private final FplRatingMergeService merger;
    private final FplClient client;

    public FplCollectCommand(FplCollectorService collector, FplRatingMergeService merger, FplClient client) {
        this.collector = collector;
        this.merger = merger;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int gameweek = requiredIntOption(args, "gameweek", "premierhub.fpl-collect.gameweek");
        int season = optionalIntOption(args, 2026, "season", "premierhub.fpl-collect.season");
        String gwTag = String.format("gw%02d-2026", gameweek);

        Path mappingDir = Path.of(optionalOption(args, "data/fpl-2026",
                "mapping-dir", "premierhub.fpl-collect.mapping-dir"));
        Path outputDir = Path.of(optionalOption(args, "data/" + gwTag + "/fpl",
                "output-dir", "premierhub.fpl-collect.output-dir"));
        String inputDirOpt = optionalOption(args, null, "input-dir", "premierhub.fpl-collect.input-dir");
        boolean merge = Boolean.parseBoolean(optionalOption(args, "false", "merge", "premierhub.fpl-collect.merge"));
        boolean mergeOnly = Boolean.parseBoolean(optionalOption(args, "false",
                "merge-only", "premierhub.fpl-collect.merge-only"));
        boolean backfillConsistency = Boolean.parseBoolean(optionalOption(args, "false",
                "backfill-consistency", "premierhub.fpl-collect.backfill-consistency"));
        Set<Integer> fixtureIds = parseFixtureIds(optionalOption(args, null,
                "fixture-ids", "premierhub.fpl-collect.fixture-ids"));
        boolean allowProvisional = Boolean.parseBoolean(optionalOption(args, "false",
                "allow-provisional", "premierhub.fpl-collect.allow-provisional"));

        if (backfillConsistency && (merge || mergeOnly)) {
            throw new IllegalArgumentException(
                    "--backfill-consistency=true cannot be combined with --merge or --merge-only");
        }
        if (backfillConsistency && (!fixtureIds.isEmpty() || allowProvisional)) {
            throw new IllegalArgumentException(
                    "--backfill-consistency=true cannot be combined with --fixture-ids or --allow-provisional");
        }
        if (allowProvisional && fixtureIds.isEmpty() && !mergeOnly) {
            throw new IllegalArgumentException(
                    "--allow-provisional=true requires --fixture-ids=<id1,id2,...>");
        }

        if (!mergeOnly) {
            FplSource source = inputDirOpt != null ? FplSource.fromDirectory(Path.of(inputDirOpt)) : client;
            FplCollectorService.Result result = backfillConsistency
                    ? collector.collectBackfill(season, gameweek, mappingDir, outputDir, source)
                    : collector.collect(season, gameweek, mappingDir, outputDir, source, fixtureIds, allowProvisional);
            System.out.printf("FPL_COLLECT season=%d gw=%d fixtures=%d provisional=%d played=%d unmapped=%d summaries=%d finalCsv=%s%n",
                    season, result.gameweek(), result.fixturesMatched(), result.provisionalFixtures(),
                    result.playedRows(), result.unmappedCount(), result.elementSummaryCalls(), result.finalCsvWritten());
            if (!result.finalCsvWritten()) {
                throw new IllegalStateException("FPL collection found " + result.unmappedCount()
                        + " unmapped played player(s); see " + outputDir.resolve("unmapped.csv")
                        + ". Final CSVs were not written.");
            }
        }

        if (merge || mergeOnly) {
            String ratingsOpt = optionalOption(args, null, "ratings-file", "premierhub.fpl-collect.ratings-file");
            if (ratingsOpt == null || ratingsOpt.isBlank()) {
                throw new IllegalArgumentException("Set --ratings-file=<path> when --merge=true or --merge-only=true");
            }
            String lineupOpt = optionalOption(args, null,
                    "lineup-players-file", "premierhub.fpl-collect.lineup-players-file");
            Path mergeOutDir = Path.of(optionalOption(args, "data/" + gwTag,
                    "merge-output-dir", "premierhub.fpl-collect.merge-output-dir"));
            FplRatingMergeService.Result merged = merger.merge(
                    season, gameweek, outputDir, Path.of(ratingsOpt),
                    lineupOpt != null && !lineupOpt.isBlank() ? Path.of(lineupOpt) : null,
                    mergeOutDir, fixtureIds);
            System.out.printf("FPL_MERGE season=%d gw=%d included=%d blocked=%d rows=%d unrated=%d missing=%d anomalies=%d%n",
                    season, merged.gameweek(), merged.fixturesIncluded(), merged.fixturesBlocked(),
                    merged.statRowsWritten(), merged.unratedCount(), merged.missingCount(), merged.anomalyCount());
        }
    }

    static Set<Integer> parseFixtureIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        for (String token : raw.split(",", -1)) {
            String trimmed = token.strip();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("fixture-ids contains an empty entry: '" + raw + "'");
            }
            int parsed;
            try {
                parsed = Integer.parseInt(trimmed);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("fixture-ids must contain integer IDs: '" + trimmed + "'", error);
            }
            if (parsed <= 0) {
                throw new IllegalArgumentException("fixture-ids must contain positive integers: " + parsed);
            }
            ids.add(parsed);
        }
        return ids;
    }

    private static int requiredIntOption(ApplicationArguments args, String... names) {
        String raw = optionalOption(args, null, names);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Set --" + names[0] + "=<number>");
        }
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(names[0] + " must be an integer: " + raw, error);
        }
    }

    private static int optionalIntOption(ApplicationArguments args, int fallback, String... names) {
        String raw = optionalOption(args, null, names);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(names[0] + " must be an integer: " + raw, error);
        }
    }

    private static String optionalOption(ApplicationArguments args, String fallback, String... names) {
        for (String name : names) {
            List<String> values = args.getOptionValues(name);
            if (values != null && !values.isEmpty()) {
                if (values.size() != 1) {
                    throw new IllegalArgumentException("Set at most one --" + name + "=<value>");
                }
                return values.getFirst().strip();
            }
        }
        return fallback;
    }
}
