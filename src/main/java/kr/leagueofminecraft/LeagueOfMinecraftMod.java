package kr.leagueofminecraft;

import kr.leagueofminecraft.champion.darius.DariusSkills;
import kr.leagueofminecraft.registry.ModItems;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint for the complete League of Minecraft module. */
public final class LeagueOfMinecraftMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ModItems.initialize();
        DariusSkills.initialize();
    }
}
