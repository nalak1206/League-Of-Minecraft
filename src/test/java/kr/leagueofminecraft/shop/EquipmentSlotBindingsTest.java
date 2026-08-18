package kr.leagueofminecraft.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class EquipmentSlotBindingsTest {
    @Test void mapsAltNumbersToSixEquipmentSlotsAndTrinket() {
        assertEquals(0, EquipmentSlotBindings.equipmentIndexForAltNumber(1));
        assertEquals(1, EquipmentSlotBindings.equipmentIndexForAltNumber(2));
        assertEquals(2, EquipmentSlotBindings.equipmentIndexForAltNumber(3));
        assertEquals(EquipmentSlotBindings.TRINKET, EquipmentSlotBindings.equipmentIndexForAltNumber(4));
        assertEquals(3, EquipmentSlotBindings.equipmentIndexForAltNumber(5));
        assertEquals(4, EquipmentSlotBindings.equipmentIndexForAltNumber(6));
        assertEquals(5, EquipmentSlotBindings.equipmentIndexForAltNumber(7));
        assertEquals(EquipmentSlotBindings.INVALID, EquipmentSlotBindings.equipmentIndexForAltNumber(8));
    }
}
