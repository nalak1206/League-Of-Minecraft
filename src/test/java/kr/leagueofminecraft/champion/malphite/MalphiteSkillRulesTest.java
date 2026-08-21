package kr.leagueofminecraft.champion.malphite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MalphiteSkillRulesTest {
    @Test
    void areaSkillsUseACircleInsteadOfTheInflatedBoundingBoxCorners() {
        assertTrue(MalphiteSkillRules.withinHorizontalRadius(3.0, 2.0, 4.0));
        assertFalse(MalphiteSkillRules.withinHorizontalRadius(3.5, 3.5, 4.0));
    }

    @Test
    void dashProgressIsClampedAcrossEightTicks() {
        assertEquals(0.0, MalphiteSkillRules.dashProgress(-1));
        assertEquals(0.5, MalphiteSkillRules.dashProgress(4));
        assertEquals(1.0, MalphiteSkillRules.dashProgress(9));
    }

    @Test
    void requestedDashRangeCannotExceedUltimateRange() {
        assertEquals(11.0, MalphiteSkillRules.clampRange(20.0, 11.0));
        assertEquals(6.5, MalphiteSkillRules.clampRange(6.5, 11.0));
        assertEquals(0.0, MalphiteSkillRules.clampRange(-2.0, 11.0));
    }
}
