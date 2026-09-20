package com.premierhub.csv;

import com.premierhub.model.Player;
import com.premierhub.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCsvReaderTest {
    @TempDir
    Path directory;

    private final PlayerCsvReader reader = new PlayerCsvReader();

    @Test
    void readsValidPlayersAndTrimsValues() throws IOException {
        List<Player> players = reader.read(writeCsv(
                "id,name,clubId,position,goals,assists\n"
                        + "1, Sample Player ,2, forward ,5,3\n"));

        assertEquals(1, players.size());
        Player player = players.get(0);
        assertEquals("Sample Player", player.getName());
        assertEquals(2, player.getClubId());
        assertEquals(Position.FORWARD, player.getPosition());
        assertEquals(5, player.getGoals());
    }

    @Test
    void acceptsHeaderOnlyFile() throws IOException {
        assertTrue(reader.read(writeCsv(
                "id,name,clubId,position,goals,assists\n")).isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "x,Player,1,FORWARD,0,0|Player id",
            "1,,1,FORWARD,0,0|Player name",
            "1,Player,x,FORWARD,0,0|Club id",
            "1,Player,1,STRIKER,0,0|Unknown position",
            "1,Player,1,FORWARD,-1,0|must not be negative",
            "1,Player,1,FORWARD,0,x|Assists",
            "1,Player,1,FORWARD,0|exactly 6 columns"
    }, delimiter = '|')
    void rejectsInvalidRows(String row, String reason) throws IOException {
        Path path = writeCsv("id,name,clubId,position,goals,assists\n" + row + "\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));
        assertTrue(exception.getMessage().startsWith("CSV line 2:"));
        assertTrue(exception.getMessage().contains(reason));
    }

    @Test
    void rejectsDuplicatePlayerIds() throws IOException {
        Path path = writeCsv("id,name,clubId,position,goals,assists\n"
                + "1,One,1,FORWARD,0,0\n1,Two,2,DEFENDER,0,0\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));
        assertEquals("CSV line 3: Duplicate player id: 1", exception.getMessage());
    }

    @Test
    void rejectsWrongHeader() throws IOException {
        Path path = writeCsv("id,name,position\n");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));

        assertTrue(exception.getMessage().startsWith("CSV line 1:"));
    }

    private Path writeCsv(String content) throws IOException {
        return Files.writeString(directory.resolve("players.csv"), content, StandardCharsets.UTF_8);
    }
}
