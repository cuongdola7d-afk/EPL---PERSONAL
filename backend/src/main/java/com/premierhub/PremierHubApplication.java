package com.premierhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PremierHubApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PremierHubApplication.class);
        if (isNonWebCommand(args)) {
            application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        }
        var context = application.run(args);
        if (isExitCommand(args)) {
            System.exit(SpringApplication.exit(context));
        }
    }

    static boolean isNonWebCommand(String[] args) {
        var list = java.util.Arrays.asList(args);
        return list.contains("--premierhub.football-data.enabled=true")
                || list.contains("--premierhub.manual-roster.enabled=true")
                || list.contains("--premierhub.manual-match-stats.enabled=true")
                || list.contains("--premierhub.manual-season-stats.enabled=true")
                || list.contains("--premierhub.club-information.enabled=true")
                || list.contains("--premierhub.match-lineups.enabled=true")
                || list.contains("--premierhub.fantasy-evidence.enabled=true")
                || list.contains("--premierhub.fpl-collect.enabled=true")
                || list.contains("--premierhub.manual-extra-stats.enabled=true");
    }

    static boolean isExitCommand(String[] args) {
        for (String arg : args) {
            if (arg.equals("--premierhub.sync.enabled=true")
                    || arg.equals("--premierhub.football-data.enabled=true")
                    || arg.equals("--premierhub.manual-roster.enabled=true")
                    || arg.equals("--premierhub.manual-match-stats.enabled=true")
                    || arg.equals("--premierhub.manual-season-stats.enabled=true")
                    || arg.equals("--premierhub.fixture-evidence.enabled=true")
                    || arg.equals("--premierhub.club-information.enabled=true")
                    || arg.equals("--premierhub.match-lineups.enabled=true")
                    || arg.equals("--premierhub.fantasy-evidence.enabled=true")
                    || arg.equals("--premierhub.fpl-collect.enabled=true")
                    || arg.equals("--premierhub.manual-extra-stats.enabled=true")
                    || arg.startsWith("--premierhub.snapshot.mode=")) {
                return true;
            }
        }
        return false;
    }
}
