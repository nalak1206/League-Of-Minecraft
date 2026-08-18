package kr.leagueofminecraft.combat;

import kr.leagueofminecraft.ModConstants;
import kr.leagueofminecraft.shop.PlayerEconomy;
import kr.leagueofminecraft.shop.LolShopItem;
import kr.leagueofminecraft.shop.LegendaryItemEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Shared server-side damage entry point for champion skills and item effects. */
public final class CombatEngine {
    public enum DamageKind { PHYSICAL, MAGIC }
    public enum KnockbackPolicy { VANILLA, PRESERVE_MOVEMENT }

    private CombatEngine() {}

    private static final ResourceKey<DamageType> LOL_PHYSICAL = ResourceKey.create(
            Registries.DAMAGE_TYPE, ModConstants.id("lol_physical"));
    private static final ResourceKey<DamageType> LOL_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, ModConstants.id("lol_magic"));

    public static boolean deal(ServerPlayer attacker, LivingEntity target, float amount,
                               DamageKind kind, KnockbackPolicy knockbackPolicy) {
        double resistance = resistanceAfterPenetration(attacker, target, kind);
        double amplified = amount;
        if (kind == DamageKind.MAGIC && PlayerEconomy.owns(attacker, LolShopItem.SHADOWFLAME)
                && target.getHealth() <= target.getMaxHealth() * 0.40f) amplified *= 1.20;
        float finalAmount = (float) (amplified * resistanceMultiplier(resistance));
        ResourceKey<DamageType> key = kind == DamageKind.PHYSICAL ? LOL_PHYSICAL : LOL_MAGIC;
        var type = attacker.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        boolean damaged = deal(attacker, target, new DamageSource(type, attacker, attacker), finalAmount, knockbackPolicy, true);
        if (damaged) LegendaryItemEffects.onChampionDamage(attacker, target, finalAmount, kind);
        return damaged;
    }

    public static double resistanceAfterPenetration(ServerPlayer attacker, LivingEntity target, DamageKind kind) {
        if (kind == DamageKind.PHYSICAL) {
            // Item armor is stored at Minecraft's 1:10 combat scale.
            double armor = target.getAttributeValue(Attributes.ARMOR) * 10.0;
            return CombatMath.resistanceAfterPenetration(armor,
                    PlayerEconomy.armorPenetrationPercent(attacker),
                    PlayerEconomy.armorPenetrationFlat(attacker));
        }
        double magicResistance = target instanceof ServerPlayer player
                ? PlayerEconomy.magicResistance(player) : 0.0;
        return CombatMath.resistanceAfterPenetration(magicResistance,
                PlayerEconomy.magicPenetrationPercent(attacker),
                PlayerEconomy.magicPenetrationFlat(attacker));
    }

    public static double resistanceMultiplier(double resistance) {
        return CombatMath.resistanceMultiplier(resistance);
    }

    public static boolean deal(ServerPlayer attacker, LivingEntity target, DamageSource source, float amount,
                               KnockbackPolicy knockbackPolicy, boolean resetInvulnerability) {
        if (amount <= 0.0f || !attacker.isAlive() || !target.isAlive() || attacker == target) return false;

        Vec3 movement = target.getDeltaMovement();
        if (resetInvulnerability) target.invulnerableTime = 0;
        boolean damaged = target.hurtServer(attacker.level(), source, amount);
        if (resetInvulnerability) target.invulnerableTime = 0;
        if (knockbackPolicy == KnockbackPolicy.PRESERVE_MOVEMENT) target.setDeltaMovement(movement);
        return damaged;
    }
}
