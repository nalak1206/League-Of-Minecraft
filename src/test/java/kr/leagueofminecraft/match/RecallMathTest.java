package kr.leagueofminecraft.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecallMathTest {
    @Test
    void remainingTicksNeverBecomeNegative() {
        assertEquals(120, RecallMath.remainingTicks(200, 80));
        assertEquals(0, RecallMath.remainingTicks(200, 250));
    }

    @Test
    void movementUsesSquaredThreeDimensionalTolerance() {
        assertFalse(RecallMath.moved(0, 64, 0, 0.1, 64, 0.1, 0.04));
        assertTrue(RecallMath.moved(0, 64, 0, 0.21, 64, 0, 0.04));
        assertTrue(RecallMath.moved(0, 64, 0, 0, 64.21, 0, 0.04));
    }
}
