package com.premierhub.csv;

import com.premierhub.model.Club;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubCsvReaderTest {
    @TempDir
    Path directory;

    private final ClubCsvReader reader = new ClubCsvReader();

    @Test
    void readsUtf8ClubsInFileOrderAndTrimsFields() throws IOException {
        List<Club> clubs = reader.read(writeCsv(
                "id,name,city\n2,  Sample United  , Hà Nội \n1,Sample City,London\n"));

        assertEquals(2, clubs.size());
        assertEquals(2, clubs.get(0).getId());
        assertEquals("Sample United", clubs.get(0).getName());
        assertEquals("Hà Nội", clubs.get(0).getCity());
        assertEquals(1, clubs.get(1).getId());
        assertEquals("Sample City", clubs.get(1).getName());
        assertEquals("London", clubs.get(1).getCity());
    }

    @Test
    void acceptsBomCrLfBlankLinesAndHeaderWhitespace() throws IOException {
        List<Club> clubs = reader.read(writeCsv(
                "\uFEFF id , name , city \r\n\r\n  \r\n 1 ,United,London\r\n"));

        assertEquals(1, clubs.size());
        assertEquals(1, clubs.get(0).getId());
    }

    @Test
    void acceptsHeaderOnlyFile() throws IOException {
        assertTrue(reader.read(writeCsv("id,name,city\n\n")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\n", "name,id,city\n", "ID,name,city\n", "id,name\n"})
    void rejectsMissingOrIncorrectHeader(String content) throws IOException {
        Path path = writeCsv(content);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));

        assertTrue(exception.getMessage().startsWith("CSV line 1:"));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abc,United,London|32-bit integer",
            "2147483648,United,London|32-bit integer",
            ",United,London|32-bit integer",
            "0,United,London|positive",
            "-1,United,London|positive",
            "1, ,London|Club name",
            "1,United,|Club city",
            "1,United|exactly 3 columns",
            "1,United,London,Extra|exactly 3 columns",
            "1,\"United\",London|Quoted fields",
            "1,\"United, FC\",London|Quoted fields"
    }, delimiter = '|')
    void rejectsInvalidRowsWithPhysicalLineNumber(String row, String reason) throws IOException {
        Path path = writeCsv("id,name,city\n2,Valid,City\n\n" + row + "\n");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));

        assertTrue(exception.getMessage().startsWith("CSV line 4:"));
        assertTrue(exception.getMessage().contains(reason));
    }

    @Test
    void rejectsDuplicateIds() throws IOException {
        Path path = writeCsv("id,name,city\n1,United,London\n 1 ,City,Manchester\n");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> reader.read(path));

        assertEquals("CSV line 3: Duplicate club id: 1", exception.getMessage());
    }

    @Test
    void propagatesMissingFileError() {
        assertThrows(IOException.class, () -> reader.read(directory.resolve("missing.csv")));
    }

    @Test
    void keepsSeparateImportsIndependent() throws IOException {
        Path path = writeCsv("id,name,city\n1,United,London\n");

        assertEquals(1, reader.read(path).size());
        assertEquals(1, reader.read(path).size());
    }

    private Path writeCsv(String content) throws IOException {
        return Files.writeString(directory.resolve("clubs.csv"), content, StandardCharsets.UTF_8);
    }
}
