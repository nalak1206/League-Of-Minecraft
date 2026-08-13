package kr.darius.skills;

import java.util.List;
import net.minecraft.network.chat.Component;
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
            "내 도끼는 아직 배가 고프다!",
            "죽음으로 쌓아올린 왕좌다.",
            "다음 전장으로!"
    );

    private UltimateVoiceLines() {}

    public static void shout(ServerPlayer caster, ChampionManager.Champion champion) {
        List<String> lines = champion == ChampionManager.Champion.YONE ? YONE : DARIUS;
        String name = champion == ChampionManager.Champion.YONE ? "요네" : "다리우스";
        String color = champion == ChampionManager.Champion.YONE ? "§d" : "§4";
        String line = lines.get(caster.getRandom().nextInt(lines.size()));
        Component message = Component.literal(color + "§l" + name + "§r§f: §o\"" + line + "\"§r");

        for (ServerPlayer viewer : caster.level().getServer().getPlayerList().getPlayers()) {
            if (viewer.level() == caster.level() && viewer.distanceToSqr(caster) <= HEARING_RANGE_SQR)
                viewer.sendSystemMessage(message);
        }
    }
}
