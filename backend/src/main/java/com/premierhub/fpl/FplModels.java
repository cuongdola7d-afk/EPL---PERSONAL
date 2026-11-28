package com.premierhub.fpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class FplModels {
    private FplModels() {
    }

    public record Team(int id, String name, String shortName) {
    }

    public record Element(int id, int code, String webName, String firstName, String secondName, int teamId) {
        public String fullName() {
            return (firstName + " " + secondName).strip();
        }
    }

    public record Event(int id, String name, boolean finished) {
    }

    public record Bootstrap(List<Team> teams, List<Element> elements, List<Event> events) {
    }

    public record FplFixture(
            int id,
            Integer event,
            Instant kickoffTime,
            int teamH,
            int teamA,
            Integer teamHScore,
            Integer teamAScore,
            boolean started,
            boolean finished,
            boolean finishedProvisional
    ) {
        public FplFixture(
                int id,
                Integer event,
                Instant kickoffTime,
                int teamH,
                int teamA,
                Integer teamHScore,
                Integer teamAScore,
                boolean finished,
                boolean finishedProvisional
        ) {
            this(id, event, kickoffTime, teamH, teamA, teamHScore, teamAScore,
                    finished || finishedProvisional, finished, finishedProvisional);
        }

        public LocalDate kickoffDateUtc() {
            return kickoffTime.atOffset(ZoneOffset.UTC).toLocalDate();
        }
    }

    public record PlayerStats(
            int minutes,
            int goalsScored,
            int assists,
            int cleanSheets,
            int goalsConceded,
            int ownGoals,
            int penaltiesSaved,
            int penaltiesMissed,
            int yellowCards,
            int redCards,
            int saves,
            int bonus,
            int bps,
            BigDecimal influence,
            BigDecimal creativity,
            BigDecimal threat,
            BigDecimal ictIndex,
            int clearancesBlocksInterceptions,
            int recoveries,
            int tackles,
            int defensiveContribution,
            int starts,
            BigDecimal expectedGoals,
            BigDecimal expectedAssists,
            BigDecimal expectedGoalInvolvements,
            BigDecimal expectedGoalsConceded
    ) {
    }

    public record LiveElement(int elementId, PlayerStats stats, List<Integer> explainFixtureIds) {
    }

    public record HistoryMatch(int fixtureId, int round, PlayerStats stats) {
    }
}
