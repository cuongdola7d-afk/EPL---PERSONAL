package com.premierhub.sync;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "premierhub.football-data.enabled", havingValue = "true")
public class FootballDataCommand implements ApplicationRunner {
    private final FootballDataSync sync;
    private final FootballDataClient client;

    public FootballDataCommand(FootballDataSync sync, FootballDataClient client) {
        this.sync = sync;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.containsOption("premierhub.football-data.season")
                && !args.getOptionValues("premierhub.football-data.season").equals(java.util.List.of("2026"))) {
            throw new IllegalArgumentException("football-data.org import currently supports only season 2026");
        }
        boolean cached = args.containsOption("premierhub.football-data.input-dir");
        FootballDataSync.Result result = cached
                ? sync.importBatch(client.readCache(Path.of(args.getOptionValues("premierhub.football-data.input-dir").getFirst())))
                : sync.sync();
        System.out.printf("FOOTBALL_DATA season=2026 requests=%d teams=%d fixtures=%d standings=%d "
                        + "teamsInserted=%d fixturesInserted=%d%n", cached ? 0 : 3,
                result.teams(), result.fixtures(), result.standings(), result.teamsInserted(), result.fixturesInserted());
    }
}
