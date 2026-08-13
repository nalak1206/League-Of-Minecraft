package kr.darius.skills.combat;

import kr.darius.skills.ChampionManager;
import kr.darius.skills.shop.PlayerEconomy;
import net.minecraft.server.level.ServerPlayer;

/** Shared League-style critical strike chance and damage calculation. */
public final class CriticalStrikeEngine {
    private static final float BASE_CRITICAL_DAMAGE = 2.0f;
    private static final float YONE_CRITICAL_DAMAGE_RATIO = 0.9f;

    private CriticalStrikeEngine() {}

    public static Roll rollAttack(ServerPlayer player) {
        boolean yone = ChampionManager.isYone(player);
        double rawChance = PlayerEconomy.criticalStrikeChance(player) * (yone ? 2.0 : 1.0);
        double chance = Math.clamp(rawChance, 0.0, 1.0);
        float multiplier = BASE_CRITICAL_DAMAGE + (float) PlayerEconomy.bonusCriticalStrikeDamage(player);
        if (yone) multiplier *= YONE_CRITICAL_DAMAGE_RATIO;
        boolean critical = chance > 0.0 && player.getRandom().nextDouble() < chance;
        return new Roll(critical, chance, critical ? multiplier : 1.0f);
    }

    public record Roll(boolean critical, double chance, float damageMultiplier) {}
}
