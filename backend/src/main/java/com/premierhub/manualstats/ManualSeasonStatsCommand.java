package com.premierhub.manualstats;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "premierhub.manual-season-stats.enabled", havingValue = "true")
public class ManualSeasonStatsCommand implements ApplicationRunner {
    private final ManualSeasonStatsService service;

    public ManualSeasonStatsCommand(ManualSeasonStatsService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> values = args.getOptionValues("premierhub.manual-season-stats.through-gameweek");
        if (values == null || values.size() != 1) {
            throw new IllegalArgumentException("Set --premierhub.manual-season-stats.through-gameweek=<1..38>");
        }
        int gameweek;
        try {
            gameweek = Integer.parseInt(values.getFirst());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("through-gameweek must be a number", error);
        }
        ManualSeasonStatsService.Result result = service.rebuildThroughGameweek(gameweek);
        System.out.printf("MANUAL_SEASON_STATS season=2026 throughGW=%d fixtures=%d matchRows=%d "
                        + "seasonRows=%d updated=%d missingMinutes=%d missingGoals=%d missingAssists=%d%n",
                gameweek, result.fixtures(), result.matchRows(), result.seasonRows(),
                result.updatedRows(), result.missingMinutes(), result.missingGoals(),
                result.missingAssists());
    }
}
