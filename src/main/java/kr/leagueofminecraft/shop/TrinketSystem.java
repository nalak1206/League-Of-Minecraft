package kr.leagueofminecraft.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.leagueofminecraft.match.MatchManager;
import kr.leagueofminecraft.match.MatchPhase;
import kr.leagueofminecraft.match.MatchScoreboardTeams;
import kr.leagueofminecraft.match.MatchTeam;
import kr.leagueofminecraft.match.TeamWardRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.InteractionResult;

/** Alt+4 trinket behavior. The M screen switches between ward and lens. */
public final class TrinketSystem {
    private static final String WARD_TAG = "lol_stealth_ward";
    private static final String OWNER_PREFIX = "lol_ward_owner_";
    private static final String TEAM_PREFIX = "lol_ward_team_";
    private static final String EXPIRES_PREFIX = "lol_ward_expires_";
    private static final String HEALTH_PREFIX = "lol_ward_health_";
    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    private static final List<PlacedWard> WARDS = new ArrayList<>();
    private static boolean initialized;

    private TrinketSystem() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (!(target instanceof ArmorStand ward) || !ward.entityTags().contains(WARD_TAG))
                return InteractionResult.PASS;
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
                attackWard(serverPlayer, ward);
            return InteractionResult.SUCCESS;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            WARDS.clear();
            READY_AT.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            if (server.getTickCount() % 20 == 0) {
                restoreLoadedWards(server, now);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) refreshWardCharges(player, now);
            }
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
        restoreLoadedWards(level.getServer(), now);
        WardChargeRules.State charge = refreshWardCharges(player, now);
        int activeWards = (int) WARDS.stream().filter(ward -> ward.owner().equals(player.getUUID())
                && ward.entity().isAlive()).count();
        if (activeWards >= WardChargeRules.MAX_ACTIVE_WARDS)
            return "설치 한도: 활성 와드는 최대 2개입니다";
        if (charge.charges() <= 0)
            return String.format(java.util.Locale.ROOT, "와드 충전 중 %.1f초",
                    Math.max(0L, charge.rechargeAt() - now) / 1000.0);
        Vec3 location = player.pick(8.0, 0.0f, false).getLocation();
        ArmorStand ward = new ArmorStand(level, location.x, location.y, location.z);
        ward.setNoGravity(true);
        ward.setInvulnerable(true);
        ward.setInvisible(true);
        ward.setGlowingTag(false);
        ward.setSilent(true);
        ward.setNoBasePlate(true);
        ward.addTag(WARD_TAG);
        ward.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LolTrinket.STEALTH_WARD.icon()));
        level.addFreshEntity(ward);
        MatchTeam ownerTeam = MatchManager.team(player);
        long expiresAt = now + 90_000;
        ward.addTag(OWNER_PREFIX + player.getUUID());
        ward.addTag(TEAM_PREFIX + ownerTeam.name());
        ward.addTag(EXPIRES_PREFIX + expiresAt);
        ward.addTag(HEALTH_PREFIX + TeamWardRules.MAX_HEALTH);
        MatchScoreboardTeams.addEntity(level.getServer(), ward, ownerTeam);
        WARDS.add(new PlacedWard(ward, player.getUUID(), ownerTeam, expiresAt, TeamWardRules.MAX_HEALTH));
        PlayerEconomy.setWardChargeState(player, WardChargeRules.consume(charge, now));
        kr.leagueofminecraft.core.LolPlayerDataStore.save(player.level().getServer());
        level.playSound(null, ward.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE,
                SoundSource.PLAYERS, 0.8f, 1.5f);
        return "투명 와드 설치";
    }

    public static void showResult(ServerPlayer player, String result) {
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§a" + result)));
    }

    public static void reset(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        PlayerEconomy.setWardChargeState(player,
                new WardChargeRules.State(WardChargeRules.MAX_CHARGES, 0L));
        List<PlacedWard> owned = WARDS.stream()
                .filter(ward -> ward.owner().equals(player.getUUID())).toList();
        for (PlacedWard ward : owned) removeWard(player.level().getServer(), ward);
    }

    public static String wardStatus(ServerPlayer player) {
        long now = System.currentTimeMillis();
        WardChargeRules.State state = refreshWardCharges(player, now);
        if (state.charges() >= WardChargeRules.MAX_CHARGES) return "§a" + state.charges() + "/2 충전";
        long seconds = Math.max(1L, (state.rechargeAt() - now + 999L) / 1000L);
        return "§e" + state.charges() + "/2 §7(다음 " + seconds + "초)";
    }

    private static WardChargeRules.State refreshWardCharges(ServerPlayer player, long now) {
        WardChargeRules.State previous = PlayerEconomy.wardChargeState(player);
        WardChargeRules.State refreshed = WardChargeRules.refresh(previous, now);
        if (!refreshed.equals(previous)) {
            PlayerEconomy.setWardChargeState(player, refreshed);
            kr.leagueofminecraft.core.LolPlayerDataStore.save(player.level().getServer());
        }
        return refreshed;
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

    private static void attackWard(ServerPlayer player, ArmorStand entity) {
        PlacedWard ward = find(entity.getUUID());
        if (ward == null) ward = parse(entity);
        if (ward == null) {
            entity.discard();
            return;
        }
        MatchTeam attackerTeam = MatchManager.team(player);
        if (!TeamWardRules.canDestroy(attackerTeam, ward.team(), MatchManager.phase())) {
            showResult(player, "아군 와드는 파괴할 수 없습니다");
            return;
        }
        int remainingHealth = TeamWardRules.remainingHealthAfterAttack(ward.health());
        if (remainingHealth > 0) {
            updateHealth(ward, remainingHealth);
            showResult(player, "와드 체력 §f" + remainingHealth + "§a/" + TeamWardRules.MAX_HEALTH);
            player.level().sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 0.6, entity.getZ(),
                    5, 0.18, 0.25, 0.18, 0.04);
            player.level().playSound(null, entity.blockPosition(), SoundEvents.ARMOR_STAND_HIT,
                    SoundSource.PLAYERS, 0.55f, 1.5f);
            return;
        }
        int gold = TeamWardRules.destructionGold(attackerTeam, ward.team(), MatchManager.phase());
        removeWard(player.level().getServer(), ward);
        if (gold > 0) {
            PlayerEconomy.addGold(player, gold);
            showResult(player, "적 와드 파괴 §6+" + gold + "G");
        } else {
            showResult(player, "와드 제거");
        }
        player.level().sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 0.6, entity.getZ(),
                12, 0.25, 0.35, 0.25, 0.08);
        player.level().playSound(null, entity.blockPosition(), SoundEvents.ARMOR_STAND_BREAK,
                SoundSource.PLAYERS, 0.8f, 1.25f);
    }

    private static void restoreLoadedWards(net.minecraft.server.MinecraftServer server, long now) {
        Set<UUID> tracked = WARDS.stream().map(ward -> ward.entity().getUUID())
                .collect(java.util.stream.Collectors.toSet());
        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ArmorStand armorStand) || !entity.entityTags().contains(WARD_TAG)
                        || tracked.contains(entity.getUUID())) continue;
                PlacedWard ward = parse(armorStand);
                if (ward == null || ward.expiresAt() <= now) {
                    MatchScoreboardTeams.removeEntity(server, armorStand);
                    armorStand.discard();
                    continue;
                }
                MatchScoreboardTeams.addEntity(server, armorStand, ward.team());
                WARDS.add(ward);
                tracked.add(entity.getUUID());
            }
        }
    }

    private static PlacedWard parse(ArmorStand entity) {
        UUID owner = null;
        MatchTeam team = MatchTeam.UNASSIGNED;
        long expiresAt = 0L;
        int health = TeamWardRules.MAX_HEALTH;
        for (String tag : entity.entityTags()) {
            try {
                if (tag.startsWith(OWNER_PREFIX)) owner = UUID.fromString(tag.substring(OWNER_PREFIX.length()));
                else if (tag.startsWith(TEAM_PREFIX)) team = MatchTeam.valueOf(tag.substring(TEAM_PREFIX.length()));
                else if (tag.startsWith(EXPIRES_PREFIX)) expiresAt = Long.parseLong(tag.substring(EXPIRES_PREFIX.length()));
                else if (tag.startsWith(HEALTH_PREFIX)) health = Integer.parseInt(tag.substring(HEALTH_PREFIX.length()));
            } catch (IllegalArgumentException ignored) { }
        }
        return owner != null && expiresAt > 0
                ? new PlacedWard(entity, owner, team, expiresAt, Math.max(1, Math.min(TeamWardRules.MAX_HEALTH, health)))
                : null;
    }

    private static PlacedWard find(UUID entityId) {
        return WARDS.stream().filter(ward -> ward.entity().getUUID().equals(entityId)).findFirst().orElse(null);
    }

    private static void removeWard(net.minecraft.server.MinecraftServer server, PlacedWard ward) {
        WARDS.removeIf(existing -> existing.entity().getUUID().equals(ward.entity().getUUID()));
        MatchScoreboardTeams.removeEntity(server, ward.entity());
        ward.entity().discard();
    }

    private static void updateHealth(PlacedWard ward, int health) {
        for (String tag : Set.copyOf(ward.entity().entityTags()))
            if (tag.startsWith(HEALTH_PREFIX)) ward.entity().removeTag(tag);
        ward.entity().addTag(HEALTH_PREFIX + health);
        int index = WARDS.indexOf(ward);
        if (index >= 0) WARDS.set(index,
                new PlacedWard(ward.entity(), ward.owner(), ward.team(), ward.expiresAt(), health));
    }

    private record PlacedWard(ArmorStand entity, UUID owner, MatchTeam team, long expiresAt, int health) {}
}
