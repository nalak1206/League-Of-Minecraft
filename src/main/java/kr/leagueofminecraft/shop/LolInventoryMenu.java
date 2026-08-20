package kr.leagueofminecraft.shop;

import java.util.List;
import kr.leagueofminecraft.core.ChampionProgression;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact 3-row status and six-slot virtual equipment screen opened with M. */
public final class LolInventoryMenu extends ChestMenu {
    private static final int[] SKILL_SLOTS = {2, 3, 5, 6};
    private static final int[] EQUIPMENT_SLOTS = {10, 11, 12, 14, 15, 16};
    private final ServerPlayer owner;
    private final SimpleContainer display;

    public LolInventoryMenu(int id, Inventory inventory, ServerPlayer owner) {
        this(id, inventory, owner, new SimpleContainer(27));
    }

    private LolInventoryMenu(int id, Inventory inventory, ServerPlayer owner, SimpleContainer display) {
        super(MenuType.GENERIC_9x3, id, inventory, display, 3);
        this.owner = owner;
        this.display = display;
        refresh();
    }

    private void refresh() {
        for (int slot = 0; slot < display.getContainerSize(); slot++)
            display.setItem(slot, LolShopMenu.named(LolShopMenu.vanillaItem("black_stained_glass_pane"), " "));

        ChampionProgression.Progress progress = ChampionProgression.get(owner);
        int needed = ChampionProgression.xpToNext(owner);
        display.setItem(1, LolShopMenu.named(Items.NETHER_STAR,
                "§b§lLEVEL " + progress.level() + " §fXP " + progress.xp()
                        + (needed == 0 ? " §6MAX" : "/" + needed)));
        for (int skill = 1; skill <= 4; skill++)
            display.setItem(SKILL_SLOTS[skill - 1], LolShopMenu.skillIcon(progress, skill));
        LolTrinket trinket = PlayerEconomy.trinket(owner);
        display.setItem(4, LolShopMenu.named(trinket.icon(),
                "§a§l장신구 §f" + trinket.displayName()
                        + (trinket == LolTrinket.STEALTH_WARD ? " §8| " + TrinketSystem.wardStatus(owner) : "")
                        + " §7(클릭 전환 / Alt+4 사용)"));
        display.setItem(7, LolShopMenu.named(Items.GOLD_INGOT,
                "§6§lGOLD " + PlayerEconomy.account(owner).gold() + "G"));

        List<LolShopItem> equipment = PlayerEconomy.equipment(owner);
        for (int index = 0; index < EQUIPMENT_SLOTS.length; index++) {
            if (index >= equipment.size()) {
                display.setItem(EQUIPMENT_SLOTS[index],
                        LolShopMenu.named(LolShopMenu.vanillaItem("gray_stained_glass_pane"), "§8빈 아이템 칸 " + (index + 1)));
                continue;
            }
            LolShopItem item = equipment.get(index);
            ItemStack icon = item.createIcon();
            icon.set(DataComponents.CUSTOM_NAME,
                    Component.literal("§f" + (index + 1) + "번칸 §b" + item.displayName()
                            + " §8[" + item.statSummary() + "]"));
            display.setItem(EQUIPMENT_SLOTS[index], icon);
        }
    }

    @Override public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex == 4) {
            PlayerEconomy.cycleTrinket(owner);
            refresh();
            return;
        }
        if (slotIndex >= 0 && slotIndex < 27) return;
        super.clicked(slotIndex, buttonNum, input, player);
    }
}
