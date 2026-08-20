package kr.leagueofminecraft.core;

/** Match reward constants and assist timing kept deterministic for tests. */
public final class TakedownRewardRules {
    public static final long ASSIST_WINDOW_TICKS = 200L;
    public static final int CHAMPION_KILL_GOLD = 300;
    public static final int CHAMPION_KILL_XP = 300;
    public static final int CHAMPION_ASSIST_GOLD = 150;
    public static final int CHAMPION_ASSIST_XP = 150;

    private TakedownRewardRules() {}

    public static boolean assistEligible(long lastHitTick, long deathTick) {
        return lastHitTick >= 0L && deathTick >= lastHitTick
                && deathTick - lastHitTick <= ASSIST_WINDOW_TICKS;
    }

    public static Reward minionReward(float maxHealth) {
        return new Reward(Math.max(12, Math.min(90, Math.round(maxHealth * 1.5f))),
                Math.max(30, Math.min(240, Math.round(maxHealth * 4.0f))));
    }

    public record Reward(int gold, int xp) {}
}
