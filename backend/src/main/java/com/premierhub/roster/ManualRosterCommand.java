package com.premierhub.roster;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "premierhub.manual-roster.enabled", havingValue = "true")
public class ManualRosterCommand implements ApplicationRunner {
    private final ManualRosterImporter importer;

    public ManualRosterCommand(ManualRosterImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> files = args.getOptionValues("premierhub.manual-roster.file");
        if (files == null || files.size() != 1 || files.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set exactly one --premierhub.manual-roster.file=<csv-path>");
        }
        ManualRosterImporter.Result result = importer.importFile(Path.of(files.getFirst()));
        System.out.printf("MANUAL_ROSTER season=2026 rows=%d playersInserted=%d membershipsInserted=%d "
                        + "intervalsInserted=%d intervalsUpdated=%d%n",
                result.rows(), result.playersInserted(), result.membershipsInserted(),
                result.intervalsInserted(), result.intervalsUpdated());
    }
}
