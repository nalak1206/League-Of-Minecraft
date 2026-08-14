package kr.leagueofminecraft.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/** Opens the player's six-slot virtual League equipment inventory. */
public final class LolInventory {
    private LolInventory() {}

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new LolInventoryMenu(id, inventory, player),
                Component.literal("LoL 아이템 인벤토리")));
    }
}
