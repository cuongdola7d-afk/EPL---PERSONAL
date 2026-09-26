package com.premierhub.roster;

import com.premierhub.service.FootballQueries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:manual-roster-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class ManualRosterImportTest {
    private static final Path EXAMPLE = Path.of("src/test/resources/manual-players-2026-example.csv");

    @Autowired ManualRosterImporter importer;
    @Autowired FootballQueries queries;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @TempDir Path temp;

    private void seedClubs() {
        jdbc.update("INSERT INTO seasons VALUES (39, 2024), (39, 2026)");
        jdbc.update("INSERT INTO clubs VALUES (42, 'Legacy Club', NULL), "
                + "(1000000057, 'Arsenal FC', NULL), (1000000061, 'Chelsea FC', NULL)");
        jdbc.update("INSERT INTO season_clubs VALUES (39, 2024, 42), "
                + "(39, 2026, 1000000057), (39, 2026, 1000000061)");
        jdbc.update("INSERT INTO players VALUES (101, 'Legacy Player')");
        jdbc.update("INSERT INTO player_season_stats VALUES (39, 2024, 101, 42, 'FORWARD', 1, 90, 1, 0)");
    }

    @Test
    void sampleFileImportsOnceApiPreservesNullStatsAndTransferHistory() throws Exception {
        seedClubs();
        var first = importer.importFile(EXAMPLE);
        assertEquals(3, first.rows());
        assertEquals(2, first.playersInserted());
        assertEquals(3, first.membershipsInserted());
        assertTrue(queries.players(2026, null, null).stream()
                .allMatch(player -> player.goals() == null && player.assists() == null));
        jdbc.update("UPDATE player_season_stats SET goals=3, assists=1 "
                + "WHERE season_year=2026 AND player_id=2000000001 AND club_id=1000000057");
        var again = importer.importFile(EXAMPLE);
        assertEquals(0, again.playersInserted());
        assertEquals(0, again.membershipsInserted());
        assertEquals(0, again.intervalsInserted());
        assertEquals(0, again.intervalsUpdated());
        assertEquals(3, queries.player(2000000001, 2026).orElseThrow().goals());
        assertEquals(2, queries.players(2026, null, null).size());
        assertEquals(1, queries.players(2026, "Arsenal FC", null).size());
        assertEquals(1, queries.players(2026, null, "defender").size());
        assertEquals(2, queries.players(2026, null, null, LocalDate.of(2026, 9, 1)).size());
        assertEquals("Arsenal FC", queries.player(2000000002, 2026, LocalDate.of(2026, 9, 14))
                .orElseThrow().club());
        assertEquals("Chelsea FC", queries.player(2000000002, 2026, LocalDate.of(2026, 9, 15))
                .orElseThrow().club());
        assertEquals(1, queries.players(2024, null, null).size());
        assertEquals(1, queries.players(2024, null, null).getFirst().goals());
        assertEquals(0, queries.players(2024, null, null).getFirst().assists());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM players WHERE id>=2000000000", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM player_season_stats WHERE season_year=2026", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM manual_player_memberships WHERE season_year=2026", Integer.class));

        mvc.perform(get("/api/players").param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].goals").value(nullValue()))
                .andExpect(jsonPath("$[0].assists").value(nullValue()))
                .andExpect(jsonPath("$[0].id").value(2000000002))
                .andExpect(jsonPath("$[0].club").value("Chelsea FC"));
        mvc.perform(get("/api/players").param("season", "2026")
                        .param("club", "Chelsea FC").param("position", "DEFENDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/players").param("season", "2026").param("asOf", "2026-09-14")
                        .param("club", "Arsenal FC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/players").param("season", "2026").param("asOf", "2026-09-15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2000000002))
                .andExpect(jsonPath("$[0].club").value("Chelsea FC"));
        mvc.perform(get("/api/players").param("season", "2026").param("asOf", "2026-07-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/players").param("season", "2026").param("asOf", "2026-02-30"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/players/2000000002").param("season", "2026")
                        .param("asOf", "2026-09-15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.club").value("Chelsea FC"));
        mvc.perform(get("/api/players").param("season", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goals").value(1))
                .andExpect(jsonPath("$[0].assists").value(0));
    }

    @Test
    void unknownClubRollsBackEarlierRowsAndDoesNotGuessFromName() throws Exception {
        seedClubs();
        Path file = temp.resolve("unknown-club.csv");
        Files.writeString(file, "season,club_id,player_id,name,fantasy_position,start_date,end_date\n"
                + "2026,1000000057,2000000001,Example Keeper,GK,2026-08-01,\n"
                + "2026,999999999,2000000003,Example Forward,FWD,2026-08-01,\n", StandardCharsets.UTF_8);
        var error = assertThrows(IllegalArgumentException.class, () -> importer.importFile(file));
        assertTrue(error.getMessage().contains("CSV line 3: club_id is not in Premier League season 2026"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM players WHERE id>=2000000000", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM player_season_stats WHERE season_year=2026", Integer.class));
    }

    @Test
    void existingIdWithAnotherNameCannotBeSilentlyReused() throws Exception {
        seedClubs();
        jdbc.update("INSERT INTO players VALUES (2000000001, 'Different Person')");
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(EXAMPLE));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM player_season_stats WHERE season_year=2026", Integer.class));
    }

    @Test
    void overlappingTransferIsRejectedWithoutChangingSavedMemberships() throws Exception {
        seedClubs();
        importer.importFile(EXAMPLE);
        Path file = temp.resolve("overlap.csv");
        Files.writeString(file, "season,club_id,player_id,name,fantasy_position,start_date,end_date\n"
                + "2026,1000000057,2000000002,Example Defender,DEF,2026-08-01,2026-10-01\n"
                + "2026,1000000061,2000000002,Example Defender,DEF,2026-09-15,\n",
                StandardCharsets.UTF_8);
        var error = assertThrows(IllegalArgumentException.class, () -> importer.importFile(file));
        assertTrue(error.getMessage().contains("Overlapping club memberships"));
        assertEquals(LocalDate.of(2026, 9, 15), jdbc.queryForObject("""
                SELECT end_date FROM manual_player_memberships WHERE player_id=2000000002
                AND club_id=1000000057
                """, java.sql.Date.class).toLocalDate());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM manual_player_memberships", Integer.class));
    }

    @Test
    void transferCanCloseExistingOpenPeriodAndReimportWithoutDuplicates() throws Exception {
        seedClubs();
        Path first = temp.resolve("first.csv");
        Files.writeString(first, "season,club_id,player_id,name,fantasy_position,start_date,end_date\n"
                + "2026,1000000057,2000000002,Example Defender,DEF,2026-08-01,\n", StandardCharsets.UTF_8);
        importer.importFile(first);
        Path transfer = temp.resolve("transfer.csv");
        Files.writeString(transfer, "season,club_id,player_id,name,fantasy_position,start_date,end_date\n"
                + "2026,1000000057,2000000002,Example Defender,DEF,2026-08-01,2026-09-15\n"
                + "2026,1000000061,2000000002,Example Defender,DEF,2026-09-15,\n", StandardCharsets.UTF_8);
        var changed = importer.importFile(transfer);
        assertEquals(1, changed.intervalsInserted());
        assertEquals(1, changed.intervalsUpdated());
        assertEquals("Arsenal FC", queries.player(2000000002, 2026, LocalDate.of(2026, 9, 14))
                .orElseThrow().club());
        assertEquals("Chelsea FC", queries.player(2000000002, 2026, LocalDate.of(2026, 9, 15))
                .orElseThrow().club());
        var again = importer.importFile(transfer);
        assertEquals(0, again.intervalsInserted());
        assertEquals(0, again.intervalsUpdated());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM manual_player_memberships", Integer.class));
    }
}
