package kr.leagueofminecraft.shop;

import kr.leagueofminecraft.ModConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.core.PerPlayerCooldowns;
import kr.leagueofminecraft.combat.CombatEngine;
import kr.leagueofminecraft.combat.CriticalStrikeEngine;
import kr.leagueofminecraft.match.MatchManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.world.InteractionResult;
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
    private static final PerPlayerCooldowns<LolShopItem> ACTIVE_COOLDOWNS = new PerPlayerCooldowns<>();
    private static final Map<UUID, Long> ZHONYA_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();
    private static final Map<UUID, Long> COMBAT_STARTED = new HashMap<>();
    private static final Map<UUID, Long> STERAK_COOLDOWN = new HashMap<>();
    private static final Set<UUID> REFLECTING = new HashSet<>();
    private static final Set<UUID> ITEM_PROC = new HashSet<>();
    private static final Map<UUID, Long> EDGE_OF_NIGHT_RECHARGE = new HashMap<>();
    private static final Map<UUID, Long> SUNDERED_SKY_TARGET = new HashMap<>();
    private static final Map<UUID, Long> OPPORTUNITY_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> IMPERIAL_TARGET_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> TAKEDOWN_GUARD = new HashMap<>();
    private static final Map<UUID, BurnState> LIANDRY_BURNS = new HashMap<>();
    private static boolean initialized;

    private LegendaryItemEffects() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer owner)
                    || !(target instanceof ServerPlayer ally)
                    || !PlayerEconomy.owns(owner, LolShopItem.KNIGHTS_VOW)) return InteractionResult.PASS;
            if (!SupportItemRules.canBindVow(owner == ally, ChampionManager.mode(owner),
                    MatchManager.team(owner), MatchManager.team(ally))) {
                owner.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                        "§c기사의 맹세: 아군 플레이어만 결속할 수 있습니다")));
                return InteractionResult.SUCCESS;
            }
            PlayerEconomy.bindKnightsVow(owner, ally.getUUID());
            owner.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                    "§a기사의 맹세 결속: §f" + ally.getName().getString())));
            owner.level().playSound(null, owner.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.65f, 1.5f);
            return InteractionResult.SUCCESS;
        });
    }

    public static String useActive(ServerPlayer player) {
        for (LolShopItem item : PlayerEconomy.equipment(player)) {
            if (isActiveItem(item)) return useActive(player, item);
        }
        return "사용할 수 있는 액티브 아이템이 없습니다";
    }

    /** Uses the item bound to one of the six virtual LoL equipment slots. */
    public static String useActive(ServerPlayer player, int slot) {
        LolShopItem item = PlayerEconomy.equipmentAt(player, slot);
        if (item == null) return (slot + 1) + "번 아이템 칸이 비어 있습니다";
        if (!isActiveItem(item)) return item.displayName() + ": 사용 효과가 없는 아이템입니다";
        return useActive(player, item);
    }

    private static boolean isActiveItem(LolShopItem item) {
        return item == LolShopItem.ZHONYAS_HOURGLASS
                || item == LolShopItem.YOUMUUS_GHOSTBLADE
                || item == LolShopItem.PROFANE_HYDRA
                || item == LolShopItem.LOCKET_OF_THE_IRON_SOLARI
                || item == LolShopItem.SHURELYAS_BATTLESONG
                || item == LolShopItem.REDEMPTION;
    }

    private static String useActive(ServerPlayer player, LolShopItem item) {
        long now = System.currentTimeMillis();
        long remaining = ACTIVE_COOLDOWNS.remainingMillis(player.getUUID(), item, now);
        if (remaining > 0) return String.format(java.util.Locale.ROOT, "%s 재사용 %.1f초",
                item.displayName(), remaining / 1000.0);
        if (item == LolShopItem.ZHONYAS_HOURGLASS) {
            ZHONYA_UNTIL.put(player.getUUID(), now + 2_500);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 255, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50, 255, false, false));
            ACTIVE_COOLDOWNS.start(player.getUUID(), item, now, 120_000);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.7f);
            return "존야의 모래시계: 경직 2.5초";
        }
        if (item == LolShopItem.YOUMUUS_GHOSTBLADE) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 2, false, false));
            ACTIVE_COOLDOWNS.start(player.getUUID(), item, now, 45_000);
            return "요우무의 유령검: 이동 속도 증가";
        }
        if (item == LolShopItem.PROFANE_HYDRA) {
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(4.0), target -> target != player && target.isAlive());
            for (LivingEntity target : targets) {
                float missing = 1.0f - target.getHealth() / target.getMaxHealth();
                extraPhysical(player, target, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.8f + missing * 0.8f));
            }
            ACTIVE_COOLDOWNS.start(player.getUUID(), item, now, 10_000);
            return "불경한 히드라: 광역 참격";
        }
        if (item == LolShopItem.LOCKET_OF_THE_IRON_SOLARI) {
            for (ServerPlayer ally : player.level().getEntitiesOfClass(ServerPlayer.class,
                    player.getBoundingBox().inflate(8.0), LivingEntity::isAlive)) {
                ally.setAbsorptionAmount(Math.max(ally.getAbsorptionAmount(), 5.0f));
                applyArdentBuff(player, ally);
            }
            ACTIVE_COOLDOWNS.start(player.getUUID(), item, now, 90_000);
            return "강철의 솔라리 펜던트: 광역 보호막";
        }
        if (item == LolShopItem.SHURELYAS_BATTLESONG) {
            for (ServerPlayer ally : player.level().getEntitiesOfClass(ServerPlayer.class,
                    player.getBoundingBox().inflate(8.0), LivingEntity::isAlive))
                ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 2, false, false));
            ACTIVE_COOLDOWNS.start(player.getUUID(), item, now, 75_000);
            return "슈렐리아의 군가: 광역 이동 속도 증가";
        }
        if (item == LolShopItem.REDEMPTION) {
            float healing = (float) (6.0 * (1.0 + PlayerEconomy.healAndShieldPower(player)));
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(8.0), LivingEntity::isAlive)) {
                if (target == player || target instanceof ServerPlayer targetPlayer
                        && SupportItemRules.isPlayerAlly(ChampionManager.mode(player),
                        MatchManager.team(player), MatchManager.team(targetPlayer))) {
                    target.heal(healing);
                    if (target instanceof ServerPlayer ally) applyArdentBuff(player, ally);
                } else {
                    extraMagic(player, target, 3.0f);
                }
            }
            redemptionRing(player.level(), player);
            ACTIVE_COOLDOWNS.start(player.getUUID(), item, now, 90_000);
            return "구원: 사용자 중심 아군 회복 및 적 피해";
        }
        return "사용할 수 있는 액티브 아이템이 없습니다";
    }

    private static void applyArdentBuff(ServerPlayer owner, ServerPlayer ally) {
        if (!PlayerEconomy.owns(owner, LolShopItem.ARDENT_CENSER)) return;
        ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0, false, false));
        ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, false, false));
    }

    public static boolean allowDamage(LivingEntity target) {
        long now = System.currentTimeMillis();
        if (ZHONYA_UNTIL.getOrDefault(target.getUUID(), 0L) > now) return false;
        if (target instanceof ServerPlayer player
                && PlayerEconomy.owns(player, LolShopItem.EDGE_OF_NIGHT)
                && EDGE_OF_NIGHT_RECHARGE.getOrDefault(player.getUUID(), 0L) <= now) {
            EDGE_OF_NIGHT_RECHARGE.put(player.getUUID(), now + 40_000);
            player.level().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1,
                    player.getZ(), 24, 0.5, 0.8, 0.5, 0.08);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
                    SoundSource.PLAYERS, 0.8f, 0.65f);
            return false;
        }
        return true;
    }

    public static void afterDamage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        long now = System.currentTimeMillis();
        long previousCombat = LAST_COMBAT.getOrDefault(target.getUUID(), 0L);
        if (now - previousCombat > 5_000) COMBAT_STARTED.put(target.getUUID(), now);
        LAST_COMBAT.put(target.getUUID(), now);
        if (source.getEntity() instanceof LivingEntity attacker) {
            long attackerPrevious = LAST_COMBAT.getOrDefault(attacker.getUUID(), 0L);
            if (now - attackerPrevious > 5_000) COMBAT_STARTED.put(attacker.getUUID(), now);
            LAST_COMBAT.put(attacker.getUUID(), now);
        }
        if (!(target instanceof ServerPlayer player)) return;
        if (PlayerEconomy.owns(player, LolShopItem.THORNMAIL) && source.getEntity() instanceof LivingEntity attacker
                && attacker != player && REFLECTING.add(player.getUUID())) {
            try {
                CombatEngine.deal(player, attacker, 1.0f + (float) player.getAttributeValue(Attributes.ARMOR) * 0.15f,
                        CombatEngine.DamageKind.MAGIC, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
                attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
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

    /** Applies legendary passives that trigger when a champion damage instance lands. */
    public static void onChampionDamage(ServerPlayer player, LivingEntity target,
                                        float damage, CombatEngine.DamageKind kind) {
        if (!ITEM_PROC.add(player.getUUID())) return;
        long now = System.currentTimeMillis();
        try {
            if (kind == CombatEngine.DamageKind.PHYSICAL && PlayerEconomy.owns(player, LolShopItem.BLACK_CLEAVER))
                applyCleaver(player, target, now);
            if (PlayerEconomy.owns(player, LolShopItem.LIANDRYS_TORMENT))
                LIANDRY_BURNS.put(target.getUUID(), new BurnState(player, target, now + 3_000, now + 1_000));
            if (PlayerEconomy.owns(player, LolShopItem.RYLAIS_CRYSTAL_SCEPTER))
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1, false, false));
            if (kind == CombatEngine.DamageKind.PHYSICAL && PlayerEconomy.owns(player, LolShopItem.SERYLDAS_GRUDGE))
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0, false, false));
            if (PlayerEconomy.owns(player, LolShopItem.IMPERIAL_MANDATE)
                    && target.hasEffect(MobEffects.SLOWNESS)
                    && IMPERIAL_TARGET_COOLDOWN.getOrDefault(target.getUUID(), 0L) <= now) {
                extraMagic(player, target, 2.0f + (float) PlayerEconomy.abilityPower(player) * 0.10f);
                IMPERIAL_TARGET_COOLDOWN.put(target.getUUID(), now + 6_000);
            }
            if (kind == CombatEngine.DamageKind.PHYSICAL && PlayerEconomy.owns(player, LolShopItem.OPPORTUNITY)
                    && OPPORTUNITY_COOLDOWN.getOrDefault(player.getUUID(), 0L) <= now) {
                extraPhysical(player, target, 1.0f + (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.15f);
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
                OPPORTUNITY_COOLDOWN.put(player.getUUID(), now + 8_000);
            }
            if (PlayerEconomy.owns(player, LolShopItem.THE_COLLECTOR)
                    && target.getHealth() <= target.getMaxHealth() * 0.05f)
                extraPhysical(player, target, target.getHealth() + 1.0f);
        } finally {
            ITEM_PROC.remove(player.getUUID());
        }
    }

    public static void onTakedown(ServerPlayer killer, LivingEntity target) {
        long now = System.currentTimeMillis();
        if (TAKEDOWN_GUARD.getOrDefault(target.getUUID(), 0L) > now) return;
        TAKEDOWN_GUARD.put(target.getUUID(), now + 2_000);
        if (PlayerEconomy.owns(killer, LolShopItem.DEATHS_DANCE))
            killer.heal(killer.getMaxHealth() * 0.12f);
        if (PlayerEconomy.owns(killer, LolShopItem.DEATHS_DANCE))
            killer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, false, false));
        if (PlayerEconomy.owns(killer, LolShopItem.AXIOM_ARC))
            ChampionManager.reduceUltimateCooldown(killer, 20_000);
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
        if (PlayerEconomy.owns(player, LolShopItem.PROFANE_HYDRA)) {
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(2.5), entity -> entity != player && entity != target && entity.isAlive()))
                extraPhysical(player, nearby, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.20f);
        }
        if (PlayerEconomy.owns(player, LolShopItem.SUNDERED_SKY)
                && SUNDERED_SKY_TARGET.getOrDefault(target.getUUID(), 0L) <= now) {
            extraPhysical(player, target, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f);
            player.heal(Math.max(1.0f, player.getMaxHealth() * 0.06f));
            SUNDERED_SKY_TARGET.put(target.getUUID(), now + 8_000);
            player.level().sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1,
                    target.getZ(), 20, 0.35, 0.55, 0.35, 0.08);
        }
        double lifeSteal = PlayerEconomy.lifeSteal(player);
        if (lifeSteal > 0) {
            double rawAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double dealtEstimate = rawAttack * CombatEngine.resistanceMultiplier(
                    CombatEngine.resistanceAfterPenetration(player, target, CombatEngine.DamageKind.PHYSICAL));
            player.heal((float) Math.max(0.0, dealtEstimate * lifeSteal));
            if (PlayerEconomy.owns(player, LolShopItem.BLOODTHIRSTER)
                    && player.getHealth() >= player.getMaxHealth() - 0.1f)
                player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), player.getMaxHealth() * 0.08f));
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
        boolean guard = ITEM_PROC.add(player.getUUID());
        try {
            CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.PHYSICAL,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        } finally {
            if (guard) ITEM_PROC.remove(player.getUUID());
        }
    }

    private static void extraMagic(ServerPlayer player, LivingEntity target, float damage) {
        boolean guard = ITEM_PROC.add(player.getUUID());
        try {
            CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.MAGIC,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        } finally {
            if (guard) ITEM_PROC.remove(player.getUUID());
        }
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
        ACTIVE_COOLDOWNS.prune(now);
        ZHONYA_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
        STERAK_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        SUNDERED_SKY_TARGET.entrySet().removeIf(entry -> entry.getValue() <= now);
        OPPORTUNITY_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        IMPERIAL_TARGET_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        TAKEDOWN_GUARD.entrySet().removeIf(entry -> entry.getValue() <= now);
        LIANDRY_BURNS.entrySet().removeIf(entry -> {
            BurnState burn = entry.getValue();
            if (!burn.target.isAlive() || burn.expiresAt <= now) return true;
            if (burn.nextTick <= now) {
                extraMagic(burn.source, burn.target, Math.max(0.5f, burn.target.getMaxHealth() * 0.01f));
                entry.setValue(new BurnState(burn.source, burn.target, burn.expiresAt, now + 1_000));
            }
            return false;
        });
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
            if (PlayerEconomy.owns(player, LolShopItem.KAENIC_ROOKERN)
                    && now - LAST_COMBAT.getOrDefault(player.getUUID(), 0L) >= 12_000)
                player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), player.getMaxHealth() * 0.18f));
            if (PlayerEconomy.owns(player, LolShopItem.JAKSHO)
                    && now - LAST_COMBAT.getOrDefault(player.getUUID(), now) <= 5_000
                    && now - COMBAT_STARTED.getOrDefault(player.getUUID(), now) >= 5_000)
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, false, false));
            if (PlayerEconomy.owns(player, LolShopItem.KNIGHTS_VOW)) {
                UUID targetId = PlayerEconomy.knightsVowTarget(player);
                ServerPlayer ally = targetId == null ? null : server.getPlayerList().getPlayer(targetId);
                if (ally != null && ally.isAlive() && ally.level() == player.level()
                        && player.distanceToSqr(ally) <= 32.0 * 32.0
                        && SupportItemRules.isPlayerAlly(ChampionManager.mode(player),
                        MatchManager.team(player), MatchManager.team(ally))) {
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, false, false));
                    if (now - LAST_COMBAT.getOrDefault(ally.getUUID(), 0L) <= 2_000) player.heal(0.25f);
                }
            }
        }
    }

    private static void redemptionRing(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 64; i++) {
            double angle = Math.PI * 2.0 * i / 64.0;
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + Math.cos(angle) * 8.0, player.getY() + 0.12,
                    player.getZ() + Math.sin(angle) * 8.0, 1, 0, 0.02, 0, 0);
        }
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.8, player.getZ(),
                70, 4.0, 0.7, 4.0, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 0.8f, 1.15f);
    }

    private record RuinedKingState(UUID attacker, int hits, long expiresAt) {}
    private record CleaverState(LivingEntity target, int stacks, long expiresAt) {}
    private record BurnState(ServerPlayer source, LivingEntity target, long expiresAt, long nextTick) {}
}
