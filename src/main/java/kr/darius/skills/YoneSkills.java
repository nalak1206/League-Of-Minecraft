package kr.darius.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import kr.darius.skills.shop.LegendaryItemEffects;

/** First Fabric port of Yone P/Q/W/E/R, sharing input and CC with Darius. */
public final class YoneSkills {
    private static final DustParticleOptions WIND = new DustParticleOptions(0xDDE7F3, 1.15f);
    private static final DustParticleOptions SPIRIT = new DustParticleOptions(0xA51C4D, 1.35f);
    private static final long[] BASE_COOLDOWN = {0, 4_000, 10_000, 80_000, 14_000};
    private static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    private static final Map<UUID, QState> Q_STATES = new HashMap<>();
    private static final Map<UUID, EState> E_STATES = new HashMap<>();
    private static final Map<UUID, RCast> R_CASTS = new HashMap<>();
    private static final Map<UUID, Boolean> SECOND_BLADE = new HashMap<>();
    private static final List<PendingMagic> PENDING_MAGIC = new ArrayList<>();

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
        R_CASTS.remove(player.getUUID());
        SECOND_BLADE.remove(player.getUUID());
    }

    public static void cast(ServerPlayer player, int wireSkill) {
        if (!ChampionManager.isYone(player)) return;
        switch (wireSkill) {
            case 1 -> q(player);
            case 4 -> w(player);
            case 2 -> e(player);
            case 3 -> r(player);
            default -> { }
        }
    }

    public static void basicAttack(ServerPlayer player, LivingEntity target) {
        boolean spiritBlade = SECOND_BLADE.getOrDefault(player.getUUID(), false);
        SECOND_BLADE.put(player.getUUID(), !spiritBlade);
        if (!spiritBlade) return;
        float attack = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        PENDING_MAGIC.add(new PendingMagic(player, target, Math.max(0.5f, attack * 0.5f), 10));
    }

    private static void q(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(cast, 1, now)) return;
        int rank = Math.max(1, ChampionProgression.get(player).rank(1));
        QState state = Q_STATES.get(player.getUUID());
        int stacks = state != null && state.expiresAt > now ? state.stacks : 0;
        boolean tornado = stacks >= 2;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        double reach = tornado ? 8.0 : 3.2;
        float damage = (float) (2.5 + rank * 1.5 + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        ServerLevel level = player.level();
        for (LivingEntity target : lineTargets(player, reach, tornado ? 1.15 : 0.8)) {
            target.hurtServer(level, player.damageSources().playerAttack(player), damage);
            markSpiritDamage(player, target, damage);
            if (tornado) {
                CrowdControl.apply(target, CrowdControl.Type.AIRBORNE, 750);
                target.push(0, 0.75, 0);
                target.hurtMarked = true;
            }
        }
        if (tornado) {
            Vec3 destination = player.position().add(forward.scale(4.0));
            if (player.level().noCollision(player, player.getBoundingBox().move(destination.subtract(player.position()))))
                player.teleportTo(destination.x, destination.y, destination.z);
            Q_STATES.remove(player.getUUID());
        } else {
            Q_STATES.put(player.getUUID(), new QState(stacks + 1, now + 6_000));
        }
        for (double d = 0.5; d <= reach; d += 0.45) {
            Vec3 point = player.position().add(forward.scale(d)).add(0, 1.0, 0);
            level.sendParticles(tornado ? WIND : SPIRIT, point.x, point.y, point.z, tornado ? 5 : 2, 0.2, 0.25, 0.2, 0.01);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, tornado ? 1.2f : 1.55f);
        cast[1] = now;
    }

    private static void w(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(cast, 4, now)) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        ServerLevel level = player.level();
        int hits = 0;
        for (LivingEntity target : coneTargets(player, forward, 5.5, 0.70)) {
            float damage = Math.max(2.0f, target.getMaxHealth() * 0.11f);
            target.hurtServer(level, player.damageSources().playerAttack(player), damage * 0.5f);
            target.invulnerableTime = 0;
            target.hurtServer(level, player.damageSources().magic(), damage * 0.5f);
            markSpiritDamage(player, target, damage);
            hits++;
        }
        if (hits > 0) player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 30, Math.min(4, hits), false, false));
        arcParticles(level, player, forward, 5.5, SPIRIT);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.75f);
        cast[4] = now;
    }

    private static void e(ServerPlayer player) {
        EState active = E_STATES.remove(player.getUUID());
        if (active != null) { returnToBody(player, active); return; }
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(cast, 2, now)) return;
        LegendaryItemEffects.onSkillInput(player);
        E_STATES.put(player.getUUID(), new EState(player.position(), now + 5_000, new HashMap<>()));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 105, 1, false, false));
        player.level().sendParticles(SPIRIT, player.getX(), player.getY() + 1.0, player.getZ(), 45, 0.55, 0.9, 0.55, 0.06);
        cast[2] = now;
    }

    private static void r(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        if (!ready(cast, 3, now) || R_CASTS.containsKey(player.getUUID())) return;
        Vec3 forward = flatLook(player);
        if (forward == null) return;
        LegendaryItemEffects.onSkillInput(player);
        R_CASTS.put(player.getUUID(), new RCast(player, player.position(), forward, now + 650));
        cast[3] = now;
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Q_STATES.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        Iterator<PendingMagic> magic = PENDING_MAGIC.iterator();
        while (magic.hasNext()) {
            PendingMagic hit = magic.next();
            if (!hit.target.isAlive() || !hit.player.isAlive()) { magic.remove(); continue; }
            if (--hit.ticks > 0) continue;
            hit.target.invulnerableTime = 0;
            hit.target.hurtServer(hit.player.level(), hit.player.damageSources().magic(), hit.damage);
            markSpiritDamage(hit.player, hit.target, hit.damage);
            hit.player.level().sendParticles(ParticleTypes.ENCHANT, hit.target.getX(), hit.target.getY() + 1, hit.target.getZ(), 14, 0.35, 0.55, 0.35, 0.02);
            magic.remove();
        }
        Iterator<Map.Entry<UUID, EState>> spirits = E_STATES.entrySet().iterator();
        while (spirits.hasNext()) {
            var entry = spirits.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) { spirits.remove(); continue; }
            EState state = entry.getValue();
            drawSpiritLink(player.level(), player.position(), state.origin);
            if (now >= state.endsAt) { spirits.remove(); returnToBody(player, state); }
        }
        Iterator<Map.Entry<UUID, RCast>> casts = R_CASTS.entrySet().iterator();
        while (casts.hasNext()) {
            var entry = casts.next();
            RCast cast = entry.getValue();
            if (!cast.player.isAlive()) { casts.remove(); continue; }
            if (now < cast.executeAt) {
                drawUltLine(cast.player.level(), cast.origin, cast.forward);
                continue;
            }
            executeR(cast);
            casts.remove();
        }
    }

    private static void executeR(RCast cast) {
        ServerPlayer player = cast.player;
        ServerLevel level = player.level();
        List<LivingEntity> targets = lineTargets(player, cast.origin, cast.forward, 8.0, 1.5);
        float damage = 10.0f + Math.max(1, ChampionProgression.get(player).rank(4)) * 5.0f;
        for (LivingEntity target : targets) {
            target.hurtServer(level, player.damageSources().playerAttack(player), damage * 0.5f);
            target.invulnerableTime = 0;
            target.hurtServer(level, player.damageSources().magic(), damage * 0.5f);
            CrowdControl.apply(target, CrowdControl.Type.AIRBORNE, 750);
            target.push(0, 0.9, 0);
            target.hurtMarked = true;
            markSpiritDamage(player, target, damage);
        }
        Vec3 destination = cast.origin.add(cast.forward.scale(8.0));
        if (!targets.isEmpty()) {
            LivingEntity last = targets.stream().max(Comparator.comparingDouble(t -> t.position().distanceToSqr(cast.origin))).orElse(targets.getFirst());
            destination = last.position().add(cast.forward.scale(0.85));
        }
        if (level.noCollision(player, player.getBoundingBox().move(destination.subtract(player.position()))))
            player.teleportTo(destination.x, destination.y, destination.z);
        level.sendParticles(SPIRIT, player.getX(), player.getY() + 1, player.getZ(), 90, 2.8, 1.2, 2.8, 0.06);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.25f);
    }

    private static void markSpiritDamage(ServerPlayer player, LivingEntity target, float damage) {
        EState state = E_STATES.get(player.getUUID());
        if (state != null) state.damage.merge(target.getUUID(), new Mark(target, damage), (oldMark, added) -> new Mark(target, oldMark.damage + added.damage));
    }

    private static void returnToBody(ServerPlayer player, EState state) {
        for (Mark mark : state.damage.values()) {
            if (!mark.target.isAlive()) continue;
            mark.target.invulnerableTime = 0;
            mark.target.hurtServer(player.level(), player.damageSources().magic(), mark.damage * 0.35f);
            player.level().sendParticles(SPIRIT, mark.target.getX(), mark.target.getY() + 1, mark.target.getZ(), 24, 0.4, 0.7, 0.4, 0.05);
        }
        player.teleportTo(state.origin.x, state.origin.y, state.origin.z);
        player.removeEffect(MobEffects.SPEED);
    }

    public static void showActionBar(ServerPlayer player, long now) {
        long[] cast = LAST_CAST.get(player.getUUID());
        QState qState = Q_STATES.get(player.getUUID());
        int stacks = qState != null && qState.expiresAt > now ? qState.stacks : 0;
        String e = E_STATES.containsKey(player.getUUID()) ? "§dACTIVE" : cooldownText(cast, 2, now);
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                "§bZ§f " + cooldownText(cast, 1, now) + " §7[Q" + stacks + "]  §8|  §bX§f " + cooldownText(cast, 4, now)
                + "  §8|  §bC§f " + e + "  §8|  §5§lV§r§f " + cooldownText(cast, 3, now))));
    }

    private static String cooldownText(long[] cast, int skill, long now) {
        if (cast == null || cast[skill] == 0) return "§aREADY";
        long left = cooldown(skill) - (now - cast[skill]);
        return left <= 0 ? "§aREADY" : "§e" + String.format(Locale.ROOT, "%.1fs", left / 1000.0);
    }

    private static boolean ready(long[] cast, int skill, long now) {
        return cast[skill] == 0 || now - cast[skill] >= cooldown(skill);
    }

    private static long cooldown(int skill) {
        return BASE_COOLDOWN[skill];
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 0.001 ? null : look.normalize();
    }

    private static List<LivingEntity> lineTargets(ServerPlayer player, double reach, double width) {
        Vec3 forward = flatLook(player);
        return forward == null ? List.of() : lineTargets(player, player.position(), forward, reach, width);
    }

    private static List<LivingEntity> lineTargets(ServerPlayer player, Vec3 origin, Vec3 forward, double reach, double width) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach + 1, 3, reach + 1),
                entity -> entity != player && entity.isAlive())) {
            Vec3 offset = target.position().subtract(origin);
            double along = offset.x * forward.x + offset.z * forward.z;
            double side = Math.abs(offset.x * forward.z - offset.z * forward.x);
            if (along >= 0 && along <= reach && side <= width && Math.abs(offset.y) <= 2.5) result.add(target);
        }
        return result;
    }

    private static List<LivingEntity> coneTargets(ServerPlayer player, Vec3 forward, double reach, double dot) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach, 2.5, reach),
                entity -> entity != player && entity.isAlive())) {
            Vec3 flat = target.position().subtract(player.position()).multiply(1, 0, 1);
            if (flat.length() <= reach && flat.lengthSqr() > 0.01 && flat.normalize().dot(forward) >= dot) result.add(target);
        }
        return result;
    }

    private static void arcParticles(ServerLevel level, ServerPlayer player, Vec3 forward, double reach, DustParticleOptions dust) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        for (int angle = -45; angle <= 45; angle += 6) {
            double radians = Math.toRadians(angle);
            Vec3 direction = forward.scale(Math.cos(radians)).add(right.scale(Math.sin(radians)));
            Vec3 point = player.position().add(direction.scale(reach)).add(0, 0.9, 0);
            level.sendParticles(dust, point.x, point.y, point.z, 2, 0.08, 0.12, 0.08, 0);
        }
    }

    private static void drawSpiritLink(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 link = to.subtract(from);
        for (int i = 1; i <= 8; i++) {
            Vec3 point = from.add(link.scale(i / 8.0)).add(0, 1, 0);
            level.sendParticles(SPIRIT, point.x, point.y, point.z, 1, 0.04, 0.04, 0.04, 0);
        }
    }

    private static void drawUltLine(ServerLevel level, Vec3 origin, Vec3 forward) {
        for (double d = 0.5; d <= 8; d += 0.5) {
            for (double side : new double[]{-1.5, 1.5}) {
                Vec3 point = origin.add(forward.scale(d)).add(new Vec3(-forward.z, 0, forward.x).scale(side)).add(0, 0.15, 0);
                level.sendParticles(SPIRIT, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0);
            }
        }
    }

    private record QState(int stacks, long expiresAt) {}
    private record RCast(ServerPlayer player, Vec3 origin, Vec3 forward, long executeAt) {}
    private record Mark(LivingEntity target, float damage) {}
    private record EState(Vec3 origin, long endsAt, Map<UUID, Mark> damage) {}
    private static final class PendingMagic {
        final ServerPlayer player; final LivingEntity target; final float damage; int ticks;
        PendingMagic(ServerPlayer player, LivingEntity target, float damage, int ticks) {
            this.player = player; this.target = target; this.damage = damage; this.ticks = ticks;
        }
    }
}
