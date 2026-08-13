package kr.darius.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import kr.darius.skills.combat.CombatEngine;
import kr.darius.skills.shop.LegendaryItemEffects;
import kr.darius.skills.shop.PlayerEconomy;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** PC League-inspired Yone P/Q/W/E/R implementation for the shared Z/X/C/V input layer. */
public final class YoneSkills {
    private static final float FULL_ATTACK_STRENGTH = 0.9f;
    private static final long Q_STACK_DURATION_MS = 6_000;
    private static final long E_DURATION_MS = 5_000;
    private static final long E_MIN_RECAST_MS = 500;
    private static final DustParticleOptions STEEL = new DustParticleOptions(0xD7E3EA, 1.05f);
    private static final DustParticleOptions WIND = new DustParticleOptions(0x77D8E8, 1.35f);
    private static final DustParticleOptions SPIRIT = new DustParticleOptions(0xB5164B, 1.45f);
    private static final DustParticleOptions AZAKANA = new DustParticleOptions(0x2A0718, 1.75f);
    private static final ResourceKey<DamageType> SOUL_UNBOUND_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("darius_skills", "soul_unbound"));

    private static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    private static final Map<UUID, QState> Q_STATES = new HashMap<>();
    private static final Map<UUID, EState> E_STATES = new HashMap<>();
    private static final Map<UUID, Long> ACTION_LOCK_UNTIL = new HashMap<>();
    private static final Map<UUID, Boolean> SECOND_BLADE = new HashMap<>();
    private static final List<PendingCast> PENDING_CASTS = new ArrayList<>();

    private YoneSkills() {}

    public static void equip(ServerPlayer player) {
        ItemStack blade = new ItemStack(Items.NETHERITE_SWORD);
        blade.set(DataComponents.CUSTOM_NAME, Component.literal("§c요네의 쌍검"));
        player.getInventory().setItem(0, blade);
        player.getInventory().setSelectedSlot(0);
    }

    public static void reset(ServerPlayer player) {
        LAST_CAST.put(player.getUUID(), new long[5]);
        Q_STATES.remove(player.getUUID());
        EState spirit = E_STATES.remove(player.getUUID());
        if (spirit != null) returnToBody(player, spirit);
        ACTION_LOCK_UNTIL.remove(player.getUUID());
        PENDING_CASTS.removeIf(cast -> cast.player == player);
        SECOND_BLADE.remove(player.getUUID());
        player.removeEffect(MobEffects.SLOWNESS);
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
        boolean spiritBlade = SECOND_BLADE.getOrDefault(player.getUUID(), false);
        SECOND_BLADE.put(player.getUUID(), !spiritBlade);
        if (!spiritBlade) {
            markSpiritDamage(player, target, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            return false;
        }

        float total = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        CombatEngine.deal(player, target, total * 0.5f, CombatEngine.DamageKind.PHYSICAL,
                CombatEngine.KnockbackPolicy.VANILLA);
        CombatEngine.deal(player, target, total * 0.5f, CombatEngine.DamageKind.MAGIC,
                CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        markSpiritDamage(player, target, total);
        player.level().sendParticles(SPIRIT, target.getX(), target.getY() + 1.0, target.getZ(),
                16, 0.3, 0.55, 0.3, 0.025);
        player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.7f, 1.45f);
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
        long castTime = scaledCastTime(player, 350, 175);
        cast[1] = now;
        lock(player, castTime);
        PENDING_CASTS.add(new PendingCast(player, Skill.Q, forward, now + castTime, tornado));
        drawQTelegraph(player.level(), player.position(), forward, tornado);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.55f, tornado ? 0.75f : 1.45f);
    }

    private static void executeQ(PendingCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        int rank = rank(player, 1, 5);
        float damage = (float) (qBase(rank) + player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.05);
        double reach = cast.empowered ? 10.5 : 4.5;
        double width = cast.empowered ? 1.6 : 0.8;
        List<LivingEntity> targets = lineTargets(player, player.position(), cast.forward, reach, width);
        for (LivingEntity target : targets) {
            if (CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.PHYSICAL,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT)) {
                markSpiritDamage(player, target, damage);
                if (cast.empowered) {
                    CrowdControl.apply(target, CrowdControl.Type.AIRBORNE, 750);
                    target.setDeltaMovement(target.getDeltaMovement().x, 0.72, target.getDeltaMovement().z);
                    target.hurtMarked = true;
                }
            }
        }
        if (cast.empowered) {
            dash(player, cast.forward, 4.5);
            Q_STATES.remove(player.getUUID());
            windTrail(level, player.position(), cast.forward, reach);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 0.9f, 0.85f);
        } else if (!targets.isEmpty()) {
            QState current = Q_STATES.get(player.getUUID());
            int stacks = current != null && current.expiresAt > System.currentTimeMillis() ? current.stacks : 0;
            Q_STATES.put(player.getUUID(), new QState(Math.min(2, stacks + 1),
                    System.currentTimeMillis() + Q_STACK_DURATION_MS));
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
        PENDING_CASTS.add(new PendingCast(player, Skill.W, forward, now + castTime, false));
        drawConeTelegraph(player.level(), player, forward, 5.5, AZAKANA);
    }

    private static void executeW(PendingCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        int rank = rank(player, 2, 5);
        List<LivingEntity> targets = coneTargets(player, cast.forward, 5.5, 0.74);
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
        }
        drawConeTelegraph(level, player, cast.forward, 5.5, SPIRIT);
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
            returnToBody(player, active);
            return;
        }
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(player, cast, 2, now)) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        Vec3 body = player.position();
        E_STATES.put(player.getUUID(), new EState(body, now, now + E_DURATION_MS, new HashMap<>()));
        SECOND_BLADE.put(player.getUUID(), false);
        cast[2] = now;
        lock(player, 225);
        dash(player, forward, 3.0);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 105, 1, false, false));
        spiritBurst(player.level(), body, 55);
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
        drawUltLine(player.level(), player.position(), forward);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 0.8f, 0.62f);
    }

    private static void executeR(PendingCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        Vec3 origin = player.position();
        List<LivingEntity> targets = lineTargets(player, origin, cast.forward, 10.0, 2.25);
        Vec3 destination = origin.add(cast.forward.scale(10.0));
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
        for (double d = 0; d <= 10; d += 0.35) {
            Vec3 point = origin.add(cast.forward.scale(d)).add(0, 1.0, 0);
            level.sendParticles(d % 0.7 < 0.2 ? SPIRIT : AZAKANA,
                    point.x, point.y, point.z, 5, 0.3, 0.7, 0.3, 0.03);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, gather.x, gather.y + 1.0, gather.z,
                35, 1.5, 1.0, 1.5, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Q_STATES.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        ACTION_LOCK_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
                pending.remove();
                continue;
            }
            if (now < cast.executeAt) {
                if (cast.skill == Skill.R) drawUltLine(cast.player.level(), cast.player.position(), cast.forward);
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
                spirits.remove();
                continue;
            }
            EState state = entry.getValue();
            drawSpiritLink(player.level(), player.position(), state.origin);
            if (now >= state.endsAt) {
                spirits.remove();
                returnToBody(player, state);
            }
        }
    }

    private static void markSpiritDamage(ServerPlayer player, LivingEntity target, float damage) {
        EState state = E_STATES.get(player.getUUID());
        if (state == null || damage <= 0) return;
        state.damage.merge(target.getUUID(), new Mark(target, damage),
                (oldMark, added) -> new Mark(target, oldMark.damage + added.damage));
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
        }
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

    private static void teleportSafely(ServerPlayer player, Vec3 destination) {
        Vec3 delta = destination.subtract(player.position());
        if (player.level().noCollision(player, player.getBoundingBox().move(delta)))
            player.teleportTo(destination.x, destination.y, destination.z);
    }

    private static List<LivingEntity> lineTargets(ServerPlayer player, Vec3 origin, Vec3 forward,
                                                   double reach, double width) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach + 2, 3, reach + 2),
                entity -> entity != player && entity.isAlive())) {
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
                entity -> entity != player && entity.isAlive())) {
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
        for (double d = 0.25; d <= reach; d += 0.22) {
            Vec3 p = origin.add(forward.scale(d)).add(0, 1.0, 0);
            level.sendParticles(STEEL, p.x, p.y, p.z, 2, 0.08, 0.12, 0.08, 0);
        }
    }

    private static void windTrail(ServerLevel level, Vec3 origin, Vec3 forward, double reach) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (double d = 0.4; d <= reach; d += 0.35) {
            double wave = Math.sin(d * 2.4) * 0.7;
            Vec3 p = origin.add(forward.scale(d)).add(right.scale(wave)).add(0, 0.8, 0);
            level.sendParticles(WIND, p.x, p.y, p.z, 5, 0.25, 0.45, 0.25, 0.02);
        }
    }

    private static void drawConeTelegraph(ServerLevel level, ServerPlayer player, Vec3 forward,
                                           double reach, DustParticleOptions dust) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (int angle = -40; angle <= 40; angle += 5) {
            double radians = Math.toRadians(angle);
            Vec3 direction = forward.scale(Math.cos(radians)).add(right.scale(Math.sin(radians)));
            for (double d = 1.0; d <= reach; d += 0.9) {
                Vec3 p = player.position().add(direction.scale(d)).add(0, 0.18, 0);
                level.sendParticles(dust, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0);
            }
        }
    }

    private static void drawSpiritLink(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 link = to.subtract(from);
        int steps = Math.max(4, Math.min(24, (int) (link.length() * 2)));
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(link.scale(i / (double) steps)).add(0, 1, 0);
            level.sendParticles(i % 2 == 0 ? SPIRIT : AZAKANA, point.x, point.y, point.z,
                    1, 0.04, 0.04, 0.04, 0);
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

    private static void drawUltLine(ServerLevel level, Vec3 origin, Vec3 forward) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (double d = 0.5; d <= 10; d += 0.45) {
            for (double side : new double[]{-2.25, 0, 2.25}) {
                Vec3 point = origin.add(forward.scale(d)).add(right.scale(side)).add(0, 0.15, 0);
                level.sendParticles(side == 0 ? SPIRIT : AZAKANA,
                        point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0);
            }
        }
    }

    private enum Skill { Q, W, R }
    private record PendingCast(ServerPlayer player, Skill skill, Vec3 forward, long executeAt, boolean empowered) {}
    private record QState(int stacks, long expiresAt) {}
    private record Mark(LivingEntity target, float damage) {}
    private record EState(Vec3 origin, long startedAt, long endsAt, Map<UUID, Mark> damage) {}
}
