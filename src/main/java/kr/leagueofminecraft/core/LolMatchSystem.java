package kr.leagueofminecraft.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.leagueofminecraft.shop.PlayerEconomy;
import kr.leagueofminecraft.shop.LegendaryItemEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Automatic League-style economy and champion progression. */
public final class LolMatchSystem {
    private static final Map<UUID, Long> MATCH_JOIN_TICK = new HashMap<>();
    private static boolean initialized;

    private LolMatchSystem() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLivingEntityEvents.AFTER_DEATH.register(LolMatchSystem::afterDeath);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) -> LegendaryItemEffects.allowDamage(target));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
            if (taken > 0) LegendaryItemEffects.afterDamage(target, source, taken);
        });
        ServerTickEvents.END_SERVER_TICK.register(LolMatchSystem::tick);
    }

    private static void afterDeath(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer) || victim == killer) return;
        int gold = victim instanceof ServerPlayer ? 300
                : Math.max(12, Math.min(90, Math.round(victim.getMaxHealth() * 1.5f)));
        int xp = victim instanceof ServerPlayer ? 300
                : Math.max(30, Math.min(240, Math.round(victim.getMaxHealth() * 4.0f)));
        PlayerEconomy.addGold(killer, gold);
        ChampionProgression.addXp(killer, xp);
        killer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                "§6+" + gold + "G §b+" + xp + " XP")));
    }

    private static void tick(MinecraftServer server) {
        long ticks = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerEconomy.tickRegen(player, ticks);
            if (ChampionManager.mode(player) != ChampionManager.GameMode.MATCH) {
                MATCH_JOIN_TICK.remove(player.getUUID());
                continue;
            }
            long joined = MATCH_JOIN_TICK.computeIfAbsent(player.getUUID(), id -> ticks);
            if (ticks - joined >= 1_800 && ticks % 100 == 0) PlayerEconomy.addGold(player, 10);
        }
    }
}
