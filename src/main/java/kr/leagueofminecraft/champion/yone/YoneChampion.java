package kr.leagueofminecraft.champion.yone;

import kr.leagueofminecraft.champion.ChampionDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class YoneChampion implements ChampionDefinition {
    @Override public String id() { return "yone"; }
    @Override public String displayName() { return "요네"; }
    @Override public void equip(ServerPlayer player) { YoneSkills.equip(player); }
    @Override public void reset(ServerPlayer player) { YoneSkills.reset(player); }
    @Override public void cast(ServerPlayer player, int wireSkill) { YoneSkills.cast(player, wireSkill); }
    @Override public boolean isChampionWeapon(ItemStack stack) { return YoneSkills.isYoneWeapon(stack); }
    @Override public void reduceUltimateCooldown(ServerPlayer player, long millis) {
        YoneSkills.reduceUltimateCooldown(player, millis);
    }
}
