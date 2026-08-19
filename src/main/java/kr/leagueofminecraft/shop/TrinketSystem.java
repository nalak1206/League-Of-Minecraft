package kr.leagueofminecraft.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.leagueofminecraft.match.MatchManager;
import kr.leagueofminecraft.match.MatchPhase;
import kr.leagueofminecraft.match.MatchScoreboardTeams;
import kr.leagueofminecraft.match.MatchTeam;
import kr.leagueofminecraft.match.TeamWardRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Alt+4 trinket behavior. The M screen switches between ward and lens. */
public final class TrinketSystem {
    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    private static final List<PlacedWard> WARDS = new ArrayList<>();
    private static boolean initialized;

    private TrinketSystem() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            Iterator<PlacedWard> iterator = WARDS.iterator();
            while (iterator.hasNext()) {
                PlacedWard ward = iterator.next();
                if (now < ward.expiresAt() && ward.entity().isAlive()) continue;
                MatchScoreboardTeams.removeEntity(server, ward.entity());
                ward.entity().discard();
                iterator.remove();
            }
        });
    }

    public static String use(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now)
            return String.format(java.util.Locale.ROOT, "장신구 재사용 %.1f초", (readyAt - now) / 1000.0);
        LolTrinket trinket = PlayerEconomy.trinket(player);
        if (trinket == LolTrinket.ORACLE_LENS) {
            if (MatchManager.phase() == MatchPhase.RUNNING && MatchManager.team(player).isPlayable())
                return revealEnemyWards(player, now);
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(12.0), target -> target != player && target.isAlive());
            for (LivingEntity target : targets)
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            READY_AT.put(player.getUUID(), now + 60_000);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.7f, 1.35f);
            return "예언자의 렌즈: 주변 대상 감지";
        }

        ServerLevel level = player.level();
        Vec3 location = player.pick(8.0, 0.0f, false).getLocation();
        ArmorStand ward = new ArmorStand(level, location.x, location.y, location.z);
        ward.setNoGravity(true);
        ward.setInvulnerable(true);
        ward.setInvisible(true);
        ward.setGlowingTag(false);
        ward.setSilent(true);
        ward.setNoBasePlate(true);
        ward.addTag("lol_stealth_ward");
        ward.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LolTrinket.STEALTH_WARD.icon()));
        level.addFreshEntity(ward);
        MatchTeam ownerTeam = MatchManager.team(player);
        MatchScoreboardTeams.addEntity(level.getServer(), ward, ownerTeam);
        WARDS.add(new PlacedWard(ward, player.getUUID(), ownerTeam, now + 90_000));
        READY_AT.put(player.getUUID(), now + 30_000);
        level.playSound(null, ward.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE,
                SoundSource.PLAYERS, 0.8f, 1.5f);
        return "투명 와드 설치";
    }

    public static void showResult(ServerPlayer player, String result) {
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§a" + result)));
    }

    private static String revealEnemyWards(ServerPlayer player, long now) {
        MatchTeam viewerTeam = MatchManager.team(player);
        int found = 0;
        for (PlacedWard ward : WARDS) {
            if (!ward.entity().isAlive() || ward.entity().distanceToSqr(player) > 144.0) continue;
            if (!TeamWardRules.canRevealWithLens(viewerTeam, ward.team(), MatchManager.phase())) continue;
            ward.entity().addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            found++;
        }
        READY_AT.put(player.getUUID(), now + 60_000);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.7f, 1.35f);
        return "예언자의 렌즈: 적 와드 " + found + "개 감지";
    }

    private record PlacedWard(ArmorStand entity, UUID owner, MatchTeam team, long expiresAt) {}
}
