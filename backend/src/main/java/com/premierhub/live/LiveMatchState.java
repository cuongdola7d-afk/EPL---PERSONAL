package com.premierhub.live;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record LiveMatchState(
        int fixtureId,
        String status,
        int minute,
        Integer homeScore,
        Integer awayScore,
        List<GoalEvent> goals,
        List<PlayerEvent> assists,
        List<PlayerEvent> redCards,
        Instant updatedAt,
        String source
) {
    public static final String STATUS_PRE = "PRE";
    public static final String STATUS_LIVE = "LIVE";
    public static final String STATUS_FT = "FT";
    public static final String SOURCE_FPL = "FPL";

    public boolean sameContentAs(LiveMatchState other) {
        if (other == null) {
            return false;
        }
        return fixtureId == other.fixtureId
                && Objects.equals(status, other.status)
                && minute == other.minute
                && Objects.equals(homeScore, other.homeScore)
                && Objects.equals(awayScore, other.awayScore)
                && Objects.equals(goals, other.goals)
                && Objects.equals(assists, other.assists)
                && Objects.equals(redCards, other.redCards);
    }

    public record GoalEvent(
            Integer playerId,
            Integer fplElementId,
            String playerName,
            String side,
            boolean ownGoal,
            Integer approxMinute
    ) {
    }

    public record PlayerEvent(
            Integer playerId,
            Integer fplElementId,
            String playerName,
            String side,
            int count,
            Integer approxMinute
    ) {
    }
}
