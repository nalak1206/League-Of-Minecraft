package kr.darius.skills.shop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import kr.darius.skills.ChampionManager;
import kr.darius.skills.combat.CombatEngine;
import kr.darius.skills.combat.CriticalStrikeEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Runtime effects for the first fighter legendary items. */
public final class LegendaryItemEffects {
    private static final float FULL_ATTACK_STRENGTH = 0.9f;
    private static final int CLEAVER_MAX_STACKS = 6;
    private static final double CLEAVER_ARMOR_REDUCTION_PER_STACK = 0.05;
    private static final Identifier CLEAVER_ARMOR_ID = Identifier.fromNamespaceAndPath("darius_skills", "black_cleaver_shred");
    private static final Map<UUID, Long> SPELLBLADE_ARMED = new HashMap<>();
    private static final Map<UUID, Long> SPELLBLADE_COOLDOWN = new HashMap<>();
    private static final Map<UUID, RuinedKingState> RUINED_KING = new HashMap<>();
    private static final Map<UUID, CleaverState> CLEAVER = new HashMap<>();
    private static final Map<UUID, Integer> KRAKEN_HITS = new HashMap<>();
    private static final Map<UUID, Integer> STATIKK_CHARGE = new HashMap<>();
    private static final Map<UUID, Long> HEARTSTEEL_TARGET_COOLDOWN = new HashMap<>();

    private LegendaryItemEffects() {}

    public static void onSkillInput(ServerPlayer player) {
        if (!PlayerEconomy.owns(player, LolShopItem.TRINITY_FORCE)) return;
        long now = System.currentTimeMillis();
        if (SPELLBLADE_COOLDOWN.getOrDefault(player.getUUID(), 0L) <= now)
            SPELLBLADE_ARMED.put(player.getUUID(), now + 10_000);
    }

