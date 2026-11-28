package com.premierhub.lineups;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves a provisional formation for 11 confirmed FPL starters using the same slot/affinity
 * matching model as frontend {@code matchLineup.js}:
 * 1. Try the club's most-used formation (same calculation as ClubOverviewService).
 * 2. If the 11 starters cannot fit that formation, try position-group-counted formations (D-M-F and split variants).
 * 3. If still unassignable, keep the candidate formation so frontend reports INSUFFICIENT_POSITIONS.
 */
public final class ProvisionalFormationResolver {
    private static final int NEG_INF = Integer.MIN_VALUE / 4;

    private static final Set<String> LEFT_POS = Set.of("LB", "LWB", "LM", "LW", "LCB", "LCM", "LAM");
    private static final Set<String> RIGHT_POS = Set.of("RB", "RWB", "RM", "RW", "RCB", "RCM", "RAM");
    private static final Set<String> CENTRAL_FAMILIES = Set.of("CB", "CM", "DM", "AM", "ST");
    private static final Set<String> LEFT_ELIGIBLE = Set.of("LB", "LWB", "LM", "LW");
    private static final Set<String> RIGHT_ELIGIBLE = Set.of("RB", "RWB", "RM", "RW");

    private static final Map<String, Map<String, Integer>> AFFINITIES = Map.ofEntries(
            Map.entry("CB", Map.of("CB", 1000, "FB", 350, "WB", 250, "DM", 400, "CM", 100)),
            Map.entry("FB", Map.of("FB", 1000, "WB", 950, "WM", 650, "CB", 250, "DM", 150, "CM", 200)),
            Map.entry("WB", Map.of("WB", 1000, "FB", 900, "WM", 800, "CM", 300, "DM", 150, "CB", 200)),
            Map.entry("DM", Map.of("DM", 1000, "CM", 850, "CB", 400, "WB", 250, "WM", 150, "AM", 100)),
            Map.entry("CM", Map.of("CM", 1000, "DM", 850, "AM", 650, "WM", 450, "WB", 250)),
            Map.entry("AM", Map.of("AM", 1000, "W", 850, "CM", 700, "WM", 650, "ST", 350, "DM", 200)),
            Map.entry("WM", Map.of("WM", 1000, "WB", 800, "AM", 700, "W", 650, "CM", 450, "FB", 250, "DM", 150)),
            Map.entry("W", Map.of("W", 1000, "AM", 850, "WM", 700, "ST", 400, "WB", 200, "CM", 100)),
            Map.entry("ST", Map.of("ST", 1000, "W", 400, "AM", 300, "WM", 100)),
            Map.entry("D", Map.of("CB", 800, "FB", 800, "WB", 600, "DM", 200)),
            Map.entry("M", Map.of("CM", 800, "DM", 700, "AM", 650, "WM", 600, "WB", 300, "W", 200)),
            Map.entry("F", Map.of("ST", 800, "W", 800, "AM", 400, "WM", 200))
    );

    public record StarterSpec(int playerId, String primaryPosition, List<String> eligiblePositions, String broadPosition) {
    }

    private record Slot(int rowIndex, int slotIndex, int lane, String role) {
    }

    private ProvisionalFormationResolver() {
    }

    public static String resolveFormation(String clubUsualFormation, List<StarterSpec> starters) {
        if (starters == null || starters.size() != 11) {
            return validFormationOrNull(clubUsualFormation);
        }
        String usual = validFormationOrNull(clubUsualFormation);
        if (usual != null && canAssign(starters, usual)) {
            return usual;
        }
        List<String> countedCandidates = countedFormationCandidates(starters);
        for (String candidate : countedCandidates) {
            if (canAssign(starters, candidate)) {
                return candidate;
            }
        }
        if (usual != null) {
            return usual;
        }
        return countedCandidates.isEmpty() ? null : countedCandidates.getFirst();
    }

    public static boolean canAssign(List<StarterSpec> starters, String formation) {
        if (starters == null || starters.size() != 11) {
            return false;
        }
        List<Integer> lines = parseFormationLines(formation);
        if (lines == null) {
            return false;
        }
        List<Slot> slots = buildSlots(lines);
        if (slots.size() != 11) {
            return false;
        }

        List<StarterSpec> ordered = new ArrayList<>(starters);
        ordered.sort(Comparator.comparingInt(StarterSpec::playerId));

        int fullMask = (1 << 11) - 1;
        Map<Integer, Integer> states = new HashMap<>();
        states.put(0, 0);

        for (StarterSpec player : ordered) {
            Map<Integer, Integer> next = new HashMap<>();
            for (var entry : states.entrySet()) {
                int mask = entry.getKey();
                int currentScore = entry.getValue();
                for (int i = 0; i < 11; i++) {
                    if ((mask & (1 << i)) != 0) {
                        continue;
                    }
                    int affinity = illustrationScore(player, slots.get(i));
                    if (affinity <= NEG_INF / 2) {
                        continue;
                    }
                    int nextMask = mask | (1 << i);
                    int score = currentScore + affinity;
                    Integer existing = next.get(nextMask);
                    if (existing == null || score > existing) {
                        next.put(nextMask, score);
                    }
                }
            }
            if (next.isEmpty()) {
                return false;
            }
            states = next;
        }
        return states.containsKey(fullMask);
    }

