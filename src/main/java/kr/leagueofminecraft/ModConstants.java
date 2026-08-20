package kr.leagueofminecraft;

import net.minecraft.resources.Identifier;

/** Canonical project identifiers plus the pre-0.14.9 compatibility namespace. */
public final class ModConstants {
    public static final String MOD_ID = "league_of_minecraft";
    public static final String LEGACY_MOD_ID = "darius_skills";

    private ModConstants() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier legacyId(String path) {
        return Identifier.fromNamespaceAndPath(LEGACY_MOD_ID, path);
    }
}
