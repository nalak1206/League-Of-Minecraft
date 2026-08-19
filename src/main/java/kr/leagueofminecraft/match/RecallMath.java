package kr.leagueofminecraft.match;

/** Minecraft-independent recall timing and movement checks. */
public final class RecallMath {
    private RecallMath() {}

    public static long remainingTicks(long completesAtTick, long currentTick) {
        return Math.max(0L, completesAtTick - currentTick);
    }

    public static boolean moved(double startX, double startY, double startZ,
                                double currentX, double currentY, double currentZ,
                                double toleranceSquared) {
        double x = currentX - startX;
        double y = currentY - startY;
        double z = currentZ - startZ;
        return x * x + y * y + z * z > Math.max(0.0, toleranceSquared);
    }
}
