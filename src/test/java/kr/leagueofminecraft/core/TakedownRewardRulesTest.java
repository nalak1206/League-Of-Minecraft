package kr.leagueofminecraft.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TakedownRewardRulesTest {
    @Test void assistWindowIncludesTenSecondBoundary() {
        assertTrue(TakedownRewardRules.assistEligible(100, 300));
        assertFalse(TakedownRewardRules.assistEligible(100, 301));
    }

    @Test void minionRewardsAreHealthScaledAndClamped() {
        assertEquals(new TakedownRewardRules.Reward(12, 30), TakedownRewardRules.minionReward(1));
        assertEquals(new TakedownRewardRules.Reward(90, 240), TakedownRewardRules.minionReward(100));
    }
}
