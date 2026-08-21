package kr.leagueofminecraft.champion.malphite;

/** Minecraft-independent geometry rules shared by Malphite skills and tests. */
public final class MalphiteSkillRules {
    public static final double Q_RANGE = 8.0;
    public static final double E_RADIUS = 4.0;
    public static final double R_RANGE = 11.0;
    public static final double R_IMPACT_RADIUS = 3.2;
    public static final int R_DASH_TICKS = 8;

    private MalphiteSkillRules() {}

    public static boolean withinHorizontalRadius(double deltaX, double deltaZ, double radius) {
        return deltaX * deltaX + deltaZ * deltaZ <= radius * radius;
    }

    public static double dashProgress(int completedTicks) {
        return Math.max(0.0, Math.min(1.0, completedTicks / (double) R_DASH_TICKS));
    }

    public static double clampRange(double requestedDistance, double maximumRange) {
        return Math.max(0.0, Math.min(maximumRange, requestedDistance));
    }
}
