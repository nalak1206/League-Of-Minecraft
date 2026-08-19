package kr.leagueofminecraft.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChampionProgressionTest {
    @Test
    void grantsExactlyOneTotalSkillPointPerLevel() {
        assertEquals(1, ChampionProgression.availableSkillPoints(1, new int[5]));
        assertEquals(8, ChampionProgression.availableSkillPoints(8, new int[5]));
        assertEquals(4, ChampionProgression.availableSkillPoints(8, new int[]{0, 2, 1, 1, 0}));
        assertEquals(0, ChampionProgression.availableSkillPoints(3, new int[]{0, 2, 1, 1, 0}));
    }
}
