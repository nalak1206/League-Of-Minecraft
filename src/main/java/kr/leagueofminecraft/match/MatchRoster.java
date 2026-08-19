package kr.leagueofminecraft.match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Minecraft-independent team roster and deterministic auto balancing. */
public final class MatchRoster {
    private final Map<UUID, MatchTeam> teams = new HashMap<>();

    public MatchTeam team(UUID playerId) {
        return teams.getOrDefault(playerId, MatchTeam.UNASSIGNED);
    }

    public MatchTeam assign(UUID playerId, MatchTeam team) {
        if (team == MatchTeam.UNASSIGNED) teams.remove(playerId);
        else teams.put(playerId, team);
        return team;
    }

    public MatchTeam autoAssign(UUID playerId) {
        MatchTeam selected = count(MatchTeam.BLUE) <= count(MatchTeam.RED)
                ? MatchTeam.BLUE : MatchTeam.RED;
        return assign(playerId, selected);
    }

    public int count(MatchTeam team) {
        return (int) teams.values().stream().filter(value -> value == team).count();
    }

    public boolean areAllies(UUID first, UUID second) {
        MatchTeam team = team(first);
        return team != MatchTeam.UNASSIGNED && team == team(second);
    }

    public Map<UUID, MatchTeam> snapshot() { return Map.copyOf(teams); }
    public void clear() { teams.clear(); }
}
