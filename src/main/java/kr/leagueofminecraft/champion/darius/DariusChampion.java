package kr.leagueofminecraft.champion.darius;

import kr.leagueofminecraft.champion.ChampionDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class DariusChampion implements ChampionDefinition {
    @Override public String id() { return "darius"; }
    @Override public String displayName() { return "다리우스"; }
    @Override public void equip(ServerPlayer player) { DariusSkills.equip(player); }
    @Override public void reset(ServerPlayer player) { DariusSkills.reset(player); }
    @Override public void cast(ServerPlayer player, int wireSkill) { DariusSkills.castSelected(player, wireSkill); }
    @Override public boolean isChampionWeapon(ItemStack stack) { return DariusSkills.isDariusWeapon(stack); }
    @Override public void reduceUltimateCooldown(ServerPlayer player, long millis) {
        DariusSkills.reduceUltimateCooldown(player, millis);
    }
}
