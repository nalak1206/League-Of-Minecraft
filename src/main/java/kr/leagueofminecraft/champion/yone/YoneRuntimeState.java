package kr.leagueofminecraft.champion.yone;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mutable per-player Yone state shared by skill, motion and return services. */
final class YoneRuntimeState {
    static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    static final Map<UUID, Boolean> E_RETURN_WARNED = new HashMap<>();
    static final Map<UUID, Long> ACTION_LOCK_UNTIL = new HashMap<>();
    static final Map<UUID, Long> Q_POSE_UNTIL = new HashMap<>();
    static final Map<UUID, Boolean> SECOND_BLADE = new HashMap<>();

    private YoneRuntimeState() {}
}
