package com.premierhub.manualstats;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "premierhub.manual-match-stats.enabled", havingValue = "true")
public class ManualMatchStatsCommand implements ApplicationRunner {
    private final ManualMatchStatsImporter importer;
    private final ManualMatchStatsPatchImporter patchImporter;

    public ManualMatchStatsCommand(ManualMatchStatsImporter importer,
                                   ManualMatchStatsPatchImporter patchImporter) {
        this.importer = importer;
        this.patchImporter = patchImporter;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> files = args.getOptionValues("premierhub.manual-match-stats.file");
        if (files == null || files.size() != 1 || files.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set exactly one --premierhub.manual-match-stats.file=<csv-path>");
        }
        List<String> patchFixtures = args.getOptionValues("premierhub.manual-match-stats.fill-missing-fixture");
        if (patchFixtures != null) {
            if (patchFixtures.size() != 1) {
                throw new IllegalArgumentException("Set exactly one --premierhub.manual-match-stats.fill-missing-fixture=<fixture_id>");
            }
            int fixtureId;
            try {
                fixtureId = Integer.parseInt(patchFixtures.getFirst());
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("fill-missing-fixture must be a fixture ID", error);
            }
            ManualMatchStatsPatchImporter.Result result = patchImporter.fillMissing(Path.of(files.getFirst()), fixtureId);
            System.out.printf("MANUAL_MATCH_STATS_PATCH fixture=%d rows=%d updated=%d filled_cells=%d%n",
                    fixtureId, result.rows(), result.updatedRows(), result.filledCells());
        } else {
            ManualMatchStatsImporter.Result result = importer.importFile(Path.of(files.getFirst()));
            System.out.printf("MANUAL_MATCH_STATS season=2026 rows=%d inserted=%d%n",
                    result.rows(), result.inserted());
        }
    }
}
