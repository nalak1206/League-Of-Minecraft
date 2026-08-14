package kr.leagueofminecraft.shop;

import kr.leagueofminecraft.ModConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.combat.CombatEngine;
import kr.leagueofminecraft.combat.CriticalStrikeEngine;
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
    private static final Identifier CLEAVER_ARMOR_ID = ModConstants.id("black_cleaver_shred");
    private static final Map<UUID, Long> SPELLBLADE_ARMED = new HashMap<>();
    private static final Map<UUID, Long> SPELLBLADE_COOLDOWN = new HashMap<>();
    private static final Map<UUID, RuinedKingState> RUINED_KING = new HashMap<>();
    private static final Map<UUID, CleaverState> CLEAVER = new HashMap<>();
    private static final Map<UUID, Integer> KRAKEN_HITS = new HashMap<>();
    private static final Map<UUID, Integer> STATIKK_CHARGE = new HashMap<>();
    private static final Map<UUID, Long> HEARTSTEEL_TARGET_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> ZHONYA_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();
    private static final Map<UUID, Long> STERAK_COOLDOWN = new HashMap<>();
    private static final Set<UUID> REFLECTING = new HashSet<>();

    private LegendaryItemEffects() {}

    public static String useActive(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long readyAt = ACTIVE_COOLDOWN.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) return String.format(java.util.Locale.ROOT, "재사용 %.1f초", (readyAt - now) / 1000.0);
        if (PlayerEconomy.owns(player, LolShopItem.ZHONYAS_HOURGLASS)) {
            ZHONYA_UNTIL.put(player.getUUID(), now + 2_500);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 255, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50, 255, false, false));
            ACTIVE_COOLDOWN.put(player.getUUID(), now + 120_000);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.7f);
            return "존야의 모래시계: 경직 2.5초";
        }
        if (PlayerEconomy.owns(player, LolShopItem.YOUMUUS_GHOSTBLADE)) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 2, false, false));
            ACTIVE_COOLDOWN.put(player.getUUID(), now + 45_000);
            return "요우무의 유령검: 이동 속도 증가";
        }
        if (PlayerEconomy.owns(player, LolShopItem.PROFANE_HYDRA)) {
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(4.0), target -> target != player && target.isAlive());
            for (LivingEntity target : targets) {
                float missing = 1.0f - target.getHealth() / target.getMaxHealth();
                extraPhysical(player, target, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.8f + missing * 0.8f));
            }
            ACTIVE_COOLDOWN.put(player.getUUID(), now + 10_000);
            return "불경한 히드라: 광역 참격";
        }
        if (PlayerEconomy.owns(player, LolShopItem.LOCKET_OF_THE_IRON_SOLARI)) {
            for (ServerPlayer ally : player.level().getEntitiesOfClass(ServerPlayer.class,
                    player.getBoundingBox().inflate(8.0), LivingEntity::isAlive))
                ally.setAbsorptionAmount(Math.max(ally.getAbsorptionAmount(), 5.0f));
            ACTIVE_COOLDOWN.put(player.getUUID(), now + 90_000);
            return "강철의 솔라리 펜던트: 광역 보호막";
        }
        if (PlayerEconomy.owns(player, LolShopItem.SHURELYAS_BATTLESONG)) {
            for (ServerPlayer ally : player.level().getEntitiesOfClass(ServerPlayer.class,
                    player.getBoundingBox().inflate(8.0), LivingEntity::isAlive))
                ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 2, false, false));
            ACTIVE_COOLDOWN.put(player.getUUID(), now + 75_000);
            return "슈렐리아의 군가: 광역 이동 속도 증가";
        }
        if (PlayerEconomy.owns(player, LolShopItem.REDEMPTION)) {
            player.heal((float) (6.0 * (1.0 + PlayerEconomy.healAndShieldPower(player))));
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(8.0), target -> target != player && target.isAlive()))
                extraMagic(player, target, 3.0f);
            ACTIVE_COOLDOWN.put(player.getUUID(), now + 90_000);
            return "구원: 주변 회복 및 피해";
        }
        return "사용할 수 있는 액티브 아이템이 없습니다";
    }

    public static boolean allowDamage(LivingEntity target) {
        return ZHONYA_UNTIL.getOrDefault(target.getUUID(), 0L) <= System.currentTimeMillis();
    }

    public static void afterDamage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        long now = System.currentTimeMillis();
        LAST_COMBAT.put(target.getUUID(), now);
        if (source.getEntity() instanceof LivingEntity attacker) LAST_COMBAT.put(attacker.getUUID(), now);
        if (!(target instanceof ServerPlayer player)) return;
        if (PlayerEconomy.owns(player, LolShopItem.THORNMAIL) && source.getEntity() instanceof LivingEntity attacker
                && attacker != player && REFLECTING.add(player.getUUID())) {
            try {
                CombatEngine.deal(player, attacker, 1.0f + (float) player.getAttributeValue(Attributes.ARMOR) * 0.15f,
                        CombatEngine.DamageKind.MAGIC, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
            } finally {
                REFLECTING.remove(player.getUUID());
            }
        }
        if (PlayerEconomy.owns(player, LolShopItem.STERAKS_GAGE)
                && player.getHealth() <= player.getMaxHealth() * 0.30f
                && STERAK_COOLDOWN.getOrDefault(player.getUUID(), 0L) <= now) {
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), player.getMaxHealth() * 0.35f));
            STERAK_COOLDOWN.put(player.getUUID(), now + 90_000);
        }
    }

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
            // Lifesteal is applied once below for every charged basic attack.
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
                    Identifier id = ModConstants.id("heartsteel_" + player.getUUID().toString().replace("-", ""));
                    AttributeModifier current = maxHealth.getModifier(id);
                    double amount = (current == null ? 0.0 : current.amount()) + 0.10;
                    maxHealth.removeModifier(id);
                    maxHealth.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
                }
                HEARTSTEEL_TARGET_COOLDOWN.put(target.getUUID(), now + 30_000);
                player.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55f, 1.35f);
            }
        }
        double lifeSteal = PlayerEconomy.lifeSteal(player);
        if (lifeSteal > 0) {
            double rawAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double dealtEstimate = rawAttack * CombatEngine.resistanceMultiplier(
                    CombatEngine.resistanceAfterPenetration(player, target, CombatEngine.DamageKind.PHYSICAL));
            player.heal((float) Math.max(0.0, dealtEstimate * lifeSteal));
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
        ACTIVE_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        ZHONYA_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
        STERAK_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (server.getTickCount() % 20 == 0) for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;
            if (PlayerEconomy.owns(player, LolShopItem.WARMOGS_ARMOR)
                    && now - LAST_COMBAT.getOrDefault(player.getUUID(), 0L) >= 8_000)
                player.heal(player.getMaxHealth() * 0.03f);
            if (PlayerEconomy.owns(player, LolShopItem.SUNFIRE_AEGIS)) {
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(3.0), target -> target != player && target.isAlive()))
                    extraMagic(player, target, 1.0f + player.getMaxHealth() * 0.01f);
            }
        }
    }

    private record RuinedKingState(UUID attacker, int hits, long expiresAt) {}
    private record CleaverState(LivingEntity target, int stacks, long expiresAt) {}
}
