package kr.darius.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import kr.darius.skills.shop.PlayerEconomy;

/** League-style champion level and Q/W/E/R rank state. */
public final class ChampionProgression {
    private static final int MAX_LEVEL = 18;
    private static final int[] XP_TO_NEXT = {
            0, 280, 380, 480, 580, 680, 780, 880, 980,
            1080, 1180, 1280, 1380, 1480, 1580, 1680, 1780, 1880
    };
    private static final Map<UUID, Progress> DATA = new HashMap<>();

    private ChampionProgression() {}

    public static Progress get(ServerPlayer player) {
        return DATA.computeIfAbsent(player.getUUID(), id -> new Progress());
    }

    public static void addXp(ServerPlayer player, int amount) {
        Progress progress = get(player);
        progress.xp += Math.max(0, amount);
        while (progress.level < MAX_LEVEL && progress.xp >= XP_TO_NEXT[progress.level]) {
            progress.xp -= XP_TO_NEXT[progress.level];
            progress.level++;
            progress.skillPoints++;
            PlayerEconomy.applyAttributes(player);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                    net.minecraft.network.chat.Component.literal("§6LEVEL UP! §f" + progress.level
                            + " §d스킬 포인트 +1")));
        }
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static void setLevel(ServerPlayer player, int level) {
        Progress progress = get(player);
        int target = Math.max(1, Math.min(MAX_LEVEL, level));
        int gained = Math.max(0, target - progress.level);
        progress.level = target;
        progress.xp = 0;
        progress.skillPoints += gained;
        PlayerEconomy.applyAttributes(player);
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static boolean rankUp(ServerPlayer player, int skill) {
        if (skill < 1 || skill > 4) return false;
        Progress progress = get(player);
        if (progress.skillPoints <= 0) return false;
        int current = progress.ranks[skill];
        int maximum = skill == 4 ? 3 : 5;
        if (current >= maximum) return false;
        if (skill == 4) {
            int required = 6 + current * 5;
            if (progress.level < required) return false;
        } else {
            int required = 1 + current * 2;
            if (progress.level < required) return false;
        }
        progress.ranks[skill]++;
        progress.skillPoints--;
        LolPlayerDataStore.save(player.level().getServer());
        return true;
    }

    static ProgressSnapshot snapshot(UUID playerId) {
        Progress progress = DATA.getOrDefault(playerId, new Progress());
        return new ProgressSnapshot(progress.level, progress.xp, progress.skillPoints, progress.ranks.clone());
    }

    static void load(UUID playerId, ProgressSnapshot snapshot) {
        Progress progress = new Progress();
        progress.level = Math.max(1, Math.min(MAX_LEVEL, snapshot.level()));
        progress.xp = Math.max(0, snapshot.xp());
        progress.skillPoints = Math.max(0, snapshot.skillPoints());
        int[] source = snapshot.ranks() == null ? new int[5] : snapshot.ranks();
        for (int skill = 1; skill <= 4 && skill < source.length; skill++) {
            progress.ranks[skill] = Math.max(0, Math.min(skill == 4 ? 3 : 5, source[skill]));
        }
        DATA.put(playerId, progress);
    }

    static void clear() { DATA.clear(); }

    record ProgressSnapshot(int level, int xp, int skillPoints, int[] ranks) {}

    public static final class Progress {
        private int level = 1;
        private int xp;
        private int skillPoints = 1;
        private final int[] ranks = new int[5];

        public int level() { return level; }
        public int xp() { return xp; }
        public int skillPoints() { return skillPoints; }
        public int rank(int skill) { return skill >= 1 && skill <= 4 ? ranks[skill] : 0; }
    }
}
