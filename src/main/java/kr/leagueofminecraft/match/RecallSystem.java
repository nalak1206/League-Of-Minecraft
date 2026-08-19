package kr.leagueofminecraft.match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

/** Eight-second League-style recall channel bound to the player's team base. */
public final class RecallSystem {
    public static final long CHANNEL_TICKS = 160L;
    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.04;
    private static final Map<UUID, RecallState> CHANNELS = new HashMap<>();
    private static boolean initialized;

    private RecallSystem() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(RecallSystem::tick);
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
                cancel(serverPlayer, "공격");
            return InteractionResult.PASS;
        });
    }

    public static void toggle(ServerPlayer player) {
        if (isRecalling(player)) {
            cancel(player, "직접 취소");
            return;
        }
        if (MatchManager.phase() != MatchPhase.RUNNING) {
            actionBar(player, "§c경기 중에만 귀환할 수 있습니다.");
            return;
        }
        MatchTeam team = MatchManager.team(player);
        if (team == MatchTeam.UNASSIGNED || MatchManager.base(team) == null) {
            actionBar(player, "§c팀과 기지가 필요합니다.");
            return;
        }
        long now = player.level().getServer().getTickCount();
        CHANNELS.put(player.getUUID(), new RecallState(player.position(),
                player.level().dimension().identifier().toString(), now + CHANNEL_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER,
                SoundSource.PLAYERS, 0.65f, 1.3f);
        actionBar(player, "§d귀환 중... §f8.0초");
    }

    public static boolean isRecalling(ServerPlayer player) {
        return CHANNELS.containsKey(player.getUUID());
    }

    public static void cancel(ServerPlayer player, String reason) {
        if (CHANNELS.remove(player.getUUID()) == null) return;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.45f, 1.5f);
        actionBar(player, "§c귀환 취소§7: " + reason);
    }

    public static void cancelAll(MinecraftServer server, String reason) {
        for (UUID id : CHANNELS.keySet().toArray(UUID[]::new)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) cancel(player, reason);
            else CHANNELS.remove(id);
        }
    }

    public static void onDamaged(ServerPlayer player) { cancel(player, "피격"); }
    public static void onSkillInput(ServerPlayer player) { cancel(player, "스킬 사용"); }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (UUID id : CHANNELS.keySet().toArray(UUID[]::new)) {
            RecallState state = CHANNELS.get(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (state == null) continue;
            if (player == null || !player.isAlive()) {
                CHANNELS.remove(id);
                continue;
            }
            if (MatchManager.phase() != MatchPhase.RUNNING
                    || MatchManager.team(player) == MatchTeam.UNASSIGNED) {
                cancel(player, "경기 상태 변경");
                continue;
            }
            if (!state.dimension.equals(player.level().dimension().identifier().toString())
                    || RecallMath.moved(state.origin.x, state.origin.y, state.origin.z,
                    player.getX(), player.getY(), player.getZ(), MOVEMENT_TOLERANCE_SQUARED)) {
                cancel(player, "이동");
                continue;
            }
            long remaining = RecallMath.remainingTicks(state.completesAtTick, now);
            if (remaining == 0) {
                CHANNELS.remove(id);
                if (MatchManager.teleportToBase(player)) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL,
                            SoundSource.PLAYERS, 0.8f, 1.1f);
                    actionBar(player, "§a기지로 귀환했습니다.");
                }
                continue;
            }
            if (now % 2 == 0)
                actionBar(player, String.format(java.util.Locale.ROOT,
                        "§d귀환 중... §f%.1f초", remaining / 20.0));
            if (now % 5 == 0) particles(player, now);
        }
    }

    private static void particles(ServerPlayer player, long tick) {
        ServerLevel level = player.level();
        double phase = tick * 0.18;
        for (int i = 0; i < 12; i++) {
            double angle = phase + Math.PI * 2.0 * i / 12.0;
            double radius = 1.0 + 0.12 * Math.sin(phase + i);
            level.sendParticles(i % 3 == 0 ? ParticleTypes.WITCH : ParticleTypes.PORTAL,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.1 + (i % 4) * 0.18,
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0.02, 0.03, 0.02, 0.01);
        }
    }

    private static void actionBar(ServerPlayer player, String text) {
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }

    private record RecallState(Vec3 origin, String dimension, long completesAtTick) {}
}
