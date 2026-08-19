package kr.leagueofminecraft.match;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

import java.util.Set;

/** Persistable team base and respawn destination. */
public record MatchBase(String dimension, double x, double y, double z, float yaw, float pitch) {
    public static MatchBase at(ServerPlayer player) {
        return new MatchBase(player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public boolean teleport(MinecraftServer server, ServerPlayer player) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
        ServerLevel level = server.getLevel(key);
        return level != null && player.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, false);
    }
}
