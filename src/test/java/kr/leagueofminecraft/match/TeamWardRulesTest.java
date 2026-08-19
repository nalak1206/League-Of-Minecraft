package kr.leagueofminecraft.match;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamWardRulesTest {
    @Test
    void alliedWardIsVisibleDuringMatch() {
        assertTrue(TeamWardRules.isAlliedWard(MatchTeam.BLUE, MatchTeam.BLUE, MatchPhase.RUNNING));
        assertFalse(TeamWardRules.isAlliedWard(MatchTeam.BLUE, MatchTeam.RED, MatchPhase.RUNNING));
    }

    @Test
    void lensOnlyRevealsEnemyWardDuringMatch() {
        assertTrue(TeamWardRules.canRevealWithLens(MatchTeam.BLUE, MatchTeam.RED, MatchPhase.RUNNING));
        assertFalse(TeamWardRules.canRevealWithLens(MatchTeam.BLUE, MatchTeam.BLUE, MatchPhase.RUNNING));
    }

    @Test
    void lobbyAndUnassignedPlayersDoNotUseTeamVision() {
        assertFalse(TeamWardRules.isAlliedWard(MatchTeam.BLUE, MatchTeam.BLUE, MatchPhase.LOBBY));
        assertFalse(TeamWardRules.canRevealWithLens(MatchTeam.UNASSIGNED, MatchTeam.RED, MatchPhase.RUNNING));
    }

    @Test
    void onlyEnemyTeamCanDestroyWardDuringMatch() {
        assertTrue(TeamWardRules.canDestroy(MatchTeam.BLUE, MatchTeam.RED, MatchPhase.RUNNING));
        assertFalse(TeamWardRules.canDestroy(MatchTeam.BLUE, MatchTeam.BLUE, MatchPhase.RUNNING));
        assertFalse(TeamWardRules.canDestroy(MatchTeam.UNASSIGNED, MatchTeam.RED, MatchPhase.RUNNING));
    }

    @Test
    void enemyWardAwardsTenGoldOnlyDuringMatch() {
        assertTrue(TeamWardRules.destructionGold(MatchTeam.BLUE, MatchTeam.RED, MatchPhase.RUNNING) == 10);
        assertTrue(TeamWardRules.destructionGold(MatchTeam.BLUE, MatchTeam.RED, MatchPhase.LOBBY) == 0);
    }
}
