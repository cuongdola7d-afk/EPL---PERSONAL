package com.premierhub.clubs;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "premierhub.club-information.enabled", havingValue = "true")
public class ClubInformationCommand implements ApplicationRunner {
    private final ClubInformationImporter importer;
    public ClubInformationCommand(ClubInformationImporter importer) { this.importer = importer; }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var files = args.getOptionValues("premierhub.club-information.file");
        if (files == null || files.size() != 1 || files.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set one --premierhub.club-information.file=<CSV>");
        }
        var result = importer.importFile(Path.of(files.getFirst()));
        System.out.printf("CLUB_INFORMATION season=2026 rows=%d inserted=%d%n", result.rows(), result.inserted());
    }
}
