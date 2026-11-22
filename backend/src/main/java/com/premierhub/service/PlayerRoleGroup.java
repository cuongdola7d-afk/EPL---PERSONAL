package com.premierhub.service;

import java.util.Locale;

/**
 * Pure role-group and comparison-group classification shared by player profile endpoints.
 * Uses verified season primary position when present, falling back to broad squad position.
 */
public final class PlayerRoleGroup {
    public static final String ATTACK = "ATTACK";
    public static final String MIXED = "MIXED";
    public static final String DEFEND = "DEFEND";
    public static final String GK = "GK";

    private PlayerRoleGroup() {
    }

    public static String roleGroup(String seasonPosition, String broadPosition) {
        String specific = normalize(seasonPosition);
        if (specific != null) {
            return switch (specific) {
                case "ST", "CF", "LW", "RW", "LM", "RM", "CAM" -> ATTACK;
                case "CM" -> MIXED;
                case "CDM", "CB", "LB", "RB", "LWB", "RWB" -> DEFEND;
                case "GK" -> GK;
                default -> roleGroupFromBroad(broadPosition);
            };
        }
        return roleGroupFromBroad(broadPosition);
    }

    public static String roleGroupFromBroad(String broadPosition) {
        String broad = normalize(broadPosition);
        if (broad == null) {
            return MIXED;
        }
        return switch (broad) {
            case "F", "FORWARD" -> ATTACK;
            case "M", "MIDFIELDER" -> MIXED;
            case "D", "DEFENDER" -> DEFEND;
            case "G", "GOALKEEPER" -> GK;
            default -> MIXED;
        };
    }

    public static String comparisonGroup(String broadPosition, String seasonPosition) {
        String broad = normalize(broadPosition);
        if (broad != null) {
            String mapped = switch (broad) {
                case "G", "GOALKEEPER" -> "G";
                case "D", "DEFENDER" -> "D";
                case "M", "MIDFIELDER" -> "M";
                case "F", "FORWARD" -> "F";
                default -> null;
            };
            if (mapped != null) {
                return mapped;
            }
        }
        String specific = normalize(seasonPosition);
        if (specific == null) {
            return "M";
        }
        return switch (specific) {
            case "GK" -> "G";
            case "CB", "LB", "RB", "LWB", "RWB" -> "D";
            case "CDM", "CM", "CAM", "LM", "RM" -> "M";
            case "ST", "CF", "LW", "RW" -> "F";
            default -> "M";
        };
    }

    public static String comparisonGroupLabel(String comparisonGroup) {
        String code = normalize(comparisonGroup);
        if (code == null) {
            return "tiền vệ";
        }
        return switch (code) {
            case "G" -> "thủ môn";
            case "D" -> "hậu vệ";
            case "M" -> "tiền vệ";
            case "F" -> "tiền đạo";
            default -> "tiền vệ";
        };
    }

    public static String broadPositionEnum(String comparisonGroup) {
        String code = normalize(comparisonGroup);
        if (code == null) {
            return "MIDFIELDER";
        }
        return switch (code) {
            case "G" -> "GOALKEEPER";
            case "D" -> "DEFENDER";
            case "M" -> "MIDFIELDER";
            case "F" -> "FORWARD";
            default -> "MIDFIELDER";
        };
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip().toUpperCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
