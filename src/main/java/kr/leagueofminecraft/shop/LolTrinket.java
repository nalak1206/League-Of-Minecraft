package kr.leagueofminecraft.shop;

import net.minecraft.world.item.Item;

/** The dedicated Alt+4 trinket slot, separate from six legendary equipment slots. */
public enum LolTrinket {
    STEALTH_WARD("투명 와드", LolShopMenu.vanillaItem("green_candle")),
    ORACLE_LENS("예언자의 렌즈", LolShopMenu.vanillaItem("ender_eye"));

    private final String displayName;
    private final Item icon;

    LolTrinket(String displayName, Item icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() { return displayName; }
    public Item icon() { return icon; }
}
