package kr.leagueofminecraft.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChampionProgressionTest {
    @Test
    void grantsExactlyOneTotalSkillPointPerLevel() {
        assertEquals(1, ChampionProgression.availableSkillPoints(1, new int[5]));
        assertEquals(8, ChampionProgression.availableSkillPoints(8, new int[5]));
        assertEquals(4, ChampionProgression.availableSkillPoints(8, new int[]{0, 2, 1, 1, 0}));
        assertEquals(0, ChampionProgression.availableSkillPoints(3, new int[]{0, 2, 1, 1, 0}));
    }

    @Test
    void resetRestoresFreshChampionProgression() {
        UUID playerId = UUID.randomUUID();
        ChampionProgression.load(playerId, new ChampionProgression.ProgressSnapshot(
                12, 345, 0, new int[]{0, 4, 3, 2, 2}));

        ChampionProgression.reset(playerId);

        ChampionProgression.ProgressSnapshot reset = ChampionProgression.snapshot(playerId);
        assertEquals(1, reset.level());
        assertEquals(0, reset.xp());
        assertEquals(1, reset.skillPoints());
        assertEquals(0, reset.ranks()[1]);
        assertEquals(0, reset.ranks()[2]);
        assertEquals(0, reset.ranks()[3]);
        assertEquals(0, reset.ranks()[4]);
    }
}
