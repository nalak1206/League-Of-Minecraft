package kr.leagueofminecraft.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import kr.leagueofminecraft.match.MatchManager;

public final class LolShop {
    private LolShop() {}

    public static void open(ServerPlayer player) {
        if (!MatchManager.canUseShop(player)) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§c상점은 아군 기지 근처에서만 이용할 수 있습니다")));
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new LolShopMenu(id, inventory, player),
                Component.literal("LOL 상점")));
    }
}
