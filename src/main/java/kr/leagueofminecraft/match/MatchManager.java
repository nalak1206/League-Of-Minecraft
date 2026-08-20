package kr.leagueofminecraft.match;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.leagueofminecraft.core.ChampionManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Persistent teams, bases, match phase and team respawning. */
public final class MatchManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MatchRoster ROSTER = new MatchRoster();
    private static final Map<MatchTeam, MatchBase> BASES = new EnumMap<>(MatchTeam.class);
    private static MatchPhase phase = MatchPhase.LOBBY;
    private static Path file;
    private static boolean initialized;

    private MatchManager() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        RecallSystem.initialize();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("league_match.json");
            load();
            MatchScoreboardTeams.ensure(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                MatchScoreboardTeams.syncPlayer(player, team(player));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(MatchManager::save);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            RecallSystem.cancel(newPlayer, "사망");
            if (phase == MatchPhase.RUNNING) teleportToBase(newPlayer);
        });
    }

    public static MatchTeam team(ServerPlayer player) { return ROSTER.team(player.getUUID()); }
    public static MatchPhase phase() { return phase; }
    public static MatchBase base(MatchTeam team) { return BASES.get(team); }

    public static MatchTeam assign(ServerPlayer player, MatchTeam team) {
        ROSTER.assign(player.getUUID(), team);
        ChampionManager.setMode(player, team == MatchTeam.UNASSIGNED
                ? ChampionManager.GameMode.ADVENTURE : ChampionManager.GameMode.MATCH);
        MatchScoreboardTeams.syncPlayer(player, team);
        save(player.level().getServer());
        return team;
    }

    public static MatchTeam autoAssign(ServerPlayer player) {
        MatchTeam team = ROSTER.autoAssign(player.getUUID());
        ChampionManager.setMode(player, ChampionManager.GameMode.MATCH);
        MatchScoreboardTeams.syncPlayer(player, team);
        save(player.level().getServer());
        return team;
    }

    public static void setBase(ServerPlayer player, MatchTeam team) {
        if (team == MatchTeam.UNASSIGNED) throw new IllegalArgumentException("A playable team is required");
        BASES.put(team, MatchBase.at(player));
        save(player.level().getServer());
    }

    public static boolean start(MinecraftServer server) {
        if (!BASES.containsKey(MatchTeam.BLUE) || !BASES.containsKey(MatchTeam.RED)) return false;
        phase = MatchPhase.RUNNING;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team(player) != MatchTeam.UNASSIGNED) {
                ChampionManager.setMode(player, ChampionManager.GameMode.MATCH);
                MatchScoreboardTeams.syncPlayer(player, team(player));
                teleportToBase(player);
            }
        }
        save(server);
        return true;
    }

    public static void stop(MinecraftServer server) {
        RecallSystem.cancelAll(server, "경기 종료");
        phase = MatchPhase.LOBBY;
        save(server);
    }

    public static boolean teleportToBase(ServerPlayer player) {
        MatchBase base = BASES.get(team(player));
        return base != null && base.teleport(player.level().getServer(), player);
    }

    public static boolean areAllies(ServerPlayer first, ServerPlayer second) {
        return phase == MatchPhase.RUNNING && ROSTER.areAllies(first.getUUID(), second.getUUID());
    }

    public static boolean canUseShop(ServerPlayer player) {
        if (ChampionManager.mode(player) != ChampionManager.GameMode.MATCH || phase != MatchPhase.RUNNING)
            return true;
        MatchBase base = BASES.get(team(player));
        return base != null && base.contains(player, 12.0);
    }

    public static void onJoin(ServerPlayer player) {
        MatchScoreboardTeams.syncPlayer(player, team(player));
        if (team(player) == MatchTeam.UNASSIGNED) return;
        ChampionManager.setMode(player, ChampionManager.GameMode.MATCH);
        if (phase == MatchPhase.RUNNING) teleportToBase(player);
    }

    public static String status(ServerPlayer player) {
        return "경기=" + phase + " 팀=" + team(player).displayName()
                + " 블루=" + ROSTER.count(MatchTeam.BLUE)
                + " 레드=" + ROSTER.count(MatchTeam.RED)
                + " 기지[B/R]=" + (BASES.containsKey(MatchTeam.BLUE) ? "O" : "X")
                + "/" + (BASES.containsKey(MatchTeam.RED) ? "O" : "X");
    }

    public static void save(MinecraftServer server) {
        if (server == null || file == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("phase", phase.name());
        JsonObject teams = new JsonObject();
        ROSTER.snapshot().forEach((id, team) -> teams.addProperty(id.toString(), team.name()));
        root.add("teams", teams);
        JsonObject bases = new JsonObject();
        BASES.forEach((team, base) -> bases.add(team.name(), GSON.toJsonTree(base)));
        root.add("bases", bases);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) { }
    }

    private static void load() {
        ROSTER.clear();
        BASES.clear();
        phase = MatchPhase.LOBBY;
        try {
            if (file == null || !Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            phase = enumValue(MatchPhase.class, root, "phase", MatchPhase.LOBBY);
            if (root.has("teams")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("teams").entrySet())
                    ROSTER.assign(UUID.fromString(entry.getKey()), MatchTeam.valueOf(entry.getValue().getAsString()));
            }
            if (root.has("bases")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("bases").entrySet())
                    BASES.put(MatchTeam.valueOf(entry.getKey()), GSON.fromJson(entry.getValue(), MatchBase.class));
            }
        } catch (Exception ignored) {
            ROSTER.clear();
            BASES.clear();
            phase = MatchPhase.LOBBY;
        }
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, JsonObject object, String key, T fallback) {
        try { return object.has(key) ? Enum.valueOf(type, object.get(key).getAsString()) : fallback; }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
