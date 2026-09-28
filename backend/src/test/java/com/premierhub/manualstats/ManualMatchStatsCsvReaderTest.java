package com.premierhub.manualstats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualMatchStatsCsvReaderTest {
    private final ManualMatchStatsCsvReader reader = new ManualMatchStatsCsvReader();
    @TempDir Path temp;

    private Path file(String... rows) throws Exception {
        Path path = Files.createTempFile(temp, "stats-", ".csv");
        Files.writeString(path, ManualMatchStatsCsvReader.HEADER + "\n"
                + String.join("\n", rows) + "\n", StandardCharsets.UTF_8);
        return path;
    }

    private String row(String status, String rating, String minutes) {
        return "2026,901,2000000001," + status + "," + rating + "," + minutes
                + ",,,,";
    }

    @Test
    void duplicateKeysAndUnsupportedSeasonAreRejected() throws Exception {
        String played = row("PLAYED", "7.25", "90");
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(played, played))).getMessage().contains("Duplicate"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(played.replaceFirst("2026", "2024"))))
                .getMessage().contains("Only season 2026"));
    }

    @Test
    void didNotPlayNeedsConfirmedZeroMinutesAndNoRatingOrPositiveEvents() throws Exception {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(row("", "", ""))))
                .getMessage().contains("status must be PLAYED or DID_NOT_PLAY"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(row("DID_NOT_PLAY", "7.2", "0"))))
                .getMessage().contains("DID_NOT_PLAY requires"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(row("DID_NOT_PLAY", "", ""))))
                .getMessage().contains("DID_NOT_PLAY requires"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(row("PLAYED", "", "0"))))
                .getMessage().contains("PLAYED minutes"));
        assertEquals(1, reader.read(file(row("PLAYED", "", ""))).size());
    }

    @Test
    void rejectsInvalidRatingAndExtraColumnsWithoutInventingMissingCounts() throws Exception {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(row("PLAYED", "10.1", "90"))))
                .getMessage().contains("rating"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> reader.read(file(row("PLAYED", "7.2", "90") + ",old-url,old-timestamp")))
                .getMessage().contains("10 columns"));
        var parsed = reader.read(file(row("PLAYED", "7.2", "90"))).getFirst();
        assertEquals(null, parsed.goals());
        assertEquals(0, parsed.rating().compareTo(new java.math.BigDecimal("7.20")));
    }
}
