package kr.leagueofminecraft.champion.malphite;

import kr.leagueofminecraft.champion.ChampionDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class MalphiteChampion implements ChampionDefinition {
    @Override public String id() { return "malphite"; }
    @Override public String displayName() { return "말파이트"; }
    @Override public void equip(ServerPlayer player) { MalphiteSkills.equip(player); }
    @Override public void reset(ServerPlayer player) { MalphiteSkills.reset(player); }
    @Override public void cast(ServerPlayer player, int wireSkill) { MalphiteSkills.cast(player, wireSkill); }
    @Override public boolean isChampionWeapon(ItemStack stack) { return MalphiteSkills.isWeapon(stack); }
    @Override public void reduceUltimateCooldown(ServerPlayer player, long millis) { MalphiteSkills.reduceUltimateCooldown(player, millis); }
}
