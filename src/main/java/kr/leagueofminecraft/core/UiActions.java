package kr.leagueofminecraft.core;

import kr.leagueofminecraft.network.UiActionPayload;
import kr.leagueofminecraft.shop.LolInventory;
import kr.leagueofminecraft.shop.LolShop;
import kr.leagueofminecraft.shop.LegendaryItemEffects;
import kr.leagueofminecraft.shop.TrinketSystem;
import kr.leagueofminecraft.match.RecallSystem;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Registers shop, virtual inventory and future recall input routing. */
public final class UiActions {
    private static boolean initialized;

    private UiActions() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        TrinketSystem.initialize();
        LegendaryItemEffects.initialize();
        PayloadTypeRegistry.serverboundPlay().register(UiActionPayload.TYPE, UiActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UiActionPayload.TYPE, (payload, context) -> {
            if (payload.action() == UiActionPayload.OPEN_SHOP) LolShop.open(context.player());
            else if (payload.action() == UiActionPayload.OPEN_INVENTORY) LolInventory.open(context.player());
            else if (payload.action() == UiActionPayload.RECALL) RecallSystem.toggle(context.player());
            else if (payload.action() >= UiActionPayload.USE_ITEM_BASE
                    && payload.action() < UiActionPayload.USE_ITEM_BASE + 6) {
                int slot = payload.action() - UiActionPayload.USE_ITEM_BASE;
                TrinketSystem.showResult(context.player(), LegendaryItemEffects.useActive(context.player(), slot));
                context.player().getInventory().setSelectedSlot(0);
                context.player().connection.send(new ClientboundSetHeldSlotPacket(0));
            } else if (payload.action() == UiActionPayload.USE_TRINKET) {
                TrinketSystem.showResult(context.player(), TrinketSystem.use(context.player()));
                context.player().getInventory().setSelectedSlot(0);
                context.player().connection.send(new ClientboundSetHeldSlotPacket(0));
            }
        });
    }
}
