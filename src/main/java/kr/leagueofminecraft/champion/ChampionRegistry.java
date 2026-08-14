package kr.leagueofminecraft.champion;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Central champion catalogue. New champions are registered here once. */
public final class ChampionRegistry {
    private static final Map<String, ChampionDefinition> DEFINITIONS = new LinkedHashMap<>();

    private ChampionRegistry() {}

    public static void register(ChampionDefinition definition) {
        String id = definition.id().toLowerCase(Locale.ROOT);
        if (DEFINITIONS.putIfAbsent(id, definition) != null)
            throw new IllegalStateException("Duplicate champion id: " + id);
    }

    public static ChampionDefinition require(String id) {
        ChampionDefinition definition = DEFINITIONS.get(id.toLowerCase(Locale.ROOT));
        if (definition == null) throw new IllegalArgumentException("Unknown champion: " + id);
        return definition;
    }

    public static Map<String, ChampionDefinition> definitions() {
        return Map.copyOf(DEFINITIONS);
    }
}
