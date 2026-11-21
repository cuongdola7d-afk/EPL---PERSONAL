package com.premierhub.fpl;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "premierhub.manual-extra-stats.enabled", havingValue = "true")
public class ManualExtraStatsCommand implements ApplicationRunner {
    private final ManualExtraStatsImporter importer;

    public ManualExtraStatsCommand(ManualExtraStatsImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String file = option(args, "premierhub.manual-extra-stats.file", "file");
        if (file == null || file.isBlank()) {
            throw new IllegalArgumentException("Set exactly one --premierhub.manual-extra-stats.file=<csv-path>");
        }
        ManualExtraStatsImporter.Result result = importer.importFile(Path.of(file));
        System.out.printf("MANUAL_EXTRA_STATS season=2026 rows=%d inserted=%d%n",
                result.rows(), result.inserted());
    }

    private static String option(ApplicationArguments args, String... names) {
        for (String name : names) {
            List<String> values = args.getOptionValues(name);
            if (values != null && !values.isEmpty()) {
                if (values.size() != 1) {
                    throw new IllegalArgumentException("Set exactly one --" + name + "=<path>");
                }
                return values.getFirst().strip();
            }
        }
        return null;
    }
}
