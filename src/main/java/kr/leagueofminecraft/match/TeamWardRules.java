package kr.leagueofminecraft.match;

/** Pure team-visibility decisions shared by wards and lens tests. */
public final class TeamWardRules {
    private TeamWardRules() {}

    public static boolean isAlliedWard(MatchTeam viewer, MatchTeam owner, MatchPhase phase) {
        return phase == MatchPhase.RUNNING && viewer.isPlayable() && viewer == owner;
    }

    public static boolean canRevealWithLens(MatchTeam viewer, MatchTeam owner, MatchPhase phase) {
        return phase == MatchPhase.RUNNING && viewer.isPlayable() && owner.isPlayable() && viewer != owner;
    }

    public static boolean canDestroy(MatchTeam attacker, MatchTeam owner, MatchPhase phase) {
        return phase != MatchPhase.RUNNING
                || (attacker.isPlayable() && owner.isPlayable() && attacker != owner);
    }

    public static int destructionGold(MatchTeam attacker, MatchTeam owner, MatchPhase phase) {
        return canDestroy(attacker, owner, phase) && phase == MatchPhase.RUNNING ? 10 : 0;
    }
}
