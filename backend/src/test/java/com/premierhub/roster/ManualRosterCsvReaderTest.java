package com.premierhub.roster;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualRosterCsvReaderTest {
    private static final String HEADER = "season,club_id,player_id,name,fantasy_position\n";
    private final ManualRosterCsvReader reader = new ManualRosterCsvReader();

    @Test
    void acceptsStableIdAcrossClubsForATransfer() throws Exception {
        var rows = reader.read(new StringReader(HEADER
                + "2026,1000000057,2000000001,Example Player,MID\n"
                + "2026,1000000061,2000000001,Example Player,MID\n"));
        assertEquals(2, rows.size());
        assertEquals("MIDFIELDER", rows.getFirst().position());
        assertEquals(rows.getFirst().playerId(), rows.getLast().playerId());
    }

    @Test
    void rejectsDuplicateMembershipAndConflictingIdentity() {
        var duplicate = assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(HEADER
                + "2026,1000000057,2000000001,Example Player,GK\n"
                + "2026,1000000057,2000000001,Example Player,GK\n")));
        assertTrue(duplicate.getMessage().contains("CSV line 3: Duplicate player_id"));

        var collision = assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(HEADER
                + "2026,1000000057,2000000001,Example Player,GK\n"
                + "2026,1000000061,2000000001,Different Person,GK\n")));
        assertTrue(collision.getMessage().contains("conflicting names"));
    }

    @Test
    void rejectsBadPositionIdSeasonAndUnsupportedCsvQuotes() {
        for (String row : new String[] {
                "2026,1000000057,2000000001,Example Player,WING",
                "2026,1000000057,42,Example Player,GK",
                "2024,1000000057,2000000001,Example Player,GK",
                "2026,1000000057,2000000001,\"Example Player\",GK"
        }) {
            assertThrows(IllegalArgumentException.class, () -> reader.read(new StringReader(HEADER + row + "\n")));
        }
    }
}
