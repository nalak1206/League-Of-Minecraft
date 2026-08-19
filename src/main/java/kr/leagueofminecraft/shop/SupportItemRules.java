package kr.leagueofminecraft.shop;

import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.match.MatchTeam;

/** Minecraft-independent ally rules for support item targeting. */
public final class SupportItemRules {
    private SupportItemRules() {}

    public static boolean canBindVow(boolean samePlayer, ChampionManager.GameMode mode,
                                     MatchTeam ownerTeam, MatchTeam targetTeam) {
        if (samePlayer) return false;
        return mode == ChampionManager.GameMode.ADVENTURE
                || (ownerTeam.isPlayable() && ownerTeam == targetTeam);
    }

    public static boolean isPlayerAlly(ChampionManager.GameMode mode,
                                       MatchTeam ownerTeam, MatchTeam targetTeam) {
        return mode == ChampionManager.GameMode.ADVENTURE
                || (ownerTeam.isPlayable() && ownerTeam == targetTeam);
    }
}
