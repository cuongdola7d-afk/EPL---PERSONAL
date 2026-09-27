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

    public ManualMatchStatsCommand(ManualMatchStatsImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> files = args.getOptionValues("premierhub.manual-match-stats.file");
        if (files == null || files.size() != 1 || files.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set exactly one --premierhub.manual-match-stats.file=<csv-path>");
        }
        ManualMatchStatsImporter.Result result = importer.importFile(Path.of(files.getFirst()));
        System.out.printf("MANUAL_MATCH_STATS season=2026 rows=%d inserted=%d%n",
                result.rows(), result.inserted());
    }
}
