package com.premierhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PremierHubApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PremierHubApplication.class);
        if (java.util.Arrays.asList(args).contains("--premierhub.football-data.enabled=true")
                || java.util.Arrays.asList(args).contains("--premierhub.manual-roster.enabled=true")
                || java.util.Arrays.asList(args).contains("--premierhub.manual-match-stats.enabled=true")
                || java.util.Arrays.asList(args).contains("--premierhub.manual-season-stats.enabled=true")
                || java.util.Arrays.asList(args).contains("--premierhub.club-information.enabled=true")) {
            application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        }
        var context = application.run(args);
        for (String arg : args) {
            if (arg.equals("--premierhub.sync.enabled=true")
                    || arg.equals("--premierhub.football-data.enabled=true")
                    || arg.equals("--premierhub.manual-roster.enabled=true")
                    || arg.equals("--premierhub.manual-match-stats.enabled=true")
                    || arg.equals("--premierhub.manual-season-stats.enabled=true")
                    || arg.equals("--premierhub.fixture-evidence.enabled=true")
                    || arg.equals("--premierhub.club-information.enabled=true")
                    || arg.startsWith("--premierhub.snapshot.mode=")) {
                System.exit(SpringApplication.exit(context));
            }
        }
    }
}
