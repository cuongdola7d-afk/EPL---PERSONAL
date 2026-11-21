package com.premierhub.fpl;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FplJsonParserTest {
    private static final Path FIXTURES_DIR = Path.of("src/test/resources/fpl");
    private final FplJsonParser parser = new FplJsonParser(new ObjectMapper());

    @Test
    void parsesAllFieldsAndConvertsDecimalStringsAccurately() throws Exception {
        String bootJson = Files.readString(FIXTURES_DIR.resolve("bootstrap-static.json"), StandardCharsets.UTF_8);
        String fixturesJson = Files.readString(FIXTURES_DIR.resolve("fixtures-gw11.json"), StandardCharsets.UTF_8);
        String liveJson = Files.readString(FIXTURES_DIR.resolve("event-gw11-live.json"), StandardCharsets.UTF_8);

        FplModels.Bootstrap boot = parser.parseBootstrap(bootJson);
        assertEquals(3, boot.teams().size());
        assertEquals(4, boot.elements().size());
        assertEquals("Alex Alpha", boot.elements().getFirst().fullName());

        List<FplModels.FplFixture> fixtures = parser.parseFixtures(fixturesJson, 11);
        assertEquals(1, fixtures.size());
        assertTrue(fixtures.getFirst().finished());
        assertEquals(2, fixtures.getFirst().teamHScore());

        List<FplModels.LiveElement> live = parser.parseEventLive(liveJson);
        assertEquals(3, live.size());
        FplModels.PlayerStats s = live.getFirst().stats();
        assertEquals(90, s.minutes());
        assertEquals(1, s.goalsScored());
        assertEquals(1, s.assists());
        assertEquals(1, s.yellowCards());
        assertEquals(0, s.redCards());
        assertEquals(3, s.bonus());
        assertEquals(42, s.bps());
        assertEquals(2, s.clearancesBlocksInterceptions());
        assertEquals(5, s.recoveries());
        assertEquals(3, s.tackles());
        assertEquals(5, s.defensiveContribution());
        assertEquals(1, s.starts());
        assertEquals(new BigDecimal("0.74"), s.expectedGoals());
        assertEquals(new BigDecimal("0.36"), s.expectedAssists());
        assertEquals(new BigDecimal("1.10"), s.expectedGoalInvolvements());
        assertEquals(new BigDecimal("0.92"), s.expectedGoalsConceded());
        assertEquals(new BigDecimal("48.6"), s.influence());
        assertEquals(new BigDecimal("31.2"), s.creativity());
        assertEquals(new BigDecimal("55.0"), s.threat());
        assertEquals(new BigDecimal("13.5"), s.ictIndex());
    }

    @Test
    void rejectsMissingRequiredFieldsOrWrongTypesWithoutDefaultingToZero() throws Exception {
        String liveJson = Files.readString(FIXTURES_DIR.resolve("event-gw11-live.json"), StandardCharsets.UTF_8);

        String missingXg = liveJson.replace("\"expected_goals\": \"0.74\",", "");
        IllegalArgumentException missingErr = assertThrows(IllegalArgumentException.class,
                () -> parser.parseEventLive(missingXg));
        assertTrue(missingErr.getMessage().contains("expected_goals"));

        String numericXg = liveJson.replace("\"expected_goals\": \"0.74\"", "\"expected_goals\": 0.74");
        IllegalArgumentException typeErr = assertThrows(IllegalArgumentException.class,
                () -> parser.parseEventLive(numericXg));
        assertTrue(typeErr.getMessage().contains("expected_goals"));

        String stringMinutes = liveJson.replace("\"minutes\": 90", "\"minutes\": \"90\"");
        IllegalArgumentException intErr = assertThrows(IllegalArgumentException.class,
                () -> parser.parseEventLive(stringMinutes));
        assertTrue(intErr.getMessage().contains("minutes"));
    }
}
