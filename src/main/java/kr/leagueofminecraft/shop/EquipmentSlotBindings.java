package kr.leagueofminecraft.shop;

/** Pure Alt-number mapping shared by client input and regression tests. */
public final class EquipmentSlotBindings {
    public static final int TRINKET = -1;
    public static final int INVALID = -2;

    private EquipmentSlotBindings() {}

    public static int equipmentIndexForAltNumber(int number) {
        if (number >= 1 && number <= 3) return number - 1;
        if (number == 4) return TRINKET;
        if (number >= 5 && number <= 7) return number - 2;
        return INVALID;
    }
}
