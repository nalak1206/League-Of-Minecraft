package kr.leagueofminecraft.core;

import kr.leagueofminecraft.network.UiActionPayload;
import kr.leagueofminecraft.shop.LolInventory;
import kr.leagueofminecraft.shop.LolShop;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Registers shop, virtual inventory and future recall input routing. */
public final class UiActions {
    private static boolean initialized;

    private UiActions() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.serverboundPlay().register(UiActionPayload.TYPE, UiActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UiActionPayload.TYPE, (payload, context) -> {
            if (payload.action() == UiActionPayload.OPEN_SHOP) LolShop.open(context.player());
            else if (payload.action() == UiActionPayload.OPEN_INVENTORY) LolInventory.open(context.player());
            // B is deliberately registered now; recall behavior will be attached later.
        });
    }
}
