package kr.leagueofminecraft.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChampionTransitionRulesTest {
    @Test void changingChampionResetsProgression() {
        assertTrue(ChampionTransitionRules.shouldResetProgression(
                ChampionManager.Champion.DARIUS, ChampionManager.Champion.YONE));
        assertTrue(ChampionTransitionRules.shouldResetProgression(
                ChampionManager.Champion.YONE, ChampionManager.Champion.MALPHITE));
    }

    @Test void reselectingSameChampionKeepsProgression() {
        assertFalse(ChampionTransitionRules.shouldResetProgression(
                ChampionManager.Champion.MALPHITE, ChampionManager.Champion.MALPHITE));
    }
}
