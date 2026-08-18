package kr.leagueofminecraft.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class CombatMathTest {
    @Test void appliesPositiveAndNegativeResistanceFormulas() {
        assertEquals(1.0, CombatMath.resistanceMultiplier(0.0), 0.000001);
        assertEquals(0.5, CombatMath.resistanceMultiplier(100.0), 0.000001);
        assertEquals(1.5, CombatMath.resistanceMultiplier(-100.0), 0.000001);
    }

    @Test void appliesPercentBeforeFlatPenetration() {
        assertEquals(30.0, CombatMath.resistanceAfterPenetration(100.0, 0.5, 20.0), 0.000001);
        assertEquals(-20.0, CombatMath.resistanceAfterPenetration(0.0, 0.5, 20.0), 0.000001);
    }

    @Test void convertsAbilityHasteIntoCooldown() {
        assertEquals(10_000L, CombatMath.cooldownMillis(10_000L, 0.0));
        assertEquals(5_000L, CombatMath.cooldownMillis(10_000L, 100.0));
        assertEquals(4_000L, CombatMath.cooldownMillis(10_000L, 150.0));
    }
}
