package com.premierhub;

import com.premierhub.model.Club;
import com.premierhub.model.Player;
import com.premierhub.model.Position;
import com.premierhub.model.Standing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    @TempDir
    Path directory;

    @Test
    void formatsClubForConsole() {
        Club club = new Club(1, "Sample United", "Sample City");

        assertEquals("1 | Sample United | Sample City", App.formatClub(club));
    }

    @Test
    void formatsPlayerAndStandingForConsole() {
        Club club = new Club(1, "Sample United", "Sample City");
        Player player = new Player(1, "Sample Player", 1,
                Position.FORWARD, 5, 3);
        Standing standing = new Standing(club, 2, 1, 1, 0, 3, 1);

        assertEquals("Sample Player | goals: 5 | assists: 3", App.formatPlayer(player));
        String row = App.formatStanding(1, standing);
        assertEquals("1   Sample United            2   1   1   0   3   1  +2   4", row);
    }

    @Test
    void runsDemoFromCsvFiles() throws IOException {
        Files.writeString(directory.resolve("clubs.csv"),
                "id,name,city\n1,Alpha,London\n2,Beta,Manchester\n",
                StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("players.csv"),
                "id,name,clubId,position,goals,assists\n"
                        + "1,Sample Player,1,FORWARD,3,2\n",
                StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("matches.csv"),
                "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals\n"
                        + "1,1,2,1,2025-08-16,FINISHED,2,0\n",
                StandardCharsets.UTF_8);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            App.runDemo(directory, output);
        }
        String result = bytes.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("1 | Alpha | London"));
        assertTrue(result.contains("Imported 2 clubs"));
        assertTrue(result.contains("Sample Player | goals: 3 | assists: 2"));
        assertTrue(result.contains("=== League table ==="));
        assertTrue(result.indexOf("Alpha") < result.indexOf("Beta"));
    }
}
