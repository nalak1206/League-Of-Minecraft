package kr.leagueofminecraft.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Independent cooldown storage keyed by player and concrete action/item. */
public final class PerPlayerCooldowns<K> {
    private final Map<Key<K>, Long> readyAt = new HashMap<>();

    public long remainingMillis(UUID playerId, K key, long now) {
        return Math.max(0L, readyAt.getOrDefault(new Key<>(playerId, key), 0L) - now);
    }

    public void start(UUID playerId, K key, long now, long durationMillis) {
        readyAt.put(new Key<>(playerId, key), now + Math.max(0L, durationMillis));
    }

    public void prune(long now) {
        readyAt.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private record Key<K>(UUID playerId, K action) {}
}
