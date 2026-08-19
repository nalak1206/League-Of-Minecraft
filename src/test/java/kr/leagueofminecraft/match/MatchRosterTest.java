package kr.leagueofminecraft.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchRosterTest {
    @Test
    void autoAssignmentKeepsTeamsBalanced() {
        MatchRoster roster = new MatchRoster();
        assertEquals(MatchTeam.BLUE, roster.autoAssign(UUID.randomUUID()));
        assertEquals(MatchTeam.RED, roster.autoAssign(UUID.randomUUID()));
        assertEquals(MatchTeam.BLUE, roster.autoAssign(UUID.randomUUID()));
        assertEquals(2, roster.count(MatchTeam.BLUE));
        assertEquals(1, roster.count(MatchTeam.RED));
    }

    @Test
    void unassignedRemovesPlayerFromRoster() {
        MatchRoster roster = new MatchRoster();
        UUID player = UUID.randomUUID();
        roster.assign(player, MatchTeam.RED);
        roster.assign(player, MatchTeam.UNASSIGNED);
        assertEquals(MatchTeam.UNASSIGNED, roster.team(player));
        assertEquals(0, roster.count(MatchTeam.RED));
    }

    @Test
    void alliesRequireTheSameAssignedTeam() {
        MatchRoster roster = new MatchRoster();
        UUID blueOne = UUID.randomUUID();
        UUID blueTwo = UUID.randomUUID();
        UUID red = UUID.randomUUID();
        roster.assign(blueOne, MatchTeam.BLUE);
        roster.assign(blueTwo, MatchTeam.BLUE);
        roster.assign(red, MatchTeam.RED);
        assertTrue(roster.areAllies(blueOne, blueTwo));
        assertFalse(roster.areAllies(blueOne, red));
        assertFalse(roster.areAllies(blueOne, UUID.randomUUID()));
    }
}
