package kr.darius.skills;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import kr.darius.skills.mixin.BlockDisplayAccessor;
import kr.darius.skills.mixin.DisplayAccessor;
import kr.darius.skills.mixin.ItemDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.darius.skills.shop.LegendaryItemEffects;
import kr.darius.skills.shop.PlayerEconomy;
import kr.darius.skills.combat.CombatEngine;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class DariusSkills implements ModInitializer {
    public static final Item NOXIAN_POWER = registerWeapon("noxian_power",
            new Item.Properties().axe(ToolMaterial.DIAMOND, -1.0f, -3.0f).rarity(Rarity.RARE).fireResistant());
    public static final Item CRIPPLING_STRIKE_WEAPON = registerWeapon("crippling_strike",
            new Item.Properties().axe(ToolMaterial.NETHERITE, -2.0f, -3.0f).rarity(Rarity.EPIC).fireResistant());
    public static final Item NOXIAN_GUILLOTINE = registerWeapon("noxian_guillotine",
            new Item.Properties().axe(ToolMaterial.NETHERITE, -2.0f, -3.2f).rarity(Rarity.EPIC).fireResistant());
    public static final Item YONE_STEEL_SWORD = registerSteelSword();
    public static final Item YONE_AZAKANA_SWORD = registerWeapon("yone_azakana_sword",
            new Item.Properties().sword(ToolMaterial.NETHERITE, -2.0f, -2.4f).rarity(Rarity.EPIC).fireResistant());
    private static final ResourceKey<DamageType> NOXIAN_GUILLOTINE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("darius_skills", "noxian_guillotine"));
    private static final Identifier APPREHEND_ARMOR_SHRED =
            Identifier.fromNamespaceAndPath("darius_skills", "apprehend_armor_shred");

    private static Item registerWeapon(String path, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("darius_skills", path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
    }

    private static Item registerSteelSword() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("darius_skills", "steel_sword"));
        Item.Properties properties = new Item.Properties().sword(ToolMaterial.NETHERITE, -2.0f, -2.4f)
                .rarity(Rarity.RARE).fireResistant().setId(key);
        return Registry.register(BuiltInRegistries.ITEM, key, new SteelSwordItem(properties));
    }
    private static final long[] COOLDOWNS_MS = {0, 5_000, 16_000, 100_000, 5_000};
    private static final float[] GUILLOTINE_MAX_HEALTH_RATIOS = {0.01f, 0.05f, 0.07f, 0.10f, 0.15f, 0.35f};
    private static final long[] CAST_TIMES_MS = {0, 750, 250, 180, 0};
    private static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    private static final Map<UUID, BleedState> BLEEDS = new HashMap<>();
    private static final Map<UUID, Long> NOXIAN_MIGHT = new HashMap<>();
    private static final Map<UUID, Long> CRIPPLING_STRIKE = new HashMap<>();
    private static final List<PendingCast> PENDING_CASTS = new ArrayList<>();
    private static final List<GuillotineSlam> GUILLOTINE_SLAMS = new ArrayList<>();
    private static final Map<UUID, LivingEntity> GUILLOTINE_TARGETS = new HashMap<>();
    private static final Map<UUID, GuillotineHeadCharge> GUILLOTINE_HEADS = new HashMap<>();
    private static final Map<UUID, Long> R_RECAST_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> GUILLOTINE_ARMED = new HashMap<>();
    private static final Map<UUID, Long> REVERT_TO_DIAMOND = new HashMap<>();
    private static final Map<UUID, Item> LOCKED_WEAPON = new HashMap<>();
    private static final Map<UUID, Long> APPREHEND_DISABLE_UNTIL = new HashMap<>();
    private static final Map<UUID, ArmorShredState> APPREHEND_ARMOR_SHRED_UNTIL = new HashMap<>();
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(0x08060C, 2.2f);
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(0x4A126B, 2.0f);
    private static final DustParticleOptions MAGENTA_DUST = new DustParticleOptions(0x96105F, 1.8f);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(SkillPayload.TYPE, SkillPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                YoneAttackAnimationPayload.TYPE, YoneAttackAnimationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SkillPayload.TYPE, (payload, context) ->
                ChampionManager.cast(context.player(), payload.skill()));
        ChampionManager.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("dariusreset").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    LAST_CAST.put(player.getUUID(), new long[5]);
                    CRIPPLING_STRIKE.remove(player.getUUID());
                    PENDING_CASTS.removeIf(cast -> {
                        if (cast.player != player) return false;
                        if (cast.qRange != null) cast.qRange.discard();
                        return true;
                    });
                    GUILLOTINE_SLAMS.removeIf(slam -> {
                        if (slam.player != player) return false;
                        if (slam.dragonHead != null) slam.dragonHead.discard();
                        return true;
                    });
                    GUILLOTINE_TARGETS.remove(player.getUUID());
                    GUILLOTINE_ARMED.remove(player.getUUID());
                    GuillotineHeadCharge charge = GUILLOTINE_HEADS.remove(player.getUUID());
                    if (charge != null) charge.head.discard();
                    R_RECAST_UNTIL.remove(player.getUUID());
                    player.setNoGravity(false);
                    return 1;
                })));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (YoneSkills.isBodyEcho(entity)) return InteractionResult.FAIL;
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && CrowdControl.blocksBasicAttack(serverPlayer)) return InteractionResult.FAIL;
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && YoneSkills.blocksBasicAttack(serverPlayer)) return InteractionResult.FAIL;
            boolean replacesVanillaAttack = false;
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && entity instanceof LivingEntity living)
                replacesVanillaAttack = LegendaryItemEffects.onBasicAttack(serverPlayer, living);
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && ChampionManager.isYone(serverPlayer) && entity instanceof LivingEntity living) {
                if (YoneSkills.basicAttack(serverPlayer, living)) return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && ChampionManager.isDarius(serverPlayer)
                    && entity instanceof LivingEntity living && hasDariusWeapon(serverPlayer)) {
                if (serverPlayer.getMainHandItem().is(NOXIAN_GUILLOTINE)
                        && GUILLOTINE_ARMED.getOrDefault(serverPlayer.getUUID(), 0L) > System.currentTimeMillis()) {
                    startGuillotineFromHit(serverPlayer, living);
                    return InteractionResult.SUCCESS;
                }
                applyBleed(serverPlayer, living);
                Long armedUntil = CRIPPLING_STRIKE.remove(serverPlayer.getUUID());
                long hitAt = System.currentTimeMillis();
                if (armedUntil != null && armedUntil <= hitAt) {
                    LAST_CAST.computeIfAbsent(serverPlayer.getUUID(), id -> new long[5])[4] = hitAt;
                } else if (armedUntil != null) {
                    long[] times = LAST_CAST.computeIfAbsent(serverPlayer.getUUID(), id -> new long[5]);
                    times[4] = hitAt;
                    float attack = (float) serverPlayer.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    boolean alive = living.isAlive();
                    CombatEngine.deal(serverPlayer, living, attack, CombatEngine.DamageKind.PHYSICAL,
                            CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 5));
                    serverPlayer.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR, living.getX(), living.getY() + 0.35,
                            living.getZ(), 10, 0.45, 0.10, 0.45, 0.14);
                    serverPlayer.level().sendParticles(ParticleTypes.CRIMSON_SPORE, living.getX(), living.getY() + 0.15,
                            living.getZ(), 14, 0.55, 0.06, 0.55, 0.04);
                    serverPlayer.level().sendParticles(ParticleTypes.WITCH, living.getX(), living.getY() + 0.7,
                            living.getZ(), 18, 0.55, 0.35, 0.55, 0.06);
                    serverPlayer.level().playSound(null, living.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                            SoundSource.PLAYERS, 0.85f, 0.78f);
                    serverPlayer.level().playSound(null, living.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                            SoundSource.PLAYERS, 0.40f, 0.92f);
                    maceShockwave(serverPlayer.level(), living, serverPlayer, false);
                    if (alive && !living.isAlive()) {
                        times[4] -= 2_500;
                    }
                    selectOrCreateWeapon(serverPlayer, NOXIAN_POWER);
                    REVERT_TO_DIAMOND.remove(serverPlayer.getUUID());
                }
            }
            if (!level.isClientSide() && !replacesVanillaAttack && player instanceof ServerPlayer serverPlayer
                    && entity instanceof LivingEntity living && !ChampionManager.isYone(serverPlayer)) {
                float strength = serverPlayer.getAttackStrengthScale(0.5f);
                float scale = 0.2f + strength * strength * 0.8f;
                float damage = (float) serverPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale;
                CombatEngine.deal(serverPlayer, living, damage, CombatEngine.DamageKind.PHYSICAL,
                        CombatEngine.KnockbackPolicy.PRESERVE_MOVEMENT);
                serverPlayer.resetAttackStrengthTicker();
                return InteractionResult.SUCCESS;
            }
            return replacesVanillaAttack ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(DariusSkills::tickBleeds);
    }

    public static void castSelected(ServerPlayer player, int skill) { cast(player, skill); }

    public static void equip(ServerPlayer player) { selectOrCreateWeapon(player, NOXIAN_POWER); }

    public static void reset(ServerPlayer player) {
        LAST_CAST.put(player.getUUID(), new long[5]);
        CRIPPLING_STRIKE.remove(player.getUUID());
        GUILLOTINE_TARGETS.remove(player.getUUID());
        GUILLOTINE_ARMED.remove(player.getUUID());
        R_RECAST_UNTIL.remove(player.getUUID());
        REVERT_TO_DIAMOND.remove(player.getUUID());
        player.setNoGravity(false);
    }

    private static void cast(ServerPlayer player, int skill) {
        if (skill < 1 || skill > 4) return;
        long now = System.currentTimeMillis();
        if (APPREHEND_DISABLE_UNTIL.getOrDefault(player.getUUID(), 0L) > now) return;
        long[] times = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        Long wArmedUntil = CRIPPLING_STRIKE.get(player.getUUID());
        if (skill == 4 && wArmedUntil != null) {
            if (wArmedUntil > now) return;
            CRIPPLING_STRIKE.remove(player.getUUID());
            times[4] = now;
        }
        long remaining = PlayerEconomy.cooldownMillis(player, COOLDOWNS_MS[skill]) - (now - times[skill]);
        boolean rRecast = skill == 3 && R_RECAST_UNTIL.getOrDefault(player.getUUID(), 0L) > now;
        if (remaining > 0 && !rRecast) return;
        LegendaryItemEffects.onSkillInput(player);
        if (skill == 3) {
            selectOrCreateWeapon(player, NOXIAN_GUILLOTINE);
            GUILLOTINE_ARMED.put(player.getUUID(), now + 10_000);
            return;
        }
        if (!selectSkillWeapon(player, skill)) {
            if (skill == 3) {
                GUILLOTINE_TARGETS.remove(player.getUUID());
                GuillotineHeadCharge charge = GUILLOTINE_HEADS.remove(player.getUUID());
                if (charge != null) charge.head.discard();
            }
            return;
        }
        if (skill == 4) {
            // W cooldown starts only when the empowered hit is consumed or the 4-second arm window expires.
            times[4] = 0;
        } else {
            times[skill] = now;
        }
        if (CAST_TIMES_MS[skill] > 0) {
            QRangeVisual qRange = skill == 1 ? createQRangeVisual(player.level(), player) : null;
            PENDING_CASTS.add(new PendingCast(player, skill, now + CAST_TIMES_MS[skill], times, qRange));
            castTelegraph(player, skill);
        } else {
            cripplingStrike(player);
        }
        if (skill == 1 || skill == 2) {
            REVERT_TO_DIAMOND.put(player.getUUID(), now + CAST_TIMES_MS[skill] + 150);
        } else if (skill == 4) {
            REVERT_TO_DIAMOND.put(player.getUUID(), now + 4_000);
        }
    }

    private static void startGuillotineFromHit(ServerPlayer player, LivingEntity target) {
        long now = System.currentTimeMillis();
        long[] times = LAST_CAST.computeIfAbsent(player.getUUID(), id -> new long[5]);
        boolean rRecast = R_RECAST_UNTIL.getOrDefault(player.getUUID(), 0L) > now;
        if (PlayerEconomy.cooldownMillis(player, COOLDOWNS_MS[3]) - (now - times[3]) > 0 && !rRecast) return;
        GUILLOTINE_ARMED.remove(player.getUUID());
        if (rRecast) R_RECAST_UNTIL.remove(player.getUUID());
        times[3] = now;
        GUILLOTINE_TARGETS.put(player.getUUID(), target);
        Display.ItemDisplay head = createDragonHeadDisplay(player.level(), target, player.getY() + 6.0);
        head.setPos(player.getX(), player.getY() + 6.0, player.getZ());
        faceDragonHead(head, target.getEyePosition());
        GUILLOTINE_HEADS.put(player.getUUID(), new GuillotineHeadCharge(player, target, head, now));
        PENDING_CASTS.add(new PendingCast(player, 3, now + CAST_TIMES_MS[3], times, null));
        castTelegraph(player, 3);
    }

    private static boolean hasDariusWeapon(ServerPlayer player) {
        var stack = player.getMainHandItem();
        return stack.is(NOXIAN_POWER) || stack.is(CRIPPLING_STRIKE_WEAPON) || stack.is(NOXIAN_GUILLOTINE);
    }

    public static boolean isDariusWeapon(ItemStack stack) {
        return stack.is(NOXIAN_POWER) || stack.is(CRIPPLING_STRIKE_WEAPON) || stack.is(NOXIAN_GUILLOTINE);
    }

    private static boolean selectSkillWeapon(ServerPlayer player, int skill) {
        Item wanted = switch (skill) {
            case 1, 2 -> NOXIAN_POWER;
            case 4 -> CRIPPLING_STRIKE_WEAPON;
            case 3 -> NOXIAN_GUILLOTINE;
            default -> null;
        };
        if (wanted == null) return false;
        return selectOrCreateWeapon(player, wanted);
    }

    private static boolean selectOrCreateWeapon(ServerPlayer player, Item wanted) {
        ItemStack weapon = new ItemStack(wanted);
        player.getInventory().setItem(0, weapon);
        for (int slot = 1; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(NOXIAN_POWER) || stack.is(CRIPPLING_STRIKE_WEAPON) || stack.is(NOXIAN_GUILLOTINE)
                    || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE) || stack.is(Items.MACE)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        player.getInventory().setSelectedSlot(0);
        player.connection.send(new ClientboundSetHeldSlotPacket(0));
        LOCKED_WEAPON.put(player.getUUID(), wanted);
        return true;
    }

    private static void enforceLockedWeapon(ServerPlayer player) {
        Item wanted = LOCKED_WEAPON.get(player.getUUID());
        if (wanted == null) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (isDariusWeapon(stack)) {
                    wanted = stack.getItem();
                    LOCKED_WEAPON.put(player.getUUID(), wanted);
                    break;
                }
            }
        }
        if (wanted == null) return;
        if (player.getInventory().getItem(0).is(wanted)) return;

        ItemStack weapon = ItemStack.EMPTY;
        for (int slot = 1; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(wanted)) {
                weapon = stack.copy();
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                break;
            }
        }
        if (weapon.isEmpty()) weapon = new ItemStack(wanted);
        ItemStack displaced = player.getInventory().getItem(0);
        if (!displaced.isEmpty() && !isDariusWeapon(displaced)) player.getInventory().add(displaced.copy());
        player.getInventory().setItem(0, weapon);
    }

    private static List<LivingEntity> nearby(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.position(), player.position()).inflate(radius, 2.0, radius),
                e -> e != player && e.isAlive());
    }

    private static void decimate(ServerPlayer player) {
        ServerLevel level = player.level();
        int outerHits = 0;
        float attack = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : nearby(player, 5.6)) {
            double horizontal = target.position().subtract(player.position()).multiply(1, 0, 1).length();
            // Only the axe-blade ring hits: the black inner circle is a complete dead zone.
            if (horizontal < 2.25 || horizontal > 5.25) continue;
            float damage = 3.0f + attack * 1.4f;
            if (target.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                outerHits++;
                applyBleed(player, target);
            }
        }
        if (outerHits > 0) {
            float missing = player.getMaxHealth() - player.getHealth();
            player.heal(missing * Math.min(0.51f, outerHits * 0.17f));
            level.sendParticles(MAGENTA_DUST, player.getX(), player.getY() + 1.2, player.getZ(),
                    Math.min(12, outerHits * 4), 0.45, 0.7, 0.45, 0.08);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(),
                    Math.min(18, outerHits * 5), 0.7, 0.5, 0.7, 0.14);
        }
        for (int i = 0; i < 72; i++) {
            double a = Math.PI * 2 * i / 72.0;
            level.sendParticles(PURPLE_DUST,
                    player.getX() + Math.cos(a) * 5.25, player.getY() + 0.25,
                    player.getZ() + Math.sin(a) * 5.25, 2, 0.02, 0.02, 0.02, 0);
            level.sendParticles(ParticleTypes.WITCH,
                    player.getX() + Math.cos(a) * 4.9, player.getY() + 0.35,
                    player.getZ() + Math.sin(a) * 4.9, 1, 0.03, 0.03, 0.03, 0.01);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.75f);
    }

    private static void apprehend(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        for (LivingEntity target : nearby(player, 6.0)) {
            Vec3 delta = target.position().subtract(player.position());
            double forward = delta.dot(look);
            double side = Math.abs(delta.dot(right));
            if (forward >= 0.5 && forward <= 5.35 && side <= 1.75 && Math.abs(delta.y) <= 2.5) {
                double oldX = target.getX();
                double oldY = target.getY();
                double oldZ = target.getZ();
                Vec3 destination = player.position().add(look.scale(1.6));
                Vec3 pull = destination.subtract(target.position());
                target.setDeltaMovement(pull.x * 0.45, Math.max(0.15, pull.y * 0.25), pull.z * 0.45);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 255, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20, 255, false, true));
                applyApprehendArmorShred(target);
                if (target instanceof ServerPlayer caughtPlayer) {
                    APPREHEND_DISABLE_UNTIL.put(caughtPlayer.getUUID(), System.currentTimeMillis() + 1_000);
                }
                level.sendParticles(BLACK_DUST, target.getX(), target.getY() + 0.2, target.getZ(),
                        10, 0.35, 0.1, 0.35, 0.08);
                level.sendParticles(MAGENTA_DUST, oldX, oldY + 0.8, oldZ,
                        45, 0.7, 0.8, 0.7, 0.18);
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 0.8, target.getZ(),
                        35, 0.55, 0.7, 0.55, 0.08);
            }
        }
        for (int i = 1; i <= 12; i++) {
            Vec3 p = player.position().add(look.scale(i * 0.5));
            level.sendParticles(MAGENTA_DUST, p.x, p.y + 1.0, p.z, 3, 0.5, 0.2, 0.5, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private static void beginGuillotine(ServerPlayer player, long[] times, long now) {
        ServerLevel level = player.level();
        LivingEntity best = GUILLOTINE_TARGETS.remove(player.getUUID());
        GuillotineHeadCharge charge = GUILLOTINE_HEADS.remove(player.getUUID());
        if (best == null || !best.isAlive()) {
            if (charge != null) charge.head.discard();
            player.setNoGravity(false);
            times[3] = 0;
            return;
        }
        UltimateVoiceLines.shout(player, ChampionManager.Champion.DARIUS);
        /* Target was validated before the leap, so a failed cast never launches the player. */
        player.teleportTo(best.getX(), best.getY() + 4.5, best.getZ());
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        guillotineChargeVfx(level, player, best);
        Display.ItemDisplay dragonHead = charge != null ? charge.head : createDragonHeadDisplay(level, best, best.getY() + 5.0);
        dragonHead.setPos(best.getX(), player.getY() + 5.0, best.getZ());
        faceDragonHead(dragonHead, best.getEyePosition());
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 0.7, player.getZ(),
                90, 1.2, 1.6, 1.2, 0.14);
        level.sendParticles(MAGENTA_DUST, player.getX(), player.getY() + 0.8, player.getZ(),
                55, 1.0, 1.4, 1.0, 0.10);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.25f, 0.68f);
        GUILLOTINE_SLAMS.add(new GuillotineSlam(player, best, dragonHead, now, now + 160, times));
    }

    private static void finishGuillotine(GuillotineSlam slam) {
        ServerPlayer player = slam.player;
        LivingEntity best = slam.target;
        Display.ItemDisplay dragonHead = slam.dragonHead;
        long[] times = slam.times;
        player.setNoGravity(false);
        if (!player.isAlive() || !best.isAlive()) {
            if (dragonHead != null) dragonHead.discard();
            times[3] = 0;
            return;
        }
        ServerLevel level = player.level();
        double topY = player.getY();
        for (int i = 0; i < 18; i++) {
            double y = best.getY() + 0.4 + (topY - best.getY()) * i / 18.0;
            level.sendParticles(i % 2 == 0 ? PURPLE_DUST : BLACK_DUST,
                    best.getX(), y, best.getZ(), 28, 0.6, 0.25, 0.6, 0.18);
            level.sendParticles(MAGENTA_DUST, best.getX(), y, best.getZ(),
                    12, 0.4, 0.18, 0.4, 0.12);
        }
        player.teleportTo(best.getX(), best.getY() + 0.15, best.getZ());
        player.setDeltaMovement(0, -1.6, 0);
        boolean wasAlive = best.isAlive();
        int stacks = bleedStacks(best);
        float attack = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float bonusAttack = Math.max(0.0f, attack - 1.0f);
        // True damage: flat attack scaling plus a bleed-stack-based share of target maximum health.
        float damage = 5.0f + bonusAttack
                + best.getMaxHealth() * GUILLOTINE_MAX_HEALTH_RATIOS[Math.min(5, stacks)];
        var damageType = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(NOXIAN_GUILLOTINE_DAMAGE);
        DamageSource guillotineDamage = new DamageSource(damageType, player, player);
        best.hurtServer(level, guillotineDamage, damage);
        guillotineImpactVfx(level, best);
        if (dragonHead != null) {
            dragonHead.setPos(best.getX(), best.getY() + 1.1, best.getZ());
            dragonHead.discard();
        }
        level.playSound(null, best.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.3f, 0.58f);
        level.playSound(null, best.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.5f, 0.75f);
        maceShockwave(level, best, player, true);
        if (wasAlive && !best.isAlive()) {
            times[3] = 0;
            R_RECAST_UNTIL.put(player.getUUID(), System.currentTimeMillis() + 20_000);
            activateNoxianMight(player);
            level.sendParticles(MAGENTA_DUST, best.getX(), best.getY() + 1.2, best.getZ(),
                    260, 3.0, 4.0, 3.0, 0.55);
            for (LivingEntity witness : nearby(player, 7.0)) {
                if (witness == best) continue;
                Vec3 flee = witness.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.1);
                witness.setDeltaMovement(flee.x, 0.35, flee.z);
                witness.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 255, false, true));
            }
        }
    }

    private static void cripplingStrike(ServerPlayer player) {
        CRIPPLING_STRIKE.put(player.getUUID(), System.currentTimeMillis() + 4_000);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.7f, 1.5f);
        player.level().sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1.0, player.getZ(),
                12, 0.50, 0.65, 0.50, 0.035);
        player.level().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                15, 0.55, 0.70, 0.55, 0.05);
    }

    private static void applyBleed(ServerPlayer source, LivingEntity target) {
        long now = System.currentTimeMillis();
        boolean empowered = NOXIAN_MIGHT.getOrDefault(source.getUUID(), 0L) > now;
        BleedState state = BLEEDS.get(target.getUUID());
        int stacks = empowered ? 5 : Math.min(5, state == null ? 1 : state.stacks + 1);
        BleedVisual visual = state == null ? new BleedVisual() : state.visual;
        updateBleedVisual(visual, target, stacks);
        BLEEDS.put(target.getUUID(), new BleedState(target, source, visual, stacks, now + 5_000, now + 1_000));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 110, stacks - 1, false, false));
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(),
                    stacks * 3, 0.35, 0.55, 0.35, 0.12 + stacks * 0.02);
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(),
                    stacks * 4, 0.4, 0.65, 0.4, 0.03 + stacks * 0.01);
        }
        if (stacks == 5) activateNoxianMight(source);
    }

    private static int bleedStacks(LivingEntity target) {
        BleedState state = BLEEDS.get(target.getUUID());
        return state != null && state.expiresAt > System.currentTimeMillis() ? state.stacks : 0;
    }

    private static void activateNoxianMight(ServerPlayer player) {
        NOXIAN_MIGHT.put(player.getUUID(), System.currentTimeMillis() + 5_000);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1, false, true));
        player.level().sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1.0, player.getZ(),
                100, 0.8, 1.1, 0.8, 0.12);
        player.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.2, player.getZ(),
                30, 0.7, 0.9, 0.7, 0.2);
    }

    private static void castTelegraph(ServerPlayer player, int skill) {
        ServerLevel level = player.level();
        if (skill == 1) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 0.8f, 0.55f);
        } else if (skill == 2) {
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            for (int i = 1; i <= 10; i++) {
                Vec3 p = player.position().add(look.scale(i * 0.5));
                level.sendParticles(BLACK_DUST, p.x, p.y + 0.8, p.z, 2, 0.35, 0.15, 0.35, 0.01);
            }
        } else if (skill == 3) {
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.5, player.getZ(),
                    80, 0.65, 1.0, 0.65, 0.12);
            player.setDeltaMovement(player.getDeltaMovement().x, 1.05, player.getDeltaMovement().z);
            player.hurtMarked = true;
            level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                    SoundSource.PLAYERS, 1.0f, 0.75f);
        }
    }

    private static void showDecimateRange(ServerLevel level, ServerPlayer player) {
        // Sparse, stationary points keep the moving telegraph readable without a dense trail.
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            level.sendParticles(BLACK_DUST, player.getX() + cos * 2.25,
                    player.getY() + 0.10, player.getZ() + sin * 2.25,
                    1, 0, 0, 0, 0);
            level.sendParticles(PURPLE_DUST, player.getX() + cos * 5.25,
                    player.getY() + 0.16, player.getZ() + sin * 5.25,
                    1, 0, 0, 0, 0);
        }
    }

    private static QRangeVisual createQRangeVisual(ServerLevel level, ServerPlayer player) {
        QRangeVisual visual = new QRangeVisual();
        addQRangeRing(level, visual, Blocks.STAINED_GLASS.black().defaultBlockState(), 2.25, 24);
        addQRangeRing(level, visual, Blocks.STAINED_GLASS.purple().defaultBlockState(), 5.25, 48);
        visual.update(player);
        return visual;
    }

    private static void addQRangeRing(ServerLevel level, QRangeVisual visual,
                                      net.minecraft.world.level.block.state.BlockState state,
                                      double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            Display.BlockDisplay marker = new Display.BlockDisplay(net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY, level);
            ((BlockDisplayAccessor) marker).darius$setBlockState(state);
            ((DisplayAccessor) marker).darius$setTransformation(new Transformation(
                    new Vector3f(-0.09f, 0, -0.09f), new Quaternionf(),
                    new Vector3f(0.18f, 0.025f, 0.18f), new Quaternionf()));
            level.addFreshEntity(marker);
            visual.markers.add(marker);
            visual.offsets.add(new Vec3(Math.cos(angle) * radius, 0.08, Math.sin(angle) * radius));
        }
    }

    private static void updateBleedVisual(BleedVisual visual, LivingEntity target, int stacks) {
        if (!(target.level() instanceof ServerLevel level)) return;
        if (stacks < 5) {
            if (visual.egg != null) {
                visual.egg.discard();
                visual.egg = null;
            }
            while (visual.candles.size() < stacks) {
                Display.BlockDisplay candle = new Display.BlockDisplay(net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY, level);
                ((BlockDisplayAccessor) candle).darius$setBlockState(Blocks.DYED_CANDLE.purple().defaultBlockState());
                level.addFreshEntity(candle);
                visual.candles.add(candle);
            }
            for (int i = 0; i < visual.candles.size(); i++) {
                double angle = Math.PI * 2 * i / Math.max(1, stacks) + level.getGameTime() * 0.035;
                visual.candles.get(i).setPos(target.getX() + Math.cos(angle) * 0.85,
                        target.getY() + target.getBbHeight() * 0.62 + Math.sin(level.getGameTime() * 0.08 + i) * 0.08,
                        target.getZ() + Math.sin(angle) * 0.85);
            }
        } else {
            visual.candles.forEach(Display.BlockDisplay::discard);
            visual.candles.clear();
            if (visual.egg == null) {
                visual.egg = new Display.ItemDisplay(net.minecraft.world.entity.EntityTypes.ITEM_DISPLAY, level);
                ((ItemDisplayAccessor) visual.egg).darius$setItemStack(new ItemStack(Items.DRAGON_HEAD));
                ((DisplayAccessor) visual.egg).darius$setTransformation(new Transformation(
                        new Vector3f(0, 0, 0), new Quaternionf(),
                        new Vector3f(0.8f, 0.8f, 0.8f), new Quaternionf()));
                level.addFreshEntity(visual.egg);
            }
            visual.egg.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.85, target.getZ());
        }
    }

    private static void discardBleedVisual(BleedVisual visual) {
        visual.candles.forEach(Display.BlockDisplay::discard);
        visual.candles.clear();
        if (visual.egg != null) visual.egg.discard();
    }

    private static void maceShockwave(ServerLevel level, LivingEntity center, ServerPlayer attacker, boolean knockback) {
        level.sendParticles(BLACK_DUST, center.getX(), center.getY() + 0.15, center.getZ(),
                80, 2.1, 0.15, 2.1, 0.18);
        level.sendParticles(PURPLE_DUST, center.getX(), center.getY() + 0.25, center.getZ(),
                55, 1.8, 0.20, 1.8, 0.14);
        if (!knockback) return;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(3.5), e -> e != center && e != attacker && e.isAlive())) {
            Vec3 push = nearby.position().subtract(center.position()).multiply(1, 0, 1);
            if (push.lengthSqr() < 0.01) push = new Vec3(0.1, 0, 0);
            push = push.normalize().scale(1.15);
            nearby.setDeltaMovement(push.x, 0.38, push.z);
        }
    }

    private static void guillotineChargeVfx(ServerLevel level, ServerPlayer player, LivingEntity target) {
        level.sendParticles(BLACK_DUST, target.getX(), target.getY() + 2.5, target.getZ(),
                65, 1.8, 2.2, 1.8, 0.10);
        level.sendParticles(PURPLE_DUST, target.getX(), target.getY() + 2.4, target.getZ(),
                50, 1.6, 2.0, 1.6, 0.08);
        level.sendParticles(MAGENTA_DUST, player.getX(), player.getY() + 0.8, player.getZ(),
                45, 1.0, 1.4, 1.0, 0.09);
    }

    private static Display.ItemDisplay createDragonHeadDisplay(ServerLevel level, LivingEntity target, double y) {
        Display.ItemDisplay head = new Display.ItemDisplay(net.minecraft.world.entity.EntityTypes.ITEM_DISPLAY, level);
        ((ItemDisplayAccessor) head).darius$setItemStack(new ItemStack(Items.DRAGON_HEAD));
        ((DisplayAccessor) head).darius$setTransformation(new Transformation(
                new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(5.0f, 5.0f, 5.0f), new Quaternionf()));
        head.setGlowingTag(true);
        head.setPos(target.getX(), y, target.getZ());
        level.addFreshEntity(head);
        return head;
    }

    private static void faceDragonHead(Display.ItemDisplay head, Vec3 targetPosition) {
        Vec3 direction = targetPosition.subtract(head.position());
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Math.atan2(direction.x, direction.z) + Math.PI);
        float pitch = (float) Math.atan2(direction.y, horizontal);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch);
        ((DisplayAccessor) head).darius$setTransformation(new Transformation(
                new Vector3f(0, 0, 0), rotation,
                new Vector3f(5.0f, 5.0f, 5.0f), new Quaternionf()));
    }

    private static void guillotineImpactVfx(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        level.sendParticles(BLACK_DUST, x, y + 1.0, z, 140, 2.8, 2.3, 2.8, 0.24);
        level.sendParticles(PURPLE_DUST, x, y + 1.2, z, 105, 2.5, 2.1, 2.5, 0.20);
        level.sendParticles(MAGENTA_DUST, x, y + 1.0, z, 75, 2.2, 2.0, 2.2, 0.16);

        // Three enormous diagonal claw slashes, not a circle or magic glyph.
        for (int claw = -1; claw <= 1; claw++) {
            for (int i = -18; i <= 18; i++) {
                double t = i / 7.0;
                double px = x + t;
                double py = y + 2.4 - t * 0.55 + claw * 0.48;
                double pz = z + claw * 0.65;
                level.sendParticles(PURPLE_DUST, px, py, pz, 3, 0.08, 0.08, 0.08, 0.025);
                level.sendParticles(MAGENTA_DUST, px, py, pz, 2, 0.06, 0.06, 0.06, 0.018);
            }
        }

        // Dense vertical darkness/energy column from the impact point into the sky.
        for (int i = 0; i < 18; i++) {
            double py = y + i * 0.32;
            level.sendParticles(BLACK_DUST, x, py, z, 8, 0.55, 0.14, 0.55, 0.08);
            level.sendParticles(PURPLE_DUST, x, py, z, 5, 0.42, 0.11, 0.42, 0.05);
            level.sendParticles(MAGENTA_DUST, x, py, z, 3, 0.30, 0.08, 0.30, 0.03);
        }
    }

    private static void clawMarks(ServerLevel level, LivingEntity target, int stacks) {
        for (int claw = 0; claw < Math.min(3, stacks); claw++) {
            for (int i = 0; i < 7; i++) {
                double x = target.getX() - 0.35 + claw * 0.35 + i * 0.035;
                double y = target.getY() + 1.75 - i * 0.12;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, target.getZ() + 0.45, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void wolfSigil(ServerLevel level, LivingEntity target, double scale, boolean burst) {
        double[][] points = {
                {-0.70, 0.70}, {-0.45, 1.05}, {-0.20, 0.72}, {0.20, 0.72}, {0.45, 1.05}, {0.70, 0.70},
                {-0.58, 0.35}, {-0.36, 0.05}, {-0.22, -0.35}, {0.0, -0.58}, {0.22, -0.35},
                {0.36, 0.05}, {0.58, 0.35}, {-0.27, 0.28}, {0.27, 0.28}, {0.0, -0.08}
        };
        double z = target.getZ();
        double baseY = target.getY() + target.getBbHeight() + 1.0;
        for (double[] p : points) {
            level.sendParticles(ParticleTypes.WITCH,
                    target.getX() + p[0] * scale, baseY + p[1] * scale, z,
                    burst ? 3 : 1, 0.03, 0.03, 0.03, 0.01);
        }
    }

    private static void tickBleeds(MinecraftServer server) {
        long now = System.currentTimeMillis();
        CrowdControl.tick();
        LegendaryItemEffects.tick(server);
        YoneSkills.tick(server);
        UltimateVoiceLines.tick(server);
        if (server.getTickCount() % 5 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (UltimateVoiceLines.isTypingFor(player)) continue;
                if (ChampionManager.isDarius(player)) showCooldownActionBar(player, now);
                else YoneSkills.showActionBar(player, now);
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ChampionManager.isDarius(player)) enforceLockedWeapon(player);
        }
        PENDING_CASTS.removeIf(cast -> {
            if (now < cast.executeAt) {
                if (cast.qRange != null && cast.player.isAlive()) cast.qRange.update(cast.player);
                return false;
            }
            if (cast.qRange != null) cast.qRange.discard();
            if (!cast.player.isAlive()) return true;
            switch (cast.skill) {
                case 1 -> decimate(cast.player);
                case 2 -> apprehend(cast.player);
                case 3 -> beginGuillotine(cast.player, cast.times, now);
                default -> { }
            }
            return true;
        });
        GUILLOTINE_HEADS.values().removeIf(charge -> {
            if (!charge.player.isAlive() || !charge.target.isAlive() || charge.head.isRemoved()) {
                if (!charge.head.isRemoved()) charge.head.discard();
                return true;
            }
            charge.head.setPos(charge.player.getX(), charge.player.getY() + 6.0, charge.player.getZ());
            faceDragonHead(charge.head, charge.target.getEyePosition());
            return false;
        });
        GUILLOTINE_SLAMS.removeIf(slam -> {
            if (slam.dragonHead != null && !slam.dragonHead.isRemoved() && slam.target.isAlive()) {
                double progress = Math.max(0.0, Math.min(1.0,
                        (now - slam.startedAt) / (double) (slam.impactAt - slam.startedAt)));
                double eased = progress * progress * progress;
                double startY = slam.player.getY() + 5.0;
                double y = startY - eased * (startY - (slam.target.getY() + 1.1));
                slam.dragonHead.setPos(slam.target.getX(), y, slam.target.getZ());
                faceDragonHead(slam.dragonHead, slam.target.getEyePosition());
                if (server.getTickCount() % 2 == 0 && slam.target.level() instanceof ServerLevel level) {
                    level.sendParticles(PURPLE_DUST, slam.target.getX(), y, slam.target.getZ(),
                            40, 0.9, 0.35, 0.9, 0.12);
                }
            }
            if (now < slam.impactAt) return false;
            finishGuillotine(slam);
            return true;
        });
        BLEEDS.entrySet().removeIf(entry -> {
            BleedState state = entry.getValue();
            if (!state.target.isAlive() || now >= state.expiresAt) {
                discardBleedVisual(state.visual);
                return true;
            }
            updateBleedVisual(state.visual, state.target, state.stacks);
            if (now >= state.nextTickAt) {
                float attack = (float) state.source.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                float bonusAttack = Math.max(0.0f, attack - 1.0f);
                float damage = state.stacks * (1.0f + bonusAttack * 0.30f) / 5.0f;
                if (state.target.level() instanceof ServerLevel level) {
                    Vec3 movementBeforeBleed = state.target.getDeltaMovement();
                    state.target.hurtServer(level, state.source.damageSources().playerAttack(state.source), damage);
                    // Hemorrhage is damage-over-time only; it must not add vanilla hit knockback.
                    state.target.setDeltaMovement(movementBeforeBleed);
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, state.target.getX(), state.target.getY() + 1.0,
                            state.target.getZ(), state.stacks, 0.25, 0.35, 0.25, 0.05);
                }
                state.nextTickAt += 1_000;
            }
            return false;
        });
        NOXIAN_MIGHT.entrySet().removeIf(entry -> now >= entry.getValue());
        APPREHEND_DISABLE_UNTIL.entrySet().removeIf(entry -> now >= entry.getValue());
        APPREHEND_ARMOR_SHRED_UNTIL.entrySet().removeIf(entry -> {
            ArmorShredState state = entry.getValue();
            if (now < state.expiresAt && state.target.isAlive()) return false;
            var armor = state.target.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(APPREHEND_ARMOR_SHRED);
            return true;
        });
        CRIPPLING_STRIKE.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) return false;
            long[] times = LAST_CAST.computeIfAbsent(entry.getKey(), id -> new long[5]);
            times[4] = now;
            return true;
        });
        R_RECAST_UNTIL.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) return false;
            long[] times = LAST_CAST.get(entry.getKey());
            if (times != null && times[3] == 0) {
                times[3] = now - COOLDOWNS_MS[3] / 2;
            }
            return true;
        });
        REVERT_TO_DIAMOND.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) return false;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) selectOrCreateWeapon(player, NOXIAN_POWER);
            return true;
        });
    }

    private static void showCooldownActionBar(ServerPlayer player, long now) {
        long[] times = LAST_CAST.get(player.getUUID());
        String q = cooldownText(player, times, 1, now);
        String w = CRIPPLING_STRIKE.getOrDefault(player.getUUID(), 0L) > now
                ? "§dACTIVE"
                : cooldownText(player, times, 4, now);
        String e = cooldownText(player, times, 2, now);
        long recastRemaining = R_RECAST_UNTIL.getOrDefault(player.getUUID(), 0L) - now;
        String r = recastRemaining > 0
                ? "§d재시전 " + String.format(java.util.Locale.ROOT, "%.1fs", recastRemaining / 1000.0)
                : cooldownText(player, times, 3, now);
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                "§cZ§f " + q + "  §8|  §cX§f " + w + "  §8|  §cC§f " + e + "  §8|  §4§lV§r§f " + r)));
    }

    private static String cooldownText(ServerPlayer player, long[] times, int skill, long now) {
        if (times == null || times[skill] == 0) return "§aREADY";
        long remaining = PlayerEconomy.cooldownMillis(player, COOLDOWNS_MS[skill]) - (now - times[skill]);
        if (remaining <= 0) return "§aREADY";
        return "§e" + String.format(java.util.Locale.ROOT, "%.1fs", remaining / 1000.0);
    }

    private static void applyApprehendArmorShred(LivingEntity target) {
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        armor.addOrUpdateTransientModifier(new AttributeModifier(
                APPREHEND_ARMOR_SHRED, -0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        APPREHEND_ARMOR_SHRED_UNTIL.put(target.getUUID(),
                new ArmorShredState(target, System.currentTimeMillis() + 4_000));
    }

    private static final class BleedState {
        private final LivingEntity target;
        private final ServerPlayer source;
        private final BleedVisual visual;
        private final int stacks;
        private final long expiresAt;
        private long nextTickAt;

        private BleedState(LivingEntity target, ServerPlayer source, BleedVisual visual, int stacks, long expiresAt, long nextTickAt) {
            this.target = target;
            this.source = source;
            this.visual = visual;
            this.stacks = stacks;
            this.expiresAt = expiresAt;
            this.nextTickAt = nextTickAt;
        }
    }

    private static final class BleedVisual {
        private final List<Display.BlockDisplay> candles = new ArrayList<>();
        private Display.ItemDisplay egg;
    }

    private static final class QRangeVisual {
        private final List<Display.BlockDisplay> markers = new ArrayList<>();
        private final List<Vec3> offsets = new ArrayList<>();

        private void update(ServerPlayer player) {
            for (int i = 0; i < markers.size(); i++) {
                Vec3 offset = offsets.get(i);
                markers.get(i).setPos(player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z);
            }
        }

        private void discard() {
            markers.forEach(Display.BlockDisplay::discard);
            markers.clear();
            offsets.clear();
        }
    }

    private record PendingCast(ServerPlayer player, int skill, long executeAt, long[] times, QRangeVisual qRange) { }
    private record GuillotineHeadCharge(ServerPlayer player, LivingEntity target, Display.ItemDisplay head, long startedAt) { }
    private record GuillotineSlam(ServerPlayer player, LivingEntity target, Display.ItemDisplay dragonHead,
                                  long startedAt, long impactAt, long[] times) { }
    private record ArmorShredState(LivingEntity target, long expiresAt) { }
}
