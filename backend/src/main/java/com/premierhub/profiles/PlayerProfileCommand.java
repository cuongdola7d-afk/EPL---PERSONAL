package com.premierhub.profiles;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@ConditionalOnProperty(name = "premierhub.player-profiles.enabled", havingValue = "true")
public class PlayerProfileCommand implements ApplicationRunner {
    private final PlayerProfileImporter importer;

    public PlayerProfileCommand(PlayerProfileImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String file = one(args, "premierhub.player-profiles.file");
        String date = one(args, "premierhub.player-profiles.as-of");
        LocalDate asOf;
        try {
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("format", date, 0);
            asOf = LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("--premierhub.player-profiles.as-of must be YYYY-MM-DD", exception);
        }
        PlayerProfileImporter.Result result = importer.importFile(Path.of(file), asOf);
        System.out.printf("PLAYER_PROFILES season=2026 asOf=%s rows=%d inserted=%d%n",
                asOf, result.rows(), result.inserted());
    }

    private static String one(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.size() != 1 || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("Set exactly one --" + name + "=<value>");
        }
        return values.getFirst();
    }
}
