package kr.leagueofminecraft.core;

/** Pure champion transition policy shared by selection and regression tests. */
public final class ChampionTransitionRules {
    private ChampionTransitionRules() {}

    public static boolean shouldResetProgression(ChampionManager.Champion current,
                                                 ChampionManager.Champion selected) {
        return current != selected;
    }
}
