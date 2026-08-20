package kr.leagueofminecraft;

import kr.leagueofminecraft.champion.darius.DariusSkills;
import kr.leagueofminecraft.champion.ChampionRegistry;
import kr.leagueofminecraft.champion.darius.DariusChampion;
import kr.leagueofminecraft.champion.yone.YoneChampion;
import kr.leagueofminecraft.champion.malphite.MalphiteChampion;
import kr.leagueofminecraft.champion.malphite.MalphiteSkills;
import kr.leagueofminecraft.registry.ModItems;
import kr.leagueofminecraft.core.UiActions;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint for the complete League of Minecraft module. */
public final class LeagueOfMinecraftMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ModItems.initialize();
        ChampionRegistry.register(new DariusChampion());
        ChampionRegistry.register(new YoneChampion());
        ChampionRegistry.register(new MalphiteChampion());
        UiActions.initialize();
        DariusSkills.initialize();
        MalphiteSkills.initialize();
    }
}
