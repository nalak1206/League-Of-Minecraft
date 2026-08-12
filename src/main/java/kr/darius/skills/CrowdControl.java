package kr.darius.skills;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Shared CC state used by every champion instead of potion-effect stand-ins. */
public final class CrowdControl {
    public enum Type { SLOW, ROOT, STUN, AIRBORNE, SILENCE, DISARM, BLIND, GROUND, SUPPRESSION }
    private static final Map<UUID, EnumMap<Type, Long>> ACTIVE = new HashMap<>();

    private CrowdControl() {}

    public static void apply(LivingEntity target, Type type, long durationMs) {
        ACTIVE.computeIfAbsent(target.getUUID(), id -> new EnumMap<>(Type.class))
                .merge(type, System.currentTimeMillis() + durationMs, Math::max);
    }

    public static boolean has(LivingEntity target, Type type) {
        EnumMap<Type, Long> states = ACTIVE.get(target.getUUID());
        return states != null && states.getOrDefault(type, 0L) > System.currentTimeMillis();
    }

    public static boolean blocksSkills(ServerPlayer player) {
        return has(player, Type.STUN) || has(player, Type.AIRBORNE)
                || has(player, Type.SILENCE) || has(player, Type.SUPPRESSION);
    }

    public static boolean blocksBasicAttack(ServerPlayer player) {
        return has(player, Type.STUN) || has(player, Type.AIRBORNE)
                || has(player, Type.DISARM) || has(player, Type.SUPPRESSION);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        ACTIVE.values().forEach(states -> states.entrySet().removeIf(entry -> entry.getValue() <= now));
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
