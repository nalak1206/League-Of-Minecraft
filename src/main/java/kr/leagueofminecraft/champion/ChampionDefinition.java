package kr.leagueofminecraft.champion;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Stable adapter used by the core layer to operate any playable champion. */
public interface ChampionDefinition {
    String id();
    String displayName();
    void equip(ServerPlayer player);
    void reset(ServerPlayer player);
    void cast(ServerPlayer player, int wireSkill);
    boolean isChampionWeapon(ItemStack stack);
    default void reduceUltimateCooldown(ServerPlayer player, long millis) {}
}
