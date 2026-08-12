package kr.darius.skills.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class LolShop {
    private LolShop() {}

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new LolShopMenu(id, inventory, player),
                Component.literal("LOL 상점")));
    }
}
