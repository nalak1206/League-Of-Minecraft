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
}
