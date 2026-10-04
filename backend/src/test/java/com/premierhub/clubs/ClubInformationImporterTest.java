package com.premierhub.clubs;

import com.premierhub.service.FootballQueries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ClubInformationImporterTest {
    @TempDir Path temp;
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private ClubInformationImporter importer;

    @BeforeEach
    void setup() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        importer = new ClubInformationImporter(jdbc, new TransactionTemplate(new DataSourceTransactionManager(database)));
        jdbc.update("INSERT INTO seasons VALUES (39,2026),(39,2024)");
        jdbc.update("INSERT INTO clubs VALUES (1,'One','City'),(2,'Two','City')");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,1),(39,2026,2),(39,2024,1)");
    }

    @AfterEach void close() { database.shutdown(); }

    private Path csv(String content) throws Exception {
        return Files.writeString(temp.resolve("clubs.csv"), ClubInformationCsvReader.HEADER + "\n" + content);
    }

    @Test
    void importsNullAndInterimIdempotentlyAndApiDoesNotLeakInto2024() throws Exception {
        Path file = csv("1,2026,Coach,INTERIM,Home,2026-10-04\n2,2026,,,,2026-10-04\n");
        assertEquals(2, importer.importFile(file).inserted());
        assertEquals(0, importer.importFile(file).inserted());
        var queries = new FootballQueries(jdbc, null, null);
        var detail = queries.club(1, 2026).orElseThrow();
        assertEquals("Coach", detail.managerName());
        assertEquals("INTERIM", detail.managerStatus());
        assertEquals("Home", detail.stadiumName());
        assertEquals(LocalDate.of(2026,10,4), detail.informationVerifiedOn());
        assertEquals(detail, queries.clubs(2026, "one").getFirst());
        assertNull(queries.club(2,2026).orElseThrow().managerName());
        assertNull(queries.club(1,2024).orElseThrow().managerName());
        assertNull(queries.club(1,2024).orElseThrow().stadiumName());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM club_season_information WHERE season_year=2024", Integer.class));
    }

    @Test
    void conflictNamesExactFieldAndRollsBackWholeBatch() throws Exception {
        importer.importFile(csv("1,2026,Coach,PERMANENT,Home,2026-10-04\n"));
        var error = assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(
                "2,2026,Other,PERMANENT,Other ground,2026-10-04\n1,2026,Changed,PERMANENT,Home,2026-10-04\n")));
        assertTrue(error.getMessage().contains("club_id=1: conflict manager_name: saved=Coach, incoming=Changed"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM club_season_information", Integer.class));
    }

    @Test
    void unknownClubCannotLeavePartialImports() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(
                "1,2026,Coach,PERMANENT,Home,2026-10-04\n99,2026,,,,2026-10-04\n")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM club_season_information", Integer.class));
    }

    @Test
    void readerRejectsDuplicateWrongSeasonDateAndInconsistentStatus() {
        var reader = new ClubInformationCsvReader();
        for (String content : new String[]{
                "1,2026,,,,2026-10-04\n1,2026,,,,2026-10-04\n",
                "1,2024,,,,2026-10-04\n", "1,2026,,,,2026-02-30\n",
                "1,2026,Coach,,Home,2026-10-04\n", "1,2026,,INTERIM,Home,2026-10-04\n"}) {
            assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(ClubInformationCsvReader.HEADER + "\n" + content)));
        }
    }
}
