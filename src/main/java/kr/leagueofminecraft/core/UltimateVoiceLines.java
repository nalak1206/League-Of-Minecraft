package kr.leagueofminecraft.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Text equivalents of champion ultimate voice lines, visible to nearby players. */
public final class UltimateVoiceLines {
    private static final double HEARING_RANGE_SQR = 48.0 * 48.0;
    private static final List<String> YONE = List.of(
            "결판을 내자!",
            "사적인 감정은 없다!",
            "내 검으로 네 이름을 밝혀주마!"
    );
    private static final List<String> DARIUS = List.of(
            "이것이 반란이다!",
            "저들을 물어뜯어라!",
            "여기서 끝내겠다."
    );
    private static final List<TypingLine> ACTIVE = new ArrayList<>();

    private UltimateVoiceLines() {}

    public static void shout(ServerPlayer caster, ChampionManager.Champion champion) {
        List<String> lines = champion == ChampionManager.Champion.YONE ? YONE : DARIUS;
        String name = champion == ChampionManager.Champion.YONE ? "요네" : "다리우스";
        String color = champion == ChampionManager.Champion.YONE ? "§d" : "§4";
        String line = lines.get(caster.getRandom().nextInt(lines.size()));
        List<UUID> viewers = caster.level().getServer().getPlayerList().getPlayers().stream()
                .filter(viewer -> viewer.level() == caster.level()
                        && viewer.distanceToSqr(caster) <= HEARING_RANGE_SQR)
                .map(ServerPlayer::getUUID)
                .toList();
        ACTIVE.removeIf(active -> active.caster.equals(caster.getUUID()));
        ACTIVE.add(new TypingLine(caster.getUUID(), viewers, name, color, line,
                0, System.currentTimeMillis()));
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Iterator<TypingLine> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            TypingLine active = iterator.next();
            if (now < active.nextCharacterAt) continue;
            active.visibleCharacters = Math.min(active.line.length(), active.visibleCharacters + 1);
            active.nextCharacterAt = now + 55;
            Component typing = message(active, active.line.substring(0, active.visibleCharacters) + "▌");
            for (UUID viewerId : active.viewers) {
                ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
                if (viewer != null) viewer.connection.send(new ClientboundSetActionBarTextPacket(typing));
            }

            if (active.visibleCharacters >= active.line.length()) {
                Component complete = message(active, active.line);
                for (UUID viewerId : active.viewers) {
                    ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
                    if (viewer != null) viewer.sendSystemMessage(complete);
                }
                iterator.remove();
            }
        }
    }

    public static boolean isTypingFor(ServerPlayer viewer) {
        return ACTIVE.stream().anyMatch(active -> active.viewers.contains(viewer.getUUID()));
    }

    private static Component message(TypingLine active, String text) {
        return Component.literal(active.color + "§l" + active.name + "§r§f: §o\"" + text + "\"§r");
    }

    private static final class TypingLine {
        private final UUID caster;
        private final List<UUID> viewers;
        private final String name;
        private final String color;
        private final String line;
        private int visibleCharacters;
        private long nextCharacterAt;

        private TypingLine(UUID caster, List<UUID> viewers, String name, String color,
                           String line, int visibleCharacters, long nextCharacterAt) {
            this.caster = caster;
            this.viewers = viewers;
            this.name = name;
            this.color = color;
            this.line = line;
            this.visibleCharacters = visibleCharacters;
            this.nextCharacterAt = nextCharacterAt;
        }
    }
}
