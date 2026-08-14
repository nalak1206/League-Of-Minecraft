package kr.leagueofminecraft.champion.yone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import static kr.leagueofminecraft.champion.yone.YoneRuntimeState.ACTION_LOCK_UNTIL;

/** Owns Yone cast locks and collision-safe skill movement. */
final class YoneMotionService {
    private YoneMotionService() {}

    static boolean isActionLocked(ServerPlayer player) {
        return ACTION_LOCK_UNTIL.getOrDefault(player.getUUID(), 0L) > System.currentTimeMillis();
    }

    static void lock(ServerPlayer player, long durationMs) {
        ACTION_LOCK_UNTIL.merge(player.getUUID(), System.currentTimeMillis() + durationMs, Math::max);
    }

    static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 0.001 ? null : look.normalize();
    }

    static boolean advanceDash(ServerPlayer player, Vec3 forward, double distance) {
        Vec3 destination = player.position().add(forward.scale(distance));
        Vec3 delta = destination.subtract(player.position());
        if (!player.level().noCollision(player, player.getBoundingBox().move(delta))) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        return true;
    }

    static void teleportSafely(ServerPlayer player, Vec3 destination) {
        Vec3 delta = destination.subtract(player.position());
        if (player.level().noCollision(player, player.getBoundingBox().move(delta)))
            player.teleportTo(destination.x, destination.y, destination.z);
    }
}