    /** Returns true when this method replaced the vanilla charged basic attack. */
    public static boolean onBasicAttack(ServerPlayer player, LivingEntity target) {
        if (player.getAttackStrengthScale(0.5f) < FULL_ATTACK_STRENGTH) return false;
        long now = System.currentTimeMillis();
        boolean replacesVanillaAttack = !ChampionManager.isYone(player)
                && PlayerEconomy.criticalStrikeChance(player) > 0.0;
        if (replacesVanillaAttack) {
            CriticalStrikeEngine.Roll critical = CriticalStrikeEngine.rollAttack(player);
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    * critical.damageMultiplier();
            extraPhysical(player, target, damage);
            if (critical.critical()) {
                player.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1,
                        target.getZ(), 20, 0.4, 0.6, 0.4, 0.06);
                player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS, 0.9f, 1.05f);
            }
        }
        if (PlayerEconomy.owns(player, LolShopItem.TRINITY_FORCE)) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
            Long armedUntil = SPELLBLADE_ARMED.get(player.getUUID());
            if (armedUntil != null && armedUntil > now && SPELLBLADE_COOLDOWN.getOrDefault(player.getUUID(), 0L) <= now) {
                double baseAttackDamage = Math.max(1.0,
                        player.getAttributeValue(Attributes.ATTACK_DAMAGE) - PlayerEconomy.attackDamage(player));
                float damage = (float) (baseAttackDamage * 2.0);
                extraPhysical(player, target, damage);
                SPELLBLADE_ARMED.remove(player.getUUID());
                SPELLBLADE_COOLDOWN.put(player.getUUID(), now + 1_500);
                player.level().playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8f, 1.25f);
            }
        }
        if (PlayerEconomy.owns(player, LolShopItem.BLADE_OF_THE_RUINED_KING)) {
            float damage = Math.max(0.5f, target.getHealth() * 0.09f);
            extraPhysical(player, target, damage);
            player.heal(Math.max(0.1f, damage * 0.10f));
            RuinedKingState previous = RUINED_KING.get(target.getUUID());
            int hits = previous != null && previous.attacker.equals(player.getUUID()) && previous.expiresAt > now ? previous.hits + 1 : 1;
            if (hits >= 3) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0, false, false));
                RUINED_KING.remove(target.getUUID());
            } else {
                RUINED_KING.put(target.getUUID(), new RuinedKingState(player.getUUID(), hits, now + 6_000));
            }
            player.level().sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.25, 0.45, 0.25, 0.02);
        }
        if (PlayerEconomy.owns(player, LolShopItem.BLACK_CLEAVER)) {
            applyCleaver(player, target, now);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
        }
        if (PlayerEconomy.owns(player, LolShopItem.THE_COLLECTOR)
                && target.getHealth() <= target.getMaxHealth() * 0.05f) {
            extraPhysical(player, target, target.getHealth() + 1.0f);
        }
        if (PlayerEconomy.owns(player, LolShopItem.KRAKEN_SLAYER)) {
            int hits = KRAKEN_HITS.merge(player.getUUID(), 1, Integer::sum);
            if (hits >= 3) {
                KRAKEN_HITS.put(player.getUUID(), 0);
                extraPhysical(player, target, 3.0f + (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45f);
                player.level().sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.6, 0.4, 0.08);
            }
        }
        if (PlayerEconomy.owns(player, LolShopItem.BLOODTHIRSTER)) {
            player.heal(Math.max(0.2f, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.12f));
        }
        if (PlayerEconomy.owns(player, LolShopItem.STATIKK_SHIV)) {
            int charge = STATIKK_CHARGE.merge(player.getUUID(), 1, Integer::sum);
            if (charge >= 6) {
                STATIKK_CHARGE.put(player.getUUID(), 0);
                List<LivingEntity> chained = player.level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(5.0), entity -> entity != player && entity != target && entity.isAlive());
                extraMagic(player, target, 4.0f);
                for (int i = 0; i < Math.min(4, chained.size()); i++) extraMagic(player, chained.get(i), 3.0f);
                player.level().playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.45f, 1.7f);
            }
        }
        if (PlayerEconomy.owns(player, LolShopItem.HEARTSTEEL)) {
            long readyAt = HEARTSTEEL_TARGET_COOLDOWN.getOrDefault(target.getUUID(), 0L);
            if (readyAt <= now) {
                float damage = Math.max(1.0f, player.getMaxHealth() * 0.06f);
                extraPhysical(player, target, damage);
                var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    Identifier id = Identifier.fromNamespaceAndPath("darius_skills", "heartsteel_" + player.getUUID().toString().replace("-", ""));
                    AttributeModifier current = maxHealth.getModifier(id);
                    double amount = (current == null ? 0.0 : current.amount()) + 0.10;
                    maxHealth.removeModifier(id);
                    maxHealth.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
                }
                HEARTSTEEL_TARGET_COOLDOWN.put(target.getUUID(), now + 30_000);
                player.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55f, 1.35f);
            }
        }
        if (replacesVanillaAttack) player.resetAttackStrengthTicker();
        return replacesVanillaAttack;
    }

    private static void applyCleaver(ServerPlayer player, LivingEntity target, long now) {
        CleaverState previous = CLEAVER.get(target.getUUID());
        int stacks = previous != null && previous.expiresAt > now
                ? Math.min(CLEAVER_MAX_STACKS, previous.stacks + 1) : 1;
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(CLEAVER_ARMOR_ID);
            armor.addTransientModifier(new AttributeModifier(CLEAVER_ARMOR_ID,
                    -CLEAVER_ARMOR_REDUCTION_PER_STACK * stacks, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        CLEAVER.put(target.getUUID(), new CleaverState(target, stacks, now + 6_000));
        if (target.level() instanceof ServerLevel level)
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1, target.getZ(), stacks * 2, 0.25, 0.4, 0.25, 0.01);
    }

    private static void extraPhysical(ServerPlayer player, LivingEntity target, float damage) {
        CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.PHYSICAL,
                CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
    }

    private static void extraMagic(ServerPlayer player, LivingEntity target, float damage) {
        CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.MAGIC,
                CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        SPELLBLADE_ARMED.entrySet().removeIf(entry -> entry.getValue() <= now);
        SPELLBLADE_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        RUINED_KING.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        CLEAVER.entrySet().removeIf(entry -> {
            CleaverState state = entry.getValue();
            if (state.expiresAt > now && state.target.isAlive()) return false;
            var armor = state.target.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(CLEAVER_ARMOR_ID);
            return true;
        });
        HEARTSTEEL_TARGET_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private record RuinedKingState(UUID attacker, int hits, long expiresAt) {}
    private record CleaverState(LivingEntity target, int stacks, long expiresAt) {}
}
