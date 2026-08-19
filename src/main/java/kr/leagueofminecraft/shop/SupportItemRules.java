package kr.leagueofminecraft.shop;

import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.match.MatchTeam;

/** Minecraft-independent ally rules for support item targeting. */
public final class SupportItemRules {
    public static final double VOW_RANGE_SQUARED = 32.0 * 32.0;
    public static final float VOW_TRANSFER_RATIO = 0.12f;
    public static final float VOW_OWNER_HEAL_RATIO = 0.10f;

    private SupportItemRules() {}

    public static boolean canBindVow(boolean samePlayer, ChampionManager.GameMode mode,
                                     MatchTeam ownerTeam, MatchTeam targetTeam) {
        if (samePlayer) return false;
        return mode == ChampionManager.GameMode.ADVENTURE
                || (ownerTeam.isPlayable() && ownerTeam == targetTeam);
    }

    public static boolean isPlayerAlly(ChampionManager.GameMode mode,
                                       MatchTeam ownerTeam, MatchTeam targetTeam) {
        return mode == ChampionManager.GameMode.ADVENTURE
                || (ownerTeam.isPlayable() && ownerTeam == targetTeam);
    }

    public static boolean isVowActive(boolean ownerAlive, float ownerHealth, float ownerMaxHealth,
                                      boolean sameLevel, double distanceSquared, boolean allied) {
        return ownerAlive && ownerMaxHealth > 0.0f
                && ownerHealth > ownerMaxHealth * 0.30f
                && sameLevel && distanceSquared <= VOW_RANGE_SQUARED && allied;
    }

    public static float redirectedDamage(float damageTaken) {
        return Math.max(0.0f, damageTaken) * VOW_TRANSFER_RATIO;
    }

    public static float vowOwnerHealing(float championDamage) {
        return Math.max(0.0f, championDamage) * VOW_OWNER_HEAL_RATIO;
    }
}
