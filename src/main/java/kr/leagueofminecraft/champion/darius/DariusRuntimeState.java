package kr.leagueofminecraft.champion.darius;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/** Mutable per-player Darius state, separated from skill formulas and rendering. */
final class DariusRuntimeState {
    static final Map<UUID, long[]> LAST_CAST = new HashMap<>();
    static final Map<UUID, Long> NOXIAN_MIGHT = new HashMap<>();
    static final Map<UUID, Long> CRIPPLING_STRIKE = new HashMap<>();
    static final Map<UUID, LivingEntity> GUILLOTINE_TARGETS = new HashMap<>();
    static final Map<UUID, Long> R_RECAST_UNTIL = new HashMap<>();
    static final Map<UUID, Long> GUILLOTINE_ARMED = new HashMap<>();
    static final Map<UUID, Long> REVERT_TO_DIAMOND = new HashMap<>();
    static final Map<UUID, Item> LOCKED_WEAPON = new HashMap<>();
    static final Map<UUID, Long> APPREHEND_DISABLE_UNTIL = new HashMap<>();

    private DariusRuntimeState() {}
}
