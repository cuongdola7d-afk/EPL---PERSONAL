package com.premierhub.positions;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "premierhub.player-positions.enabled", havingValue = "true")
public class PlayerPositionCommand implements ApplicationRunner {
    private final PlayerPositionImporter importer;

    public PlayerPositionCommand(PlayerPositionImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> files = args.getOptionValues("premierhub.player-positions.file");
        if (files == null || files.size() != 1 || files.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set exactly one --premierhub.player-positions.file=<path>");
        }
        PlayerPositionImporter.Result result = importer.importFile(Path.of(files.getFirst()));
        System.out.printf("PLAYER_POSITIONS season=2026 rows=%d inserted=%d updated=%d%n",
                result.rows(), result.inserted(), result.updated());
    }
}
