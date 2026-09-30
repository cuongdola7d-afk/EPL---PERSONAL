package com.premierhub.roster;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualRosterCsvReaderTest {
    private static final String HEADER = "season,club_id,player_id,name,fantasy_position,start_date,end_date\n";
    private final ManualRosterCsvReader reader = new ManualRosterCsvReader();

    @Test
    void acceptsStableIdAcrossClubsForATransfer() throws Exception {
        var rows = reader.read(new StringReader(HEADER
                + "2026,1000000057,2000000001,Example Player,MID,2026-08-01,2026-09-15\n"
                + "2026,1000000061,2000000001,Example Player,MID,2026-09-15,\n"));
        assertEquals(2, rows.size());
        assertEquals("MIDFIELDER", rows.getFirst().position());
        assertEquals(rows.getFirst().playerId(), rows.getLast().playerId());
        assertEquals(LocalDate.of(2026, 9, 15), rows.getFirst().endDate());
        assertEquals(null, rows.getLast().endDate());
    }

    @Test
    void rejectsDuplicateMembershipAndConflictingIdentity() {
        var duplicate = assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(HEADER
                + "2026,1000000057,2000000001,Example Player,GK,2026-08-01,\n"
                + "2026,1000000057,2000000001,Example Player,GK,2026-08-01,\n")));
        assertTrue(duplicate.getMessage().contains("CSV line 3: Duplicate membership start"));

        var collision = assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(HEADER
                + "2026,1000000057,2000000001,Example Player,GK,2026-08-01,2026-09-15\n"
                + "2026,1000000061,2000000001,Different Person,GK,2026-09-15,\n")));
        assertTrue(collision.getMessage().contains("conflicting names"));
    }

    @Test
    void rejectsBadPositionIdSeasonAndUnsupportedCsvQuotes() {
        for (String row : new String[] {
                "2026,1000000057,2000000001,Example Player,WING,2026-08-01,",
                "2026,1000000057,0,Example Player,GK,2026-08-01,",
                "2026,1000000057,-1,Example Player,GK,2026-08-01,",
                "2024,1000000057,2000000001,Example Player,GK,2026-08-01,",
                "2026,1000000057,2000000001,\"Example Player\",GK,2026-08-01,",
                "2026,1000000057,2000000001,Example Player,GK,2026-02-30,",
                "2026,1000000057,2000000001,Example Player,GK,2026-09-15,2026-09-15",
                "2026,1000000057,2000000001,Example Player,GK,,"
        }) {
            assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(HEADER + row + "\n")));
        }
    }
}
