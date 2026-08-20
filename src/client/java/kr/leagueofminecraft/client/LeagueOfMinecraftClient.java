package kr.leagueofminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.leagueofminecraft.ModConstants;
import kr.leagueofminecraft.network.SkillPayload;
import kr.leagueofminecraft.network.UiActionPayload;
import kr.leagueofminecraft.network.YoneAttackAnimationPayload;
import kr.leagueofminecraft.shop.EquipmentSlotBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

public final class LeagueOfMinecraftClient implements ClientModInitializer {
    private static final int[] ALT_ITEM_KEYS = {
            InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3,
            InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6,
            InputConstants.KEY_7
    };
    private static final boolean[] ALT_ITEM_DOWN = new boolean[ALT_ITEM_KEYS.length];
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ModConstants.id("skills"));
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(YoneAttackAnimationPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player == null) return;
                    InteractionHand hand = payload.offhand()
                            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    // Left click begins a local main-hand animation before the server chooses
                    // Yone's blade. Reset it so the authoritative alternating hand is visible.
                    context.client().player.swinging = false;
                    context.client().player.swingTime = -1;
                    context.client().player.swingingArm = hand;
                    context.client().player.swing(hand);
                }));
        KeyMapping q = key("key.league_of_minecraft.q_keyboard", InputConstants.KEY_Z);
        KeyMapping w = key("key.league_of_minecraft.w_keyboard", InputConstants.KEY_X);
        KeyMapping e = key("key.league_of_minecraft.e_keyboard", InputConstants.KEY_C);
        KeyMapping r = key("key.league_of_minecraft.r_keyboard", InputConstants.KEY_V);
        KeyMapping shop = key("key.league_of_minecraft.shop", InputConstants.KEY_P);
        KeyMapping inventory = key("key.league_of_minecraft.lol_inventory", InputConstants.KEY_M);
        KeyMapping recall = key("key.league_of_minecraft.recall", InputConstants.KEY_B);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (q.consumeClick()) send(1);
            while (w.consumeClick()) send(4);
            while (e.consumeClick()) send(2);
            while (r.consumeClick()) send(3);
            while (shop.consumeClick()) sendUi(UiActionPayload.OPEN_SHOP);
            while (inventory.consumeClick()) sendUi(UiActionPayload.OPEN_INVENTORY);
            while (recall.consumeClick()) sendUi(UiActionPayload.RECALL);
            handleAltItemKeys(client);
        });
    }

    private static KeyMapping key(String name, int code) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, code, CATEGORY));
    }

    private static void send(int skill) {
        if (ClientPlayNetworking.canSend(SkillPayload.TYPE)) ClientPlayNetworking.send(new SkillPayload(skill));
    }

    private static void sendUi(int action) {
        if (ClientPlayNetworking.canSend(UiActionPayload.TYPE))
            ClientPlayNetworking.send(new UiActionPayload(action));
    }

    private static void handleAltItemKeys(net.minecraft.client.Minecraft client) {
        boolean alt = InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_RALT);
        for (int index = 0; index < ALT_ITEM_KEYS.length; index++) {
            boolean down = alt && InputConstants.isKeyDown(client.getWindow(), ALT_ITEM_KEYS[index]);
            if (down && !ALT_ITEM_DOWN[index] && client.player != null
                    && client.player.containerMenu == client.player.inventoryMenu) {
                int number = index + 1;
                int equipmentIndex = EquipmentSlotBindings.equipmentIndexForAltNumber(number);
                int action = equipmentIndex == EquipmentSlotBindings.TRINKET
                        ? UiActionPayload.USE_TRINKET
                        : UiActionPayload.USE_ITEM_BASE + equipmentIndex;
                sendUi(action);
                client.player.getInventory().setSelectedSlot(0);
            }
            ALT_ITEM_DOWN[index] = down;
        }
    }
}
