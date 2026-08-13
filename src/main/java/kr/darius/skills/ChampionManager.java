package kr.darius.skills;

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
import kr.darius.skills.shop.LolShop;
import kr.darius.skills.shop.PlayerEconomy;
import kr.darius.skills.shop.LegendaryItemEffects;

public final class ChampionManager {
    public enum Champion { DARIUS, YONE }
    public enum GameMode { ADVENTURE, MATCH }

    private static final Map<UUID, Champion> CHAMPIONS = new HashMap<>();
    private static final Map<UUID, GameMode> MODES = new HashMap<>();

    private ChampionManager() {}

    public static void initialize() {
        LolPlayerDataStore.initialize();
        LolMatchSystem.initialize();
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
        DariusSkills.reset(player);
        YoneSkills.reset(player);
        CHAMPIONS.put(player.getUUID(), champion);
        if (champion == Champion.DARIUS) DariusSkills.equip(player);
        else YoneSkills.equip(player);
        PlayerEconomy.applyAttributes(player);
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static boolean isDarius(ServerPlayer player) { return champion(player) == Champion.DARIUS; }
    public static boolean isYone(ServerPlayer player) { return champion(player) == Champion.YONE; }

    public static void cast(ServerPlayer player, int wireSkill) {
        if (CrowdControl.blocksSkills(player)) return;
        if (isDarius(player)) DariusSkills.castSelected(player, wireSkill);
        else YoneSkills.cast(player, wireSkill);
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
        if (isDarius(player)) DariusSkills.equip(player);
        else YoneSkills.equip(player);
        PlayerEconomy.applyAttributes(player);
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
