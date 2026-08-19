package kr.leagueofminecraft.match;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

/** Bridges League match teams to vanilla scoreboard teams for team-only visibility. */
public final class MatchScoreboardTeams {
    private static final String BLUE = "lom_blue";
    private static final String RED = "lom_red";

    private MatchScoreboardTeams() {}

    public static void ensure(MinecraftServer server) {
        configure(team(server, MatchTeam.BLUE), MatchTeam.BLUE);
        configure(team(server, MatchTeam.RED), MatchTeam.RED);
    }

    public static void syncPlayer(ServerPlayer player, MatchTeam matchTeam) {
        Scoreboard scoreboard = player.level().getServer().getScoreboard();
        removeFromLeagueTeam(scoreboard, player.getScoreboardName());
        if (matchTeam.isPlayable()) scoreboard.addPlayerToTeam(player.getScoreboardName(), team(player.level().getServer(), matchTeam));
    }

    public static void addEntity(MinecraftServer server, Entity entity, MatchTeam matchTeam) {
        if (!matchTeam.isPlayable()) return;
        server.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), team(server, matchTeam));
    }

    public static void removeEntity(MinecraftServer server, Entity entity) {
        removeFromLeagueTeam(server.getScoreboard(), entity.getScoreboardName());
    }

    private static PlayerTeam team(MinecraftServer server, MatchTeam matchTeam) {
        Scoreboard scoreboard = server.getScoreboard();
        String name = matchTeam == MatchTeam.BLUE ? BLUE : RED;
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) team = scoreboard.addPlayerTeam(name);
        configure(team, matchTeam);
        return team;
    }

    private static void configure(PlayerTeam team, MatchTeam matchTeam) {
        team.setDisplayName(Component.literal(matchTeam.displayName()));
        team.setColor(Optional.of(matchTeam == MatchTeam.BLUE ? TeamColor.BLUE : TeamColor.RED));
        team.setAllowFriendlyFire(false);
        team.setSeeFriendlyInvisibles(true);
    }

    private static void removeFromLeagueTeam(Scoreboard scoreboard, String member) {
        PlayerTeam current = scoreboard.getPlayersTeam(member);
        if (current != null && (BLUE.equals(current.getName()) || RED.equals(current.getName())))
            scoreboard.removePlayerFromTeam(member, current);
    }
}
