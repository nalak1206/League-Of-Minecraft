package kr.leagueofminecraft;

import net.minecraft.resources.Identifier;

/** Shared identifiers that deliberately remain stable for save and resource-pack compatibility. */
public final class ModConstants {
    public static final String MOD_ID = "darius_skills";

    private ModConstants() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
