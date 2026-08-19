package kr.leagueofminecraft.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.match.MatchTeam;
import org.junit.jupiter.api.Test;

class SupportItemRulesTest {
    @Test
    void adventureCanBindAnotherPlayerButNotSelf() {
        assertTrue(SupportItemRules.canBindVow(false, ChampionManager.GameMode.ADVENTURE,
                MatchTeam.UNASSIGNED, MatchTeam.UNASSIGNED));
        assertFalse(SupportItemRules.canBindVow(true, ChampionManager.GameMode.ADVENTURE,
                MatchTeam.UNASSIGNED, MatchTeam.UNASSIGNED));
    }

    @Test
    void matchCanOnlyBindSamePlayableTeam() {
        assertTrue(SupportItemRules.canBindVow(false, ChampionManager.GameMode.MATCH,
                MatchTeam.BLUE, MatchTeam.BLUE));
        assertFalse(SupportItemRules.canBindVow(false, ChampionManager.GameMode.MATCH,
                MatchTeam.BLUE, MatchTeam.RED));
        assertFalse(SupportItemRules.canBindVow(false, ChampionManager.GameMode.MATCH,
                MatchTeam.UNASSIGNED, MatchTeam.UNASSIGNED));
    }
}