    static List<String> countedFormationCandidates(List<StarterSpec> starters) {
        int defenders = 0;
        int midfielders = 0;
        int forwards = 0;
        int goalkeepers = 0;

        for (StarterSpec s : starters) {
            String group = broadGroupOf(s);
            switch (group) {
                case "G" -> goalkeepers++;
                case "D" -> defenders++;
                case "M" -> midfielders++;
                case "F" -> forwards++;
                default -> { }
            }
        }
        if (goalkeepers != 1 || defenders + midfielders + forwards != 10) {
            return List.of();
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addIfValid(candidates, defenders + "-" + midfielders + "-" + forwards);

        // Split variants when a midfield line is dense or common tactical splits apply.
        if (defenders == 4 && midfielders == 5 && forwards == 1) {
            addIfValid(candidates, "4-2-3-1");
            addIfValid(candidates, "4-1-4-1");
            addIfValid(candidates, "4-5-1");
        } else if (defenders == 4 && midfielders == 3 && forwards == 3) {
            addIfValid(candidates, "4-3-3");
            addIfValid(candidates, "4-2-3-1");
        } else if (defenders == 3 && midfielders == 5 && forwards == 2) {
            addIfValid(candidates, "3-5-2");
            addIfValid(candidates, "3-4-1-2");
            addIfValid(candidates, "5-3-2");
        } else if (defenders == 3 && midfielders == 4 && forwards == 3) {
            addIfValid(candidates, "3-4-3");
            addIfValid(candidates, "3-4-2-1");
        } else if (defenders == 5 && midfielders == 4 && forwards == 1) {
            addIfValid(candidates, "5-4-1");
            addIfValid(candidates, "3-4-2-1");
        } else if (defenders == 5 && midfielders == 3 && forwards == 2) {
            addIfValid(candidates, "5-3-2");
            addIfValid(candidates, "3-5-2");
        } else if (defenders == 4 && midfielders == 4 && forwards == 2) {
            addIfValid(candidates, "4-4-2");
            addIfValid(candidates, "4-2-3-1");
        } else if (defenders == 4 && midfielders == 2 && forwards == 4) {
            addIfValid(candidates, "4-2-3-1");
            addIfValid(candidates, "4-2-4");
            addIfValid(candidates, "4-4-2");
        }

        // General splits if defenders == 3/4/5 and a 3-line count had >5 in midfield.
        if (midfielders > 5 && defenders >= 1 && defenders <= 5 && forwards >= 1 && forwards <= 5) {
            int m1 = midfielders / 2;
            int m2 = midfielders - m1;
            addIfValid(candidates, defenders + "-" + m1 + "-" + m2 + "-" + forwards);
        }
        // Also try common Premier League shapes as final counted fallbacks when groups are close
        for (String fallback : List.of("4-2-3-1", "4-3-3", "4-4-2", "3-4-3", "5-3-2", "5-4-1", "3-5-2", "4-1-4-1", "3-4-2-1")) {
            addIfValid(candidates, fallback);
        }
        return List.copyOf(candidates);
    }

    private static void addIfValid(Set<String> out, String formation) {
        if (parseFormationLines(formation) != null) {
            out.add(formation);
        }
    }

    private static String broadGroupOf(StarterSpec s) {
        String p = s.primaryPosition();
        if (p != null && !p.isBlank()) {
            String up = p.strip().toUpperCase();
            if ("GK".equals(up) || "G".equals(up)) return "G";
            if (Set.of("CB", "LCB", "RCB", "LB", "RB", "LWB", "RWB", "D").contains(up)) return "D";
            if (Set.of("CDM", "LDM", "RDM", "CM", "LCM", "RCM", "CAM", "LAM", "RAM", "LM", "RM", "M").contains(up)) return "M";
            if (Set.of("ST", "CF", "LST", "RST", "LW", "RW", "F").contains(up)) return "F";
        }
        String b = s.broadPosition();
        if (b != null && !b.isBlank()) {
            String up = b.strip().toUpperCase();
            if (up.startsWith("G")) return "G";
            if (up.startsWith("D")) return "D";
            if (up.startsWith("M")) return "M";
            if (up.startsWith("F")) return "F";
        }
        return "";
    }

    private static String validFormationOrNull(String formation) {
        return parseFormationLines(formation) != null ? formation.strip() : null;
    }

    private static List<Integer> parseFormationLines(String formation) {
        if (formation == null || !formation.strip().matches("^[1-5](?:-[1-5]){1,4}$")) {
            return null;
        }
        String[] parts = formation.strip().split("-");
        List<Integer> lines = new ArrayList<>(parts.length + 1);
        lines.add(1);
        int sum = 0;
        for (String part : parts) {
            int val = Integer.parseInt(part);
            sum += val;
            lines.add(val);
        }
        return sum == 10 ? lines : null;
    }

    private static List<Slot> buildSlots(List<Integer> lines) {
        int last = lines.size() - 1;
        List<Slot> slots = new ArrayList<>(11);
        for (int rowIndex = 0; rowIndex < lines.size(); rowIndex++) {
            int count = lines.get(rowIndex);
            for (int slotIndex = 0; slotIndex < count; slotIndex++) {
                int lane = Integer.compare(slotIndex * 2, count - 1);
                boolean edge = count > 1 && (slotIndex == 0 || slotIndex == count - 1);
                String role;
                if (rowIndex == 0) {
                    role = "GK";
                } else if (rowIndex == 1) {
                    role = (count >= 4 && edge) ? "FB" : "CB";
                } else if (rowIndex == last) {
                    role = (count >= 3 && edge) ? "W" : "ST";
                } else if (count >= 4 && edge) {
                    role = lines.get(1) == 3 ? "WB" : "WM";
                } else if (lines.size() >= 5 && rowIndex == last - 1 && count <= 3) {
                    role = "AM";
                } else {
                    role = (rowIndex == 2 && count <= 2 && lines.size() >= 5) ? "DM" : "CM";
                }
                slots.add(new Slot(rowIndex, slotIndex, lane, role));
            }
        }
        return slots;
    }

    private static int positionAffinity(String position, Slot slot) {
        if (position == null || position.isBlank()) {
            return 0;
        }
        String pos = position.strip().toUpperCase();
        if ("GK".equals(pos) || "G".equals(pos)) {
            return "GK".equals(slot.role()) ? 1000 : NEG_INF;
        }
        if ("GK".equals(slot.role())) {
            return NEG_INF;
        }
        boolean left = LEFT_POS.contains(pos);
        boolean right = RIGHT_POS.contains(pos);
        if ((left && slot.lane() > 0) || (right && slot.lane() < 0)) {
            return NEG_INF;
        }
        String family = switch (pos) {
            case "LB", "RB" -> "FB";
            case "LWB", "RWB" -> "WB";
            case "LM", "RM" -> "WM";
            case "LW", "RW" -> "W";
            case "LCB", "RCB" -> "CB";
            case "LCM", "RCM" -> "CM";
            case "CAM", "LAM", "RAM" -> "AM";
            case "CDM", "LDM", "RDM" -> "DM";
            case "CF", "LST", "RST" -> "ST";
            default -> pos;
        };
        Map<String, Integer> map = AFFINITIES.get(family);
        if (map == null) {
            return 0;
        }
        Integer score = map.get(slot.role());
        if (score == null) {
            return NEG_INF;
        }
        int laneBonus = (left || right)
                ? (slot.lane() != 0 ? 40 : 0)
                : (CENTRAL_FAMILIES.contains(family) && slot.lane() == 0 ? 40 : 0);
        return score + laneBonus;
    }

    private static int illustrationScore(StarterSpec player, Slot slot) {
        String primary = (player.primaryPosition() != null && !player.primaryPosition().isBlank())
                ? player.primaryPosition()
                : player.broadPosition();
        List<String> eligible = player.eligiblePositions() == null ? List.of() : player.eligiblePositions();
        int score = positionAffinity(primary, slot);
        for (String pos : eligible) {
            if (!pos.equals(primary)) {
                int alt = positionAffinity(pos, slot);
                if (alt > NEG_INF / 2) {
                    score = Math.max(score, alt - 120);
                }
            }
        }
        if (score <= NEG_INF / 2) {
            return NEG_INF;
        }
        boolean left = eligible.stream().anyMatch(LEFT_ELIGIBLE::contains);
        boolean right = eligible.stream().anyMatch(RIGHT_ELIGIBLE::contains);
        if (left != right && slot.lane() == (left ? -1 : 1)) {
            score += 25;
        }
        return score;
    }
}
