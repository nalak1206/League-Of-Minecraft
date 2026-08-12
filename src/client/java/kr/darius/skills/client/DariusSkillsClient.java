package kr.darius.skills.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.darius.skills.SkillPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class DariusSkillsClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("darius_skills", "skills"));
    @Override
    public void onInitializeClient() {
        KeyMapping q = key("key.darius_skills.q_keyboard", InputConstants.KEY_Z);
        KeyMapping w = key("key.darius_skills.w_keyboard", InputConstants.KEY_X);
        KeyMapping e = key("key.darius_skills.e_keyboard", InputConstants.KEY_C);
        KeyMapping r = key("key.darius_skills.r_keyboard", InputConstants.KEY_V);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (q.consumeClick()) send(1);
            while (w.consumeClick()) send(4);
            while (e.consumeClick()) send(2);
            while (r.consumeClick()) send(3);
        });
    }

    private static KeyMapping key(String name, int code) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, code, CATEGORY));
    }

    private static void send(int skill) {
        if (ClientPlayNetworking.canSend(SkillPayload.TYPE)) ClientPlayNetworking.send(new SkillPayload(skill));
    }
}
