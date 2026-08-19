package kr.leagueofminecraft.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void vowOnlyTransfersWhileOwnerIsHealthyNearbyAndAllied() {
        assertTrue(SupportItemRules.isVowActive(true, 7.0f, 20.0f,
                true, 32.0 * 32.0, true));
        assertFalse(SupportItemRules.isVowActive(true, 6.0f, 20.0f,
                true, 10.0, true));
        assertFalse(SupportItemRules.isVowActive(true, 20.0f, 20.0f,
                true, 32.01 * 32.01, true));
        assertFalse(SupportItemRules.isVowActive(true, 20.0f, 20.0f,
                false, 10.0, true));
        assertFalse(SupportItemRules.isVowActive(true, 20.0f, 20.0f,
                true, 10.0, false));
    }

    @Test
    void vowUsesTwelvePercentTransferAndTenPercentHealing() {
        assertEquals(1.2f, SupportItemRules.redirectedDamage(10.0f), 0.0001f);
        assertEquals(0.0f, SupportItemRules.redirectedDamage(-10.0f), 0.0001f);
        assertEquals(1.0f, SupportItemRules.vowOwnerHealing(10.0f), 0.0001f);
    }
}
