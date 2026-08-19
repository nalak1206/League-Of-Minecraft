package kr.leagueofminecraft.core;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import kr.leagueofminecraft.champion.darius.DariusSkills;
import kr.leagueofminecraft.champion.yone.YoneSkills;
import kr.leagueofminecraft.champion.ChampionDefinition;
import kr.leagueofminecraft.champion.ChampionRegistry;
import kr.leagueofminecraft.shop.LolShop;
import kr.leagueofminecraft.shop.PlayerEconomy;
import kr.leagueofminecraft.shop.LegendaryItemEffects;
import kr.leagueofminecraft.match.MatchManager;
import kr.leagueofminecraft.match.MatchTeam;

public final class ChampionManager {
    public enum Champion { DARIUS, YONE }
    public enum GameMode { ADVENTURE, MATCH }

    private static final Map<UUID, Champion> CHAMPIONS = new HashMap<>();
    private static final Map<UUID, GameMode> MODES = new HashMap<>();

    private ChampionManager() {}

    public static void initialize() {
        LolPlayerDataStore.initialize();
        LolMatchSystem.initialize();
        MatchManager.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("lol")
                .then(Commands.literal("champion")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(ctx, "name").toUpperCase(Locale.ROOT);
                        try {
                            select(player, Champion.valueOf(name));
                            ctx.getSource().sendSuccess(() -> Component.literal("챔피언: " + name), false);
                            return 1;
                        } catch (IllegalArgumentException ignored) { return 0; }
                    })))
                .then(Commands.literal("mode")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(ctx, "name").toUpperCase(Locale.ROOT);
                        try {
                            MODES.put(player.getUUID(), GameMode.valueOf(name));
                            LolPlayerDataStore.save(player.level().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("LOL 모드: " + name), false);
                            return 1;
                        } catch (IllegalArgumentException ignored) { return 0; }
                    })))
                .then(Commands.literal("match")
                    .then(Commands.literal("team")
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            try {
                                MatchTeam team = name.equalsIgnoreCase("auto")
                                        ? MatchManager.autoAssign(player)
                                        : MatchManager.assign(player, MatchTeam.parse(name));
                                ctx.getSource().sendSuccess(() -> Component.literal("팀: " + team.coloredName()), false);
                                return 1;
                            } catch (IllegalArgumentException ignored) { return 0; }
                        })))
                    .then(Commands.literal("base")
                        .then(Commands.literal("set")
                            .then(Commands.argument("team", StringArgumentType.word()).executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                try {
                                    MatchTeam team = MatchTeam.parse(StringArgumentType.getString(ctx, "team"));
                                    MatchManager.setBase(player, team);
                                    ctx.getSource().sendSuccess(() -> Component.literal(team.coloredName() + "§r 기지 지정 완료"), false);
                                    return 1;
                                } catch (IllegalArgumentException ignored) { return 0; }
                            }))))
                    .then(Commands.literal("start").executes(ctx -> {
                        boolean started = MatchManager.start(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal(started
                                ? "§aMATCH 시작" : "§c블루와 레드 기지를 먼저 지정하세요."), false);
                        return started ? 1 : 0;
                    }))
                    .then(Commands.literal("stop").executes(ctx -> {
                        MatchManager.stop(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal("§eMATCH 종료"), false);
                        return 1;
                    }))
                    .then(Commands.literal("spawn").executes(ctx ->
                            MatchManager.teleportToBase(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                    .then(Commands.literal("status").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ctx.getSource().sendSuccess(() -> Component.literal(MatchManager.status(player)), false);
                        return 1;
                    })))
                .then(Commands.literal("level")
                    .then(Commands.argument("value", IntegerArgumentType.integer(1, 18)).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ChampionProgression.setLevel(player, IntegerArgumentType.getInteger(ctx, "value"));
                        return 1;
                    })))
                .then(Commands.literal("xp")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(ctx -> {
                        ChampionProgression.addXp(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value"));
                        return 1;
                    })))
                .then(Commands.literal("rank")
                    .then(Commands.argument("skill", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int skill = parseSkill(StringArgumentType.getString(ctx, "skill"));
                        boolean success = ChampionProgression.rankUp(player, skill);
                        ctx.getSource().sendSuccess(() -> Component.literal(success ? "Skill ranked up." : "Cannot rank that skill now."), false);
                        return success ? 1 : 0;
                    })))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ChampionProgression.Progress progress = ChampionProgression.get(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Champion=" + champion(player) + " Mode=" + mode(player)
                            + " Team=" + MatchManager.team(player)
                            + " Level=" + progress.level() + " XP=" + progress.xp()
                            + " Points=" + progress.skillPoints()
                            + " Q/W/E/R=" + progress.rank(1) + "/" + progress.rank(2)
                            + "/" + progress.rank(3) + "/" + progress.rank(4)
                            + " Gold=" + PlayerEconomy.account(player).gold()), false);
                    return 1;
                }))
                .then(Commands.literal("shop").executes(ctx -> {
                    LolShop.open(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("inventory").executes(ctx -> {
                    LolShop.open(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("item")
                    .then(Commands.literal("use").executes(ctx -> {
                        String result = LegendaryItemEffects.useActive(ctx.getSource().getPlayerOrException());
                        ctx.getSource().sendSuccess(() -> Component.literal(result), false);
                        return 1;
                    })))
                .then(Commands.literal("stats").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                            "AD %.1f | AP %.1f | 방어 %.0f | 마저 %.0f | 물관 %.0f+%.0f%% | 마관 %.0f+%.0f%% | 가속 %.0f | 흡혈 %.0f%% | 재생/5초 %.1f | 강인함 %.0f%%",
                            player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                            PlayerEconomy.abilityPower(player),
                            player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) * 10,
                            PlayerEconomy.magicResistance(player), PlayerEconomy.armorPenetrationFlat(player),
                            PlayerEconomy.armorPenetrationPercent(player) * 100,
                            PlayerEconomy.magicPenetrationFlat(player), PlayerEconomy.magicPenetrationPercent(player) * 100,
                            PlayerEconomy.abilityHaste(player), PlayerEconomy.lifeSteal(player) * 100,
                            PlayerEconomy.healthRegenPerFive(player), PlayerEconomy.tenacity(player) * 100)), false);
                    return 1;
                }))
                .then(Commands.literal("gold")
                    .then(Commands.literal("add")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(ctx -> {
                            PlayerEconomy.addGold(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value"));
                            return 1;
                        })))
                    .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(ctx -> {
                            PlayerEconomy.setGold(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value"));
                            return 1;
                        }))))
                .then(Commands.literal("cooldown")
                    .then(Commands.literal("reset").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DariusSkills.reset(player);
                        YoneSkills.reset(player);
                        ctx.getSource().sendSuccess(() -> Component.literal("Cooldowns reset."), false);
                        return 1;
                    })))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    DariusSkills.reset(player);
                    YoneSkills.reset(player);
                    return 1;
                }))));
    }

    public static Champion champion(ServerPlayer player) {
        return CHAMPIONS.getOrDefault(player.getUUID(), Champion.DARIUS);
    }

    public static GameMode mode(ServerPlayer player) {
        return MODES.getOrDefault(player.getUUID(), GameMode.ADVENTURE);
    }

    public static void select(ServerPlayer player, Champion champion) {
        definitions().values().forEach(definition -> definition.reset(player));
        clearChampionWeapons(player);
        CHAMPIONS.put(player.getUUID(), champion);
        definition(champion).equip(player);
        PlayerEconomy.applyAttributes(player);
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static boolean isDarius(ServerPlayer player) { return champion(player) == Champion.DARIUS; }
    public static boolean isYone(ServerPlayer player) { return champion(player) == Champion.YONE; }

    public static void cast(ServerPlayer player, int wireSkill) {
        if (CrowdControl.blocksSkills(player)) return;
        definition(champion(player)).cast(player, wireSkill);
    }

    static Map<UUID, Champion> champions() { return Map.copyOf(CHAMPIONS); }
    static Map<UUID, GameMode> modes() { return Map.copyOf(MODES); }

    static void load(UUID playerId, Champion champion, GameMode mode) {
        CHAMPIONS.put(playerId, champion == null ? Champion.DARIUS : champion);
        MODES.put(playerId, mode == null ? GameMode.ADVENTURE : mode);
    }

    static void clear() {
        CHAMPIONS.clear();
        MODES.clear();
    }

    static void onJoin(ServerPlayer player) {
        clearChampionWeapons(player);
        definition(champion(player)).equip(player);
        PlayerEconomy.applyAttributes(player);
        MatchManager.onJoin(player);
    }

    private static void clearChampionWeapons(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (definitions().values().stream().anyMatch(definition -> definition.isChampionWeapon(stack)))
                player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        ItemStack offhand = player.getOffhandItem();
        if (definitions().values().stream().anyMatch(definition -> definition.isChampionWeapon(offhand)))
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    public static void setMode(ServerPlayer player, GameMode mode) {
        MODES.put(player.getUUID(), mode == null ? GameMode.ADVENTURE : mode);
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static void reduceUltimateCooldown(ServerPlayer player, long millis) {
        definition(champion(player)).reduceUltimateCooldown(player, Math.max(0L, millis));
    }

    private static ChampionDefinition definition(Champion champion) {
        return ChampionRegistry.require(champion.name());
    }

    private static Map<String, ChampionDefinition> definitions() {
        return ChampionRegistry.definitions();
    }
    private static int parseSkill(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "1", "q" -> 1;
            case "2", "w" -> 2;
            case "3", "e" -> 3;
            case "4", "r" -> 4;
            default -> 0;
        };
    }
}
