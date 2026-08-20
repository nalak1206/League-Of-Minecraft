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
import kr.leagueofminecraft.match.MatchManager;
import kr.leagueofminecraft.match.MatchPhase;
import kr.leagueofminecraft.match.RecallSystem;
import kr.leagueofminecraft.champion.malphite.MalphiteSkills;

/** Automatic League-style economy and champion progression. */
public final class LolMatchSystem {
    private static final Map<UUID, Long> MATCH_JOIN_TICK = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> CONTRIBUTORS = new HashMap<>();
    private static boolean initialized;

    private LolMatchSystem() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLivingEntityEvents.AFTER_DEATH.register(LolMatchSystem::afterDeath);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) -> {
            if (LegendaryItemEffects.isKnightsVowTransfer(target)) return true;
            if (target instanceof ServerPlayer victim && source.getEntity() instanceof ServerPlayer attacker
                    && MatchManager.areAllies(victim, attacker)) return false;
            return LegendaryItemEffects.allowDamage(target);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
            if (taken > 0) {
                if (target instanceof ServerPlayer player) {
                    RecallSystem.onDamaged(player);
                    MalphiteSkills.onDamaged(player);
                }
                if (source.getEntity() instanceof ServerPlayer attacker && target != attacker)
                    CONTRIBUTORS.computeIfAbsent(target.getUUID(), id -> new HashMap<>())
                            .put(attacker.getUUID(), (long) target.level().getServer().getTickCount());
                LegendaryItemEffects.afterDamage(target, source, taken);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(LolMatchSystem::tick);
    }

    private static void afterDeath(LivingEntity victim, DamageSource source) {
        Map<UUID, Long> contributors = CONTRIBUTORS.remove(victim.getUUID());
        if (!(source.getEntity() instanceof ServerPlayer killer) || victim == killer) return;
        LegendaryItemEffects.onTakedown(killer, victim);
        TakedownRewardRules.Reward reward = victim instanceof ServerPlayer
                ? new TakedownRewardRules.Reward(TakedownRewardRules.CHAMPION_KILL_GOLD,
                        TakedownRewardRules.CHAMPION_KILL_XP)
                : TakedownRewardRules.minionReward(victim.getMaxHealth());
        int gold = reward.gold();
        int xp = reward.xp();
        PlayerEconomy.addGold(killer, gold);
        ChampionProgression.addXp(killer, xp);
        killer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                "§6+" + gold + "G §b+" + xp + " XP")));
        if (!(victim instanceof ServerPlayer defeated) || contributors == null) return;
        long deathTick = defeated.level().getServer().getTickCount();
        for (Map.Entry<UUID, Long> entry : contributors.entrySet()) {
            if (entry.getKey().equals(killer.getUUID())
                    || !TakedownRewardRules.assistEligible(entry.getValue(), deathTick)) continue;
            ServerPlayer assistant = defeated.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (assistant == null || MatchManager.areAllies(assistant, defeated)) continue;
            PlayerEconomy.addGold(assistant, TakedownRewardRules.CHAMPION_ASSIST_GOLD);
            ChampionProgression.addXp(assistant, TakedownRewardRules.CHAMPION_ASSIST_XP);
            LegendaryItemEffects.onTakedown(assistant, victim);
            assistant.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                    "§e어시스트 §6+" + TakedownRewardRules.CHAMPION_ASSIST_GOLD
                            + "G §b+" + TakedownRewardRules.CHAMPION_ASSIST_XP + " XP")));
        }
    }

    private static void tick(MinecraftServer server) {
        long ticks = server.getTickCount();
        if (ticks % 200 == 0) CONTRIBUTORS.values().forEach(map ->
                map.entrySet().removeIf(entry -> ticks - entry.getValue() > TakedownRewardRules.ASSIST_WINDOW_TICKS));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerEconomy.tickRegen(player, ticks);
            if (ChampionManager.mode(player) != ChampionManager.GameMode.MATCH
                    || MatchManager.phase() != MatchPhase.RUNNING) {
                MATCH_JOIN_TICK.remove(player.getUUID());
                continue;
            }
            long joined = MATCH_JOIN_TICK.computeIfAbsent(player.getUUID(), id -> ticks);
            if (ticks - joined >= 1_800 && ticks % 100 == 0) PlayerEconomy.addGold(player, 10);
        }
    }
}
