package kr.leagueofminecraft.champion.malphite;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import kr.leagueofminecraft.combat.CombatEngine;
import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.core.ChampionProgression;
import kr.leagueofminecraft.core.CrowdControl;
import kr.leagueofminecraft.core.UltimateVoiceLines;
import kr.leagueofminecraft.registry.ModItems;
import kr.leagueofminecraft.shop.LegendaryItemEffects;
import kr.leagueofminecraft.shop.PlayerEconomy;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** League-inspired Malphite P/Q/W/E/R for the common Z/X/C/V input layer. */
public final class MalphiteSkills {
    private static final long[] BASE_COOLDOWNS = {0L, 8_000L, 10_000L, 7_000L, 100_000L};
    private static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    private static final Map<UUID, Long> LAST_DAMAGED = new HashMap<>();
    private static final Map<UUID, WState> W_ACTIVE = new HashMap<>();
    private static final Map<UUID, Dash> DASHES = new HashMap<>();
    private static final DustParticleOptions ROCK = new DustParticleOptions(0x706B68, 1.7f);
    private static final DustParticleOptions EARTH = new DustParticleOptions(0xA18B69, 1.25f);
    private static boolean initialized;

    private MalphiteSkills() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(MalphiteSkills::tick);
    }

    public static void equip(ServerPlayer player) {
        ItemStack displaced = player.getInventory().getItem(0);
        if (!displaced.isEmpty() && !isWeapon(displaced)) player.getInventory().add(displaced.copy());
        removeWeapons(player);
        ItemStack fist = new ItemStack(ModItems.MALPHITE_FIST);
        fist.set(DataComponents.CUSTOM_NAME, Component.literal("§8§l바위 주먹"));
        player.getInventory().setItem(0, fist);
        player.getInventory().setSelectedSlot(0);
        LAST_DAMAGED.put(player.getUUID(), System.currentTimeMillis() - 10_000L);
    }

    public static boolean isWeapon(ItemStack stack) { return stack.is(ModItems.MALPHITE_FIST); }

    public static void reset(ServerPlayer player) {
        LAST_CAST.remove(player.getUUID());
        LAST_DAMAGED.remove(player.getUUID());
        W_ACTIVE.remove(player.getUUID());
        DASHES.remove(player.getUUID());
        player.setAbsorptionAmount(0.0f);
        player.setNoGravity(false);
        removeWeapons(player);
    }

    public static void cast(ServerPlayer player, int wireSkill) {
        if (!ChampionManager.isMalphite(player) || DASHES.containsKey(player.getUUID())) return;
        switch (wireSkill) {
            case 1 -> q(player);
            case 4 -> w(player);
            case 2 -> e(player);
            case 3 -> r(player);
            default -> { }
        }
    }

    public static void onDamaged(ServerPlayer player) {
        if (ChampionManager.isMalphite(player)) LAST_DAMAGED.put(player.getUUID(), System.currentTimeMillis());
    }

    public static boolean empoweredAttack(ServerPlayer player, LivingEntity target) {
        WState state = W_ACTIVE.get(player.getUUID());
        long now = System.currentTimeMillis();
        if (!ChampionManager.isMalphite(player) || state == null || state.expiresAt <= now) return false;
        float armor = (float) player.getAttributeValue(Attributes.ARMOR) * 10.0f;
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                + 2.0f + rank(player, 2) * 0.8f + armor * (state.firstHit ? 0.04f : 0.02f);
        CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.PHYSICAL,
                CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(2.4), entity -> entity != player && entity != target && entity.isAlive()
                        && target.hasLineOfSight(entity)
                        && MalphiteSkillRules.withinHorizontalRadius(
                                entity.getX() - target.getX(), entity.getZ() - target.getZ(), 2.4)))
            CombatEngine.deal(player, nearby, damage * 0.45f, CombatEngine.DamageKind.PHYSICAL,
                    CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        W_ACTIVE.put(player.getUUID(), new WState(state.expiresAt, false));
        player.level().sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.7, target.getZ(), 18, 0.7, 0.35, 0.7, 0.08);
        player.level().playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.65f, 0.7f);
        player.resetAttackStrengthTicker();
        return true;
    }

    public static void reduceUltimateCooldown(ServerPlayer player, long millis) {
        long[] cast = LAST_CAST.get(player.getUUID());
        if (cast != null) cast[4] = Math.max(1L, cast[4] - millis);
    }

    private static void q(ServerPlayer player) {
        long now = System.currentTimeMillis();
        if (!ready(player, 1, now)) return;
        LivingEntity target = aimedTarget(player, MalphiteSkillRules.Q_RANGE, 0.72);
        if (target == null) return;
        markCast(player, 1, now);
        LegendaryItemEffects.onSkillInput(player);
        int rank = rank(player, 1);
        float damage = 3.0f + rank * 1.25f + (float) PlayerEconomy.abilityPower(player) * 0.35f;
        line(player.level(), player.position().add(0, 1, 0), target.position().add(0, 0.8, 0));
        CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.MAGIC, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, Math.min(4, rank), false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, Math.min(2, (rank + 1) / 2), false, false));
        player.level().playSound(null, target.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.9f, 0.65f);
    }

    private static void w(ServerPlayer player) {
        long now = System.currentTimeMillis();
        if (!ready(player, 2, now)) return;
        markCast(player, 2, now);
        LegendaryItemEffects.onSkillInput(player);
        W_ACTIVE.put(player.getUUID(), new WState(now + 6_000L, true));
        player.level().sendParticles(ROCK, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.8, 0.9, 0.8, 0.03);
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.75f, 0.65f);
    }

    private static void e(ServerPlayer player) {
        long now = System.currentTimeMillis();
        if (!ready(player, 3, now)) return;
        markCast(player, 3, now);
        LegendaryItemEffects.onSkillInput(player);
        int rank = rank(player, 3);
        float armor = (float) player.getAttributeValue(Attributes.ARMOR) * 10.0f;
        float damage = 3.0f + rank * 1.15f + armor * 0.08f + (float) PlayerEconomy.abilityPower(player) * 0.30f;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(MalphiteSkillRules.E_RADIUS),
                entity -> validAreaTarget(player, entity, MalphiteSkillRules.E_RADIUS))) {
            CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.MAGIC, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, Math.min(4, rank), false, false));
        }
        for (int i = 0; i < 90; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = player.getRandom().nextDouble() * 4.0;
            player.level().sendParticles(i % 3 == 0 ? ParticleTypes.POOF : EARTH,
                    player.getX() + Math.cos(angle) * radius, player.getY() + 0.12,
                    player.getZ() + Math.sin(angle) * radius, 1, 0, 0.08, 0, 0.01);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.55f);
    }

    private static void r(ServerPlayer player) {
        long now = System.currentTimeMillis();
        if (!ready(player, 4, now)) return;
        Vec3 end = resolveDashEnd(player);
        if (end == null || end.distanceToSqr(player.position()) < 0.56) return;
        markCast(player, 4, now);
        LegendaryItemEffects.onSkillInput(player);
        UltimateVoiceLines.shout(player, ChampionManager.Champion.MALPHITE);
        Vec3 start = player.position();
        DASHES.put(player.getUUID(), new Dash(start, end, 0));
        player.setNoGravity(true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.65f);
    }

    private static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ChampionManager.isMalphite(player)) continue;
            ensureWeapon(player);
            if (now - LAST_DAMAGED.getOrDefault(player.getUUID(), 0L) >= 8_000L && player.getAbsorptionAmount() <= 0.0f)
                player.setAbsorptionAmount(player.getMaxHealth() * 0.10f);
            showCooldowns(player, now);
        }
        for (Map.Entry<UUID, Dash> entry : List.copyOf(DASHES.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) { DASHES.remove(entry.getKey()); continue; }
            if (!player.isAlive()) {
                player.setNoGravity(false);
                DASHES.remove(entry.getKey());
                continue;
            }
            Dash dash = entry.getValue();
            int nextTick = dash.tick + 1;
            double progress = MalphiteSkillRules.dashProgress(nextTick);
            Vec3 position = dash.start.lerp(dash.end, progress);
            player.teleportTo(position.x, position.y, position.z);
            player.level().sendParticles(ROCK, position.x, position.y + 0.8, position.z, 12, 0.55, 0.55, 0.55, 0.02);
            if (progress < 1.0) { DASHES.put(entry.getKey(), new Dash(dash.start, dash.end, nextTick)); continue; }
            DASHES.remove(entry.getKey());
            player.setNoGravity(false);
            float damage = 7.0f + rank(player, 4) * 2.5f + (float) PlayerEconomy.abilityPower(player) * 0.45f;
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(MalphiteSkillRules.R_IMPACT_RADIUS),
                    entity -> validAreaTarget(player, entity, MalphiteSkillRules.R_IMPACT_RADIUS))) {
                CombatEngine.deal(player, target, damage, CombatEngine.DamageKind.MAGIC, CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
                CrowdControl.apply(target, CrowdControl.Type.AIRBORNE, 1_500L);
                target.setDeltaMovement(0, 0.85, 0);
                target.hurtMarked = true;
            }
            player.level().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), 5, 1.2, 0.3, 1.2, 0.05);
            player.level().sendParticles(ROCK, player.getX(), player.getY() + 0.5, player.getZ(), 80, 2.3, 0.9, 2.3, 0.12);
            player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.25f, 0.55f);
        }
    }

    private static void showCooldowns(ServerPlayer player, long now) {
        if (UltimateVoiceLines.isTypingFor(player)) return;
        long[] cast = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        StringBuilder text = new StringBuilder("§7Z ");
        for (int skill = 1; skill <= 4; skill++) {
            if (skill > 1) text.append(" §8| §7").append(skill == 2 ? "X " : skill == 3 ? "C " : "V ");
            long remaining = Math.max(0L, cast[skill] + cooldown(player, skill) - now);
            text.append(remaining == 0 ? "§aREADY" : String.format(Locale.ROOT, "§c%.1fs", remaining / 1000.0));
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text.toString())));
    }

    private static LivingEntity aimedTarget(ServerPlayer player, double range, double minimumDot) {
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), target -> target != player && target.isAlive())
                .stream().filter(target -> player.distanceToSqr(target) <= range * range
                        && player.hasLineOfSight(target)
                        && target.position().add(0, target.getBbHeight() * 0.5, 0)
                        .subtract(player.getEyePosition()).normalize().dot(look) >= minimumDot)
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static boolean validAreaTarget(ServerPlayer player, LivingEntity target, double radius) {
        if (target == player || !target.isAlive() || !player.hasLineOfSight(target)) return false;
        return MalphiteSkillRules.withinHorizontalRadius(
                target.getX() - player.getX(), target.getZ() - player.getZ(), radius);
    }

    /** Uses the crosshair point when available, then shortens the path before the first solid collision. */
    private static Vec3 resolveDashEnd(ServerPlayer player) {
        Vec3 start = player.position();
        Vec3 forward = flatLook(player);
        if (forward == null) return null;
        double requestedDistance = MalphiteSkillRules.R_RANGE;
        HitResult hit = player.pick(MalphiteSkillRules.R_RANGE, 0.0f, false);
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 horizontal = new Vec3(hit.getLocation().x - start.x, 0, hit.getLocation().z - start.z);
            if (horizontal.lengthSqr() > 0.01)
                requestedDistance = MalphiteSkillRules.clampRange(horizontal.length(), MalphiteSkillRules.R_RANGE);
        }
        Vec3 requestedEnd = start.add(forward.scale(requestedDistance));
        Vec3 safe = start;
        double total = requestedEnd.subtract(start).length();
        for (double distance = 0.25; distance <= total + 0.001; distance += 0.25) {
            Vec3 candidate = start.add(forward.scale(Math.min(distance, total)));
            Vec3 offset = candidate.subtract(start);
            if (!player.level().noCollision(player, player.getBoundingBox().move(offset))) break;
            safe = candidate;
        }
        return safe;
    }

    private static void line(ServerLevel level, Vec3 from, Vec3 to) {
        for (int i = 0; i <= 18; i++) {
            Vec3 point = from.lerp(to, i / 18.0);
            level.sendParticles(ROCK, point.x, point.y, point.z, 2, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 flat = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        return flat.lengthSqr() < 0.001 ? null : flat.normalize();
    }

    private static boolean ready(ServerPlayer player, int skill, long now) {
        return now >= LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5])[skill] + cooldown(player, skill);
    }
    private static void markCast(ServerPlayer player, int skill, long now) { LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5])[skill] = now; }
    private static long cooldown(ServerPlayer player, int skill) { return PlayerEconomy.cooldownMillis(player, BASE_COOLDOWNS[skill]); }
    private static int rank(ServerPlayer player, int skill) { return Math.max(1, ChampionProgression.get(player).rank(skill)); }
    private static void ensureWeapon(ServerPlayer player) { if (!player.getInventory().getItem(0).is(ModItems.MALPHITE_FIST)) equip(player); }
    private static void removeWeapons(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (isWeapon(player.getInventory().getItem(slot))) player.getInventory().setItem(slot, ItemStack.EMPTY);
        if (isWeapon(player.getOffhandItem())) player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private record WState(long expiresAt, boolean firstHit) {}
    private record Dash(Vec3 start, Vec3 end, int tick) {}
}
