package kr.leagueofminecraft.shop;

/** Pure ward charge rules, kept independent so recharge behavior is unit-testable. */
public final class WardChargeRules {
    public static final int MAX_CHARGES = 2;
    public static final int MAX_ACTIVE_WARDS = 2;
    public static final long RECHARGE_MILLIS = 90_000L;

    private WardChargeRules() {}

    public static State refresh(State state, long now) {
        int charges = Math.max(0, Math.min(MAX_CHARGES, state.charges()));
        long rechargeAt = state.rechargeAt();
        if (charges >= MAX_CHARGES) return new State(MAX_CHARGES, 0L);
        if (rechargeAt <= 0L) rechargeAt = now + RECHARGE_MILLIS;
        while (charges < MAX_CHARGES && now >= rechargeAt) {
            charges++;
            rechargeAt += RECHARGE_MILLIS;
        }
        return charges >= MAX_CHARGES ? new State(MAX_CHARGES, 0L) : new State(charges, rechargeAt);
    }

    public static State consume(State state, long now) {
        State refreshed = refresh(state, now);
        if (refreshed.charges() <= 0) return refreshed;
        int charges = refreshed.charges() - 1;
        long rechargeAt = refreshed.rechargeAt() > 0L ? refreshed.rechargeAt() : now + RECHARGE_MILLIS;
        return new State(charges, rechargeAt);
    }

    public static boolean canPlace(State state, int activeWards, long now) {
        return refresh(state, now).charges() > 0 && activeWards < MAX_ACTIVE_WARDS;
    }

    public record State(int charges, long rechargeAt) {}
}
