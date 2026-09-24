package com.premierhub.sync;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "premierhub.sync.enabled", havingValue = "true")
public class SyncCommand implements ApplicationRunner {
    private final FootballSync sync;

    public SyncCommand(FootballSync sync) { this.sync = sync; }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int season = number(args, "premierhub.sync.season", 2024);
        int gameweek = number(args, "premierhub.sync.gameweek", 1);
        int budget = number(args, "premierhub.sync.budget", 70);
        boolean refreshPlayers = flag(args, "premierhub.sync.refresh-players");
        boolean refreshStats = flag(args, "premierhub.sync.refresh-stats");
        FootballSync.SyncResult result = sync.sync(season, gameweek, budget, refreshPlayers, refreshStats);
        System.out.printf("SYNC season=%d gameweek=%d requests=%d dailyRemaining=%s "
                        + "fixturesUpdated=%d fixtureStatisticsUpdated=%d playerPagesUpdated=%d "
                        + "complete=%s truncatedClubIds=%s%n",
                result.season(), result.gameweek(), result.requests(),
                result.dailyRemaining() == Integer.MAX_VALUE ? "unknown" : result.dailyRemaining(),
                result.fixturesUpdated(), result.fixtureStatisticsUpdated(),
                result.playerPagesUpdated(), result.complete(), result.truncatedClubIds());
    }

    private static int number(ApplicationArguments args, String name, int fallback) {
        return args.containsOption(name) ? Integer.parseInt(args.getOptionValues(name).getFirst()) : fallback;
    }

    private static boolean flag(ApplicationArguments args, String name) {
        return args.containsOption(name) && Boolean.parseBoolean(args.getOptionValues(name).getFirst());
    }
}
