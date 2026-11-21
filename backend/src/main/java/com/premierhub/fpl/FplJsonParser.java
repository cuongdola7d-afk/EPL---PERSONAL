package com.premierhub.fpl;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FplJsonParser {
    private static final BigDecimal MAX_DECIMAL_5_2 = new BigDecimal("999.99");
    private static final BigDecimal MAX_DECIMAL_6_1 = new BigDecimal("99999.9");

    private final ObjectMapper mapper;

    public FplJsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public FplModels.Bootstrap parseBootstrap(String json) {
        JsonNode root = parseObject(json, "bootstrap-static");
        JsonNode teamsNode = requiredArray(root, "teams", "bootstrap-static");
        JsonNode elementsNode = requiredArray(root, "elements", "bootstrap-static");
        JsonNode eventsNode = requiredArray(root, "events", "bootstrap-static");

        List<FplModels.Team> teams = new ArrayList<>();
        for (int i = 0; i < teamsNode.size(); i++) {
            JsonNode node = teamsNode.get(i);
            String ctx = "bootstrap-static.teams[" + i + "]";
            teams.add(new FplModels.Team(
                    requiredInt(node, "id", ctx),
                    requiredString(node, "name", ctx),
                    requiredString(node, "short_name", ctx)
            ));
        }

        List<FplModels.Element> elements = new ArrayList<>();
        for (int i = 0; i < elementsNode.size(); i++) {
            JsonNode node = elementsNode.get(i);
            String ctx = "bootstrap-static.elements[" + i + "]";
            elements.add(new FplModels.Element(
                    requiredInt(node, "id", ctx),
                    requiredInt(node, "code", ctx),
                    requiredString(node, "web_name", ctx),
                    requiredString(node, "first_name", ctx),
                    requiredString(node, "second_name", ctx),
                    requiredInt(node, "team", ctx)
            ));
        }

        List<FplModels.Event> events = new ArrayList<>();
        for (int i = 0; i < eventsNode.size(); i++) {
            JsonNode node = eventsNode.get(i);
            String ctx = "bootstrap-static.events[" + i + "]";
            events.add(new FplModels.Event(
                    requiredInt(node, "id", ctx),
                    requiredString(node, "name", ctx),
                    requiredBoolean(node, "finished", ctx)
            ));
        }

        return new FplModels.Bootstrap(List.copyOf(teams), List.copyOf(elements), List.copyOf(events));
    }

    public List<FplModels.FplFixture> parseFixtures(String json, int expectedGameweek) {
        JsonNode root = parseArray(json, "fixtures");
        List<FplModels.FplFixture> fixtures = new ArrayList<>();
        for (int i = 0; i < root.size(); i++) {
            JsonNode node = root.get(i);
            String ctx = "fixtures[" + i + "]";
            int id = requiredInt(node, "id", ctx);
            Integer event = nullableInt(node, "event", ctx);
            if (event != null && event != expectedGameweek) {
                throw new IllegalArgumentException(ctx + ": event=" + event
                        + " does not match expected gameweek " + expectedGameweek);
            }
            Instant kickoff = requiredInstant(node, "kickoff_time", ctx);
            int teamH = requiredInt(node, "team_h", ctx);
            int teamA = requiredInt(node, "team_a", ctx);
            Integer teamHScore = nullableInt(node, "team_h_score", ctx);
            Integer teamAScore = nullableInt(node, "team_a_score", ctx);
            boolean finished = requiredBoolean(node, "finished", ctx);
            boolean finishedProvisional = requiredBoolean(node, "finished_provisional", ctx);
            fixtures.add(new FplModels.FplFixture(id, event, kickoff, teamH, teamA,
                    teamHScore, teamAScore, finished, finishedProvisional));
        }
        return List.copyOf(fixtures);
    }

    public List<FplModels.LiveElement> parseEventLive(String json) {
        JsonNode root = parseObject(json, "event-live");
        JsonNode elementsNode = requiredArray(root, "elements", "event-live");
        List<FplModels.LiveElement> elements = new ArrayList<>();
        for (int i = 0; i < elementsNode.size(); i++) {
            JsonNode node = elementsNode.get(i);
            String ctx = "event-live.elements[" + i + "]";
            int elementId = requiredInt(node, "id", ctx);
            String elCtx = "event-live.element(" + elementId + ")";
            JsonNode statsNode = requiredObject(node, "stats", elCtx);
            FplModels.PlayerStats stats = parsePlayerStats(statsNode, elCtx + ".stats");
            JsonNode explainNode = requiredArray(node, "explain", elCtx);
            Set<Integer> fixtureIds = new LinkedHashSet<>();
            for (int j = 0; j < explainNode.size(); j++) {
                JsonNode ex = explainNode.get(j);
                fixtureIds.add(requiredInt(ex, "fixture", elCtx + ".explain[" + j + "]"));
            }
            elements.add(new FplModels.LiveElement(elementId, stats, List.copyOf(fixtureIds)));
        }
        return List.copyOf(elements);
    }

    public List<FplModels.HistoryMatch> parseElementSummary(String json, int elementId) {
        String ctx = "element-summary(" + elementId + ")";
        JsonNode root = parseObject(json, ctx);
        JsonNode historyNode = requiredArray(root, "history", ctx);
        List<FplModels.HistoryMatch> matches = new ArrayList<>();
        for (int i = 0; i < historyNode.size(); i++) {
            JsonNode node = historyNode.get(i);
            String rowCtx = ctx + ".history[" + i + "]";
            int fixtureId = requiredInt(node, "fixture", rowCtx);
            int round = requiredInt(node, "round", rowCtx);
            FplModels.PlayerStats stats = parsePlayerStats(node, rowCtx);
            matches.add(new FplModels.HistoryMatch(fixtureId, round, stats));
        }
        return List.copyOf(matches);
    }

    private FplModels.PlayerStats parsePlayerStats(JsonNode node, String ctx) {
        return new FplModels.PlayerStats(
                requiredInt(node, "minutes", ctx),
                requiredInt(node, "goals_scored", ctx),
                requiredInt(node, "assists", ctx),
                requiredInt(node, "clean_sheets", ctx),
                requiredInt(node, "goals_conceded", ctx),
                requiredInt(node, "own_goals", ctx),
                requiredInt(node, "penalties_saved", ctx),
                requiredInt(node, "penalties_missed", ctx),
                requiredInt(node, "yellow_cards", ctx),
                requiredInt(node, "red_cards", ctx),
                requiredInt(node, "saves", ctx),
                requiredInt(node, "bonus", ctx),
                requiredInt(node, "bps", ctx),
                requiredDecimalString(node, "influence", ctx, 1, MAX_DECIMAL_6_1),
                requiredDecimalString(node, "creativity", ctx, 1, MAX_DECIMAL_6_1),
                requiredDecimalString(node, "threat", ctx, 1, MAX_DECIMAL_6_1),
                requiredDecimalString(node, "ict_index", ctx, 1, MAX_DECIMAL_6_1),
                requiredInt(node, "clearances_blocks_interceptions", ctx),
                requiredInt(node, "recoveries", ctx),
                requiredInt(node, "tackles", ctx),
                requiredInt(node, "defensive_contribution", ctx),
                requiredInt(node, "starts", ctx),
                requiredDecimalString(node, "expected_goals", ctx, 2, MAX_DECIMAL_5_2),
                requiredDecimalString(node, "expected_assists", ctx, 2, MAX_DECIMAL_5_2),
                requiredDecimalString(node, "expected_goal_involvements", ctx, 2, MAX_DECIMAL_5_2),
                requiredDecimalString(node, "expected_goals_conceded", ctx, 2, MAX_DECIMAL_5_2)
        );
    }

    private JsonNode parseObject(String json, String context) {
        JsonNode root = parseTree(json, context);
        if (!root.isObject()) {
            throw new IllegalArgumentException(context + ": expected JSON object");
        }
        return root;
    }

    private JsonNode parseArray(String json, String context) {
        JsonNode root = parseTree(json, context);
        if (!root.isArray()) {
            throw new IllegalArgumentException(context + ": expected JSON array");
        }
        return root;
    }

    private JsonNode parseTree(String json, String context) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(context + ": empty JSON payload");
        }
        try {
            return mapper.readTree(json);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException(context + ": invalid JSON payload: " + error.getMessage(), error);
        }
    }

    private static JsonNode requiredField(JsonNode parent, String field, String ctx) {
        if (parent == null || !parent.isObject() || !parent.has(field)) {
            throw new IllegalArgumentException(ctx + ": missing required field '" + field + "'");
        }
        return parent.get(field);
    }

    private static JsonNode requiredObject(JsonNode parent, String field, String ctx) {
        JsonNode node = requiredField(parent, field, ctx);
        if (!node.isObject()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must be a JSON object");
        }
        return node;
    }

    private static JsonNode requiredArray(JsonNode parent, String field, String ctx) {
        JsonNode node = requiredField(parent, field, ctx);
        if (!node.isArray()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must be a JSON array");
        }
        return node;
    }

    private static int requiredInt(JsonNode parent, String field, String ctx) {
        JsonNode node = requiredField(parent, field, ctx);
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must be an integer");
        }
        return node.intValue();
    }

    private static Integer nullableInt(JsonNode parent, String field, String ctx) {
        JsonNode node = requiredField(parent, field, ctx);
        if (node.isNull()) {
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must be an integer or null");
        }
        return node.intValue();
    }

    private static boolean requiredBoolean(JsonNode parent, String field, String ctx) {
        JsonNode node = requiredField(parent, field, ctx);
        if (!node.isBoolean()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must be a boolean");
        }
        return node.booleanValue();
    }

    private static String requiredString(JsonNode parent, String field, String ctx) {
        JsonNode node = requiredField(parent, field, ctx);
        if (!node.isString()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must be a string");
        }
        return node.stringValue();
    }

    private static Instant requiredInstant(JsonNode parent, String field, String ctx) {
        String raw = requiredString(parent, field, ctx);
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(ctx + ": field '" + field
                    + "' must be an ISO-8601 UTC instant, got '" + raw + "'", error);
        }
    }

    private static BigDecimal requiredDecimalString(JsonNode parent, String field, String ctx,
                                                    int scale, BigDecimal maxAbs) {
        JsonNode node = requiredField(parent, field, ctx);
        if (!node.isString()) {
            throw new IllegalArgumentException(ctx + ": field '" + field
                    + "' must be a numeric string (e.g. \"0.36\")");
        }
        String raw = node.stringValue().strip();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException(ctx + ": field '" + field + "' must not be blank");
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(ctx + ": field '" + field
                    + "' is not a valid decimal string: '" + raw + "'", error);
        }
        if (parsed.scale() > scale) {
            throw new IllegalArgumentException(ctx + ": field '" + field
                    + "' exceeds scale " + scale + ": '" + raw + "'");
        }
        if (parsed.abs().compareTo(maxAbs) > 0) {
            throw new IllegalArgumentException(ctx + ": field '" + field
                    + "' out of range: '" + raw + "'");
        }
        return parsed.setScale(scale);
    }
}
