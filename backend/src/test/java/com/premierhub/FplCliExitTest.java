package com.premierhub;

import com.premierhub.fpl.FplMappingReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FplCliExitTest {
    @TempDir Path temp;

    @Test
    void fplFlagsAreRegisteredInBothNonWebAndExitLists() {
        String[] collectArgs = {"--premierhub.fpl-collect.enabled=true", "--gameweek=11", "--season=2026"};
        assertTrue(PremierHubApplication.isNonWebCommand(collectArgs));
        assertTrue(PremierHubApplication.isExitCommand(collectArgs));

        String[] extraArgs = {"--premierhub.manual-extra-stats.enabled=true", "--file=x.csv"};
        assertTrue(PremierHubApplication.isNonWebCommand(extraArgs));
        assertTrue(PremierHubApplication.isExitCommand(extraArgs));

        String[] webArgs = {"--server.port=8080"};
        assertFalse(PremierHubApplication.isNonWebCommand(webArgs));
        assertFalse(PremierHubApplication.isExitCommand(webArgs));
    }

    @Test
    void fplCollectCliRunsWithoutWebServerAndExitsCleanly() throws Exception {
        String dbUrl = "jdbc:h2:mem:fpl-cli-exit-test;MODE=MySQL;DB_CLOSE_DELAY=-1";

        // Seed minimal DB in a quick non-web context first
        SpringApplication seedApp = new SpringApplication(PremierHubApplication.class);
        seedApp.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext ctx = seedApp.run("--spring.datasource.url=" + dbUrl)) {
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO seasons VALUES (39, 2026)");
            jdbc.update("INSERT INTO clubs VALUES (1000000057, 'Arsenal FC', NULL), (1000000061, 'Chelsea FC', NULL)");
            jdbc.update("INSERT INTO season_clubs VALUES (39, 2026, 1000000057), (39, 2026, 1000000061)");
            jdbc.update("INSERT INTO players VALUES (2000001001, 'Alex Alpha'), (2000001002, 'Ben Beta')");
            jdbc.update("""
                    INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                      gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                    VALUES (1000560650, 39, 2026, 1000000057, 1000000061, 11, DATE '2026-11-21',
                            'FINISHED', 'FINISHED', 2, 1, 'hash11', CURRENT_TIMESTAMP)
                    """);
        }

        Path mapDir = temp.resolve("maps");
        Files.createDirectories(mapDir);
        Files.writeString(mapDir.resolve("team-map.csv"),
                FplMappingReader.TEAM_HEADER + "\n"
                + "1,1000000057,Arsenal,Arsenal FC\n"
                + "6,1000000061,Chelsea,Chelsea FC\n", StandardCharsets.UTF_8);
        Files.writeString(mapDir.resolve("player-map.csv"),
                FplMappingReader.PLAYER_HEADER + "\n"
                + "101,2000001001,Alex Alpha,Alex Alpha,test,2026-11-21\n"
                + "102,2000001002,Ben Beta,Ben Beta,test,2026-11-21\n", StandardCharsets.UTF_8);

        Path outDir = temp.resolve("cli-out");
        String[] args = {
                "--spring.datasource.url=" + dbUrl,
                "--spring.sql.init.mode=never",
                "--premierhub.fpl-collect.enabled=true",
                "--gameweek=11",
                "--season=2026",
                "--mapping-dir=" + mapDir,
                "--output-dir=" + outDir,
                "--input-dir=" + Path.of("src/test/resources/fpl").toAbsolutePath()
        };
        assertTrue(PremierHubApplication.isNonWebCommand(args));
        assertTrue(PremierHubApplication.isExitCommand(args));

        SpringApplication cliApp = new SpringApplication(PremierHubApplication.class);
        cliApp.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext cliCtx = cliApp.run(args);
        assertEquals(0, SpringApplication.exit(cliCtx));
        assertTrue(Files.exists(outDir.resolve("fpl-match-stats-2026-GW11.csv")));
        assertTrue(Files.exists(outDir.resolve("fpl-extra-stats-2026-GW11.csv")));
    }
}
