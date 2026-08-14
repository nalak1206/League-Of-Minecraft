package kr.leagueofminecraft.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import kr.leagueofminecraft.shop.PlayerEconomy;

/** Small world-local JSON store for champion selection and progression. */
public final class LolPlayerDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;
    private static boolean loading;

    private LolPlayerDataStore() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("lol_hyunmin_players.json");
            load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(LolPlayerDataStore::save);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ChampionManager.onJoin(handler.getPlayer()));
    }

    public static void save(MinecraftServer server) {
        if (server == null || file == null || loading) return;
        JsonObject root = new JsonObject();
        Set<UUID> players = new HashSet<>();
        players.addAll(ChampionManager.champions().keySet());
        players.addAll(ChampionManager.modes().keySet());
        players.addAll(PlayerEconomy.playerIds());
        for (UUID playerId : players) {
            JsonObject data = new JsonObject();
            data.addProperty("champion", ChampionManager.champions().getOrDefault(playerId, ChampionManager.Champion.DARIUS).name());
            data.addProperty("mode", ChampionManager.modes().getOrDefault(playerId, ChampionManager.GameMode.ADVENTURE).name());
            ChampionProgression.ProgressSnapshot progress = ChampionProgression.snapshot(playerId);
            data.addProperty("level", progress.level());
            data.addProperty("xp", progress.xp());
            data.addProperty("skillPoints", progress.skillPoints());
            data.add("ranks", GSON.toJsonTree(progress.ranks()));
            PlayerEconomy.AccountSnapshot economy = PlayerEconomy.snapshot(playerId);
            data.addProperty("gold", economy.gold());
            data.add("items", GSON.toJsonTree(economy.items()));
            data.addProperty("trinket", economy.trinket());
            root.add(playerId.toString(), data);
        }
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) { }
    }

    private static void load() {
        loading = true;
        ChampionManager.clear();
        ChampionProgression.clear();
        PlayerEconomy.clear();
        try {
            if (file == null || !Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                UUID playerId = UUID.fromString(entry.getKey());
                JsonObject data = entry.getValue().getAsJsonObject();
                ChampionManager.Champion champion = enumValue(ChampionManager.Champion.class, data, "champion", ChampionManager.Champion.DARIUS);
                ChampionManager.GameMode mode = enumValue(ChampionManager.GameMode.class, data, "mode", ChampionManager.GameMode.ADVENTURE);
                ChampionManager.load(playerId, champion, mode);
                int[] ranks = data.has("ranks") ? GSON.fromJson(data.get("ranks"), int[].class) : new int[5];
                ChampionProgression.load(playerId, new ChampionProgression.ProgressSnapshot(
                        integer(data, "level", 1), integer(data, "xp", 0), integer(data, "skillPoints", 1), ranks));
                String[] items = data.has("items") ? GSON.fromJson(data.get("items"), String[].class) : new String[0];
                PlayerEconomy.load(playerId, new PlayerEconomy.AccountSnapshot(integer(data, "gold", 500), items,
                        data.has("trinket") ? data.get("trinket").getAsString() : "STEALTH_WARD"));
            }
        } catch (Exception ignored) {
            ChampionManager.clear();
            ChampionProgression.clear();
            PlayerEconomy.clear();
        } finally {
            loading = false;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, JsonObject object, String key, T fallback) {
        try { return object.has(key) ? Enum.valueOf(type, object.get(key).getAsString()) : fallback; }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
