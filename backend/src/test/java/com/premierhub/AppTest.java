package com.premierhub;

import com.premierhub.model.Club;
import com.premierhub.model.Player;
import com.premierhub.model.Position;
import com.premierhub.model.Standing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
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
}
