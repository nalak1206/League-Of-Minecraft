package kr.leagueofminecraft.shop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WardChargeRulesTest {
    @Test void consumesAndRechargesOneChargeAtATime() {
        var full = new WardChargeRules.State(2, 0L);
        var one = WardChargeRules.consume(full, 1_000L);
        assertEquals(1, one.charges());
        assertEquals(91_000L, one.rechargeAt());
        assertEquals(2, WardChargeRules.refresh(one, 91_000L).charges());
    }

    @Test void emptyStateRechargesBothChargesAcrossTwoIntervals() {
        var empty = new WardChargeRules.State(0, 91_000L);
        assertEquals(1, WardChargeRules.refresh(empty, 91_000L).charges());
        assertEquals(2, WardChargeRules.refresh(empty, 181_000L).charges());
    }

    @Test void placementRequiresChargeAndFreeActiveSlot() {
        assertTrue(WardChargeRules.canPlace(new WardChargeRules.State(1, 0L), 1, 1_000L));
        assertFalse(WardChargeRules.canPlace(new WardChargeRules.State(1, 0L), 2, 1_000L));
        assertFalse(WardChargeRules.canPlace(new WardChargeRules.State(0, 91_000L), 0, 1_000L));
    }
}
