package com.premierhub.csv;

import com.premierhub.model.Match;
import com.premierhub.model.MatchStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchCsvReaderTest {
    @TempDir
    Path directory;

    private final MatchCsvReader reader = new MatchCsvReader();

    @Test
    void readsFinishedAndScheduledMatches() throws IOException {
        List<Match> matches = reader.read(writeCsv(
                "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals\n"
                        + "1,1,2,1,2025-08-16,finished,2,1\n"
                        + "2,2,3,2,2025-08-20,SCHEDULED,,\n"));

        assertEquals(2, matches.size());
        assertEquals(LocalDate.of(2025, 8, 16), matches.get(0).getMatchDate());
        assertEquals(MatchStatus.FINISHED, matches.get(0).getStatus());
        assertEquals(2, matches.get(0).getHomeGoals());
        assertEquals(MatchStatus.SCHEDULED, matches.get(1).getStatus());
        assertEquals(2, matches.get(1).getMatchweek());
        assertNull(matches.get(1).getHomeGoals());
    }

    @Test
    void acceptsHeaderOnlyFile() throws IOException {
        assertTrue(reader.read(writeCsv(
                "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals\n")).isEmpty());
    }

    @Test
    void readsFromClasspathStyleInputStream() throws IOException {
        String csv = "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals\n"
                + "1,1,2,3,2025-08-30,SCHEDULED,,\n";

        List<Match> matches = reader.read(new ByteArrayInputStream(
                csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, matches.get(0).getMatchweek());
        assertEquals(MatchStatus.SCHEDULED, matches.get(0).getStatus());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "x,1,2,1,2025-08-16,FINISHED,1,0|Match id",
            "1,x,2,1,2025-08-16,FINISHED,1,0|Home club id",
            "1,1,1,1,2025-08-16,FINISHED,1,0|against itself",
            "1,1,2,0,2025-08-16,FINISHED,1,0|Matchweek",
            "1,1,2,1,16-08-2025,FINISHED,1,0|yyyy-MM-dd",
            "1,1,2,1,2025-08-16,PLAYED,1,0|Unknown match status",
            "1,1,2,1,2025-08-16,FINISHED,,0|both scores",
            "1,1,2,1,2025-08-16,FINISHED,-1,0|must not be negative",
            "1,1,2,1,2025-08-16,SCHEDULED,0,0|must not have a score",
            "1,1,2,1,2025-08-16,FINISHED,1|exactly 8 columns"
    }, delimiter = '|')
    void rejectsInvalidRows(String row, String reason) throws IOException {
        Path path = writeCsv(
                "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals\n" + row + "\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));
        assertTrue(exception.getMessage().startsWith("CSV line 2:"));
        assertTrue(exception.getMessage().contains(reason));
    }

    @Test
    void rejectsDuplicateMatchIdsAndWrongHeader() throws IOException {
        Path duplicate = writeCsv(
                "id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals\n"
                        + "1,1,2,1,2025-08-16,FINISHED,1,0\n"
                        + "1,2,3,1,2025-08-17,FINISHED,0,0\n");
        assertThrows(IllegalArgumentException.class, () -> reader.read(duplicate));

        Path wrongHeader = writeCsv("id,date,status\n");
        assertThrows(IllegalArgumentException.class, () -> reader.read(wrongHeader));
    }

    private Path writeCsv(String content) throws IOException {
        return Files.writeString(directory.resolve("matches.csv"), content, StandardCharsets.UTF_8);
    }
}
