package kr.darius.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.mojang.math.Transformation;
import kr.darius.skills.mixin.DisplayAccessor;
import kr.darius.skills.mixin.ItemDisplayAccessor;
import kr.darius.skills.mixin.ArmorStandAccessor;
import kr.darius.skills.combat.CombatEngine;
import kr.darius.skills.combat.CriticalStrikeEngine;
import kr.darius.skills.shop.LegendaryItemEffects;
import kr.darius.skills.shop.PlayerEconomy;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** PC League-inspired Yone P/Q/W/E/R implementation for the shared Z/X/C/V input layer. */
public final class YoneSkills {
    private static final float FULL_ATTACK_STRENGTH = 0.9f;
    private static final long Q_STACK_DURATION_MS = 6_000;
    private static final long E_DURATION_MS = 5_000;
    private static final long E_MIN_RECAST_MS = 500;
    private static final double R_RANGE = 11.5;
    private static final double R_WIDTH = 2.25;
    private static final DustParticleOptions STEEL = new DustParticleOptions(0x91A8B8, 1.15f);
    private static final DustParticleOptions STEEL_GLOW = new DustParticleOptions(0xDDE8EE, 1.75f);
    private static final DustParticleOptions STORM_CORE = new DustParticleOptions(0x263A48, 1.7f);
    private static final DustParticleOptions WIND = new DustParticleOptions(0xAAB7BE, 1.35f);
    private static final DustParticleOptions SPIRIT = new DustParticleOptions(0x9A102F, 1.65f);
    private static final DustParticleOptions CRIMSON_GLOW = new DustParticleOptions(0xE02B48, 2.0f);
    private static final DustParticleOptions AZAKANA = new DustParticleOptions(0x21030D, 1.9f);
    private static final DustParticleOptions VOID = new DustParticleOptions(0x08070D, 2.1f);
    private static final ResourceKey<DamageType> SOUL_UNBOUND_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("darius_skills", "soul_unbound"));

    private static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    private static final Map<UUID, QState> Q_STATES = new HashMap<>();
    private static final List<SmoothDash> Q_DASHES = new ArrayList<>();
    private static final Map<UUID, EState> E_STATES = new HashMap<>();
    private static final Map<UUID, Boolean> E_RETURN_WARNED = new HashMap<>();
    private static final Map<UUID, ShieldVfx> SHIELD_VFX = new HashMap<>();
    private static final List<FateTrailVfx> FATE_TRAILS = new ArrayList<>();
    private static final Map<UUID, Long> ACTION_LOCK_UNTIL = new HashMap<>();
    private static final Map<UUID, PositionLock> W_POSITION_LOCKS = new HashMap<>();
    private static final Map<UUID, Long> Q_POSE_UNTIL = new HashMap<>();
    private static final Map<UUID, Boolean> SECOND_BLADE = new HashMap<>();
    private static final List<PendingCast> PENDING_CASTS = new ArrayList<>();
    private static final List<UltEchoSound> ULT_ECHO_SOUNDS = new ArrayList<>();

    private YoneSkills() {}

    public static void equip(ServerPlayer player) {
        installDualBlades(player);
        player.getInventory().setSelectedSlot(0);
    }

    public static boolean isYoneWeapon(ItemStack stack) {
        return stack.is(DariusSkills.YONE_STEEL_SWORD) || stack.is(DariusSkills.YONE_AZAKANA_SWORD);
    }

    private static void installDualBlades(ServerPlayer player) {
        ItemStack displacedMain = player.getInventory().getItem(0);
        ItemStack displacedOffhand = player.getOffhandItem();
        if (!displacedMain.isEmpty() && !isYoneWeapon(displacedMain))
            player.getInventory().add(displacedMain.copy());
        if (!displacedOffhand.isEmpty() && !isYoneWeapon(displacedOffhand))
            player.getInventory().add(displacedOffhand.copy());
        removeYoneWeapons(player);
        player.getInventory().setItem(0, new ItemStack(DariusSkills.YONE_AZAKANA_SWORD));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DariusSkills.YONE_STEEL_SWORD));
    }

    private static void enforceDualBlades(ServerPlayer player) {
        boolean correct = player.getInventory().getItem(0).is(DariusSkills.YONE_AZAKANA_SWORD)
                && player.getOffhandItem().is(DariusSkills.YONE_STEEL_SWORD);
        if (!correct) installDualBlades(player);
    }

    public static boolean isBodyEcho(Entity entity) {
        for (EState state : E_STATES.values())
            if (state.bodyEcho == entity) return true;
        return false;
    }

    private static void removeYoneWeapons(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (isYoneWeapon(player.getInventory().getItem(slot)))
                player.getInventory().setItem(slot, ItemStack.EMPTY);
        if (isYoneWeapon(player.getOffhandItem()))
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    public static void reset(ServerPlayer player) {
        LAST_CAST.put(player.getUUID(), new long[5]);
        Q_STATES.remove(player.getUUID());
        Q_DASHES.removeIf(dash -> dash.player == player);
        EState spirit = E_STATES.remove(player.getUUID());
        E_RETURN_WARNED.remove(player.getUUID());
        if (spirit != null) returnToBody(player, spirit);
        SHIELD_VFX.remove(player.getUUID());
        ACTION_LOCK_UNTIL.remove(player.getUUID());
        W_POSITION_LOCKS.remove(player.getUUID());
        Q_POSE_UNTIL.remove(player.getUUID());
        PENDING_CASTS.removeIf(cast -> cast.player == player);
        ULT_ECHO_SOUNDS.removeIf(sound -> sound.player == player);
        SECOND_BLADE.remove(player.getUUID());
        player.stopUsingItem();
        player.removeEffect(MobEffects.SLOWNESS);
        removeYoneWeapons(player);
    }

    public static void cast(ServerPlayer player, int wireSkill) {
        if (!ChampionManager.isYone(player) || isActionLocked(player)) return;
        switch (wireSkill) {
            case 1 -> q(player);
            case 4 -> w(player);
            case 2 -> e(player);
            case 3 -> r(player);
            default -> { }
        }
    }

    /** Returns true when the vanilla attack must be cancelled because the mixed second strike was dealt manually. */
    public static boolean basicAttack(ServerPlayer player, LivingEntity target) {
        if (isActionLocked(player) || player.getAttackStrengthScale(0.5f) < FULL_ATTACK_STRENGTH) return false;
        CriticalStrikeEngine.Roll critical = CriticalStrikeEngine.rollAttack(player);
        boolean spiritBlade = SECOND_BLADE.getOrDefault(player.getUUID(), false);
        SECOND_BLADE.put(player.getUUID(), !spiritBlade);
        if (!spiritBlade) {
            float total = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    * critical.damageMultiplier();
            CombatEngine.deal(player, target, total, CombatEngine.DamageKind.PHYSICAL,
                    CombatEngine.KnockbackPolicy.VANILLA);
            markSpiritDamage(player, target, total);
            playBladeSwing(player, InteractionHand.OFF_HAND);
            steelBasicAttackVfx(player.level(), player, target, critical.critical());
            player.level().playSound(null, target.blockPosition(), critical.critical()
                            ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 0.68f, 1.15f);
            player.resetAttackStrengthTicker();
            return true;
        }

        float total = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * critical.damageMultiplier();
        CombatEngine.deal(player, target, total * 0.5f, CombatEngine.DamageKind.PHYSICAL,
                CombatEngine.KnockbackPolicy.VANILLA);
        CombatEngine.deal(player, target, total * 0.5f, CombatEngine.DamageKind.MAGIC,
                CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        markSpiritDamage(player, target, total);
        playBladeSwing(player, InteractionHand.MAIN_HAND);
        azakanaBasicAttackVfx(player.level(), player, target, critical.critical());
        player.level().playSound(null, target.blockPosition(), critical.critical()
                        ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.7f, 1.45f);
        player.resetAttackStrengthTicker();
        return true;
    }

    public static boolean blocksBasicAttack(ServerPlayer player) {
        return ChampionManager.isYone(player) && isActionLocked(player);
    }

    private static void q(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(player, cast, 1, now)) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        QState state = Q_STATES.get(player.getUUID());
        boolean tornado = state != null && state.expiresAt > now && state.stacks >= 2;
        LegendaryItemEffects.onSkillInput(player);
        cast[1] = now;
        player.startUsingItem(InteractionHand.OFF_HAND);
        Q_POSE_UNTIL.put(player.getUUID(), now + 140);
        drawQTelegraph(player.level(), player.position(), forward, tornado);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.55f, tornado ? 0.75f : 1.45f);
        // Q damage, dash and crowd control resolve on the input tick. The short
        // off-hand use state only preserves the steel-sword spear pose visually.
        executeQ(new PendingCast(player, Skill.Q, forward, now, tornado));
    }

    private static void executeQ(PendingCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        playBladeSwing(player, InteractionHand.OFF_HAND);
        int rank = rank(player, 1, 5);
        float baseDamage = qBase(rank);
        float attackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.05f;
        double reach = cast.empowered ? 10.5 : 4.5;
        double width = cast.empowered ? 1.6 : 0.8;
        List<LivingEntity> targets = lineTargets(player, player.position(), cast.forward, reach, width);
        for (LivingEntity target : targets) {
            CriticalStrikeEngine.Roll critical = CriticalStrikeEngine.rollAttack(player);
            float damage = baseDamage + attackDamage * critical.damageMultiplier();
            if (CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.PHYSICAL,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT)) {
                markSpiritDamage(player, target, damage);
                if (critical.critical())
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0,
                            target.getZ(), 22, 0.38, 0.58, 0.38, 0.07);
                if (cast.empowered) {
                    CrowdControl.apply(target, CrowdControl.Type.AIRBORNE, 750);
                    target.setDeltaMovement(target.getDeltaMovement().x, 0.72, target.getDeltaMovement().z);
                    target.hurtMarked = true;
                    airborneWindColumn(level, target);
                }
            }
        }
        if (cast.empowered) {
            Q_DASHES.removeIf(dash -> dash.player == player);
            Q_DASHES.add(new SmoothDash(player, cast.forward, 6));
            lock(player, 300);
            Q_STATES.remove(player.getUUID());
            windTrail(level, player.position(), cast.forward, reach);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 0.9f, 0.85f);
        } else if (!targets.isEmpty()) {
            QState current = Q_STATES.get(player.getUUID());
            int stacks = current != null && current.expiresAt > System.currentTimeMillis() ? current.stacks : 0;
            Q_STATES.put(player.getUUID(), new QState(Math.min(2, stacks + 1),
                    System.currentTimeMillis() + Q_STACK_DURATION_MS));
            qStackCloud(level, player, Math.min(2, stacks + 1));
        }
        steelLine(level, player.position(), cast.forward, reach);
    }

    private static void w(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(player, cast, 4, now)) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        long castTime = scaledCastTime(player, 500, 190);
        cast[4] = now;
        lock(player, castTime);
        W_POSITION_LOCKS.put(player.getUUID(), new PositionLock(player.position(), now + castTime));
        PENDING_CASTS.add(new PendingCast(player, Skill.W, forward, now + castTime, false));
    }

    private static void executeW(PendingCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        W_POSITION_LOCKS.remove(player.getUUID());
        player.swing(InteractionHand.MAIN_HAND, true);
        int rank = rank(player, 2, 5);
        List<LivingEntity> targets = coneTargets(player, cast.forward, 5.5, 0.0);
        int hits = 0;
        for (LivingEntity target : targets) {
            float total = wBase(rank) + target.getMaxHealth() * wHealthRatio(rank);
            boolean physical = CombatEngine.deal(player, target, total * 0.5f,
                    CombatEngine.DamageKind.PHYSICAL, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
            CombatEngine.deal(player, target, total * 0.5f,
                    CombatEngine.DamageKind.MAGIC, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
            if (physical) {
                markSpiritDamage(player, target, total);
                hits++;
            }
        }
        if (hits > 0) {
            int amplifier = Math.min(4, Math.max(0, hits - 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 30, amplifier, false, false));
            shieldSphere(level, player, hits);
            SHIELD_VFX.put(player.getUUID(), new ShieldVfx(player, hits, System.currentTimeMillis() + 1_500));
        }
        deepRedCleave(level, player, cast.forward, 5.5);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + cast.forward.x * 2.2,
                player.getY() + 1.0, player.getZ() + cast.forward.z * 2.2, 12, 1.5, 0.5, 1.5, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 0.72f);
    }

    private static void e(ServerPlayer player) {
        long now = System.currentTimeMillis();
        EState active = E_STATES.get(player.getUUID());
        if (active != null) {
            if (now - active.startedAt < E_MIN_RECAST_MS) return;
            E_STATES.remove(player.getUUID());
            E_RETURN_WARNED.remove(player.getUUID());
            returnToBody(player, active);
            return;
        }
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(player, cast, 2, now)) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        Vec3 body = player.position();
        ArmorStand bodyEcho = createBodyEcho(player.level(), player, body);
        E_STATES.put(player.getUUID(), new EState(body, now, now + E_DURATION_MS, new HashMap<>(), bodyEcho));
        E_RETURN_WARNED.remove(player.getUUID());
        SECOND_BLADE.put(player.getUUID(), false);
        cast[2] = now;
        lock(player, 225);
        dash(player, forward, 3.0);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 105, 1, false, false));
        spiritBurst(player.level(), body, 55);
        spiritAfterimage(player.level(), player, 1.0f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8f, 1.35f);
    }

    private static void r(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(player, cast, 3, now)) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        cast[3] = now;
        lock(player, 1_200);
        PENDING_CASTS.add(new PendingCast(player, Skill.R, forward, now + 750, false));
        drawUltTelegraph(player.level(), player.position(), forward, 0.0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_HIT_GROUND,
                SoundSource.PLAYERS, 0.75f, 1.5f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.45f, 0.7f);
    }

    private static void executeR(PendingCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        player.swing(InteractionHand.MAIN_HAND, true);
        player.swing(InteractionHand.OFF_HAND, true);
        Vec3 origin = player.position();
        List<LivingEntity> targets = lineTargets(player, origin, cast.forward, R_RANGE, R_WIDTH);
        Vec3 destination = origin.add(cast.forward.scale(R_RANGE));
        if (!targets.isEmpty()) {
            LivingEntity furthest = targets.stream().max(Comparator.comparingDouble(
                    target -> target.position().subtract(origin).dot(cast.forward))).orElse(targets.getFirst());
            destination = furthest.position().add(cast.forward.scale(2.0));
        }
        teleportSafely(player, destination);

        int rank = rank(player, 4, 3);
        float bonusAttack = (float) Math.max(0.0,
                player.getAttributeValue(Attributes.ATTACK_DAMAGE) - PlayerEconomy.attackDamage(player));
        float total = rBase(rank) + bonusAttack * 0.8f;
        Vec3 gather = player.position().subtract(cast.forward.scale(0.8));
        for (LivingEntity target : targets) {
            CombatEngine.deal(player, target, total * 0.5f, CombatEngine.DamageKind.MAGIC,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
            CombatEngine.deal(player, target, total * 0.5f, CombatEngine.DamageKind.PHYSICAL,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
            target.teleportTo(gather.x, gather.y, gather.z);
            CrowdControl.apply(target, CrowdControl.Type.AIRBORNE, 750);
            CrowdControl.apply(target, CrowdControl.Type.STUN, 1_000);
            target.setDeltaMovement(0, 0.75, 0);
            target.hurtMarked = true;
            markSpiritDamage(player, target, total);
        }
        fateSealedTrail(level, origin, cast.forward, R_RANGE, 1.0);
        FATE_TRAILS.add(new FateTrailVfx(level, origin, cast.forward,
                System.currentTimeMillis(), System.currentTimeMillis() + 650));
        vacuumBurst(level, gather, cast.forward, targets.size());
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, gather.x, gather.y + 1.0, gather.z,
                8, 0.9, 0.65, 0.9, 0.025);
        level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_THROW,
                SoundSource.PLAYERS, 1.0f, 0.62f);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.9f, 0.92f);
        Vec3 echoPosition = player.position();
        long echoStart = System.currentTimeMillis();
        ULT_ECHO_SOUNDS.add(new UltEchoSound(player, level, echoPosition, echoStart + 180, 0.32f, 0.72f));
        ULT_ECHO_SOUNDS.add(new UltEchoSound(player, level, echoPosition, echoStart + 420, 0.18f, 0.58f));
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Q_STATES.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        ACTION_LOCK_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
        W_POSITION_LOCKS.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        Iterator<SmoothDash> dashes = Q_DASHES.iterator();
        while (dashes.hasNext()) {
            SmoothDash dash = dashes.next();
            if (!dash.player.isAlive() || !ChampionManager.isYone(dash.player)
                    || !advanceDash(dash.player, dash.forward, 0.75)) {
                dashes.remove();
                continue;
            }
            dash.ticksRemaining--;
            if (dash.ticksRemaining <= 0) dashes.remove();
        }
        Iterator<UltEchoSound> echoes = ULT_ECHO_SOUNDS.iterator();
        while (echoes.hasNext()) {
            UltEchoSound echo = echoes.next();
            if (now < echo.executeAt) continue;
            if (echo.player.isAlive())
                echo.level.playSound(null, echo.position.x, echo.position.y, echo.position.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, echo.volume, echo.pitch);
            echoes.remove();
        }
        Q_POSE_UNTIL.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) return false;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isUsingItem()
                    && player.getUsedItemHand() == InteractionHand.OFF_HAND
                    && player.getUseItem().is(DariusSkills.YONE_STEEL_SWORD))
                player.stopUsingItem();
            return true;
        });
        SHIELD_VFX.entrySet().removeIf(entry -> {
            ShieldVfx shield = entry.getValue();
            if (!shield.player.isAlive() || shield.expiresAt <= now) return true;
            shieldSphere(shield.player.level(), shield.player, shield.hits);
            return false;
        });
        FATE_TRAILS.removeIf(trail -> {
            if (trail.expiresAt <= now) return true;
            double remaining = (trail.expiresAt - now) / (double) (trail.expiresAt - trail.startedAt);
            fateSealedTrail(trail.level, trail.origin, trail.forward, R_RANGE, remaining);
            return false;
        });

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ChampionManager.isYone(player)) enforceDualBlades(player);
            PositionLock wLock = W_POSITION_LOCKS.get(player.getUUID());
            if (wLock != null && wLock.expiresAt > now) {
                player.teleportTo(wLock.origin.x, wLock.origin.y, wLock.origin.z);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
            }
            QState qState = Q_STATES.get(player.getUUID());
            if (qState != null && qState.expiresAt > now && player.level().getGameTime() % 3 == 0)
                qStackCloud(player.level(), player, qState.stacks);
            if (isActionLocked(player)) {
                Vec3 movement = player.getDeltaMovement();
                player.setDeltaMovement(0, Math.min(0, movement.y), 0);
                player.hurtMarked = true;
            }
        }

        Iterator<PendingCast> pending = PENDING_CASTS.iterator();
        while (pending.hasNext()) {
            PendingCast cast = pending.next();
            if (!cast.player.isAlive() || !ChampionManager.isYone(cast.player)) {
                if (cast.skill == Skill.Q) cast.player.stopUsingItem();
                pending.remove();
                continue;
            }
            if (now < cast.executeAt) {
                if (cast.skill == Skill.R) {
                    double progress = Math.max(0.0, Math.min(1.0,
                            1.0 - (cast.executeAt - now) / 750.0));
                    drawUltTelegraph(cast.player.level(), cast.player.position(), cast.forward, progress);
                    fateChargeVfx(cast.player.level(), cast.player, cast.forward);
                }
                continue;
            }
            if (cast.skill == Skill.Q) executeQ(cast);
            else if (cast.skill == Skill.W) executeW(cast);
            else executeR(cast);
            pending.remove();
        }

        Iterator<Map.Entry<UUID, EState>> spirits = E_STATES.entrySet().iterator();
        while (spirits.hasNext()) {
            var entry = spirits.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                discardSpiritVisuals(entry.getValue());
                E_RETURN_WARNED.remove(entry.getKey());
                spirits.remove();
                continue;
            }
            EState state = entry.getValue();
            double remaining = Math.max(0.0, (state.endsAt - now) / (double) E_DURATION_MS);
            if (state.endsAt - now <= 1_000 && E_RETURN_WARNED.putIfAbsent(entry.getKey(), true) == null)
                player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.PLAYERS, 0.9f, 0.72f);
            drawSpiritLink(player.level(), player.position(), state.origin, remaining);
            spiritAfterimage(player.level(), player, (float) remaining);
            updateSpiritMarks(player, state, now);
            if (now >= state.endsAt) {
                spirits.remove();
                E_RETURN_WARNED.remove(entry.getKey());
                returnToBody(player, state);
            }
        }
    }

    private static void markSpiritDamage(ServerPlayer player, LivingEntity target, float damage) {
        EState state = E_STATES.get(player.getUUID());
        if (state == null || damage <= 0) return;
        state.damage.merge(target.getUUID(), new Mark(target, damage),
                (oldMark, added) -> new Mark(target, oldMark.damage + added.damage));
        SpiritMarkVisual visual = state.visuals.computeIfAbsent(target.getUUID(), ignored ->
                createSpiritMark(player.level(), target));
        updateSpiritMark(player, state, target, visual, System.currentTimeMillis());
        player.level().sendParticles(SPIRIT, target.getX(), target.getY() + 1.8, target.getZ(),
                5, 0.2, 0.2, 0.2, 0.01);
    }

    private static void returnToBody(ServerPlayer player, EState state) {
        int rank = rank(player, 3, 5);
        float repeatRatio = 0.25f + (rank - 1) * 0.025f;
        for (Mark mark : state.damage.values()) {
            if (!mark.target.isAlive()) continue;
            var type = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(SOUL_UNBOUND_DAMAGE);
            DamageSource source = new DamageSource(type, player, player);
            CombatEngine.deal(player, mark.target, source, mark.damage * repeatRatio,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT, true);
            spiritBurst(player.level(), mark.target.position().add(0, 1, 0), 26);
            snapBackSlash(player.level(), mark.target);
        }
        discardSpiritVisuals(state);
        Vec3 from = player.position();
        teleportSafely(player, state.origin);
        drawReturnTrail(player.level(), from, state.origin);
        player.removeEffect(MobEffects.SPEED);
        lock(player, 225);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9f, 0.75f);
    }

    public static void showActionBar(ServerPlayer player, long now) {
        long[] cast = LAST_CAST.get(player.getUUID());
        QState qState = Q_STATES.get(player.getUUID());
        int stacks = qState != null && qState.expiresAt > now ? qState.stacks : 0;
        EState spirit = E_STATES.get(player.getUUID());
        String e = spirit != null ? "§d영혼 " + String.format(Locale.ROOT, "%.1fs", (spirit.endsAt - now) / 1000.0)
                : cooldownText(player, cast, 2, now);
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                "§bZ§f " + cooldownText(player, cast, 1, now) + " §7[Q" + stacks + "]  §8|  §bX§f "
                + cooldownText(player, cast, 4, now) + "  §8|  §bC§f " + e
                + "  §8|  §5§lV§r§f " + cooldownText(player, cast, 3, now))));
    }

    private static String cooldownText(ServerPlayer player, long[] cast, int skill, long now) {
        if (cast == null || cast[skill] == 0) return "§aREADY";
        long left = cooldown(player, skill) - (now - cast[skill]);
        return left <= 0 ? "§aREADY" : "§e" + String.format(Locale.ROOT, "%.1fs", left / 1000.0);
    }

    private static boolean ready(ServerPlayer player, long[] cast, int skill, long now) {
        return cast[skill] == 0 || now - cast[skill] >= cooldown(player, skill);
    }

    private static long cooldown(ServerPlayer player, int skill) {
        return switch (skill) {
            case 1 -> scaledCooldown(player, 4_000, 1_330);
            case 2 -> new long[]{22_000, 19_000, 16_000, 13_000, 10_000}[rank(player, 3, 5) - 1];
            case 3 -> new long[]{120_000, 100_000, 80_000}[rank(player, 4, 3) - 1];
            case 4 -> scaledCooldown(player, 14_000, 6_000);
            default -> 0;
        };
    }

    private static long scaledCooldown(ServerPlayer player, long base, long minimum) {
        double multiplier = Math.max(1.0, 1.0 + PlayerEconomy.attackSpeed(player));
        return Math.max(minimum, (long) (base / multiplier));
    }

    private static long scaledCastTime(ServerPlayer player, long base, long minimum) {
        double multiplier = Math.max(1.0, 1.0 + PlayerEconomy.attackSpeed(player));
        return Math.max(minimum, (long) (base / multiplier));
    }

    private static int rank(ServerPlayer player, int skill, int max) {
        return Math.min(max, Math.max(1, ChampionProgression.get(player).rank(skill)));
    }

    private static float qBase(int rank) { return new float[]{2.0f, 4.5f, 7.0f, 9.5f, 12.0f}[rank - 1]; }
    private static float wBase(int rank) { return new float[]{1, 2, 3, 4, 5}[rank - 1]; }
    private static float wHealthRatio(int rank) { return 0.06f + (rank - 1) * 0.005f; }
    private static float rBase(int rank) { return new float[]{20, 40, 60}[rank - 1]; }

    private static boolean isActionLocked(ServerPlayer player) {
        return ACTION_LOCK_UNTIL.getOrDefault(player.getUUID(), 0L) > System.currentTimeMillis();
    }

    private static void lock(ServerPlayer player, long durationMs) {
        ACTION_LOCK_UNTIL.merge(player.getUUID(), System.currentTimeMillis() + durationMs, Math::max);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 0.001 ? null : look.normalize();
    }

    private static void dash(ServerPlayer player, Vec3 forward, double distance) {
        Vec3 destination = player.position().add(forward.scale(distance));
        teleportSafely(player, destination);
    }

    private static boolean advanceDash(ServerPlayer player, Vec3 forward, double distance) {
        Vec3 destination = player.position().add(forward.scale(distance));
        Vec3 delta = destination.subtract(player.position());
        if (!player.level().noCollision(player, player.getBoundingBox().move(delta))) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        return true;
    }

    private static void teleportSafely(ServerPlayer player, Vec3 destination) {
        Vec3 delta = destination.subtract(player.position());
        if (player.level().noCollision(player, player.getBoundingBox().move(delta)))
            player.teleportTo(destination.x, destination.y, destination.z);
    }

    private static void playBladeSwing(ServerPlayer player, InteractionHand hand) {
        player.swing(hand, true);
        if (ServerPlayNetworking.canSend(player, YoneAttackAnimationPayload.TYPE))
            ServerPlayNetworking.send(player,
                    new YoneAttackAnimationPayload(hand == InteractionHand.OFF_HAND));
    }

    private static void steelBasicAttackVfx(ServerLevel level, ServerPlayer player, LivingEntity target,
                                             boolean critical) {
        Vec3 from = player.position().add(0, 1.15, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.55, 0);
        int multiplier = critical ? 3 : 2;
        drawParticleLine(level, from, to, STEEL, 9, multiplier);
        level.sendParticles(STEEL_GLOW, to.x, to.y, to.z, critical ? 24 : 14,
                critical ? 0.42 : 0.28, critical ? 0.52 : 0.35, critical ? 0.42 : 0.28, 0.035);
        level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, critical ? 20 : 12,
                critical ? 0.38 : 0.25, critical ? 0.52 : 0.35, critical ? 0.38 : 0.25, 0.08);
    }

    private static void azakanaBasicAttackVfx(ServerLevel level, ServerPlayer player, LivingEntity target,
                                               boolean critical) {
        Vec3 from = player.position().add(0, 1.25, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 right = to.subtract(from).cross(new Vec3(0, 1, 0)).normalize().scale(0.28);
        drawParticleLine(level, from.add(right), to.add(right), SPIRIT, 11, critical ? 5 : 3);
        drawParticleLine(level, from.subtract(right), to.subtract(right), AZAKANA, 11, critical ? 3 : 2);
        level.sendParticles(CRIMSON_GLOW, to.x, to.y, to.z, critical ? 32 : 20,
                critical ? 0.6 : 0.4, critical ? 0.75 : 0.5, critical ? 0.6 : 0.4, 0.045);
        level.sendParticles(ParticleTypes.SMOKE, to.x, to.y, to.z, critical ? 24 : 16,
                critical ? 0.54 : 0.36, critical ? 0.68 : 0.45, critical ? 0.54 : 0.36, 0.035);
    }

    private static void drawParticleLine(ServerLevel level, Vec3 from, Vec3 to,
                                         DustParticleOptions dust, int steps, int count) {
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double) steps));
            level.sendParticles(dust, point.x, point.y, point.z, count, 0.04, 0.04, 0.04, 0);
        }
    }

    private static void qStackCloud(ServerLevel level, ServerPlayer player, int stacks) {
        double time = level.getGameTime() * (0.22 + stacks * 0.06);
        int points = 8 + stacks * 6;
        for (int i = 0; i < points; i++) {
            double angle = time + Math.PI * 2 * i / points;
            double radius = 0.55 + stacks * 0.16 + Math.sin(time + i) * 0.08;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(i % 3 == 0 ? STORM_CORE : WIND, x, player.getY() + 0.12, z,
                    1, 0.06, 0.03, 0.06, 0);
        }
    }

    private static void airborneWindColumn(ServerLevel level, LivingEntity target) {
        for (double y = 0.0; y <= 3.2; y += 0.24) {
            double angle = y * 4.8 + level.getGameTime() * 0.2;
            double radius = 0.75 - Math.min(0.45, y * 0.11);
            for (int strand = 0; strand < 3; strand++) {
                double a = angle + strand * Math.PI * 2 / 3;
                double x = target.getX() + Math.cos(a) * radius;
                double z = target.getZ() + Math.sin(a) * radius;
                level.sendParticles(strand == 0 ? STORM_CORE : WIND, x, target.getY() + y, z,
                        2, 0.05, 0.08, 0.05, 0.015);
            }
        }
    }

    private static void deepRedCleave(ServerLevel level, ServerPlayer player, Vec3 forward, double reach) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (int angle = -90; angle <= 90; angle += 3) {
            double radians = Math.toRadians(angle);
            Vec3 direction = forward.scale(Math.cos(radians)).add(right.scale(Math.sin(radians)));
            for (double radius = 2.0; radius <= reach; radius += 0.55) {
                Vec3 p = player.position().add(direction.scale(radius)).add(0, 0.65 + radius * 0.08, 0);
                DustParticleOptions color = radius > reach - 0.8 ? AZAKANA : SPIRIT;
                level.sendParticles(color, p.x, p.y, p.z, radius > reach - 0.8 ? 3 : 1,
                        0.05, 0.09, 0.05, 0);
            }
        }
    }

    private static void shieldSphere(ServerLevel level, ServerPlayer player, int hits) {
        int points = Math.min(52, 18 + hits * 7);
        double time = level.getGameTime() * 0.18;
        double radius = 1.0 + Math.min(0.35, hits * 0.07);
        DustParticleOptions bright = hits >= 3 ? CRIMSON_GLOW : SPIRIT;
        for (int i = 0; i < points; i++) {
            double phi = Math.acos(1 - 2 * (i + 0.5) / points);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i + time;
            double x = player.getX() + Math.cos(theta) * Math.sin(phi) * radius;
            double y = player.getY() + 1.0 + Math.cos(phi) * radius;
            double z = player.getZ() + Math.sin(theta) * Math.sin(phi) * radius;
            level.sendParticles(i % 4 == 0 ? bright : AZAKANA, x, y, z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static ArmorStand createBodyEcho(ServerLevel level, ServerPlayer player, Vec3 origin) {
        ArmorStand body = new ArmorStand(level, origin.x, origin.y, origin.z);
        body.setNoGravity(true);
        body.setInvulnerable(true);
        ((ArmorStandAccessor) body).darius$setMarker(true);
        body.setSilent(true);
        body.setShowArms(true);
        body.setNoBasePlate(true);
        body.addTag("lol_yone_body_echo");
        body.setYRot(player.getYRot());
        body.setYBodyRot(player.getYRot());
        body.setYHeadRot(player.getYHeadRot());
        body.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        body.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        body.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        body.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        body.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DariusSkills.YONE_AZAKANA_SWORD));
        body.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DariusSkills.YONE_STEEL_SWORD));
        level.addFreshEntity(body);
        return body;
    }

    private static void spiritAfterimage(ServerLevel level, ServerPlayer player, float remaining) {
        Vec3 backward = flatLook(player);
        if (backward == null) backward = new Vec3(0, 0, 1);
        backward = backward.scale(-0.45);
        int trails = 3;
        for (int i = 1; i <= trails; i++) {
            Vec3 point = player.position().add(backward.scale(i)).add(0, 1.0, 0);
            int count = Math.max(2, (int) (7 * remaining));
            level.sendParticles(i == trails ? AZAKANA : SPIRIT, point.x, point.y, point.z,
                    count, 0.25, 0.65, 0.25, 0.02);
        }
    }

    private static SpiritMarkVisual createSpiritMark(ServerLevel level, LivingEntity target) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
        ((ItemDisplayAccessor) display).darius$setItemStack(new ItemStack(Items.REDSTONE));
        ((DisplayAccessor) display).darius$setTransformation(new Transformation(
                new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(0.55f, 0.55f, 0.55f), new Quaternionf()));
        display.setGlowingTag(true);
        display.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.65, target.getZ());
        level.addFreshEntity(display);
        return new SpiritMarkVisual(display, target);
    }

    private static void updateSpiritMarks(ServerPlayer player, EState state, long now) {
        Iterator<Map.Entry<UUID, SpiritMarkVisual>> iterator = state.visuals.entrySet().iterator();
        while (iterator.hasNext()) {
            SpiritMarkVisual visual = iterator.next().getValue();
            if (!visual.target.isAlive()) {
                visual.display.discard();
                iterator.remove();
                continue;
            }
            updateSpiritMark(player, state, visual.target, visual, now);
        }
    }

    private static void updateSpiritMark(ServerPlayer player, EState state, LivingEntity target,
                                         SpiritMarkVisual visual, long now) {
        Mark mark = state.damage.get(target.getUUID());
        if (mark == null) return;
        int rank = rank(player, 3, 5);
        float stored = mark.damage * (0.25f + (rank - 1) * 0.025f);
        boolean execute = stored >= target.getHealth();
        ((ItemDisplayAccessor) visual.display).darius$setItemStack(
                new ItemStack(execute ? Items.NETHER_WART : Items.REDSTONE));
        double pulse = 0.5 + 0.5 * Math.sin(now * (execute ? 0.025 : 0.010));
        float scale = (float) ((execute ? 1.1 : 0.55) + pulse * (execute ? 0.5 : 0.12));
        Quaternionf rotation = new Quaternionf().rotateY((float) (now * (execute ? 0.008 : 0.003)));
        ((DisplayAccessor) visual.display).darius$setTransformation(new Transformation(
                new Vector3f(0, 0, 0), rotation, new Vector3f(scale, scale, scale), new Quaternionf()));
        visual.display.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.65, target.getZ());
        if (execute && target.level() instanceof ServerLevel level) {
            level.sendParticles(CRIMSON_GLOW, target.getX(), target.getY() + target.getBbHeight() + 0.65,
                    target.getZ(), 8, 0.4, 0.35, 0.4, 0.04);
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() + 0.65,
                    target.getZ(), 5, 0.32, 0.28, 0.32, 0.025);
        }
    }

    private static void discardSpiritVisuals(EState state) {
        if (state.bodyEcho != null) state.bodyEcho.discard();
        state.visuals.values().forEach(visual -> visual.display.discard());
        state.visuals.clear();
    }

    private static void snapBackSlash(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.55, 0);
        for (int i = -8; i <= 8; i++) {
            double offset = i * 0.13;
            level.sendParticles(i % 2 == 0 ? CRIMSON_GLOW : SPIRIT,
                    center.x + offset, center.y + offset * 0.7, center.z, 3, 0.08, 0.08, 0.08, 0);
            level.sendParticles(AZAKANA, center.x - offset, center.y + offset * 0.7, center.z,
                    2, 0.06, 0.06, 0.06, 0);
        }
    }

    private static void fateChargeVfx(ServerLevel level, ServerPlayer player, Vec3 forward) {
        Vec3 center = player.position().add(0, 1.1, 0);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + level.getGameTime() * 0.14;
            Vec3 offset = right.scale(Math.cos(angle) * 0.62).add(0, Math.sin(angle) * 0.7, 0)
                    .add(forward.scale(Math.sin(angle) * 0.24));
            DustParticleOptions color = i % 3 == 0 ? STEEL_GLOW : i % 2 == 0 ? CRIMSON_GLOW : SPIRIT;
            level.sendParticles(color, center.x + offset.x, center.y + offset.y, center.z + offset.z,
                    1, 0.025, 0.025, 0.025, 0);
        }
    }

    private static void fateSealedTrail(ServerLevel level, Vec3 origin, Vec3 forward,
                                        double reach, double intensity) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        boolean impactFrame = intensity > 0.68;
        double step = impactFrame ? 0.22 : 0.42;
        for (double d = 0.25; d <= reach; d += step) {
            double t = d / reach;
            Vec3 base = origin.add(forward.scale(d)).add(0, 0.18 + Math.sin(t * Math.PI) * 0.24, 0);

            // The two bright rails are the readable silhouette of Fate Sealed.
            for (double side : new double[]{-1.72, 1.72}) {
                for (double width : impactFrame ? new double[]{-0.16, 0.0, 0.16} : new double[]{0.0}) {
                    Vec3 rail = base.add(right.scale(side + width));
                    level.sendParticles(width == 0.0 ? CRIMSON_GLOW : SPIRIT,
                            rail.x, rail.y, rail.z, impactFrame ? 2 : 1,
                            0.055, 0.075, 0.055, impactFrame ? 0.008 : 0);
                }
            }

            if (impactFrame) {
                // Parallel black/crimson cuts replace the old X-shaped or filled rectangle look.
                for (int slash = -2; slash <= 2; slash++) {
                    double side = slash * 0.48 + Math.sin(t * Math.PI * 3 + slash) * 0.08;
                    Vec3 cut = base.add(right.scale(side)).add(0, 0.12 + Math.abs(slash) * 0.07, 0);
                    level.sendParticles(slash % 2 == 0 ? AZAKANA : VOID,
                            cut.x, cut.y, cut.z, 3, 0.075, 0.13, 0.075, 0.012);
                    if (slash == -1 || slash == 1)
                        level.sendParticles(SPIRIT, cut.x, cut.y + 0.08, cut.z,
                                1, 0.04, 0.06, 0.04, 0);
                }

                if (((int) (d * 10)) % 8 == 0) {
                    Vec3 steel = base.add(right.scale(-0.12)).add(0, 0.3, 0);
                    level.sendParticles(STEEL_GLOW, steel.x, steel.y, steel.z,
                            1, 0.025, 0.035, 0.025, 0);
                }
            }
            if (!impactFrame && ((int) (d * 10)) % 9 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, base.x, base.y + 0.12, base.z,
                        intensity > 0.28 ? 3 : 1, 0.85, 0.20, 0.85, 0.012);
                level.sendParticles(AZAKANA, base.x, base.y + 0.08, base.z,
                        1, 0.42, 0.08, 0.42, 0);
            }
        }
    }

    private static void vacuumBurst(ServerLevel level, Vec3 center, Vec3 forward, int targets) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        int arcs = 3 + Math.min(2, targets);

        // Curved soul blades fold inward around the gathered targets.
        for (int arc = 0; arc < arcs; arc++) {
            double radius = 1.25 + arc * 0.34;
            for (int angle = -135; angle <= 135; angle += 9) {
                double radians = Math.toRadians(angle + arc * 7);
                Vec3 point = center.add(right.scale(Math.sin(radians) * radius))
                        .add(forward.scale(Math.cos(radians) * radius * 0.62))
                        .add(0, 0.35 + arc * 0.16 + Math.sin(radians * 2) * 0.16, 0);
                DustParticleOptions color = arc % 3 == 0 ? CRIMSON_GLOW : arc % 2 == 0 ? AZAKANA : SPIRIT;
                level.sendParticles(color, point.x, point.y, point.z,
                        2, 0.055, 0.08, 0.055, 0.006);
            }
        }

        // Several upright black-red cuts give the impact the torn-soul silhouette from the reference.
        for (int slash = -3; slash <= 3; slash++) {
            double side = slash * 0.31;
            for (double y = 0.15; y <= 2.45 - Math.abs(slash) * 0.13; y += 0.18) {
                Vec3 point = center.add(right.scale(side + y * 0.10 * Math.signum(slash)))
                        .subtract(forward.scale(0.18 + y * 0.07)).add(0, y, 0);
                level.sendParticles(slash % 2 == 0 ? VOID : AZAKANA,
                        point.x, point.y, point.z, 2, 0.045, 0.07, 0.045, 0.005);
                if (y > 0.7 && slash % 2 != 0)
                    level.sendParticles(CRIMSON_GLOW, point.x, point.y, point.z,
                            1, 0.025, 0.04, 0.025, 0);
            }
        }

        level.sendParticles(SPIRIT, center.x, center.y + 0.85, center.z,
                26 + targets * 5, 1.15, 0.85, 1.15, 0.055);
        level.sendParticles(AZAKANA, center.x, center.y + 0.75, center.z,
                18 + targets * 3, 0.95, 0.72, 0.95, 0.04);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.28, center.z,
                30, 1.55, 0.28, 1.55, 0.035);
    }

    private static List<LivingEntity> lineTargets(ServerPlayer player, Vec3 origin, Vec3 forward,
                                                   double reach, double width) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach + 2, 3, reach + 2),
                entity -> entity != player && entity.isAlive() && !isBodyEcho(entity))) {
            Vec3 offset = target.position().subtract(origin);
            double along = offset.x * forward.x + offset.z * forward.z;
            double side = Math.abs(offset.x * forward.z - offset.z * forward.x);
            if (along >= 0 && along <= reach && side <= width && Math.abs(offset.y) <= 2.5) result.add(target);
        }
        result.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(origin)));
        return result;
    }

    private static List<LivingEntity> coneTargets(ServerPlayer player, Vec3 forward, double reach, double dot) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach, 2.5, reach),
                entity -> entity != player && entity.isAlive() && !isBodyEcho(entity))) {
            Vec3 flat = target.position().subtract(player.position()).multiply(1, 0, 1);
            if (flat.length() <= reach && flat.lengthSqr() > 0.01 && flat.normalize().dot(forward) >= dot)
                result.add(target);
        }
        return result;
    }

    private static void drawQTelegraph(ServerLevel level, Vec3 origin, Vec3 forward, boolean empowered) {
        double reach = empowered ? 10.5 : 4.5;
        for (double d = 0.4; d <= reach; d += 0.4) {
            Vec3 p = origin.add(forward.scale(d)).add(0, 0.12, 0);
            level.sendParticles(empowered ? WIND : STEEL, p.x, p.y, p.z, empowered ? 3 : 1,
                    0.08, 0.04, 0.08, 0);
        }
    }

    private static void steelLine(ServerLevel level, Vec3 origin, Vec3 forward, double reach) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (double d = 0.25; d <= reach; d += 0.22) {
            double spread = 0.08 + (d / reach) * 0.62;
            Vec3 center = origin.add(forward.scale(d)).add(0, 1.0, 0);
            level.sendParticles(d > reach * 0.65 ? STEEL_GLOW : STEEL,
                    center.x, center.y, center.z, 2, 0.05, 0.10, 0.05, 0);
            for (double side : new double[]{-spread, spread}) {
                Vec3 edge = center.add(right.scale(side));
                level.sendParticles(WIND, edge.x, edge.y, edge.z, 1, 0.04, 0.08, 0.04, 0);
            }
        }
    }

    private static void windTrail(ServerLevel level, Vec3 origin, Vec3 forward, double reach) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (double d = 0.25; d <= reach; d += 0.24) {
            Vec3 core = origin.add(forward.scale(d)).add(0, 0.85, 0);
            level.sendParticles(STORM_CORE, core.x, core.y, core.z, 4, 0.18, 0.28, 0.18, 0.015);
            for (int strand = 0; strand < 3; strand++) {
                double angle = d * 2.9 + strand * Math.PI * 2 / 3;
                double radius = 0.55 + Math.sin(d * 0.9) * 0.15;
                Vec3 spiral = core.add(right.scale(Math.cos(angle) * radius))
                        .add(0, Math.sin(angle) * radius, 0);
                level.sendParticles(WIND, spiral.x, spiral.y, spiral.z,
                        3, 0.16, 0.22, 0.16, 0.02);
            }
        }
    }

    private static void drawSpiritLink(ServerLevel level, Vec3 from, Vec3 to, double remaining) {
        Vec3 link = to.subtract(from);
        int steps = Math.max(4, Math.min(24, (int) (link.length() * 2)));
        double pulse = 0.5 + 0.5 * Math.sin(level.getGameTime() * (0.25 + (1.0 - remaining) * 1.25));
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(link.scale(i / (double) steps)).add(0, 1, 0);
            level.sendParticles(i % 2 == 0 ? SPIRIT : CRIMSON_GLOW, point.x, point.y, point.z,
                    pulse > 0.45 ? 2 : 1, 0.04 + pulse * 0.04, 0.04, 0.04 + pulse * 0.04, 0);
        }
    }

    private static void drawReturnTrail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 link = to.subtract(from);
        int steps = Math.max(6, (int) (link.length() * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(link.scale(i / (double) steps)).add(0, 1, 0);
            level.sendParticles(SPIRIT, p.x, p.y, p.z, 4, 0.12, 0.2, 0.12, 0.02);
        }
    }

    private static void spiritBurst(ServerLevel level, Vec3 point, int count) {
        level.sendParticles(SPIRIT, point.x, point.y, point.z, count, 0.55, 0.8, 0.55, 0.05);
        level.sendParticles(AZAKANA, point.x, point.y, point.z, count / 2, 0.45, 0.65, 0.45, 0.035);
    }

    private static void drawUltTelegraph(ServerLevel level, Vec3 origin, Vec3 forward, double progress) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        int edgeCount = progress > 0.72 ? 2 : 1;
        double pulse = 0.75 + Math.sin(level.getGameTime() * 0.65) * 0.25;
        for (double d = 0.5; d <= R_RANGE; d += 0.44) {
            for (double side : new double[]{-1.78, 1.78}) {
                Vec3 point = origin.add(forward.scale(d)).add(right.scale(side)).add(0, 0.12, 0);
                level.sendParticles(progress > 0.52 ? CRIMSON_GLOW : SPIRIT,
                        point.x, point.y, point.z, edgeCount, 0.025, 0.025, 0.025, 0);
            }
            if (progress > 0.58 && ((int) (d * 10)) % 9 == 0) {
                for (double side : new double[]{-0.72, 0.72}) {
                    Vec3 inner = origin.add(forward.scale(d)).add(right.scale(side)).add(0, 0.09, 0);
                    level.sendParticles(SPIRIT, inner.x, inner.y, inner.z,
                            1, 0.06 * pulse, 0.02, 0.06 * pulse, 0);
                }
            }
        }
        for (double side = -1.78; side <= 1.78; side += 0.34) {
            for (double d : new double[]{0.5, R_RANGE}) {
                Vec3 point = origin.add(forward.scale(d)).add(right.scale(side)).add(0, 0.15, 0);
                level.sendParticles(progress > 0.45 ? CRIMSON_GLOW : SPIRIT, point.x, point.y, point.z,
                        edgeCount, 0.02, 0.02, 0.02, 0);
            }
        }
    }

    private enum Skill { Q, W, R }
    private record PendingCast(ServerPlayer player, Skill skill, Vec3 forward, long executeAt, boolean empowered) {}
    private record QState(int stacks, long expiresAt) {}
    private record Mark(LivingEntity target, float damage) {}
    private record EState(Vec3 origin, long startedAt, long endsAt, Map<UUID, Mark> damage,
                          ArmorStand bodyEcho, Map<UUID, SpiritMarkVisual> visuals) {
        EState(Vec3 origin, long startedAt, long endsAt, Map<UUID, Mark> damage, ArmorStand bodyEcho) {
            this(origin, startedAt, endsAt, damage, bodyEcho, new HashMap<>());
        }
    }
    private record SpiritMarkVisual(Display.ItemDisplay display, LivingEntity target) {}
    private record ShieldVfx(ServerPlayer player, int hits, long expiresAt) {}
    private record PositionLock(Vec3 origin, long expiresAt) {}
    private static final class SmoothDash {
        private final ServerPlayer player;
        private final Vec3 forward;
        private int ticksRemaining;

        private SmoothDash(ServerPlayer player, Vec3 forward, int ticksRemaining) {
            this.player = player;
            this.forward = forward;
            this.ticksRemaining = ticksRemaining;
        }
    }
    private record UltEchoSound(ServerPlayer player, ServerLevel level, Vec3 position,
                                long executeAt, float volume, float pitch) {}
    private record FateTrailVfx(ServerLevel level, Vec3 origin, Vec3 forward,
                                long startedAt, long expiresAt) {}
}
