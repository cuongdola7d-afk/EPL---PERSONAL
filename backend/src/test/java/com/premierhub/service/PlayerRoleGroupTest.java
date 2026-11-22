package com.premierhub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerRoleGroupTest {

    @ParameterizedTest
    @CsvSource({
            "ST, FORWARD, ATTACK",
            "CF, FORWARD, ATTACK",
            "LW, FORWARD, ATTACK",
            "RW, FORWARD, ATTACK",
            "LM, MIDFIELDER, ATTACK",
            "RM, MIDFIELDER, ATTACK",
            "CAM, MIDFIELDER, ATTACK",
            "CM, MIDFIELDER, MIXED",
            "CDM, MIDFIELDER, DEFEND",
            "CB, DEFENDER, DEFEND",
            "LB, DEFENDER, DEFEND",
            "RB, DEFENDER, DEFEND",
            "LWB, DEFENDER, DEFEND",
            "RWB, DEFENDER, DEFEND",
            "GK, GOALKEEPER, GK"
    })
    void mapsSpecificSeasonPositionsToExpectedRoleGroups(String specific, String broad, String expected) {
        assertEquals(expected, PlayerRoleGroup.roleGroup(specific, broad));
    }

    @Test
    void fallsBackToBroadPositionWhenSeasonPositionMissingOrUnknown() {
        assertEquals("ATTACK", PlayerRoleGroup.roleGroup(null, "F"));
        assertEquals("ATTACK", PlayerRoleGroup.roleGroup("", "FORWARD"));
        assertEquals("MIXED", PlayerRoleGroup.roleGroup(null, "M"));
        assertEquals("MIXED", PlayerRoleGroup.roleGroup(" ", "MIDFIELDER"));
        assertEquals("DEFEND", PlayerRoleGroup.roleGroup(null, "D"));
        assertEquals("DEFEND", PlayerRoleGroup.roleGroup("UNKNOWN", "DEFENDER"));
        assertEquals("GK", PlayerRoleGroup.roleGroup(null, "G"));
        assertEquals("GK", PlayerRoleGroup.roleGroup(null, "GOALKEEPER"));
        assertEquals("MIXED", PlayerRoleGroup.roleGroup(null, null));
    }

    @Test
    void mapsComparisonGroupsAndVietnameseLabels() {
        assertEquals("G", PlayerRoleGroup.comparisonGroup("GOALKEEPER", "GK"));
        assertEquals("D", PlayerRoleGroup.comparisonGroup("DEFENDER", "CB"));
        assertEquals("M", PlayerRoleGroup.comparisonGroup("MIDFIELDER", "CAM"));
        assertEquals("F", PlayerRoleGroup.comparisonGroup("FORWARD", "ST"));

        assertEquals("thủ môn", PlayerRoleGroup.comparisonGroupLabel("G"));
        assertEquals("hậu vệ", PlayerRoleGroup.comparisonGroupLabel("D"));
        assertEquals("tiền vệ", PlayerRoleGroup.comparisonGroupLabel("M"));
        assertEquals("tiền đạo", PlayerRoleGroup.comparisonGroupLabel("F"));

        assertEquals("GOALKEEPER", PlayerRoleGroup.broadPositionEnum("G"));
        assertEquals("DEFENDER", PlayerRoleGroup.broadPositionEnum("D"));
        assertEquals("MIDFIELDER", PlayerRoleGroup.broadPositionEnum("M"));
        assertEquals("FORWARD", PlayerRoleGroup.broadPositionEnum("F"));
    }
}
