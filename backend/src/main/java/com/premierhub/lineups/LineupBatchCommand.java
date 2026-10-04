package com.premierhub.lineups;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name="premierhub.match-lineups.enabled", havingValue="true")
public class LineupBatchCommand implements ApplicationRunner {
    private final LineupBatchImporter importer;
    public LineupBatchCommand(LineupBatchImporter importer) { this.importer=importer; }
    @Override public void run(ApplicationArguments args) throws Exception {
        var directories=args.getOptionValues("premierhub.match-lineups.directory");
        if (directories==null || directories.size()!=1 || directories.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set one --premierhub.match-lineups.directory=<directory>");
        }
        System.out.println("MATCH_LINEUPS season=2026 " + importer.importDirectory(Path.of(directories.getFirst())));
    }
}
