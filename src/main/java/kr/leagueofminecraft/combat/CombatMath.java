package kr.leagueofminecraft.combat;

/** Pure League combat formulas kept independent from Minecraft runtime objects. */
public final class CombatMath {
    private CombatMath() {}

    public static double resistanceAfterPenetration(double resistance,
                                                     double percentPenetration,
                                                     double flatPenetration) {
        double percent = Math.max(0.0, Math.min(1.0, percentPenetration));
        return resistance * (1.0 - percent) - Math.max(0.0, flatPenetration);
    }

    public static double resistanceMultiplier(double resistance) {
        return resistance >= 0.0
                ? 100.0 / (100.0 + resistance)
                : 2.0 - 100.0 / (100.0 - resistance);
    }

    public static long cooldownMillis(long baseMillis, double abilityHaste) {
        return Math.max(1L, Math.round(baseMillis * 100.0 / (100.0 + Math.max(0.0, abilityHaste))));
    }
}
