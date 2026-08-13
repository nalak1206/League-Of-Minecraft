package kr.darius.skills.shop;

import java.util.List;
import java.util.Arrays;
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
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;

public final class LolShopMenu extends ChestMenu {
    private static final int PAGE_SIZE = 21;
    private static final int[] ITEM_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
    private final ServerPlayer customer;
    private final SimpleContainer shop;
    private int page;

    public LolShopMenu(int id, Inventory inventory, ServerPlayer customer) {
        this(id, inventory, customer, new SimpleContainer(54));
    }

    private LolShopMenu(int id, Inventory inventory, ServerPlayer customer, SimpleContainer shop) {
        super(MenuType.GENERIC_9x6, id, inventory, shop, 6);
        this.customer = customer;
        this.shop = shop;
        refresh();
    }

    private void refresh() {
        for (int slot = 0; slot < shop.getContainerSize(); slot++) shop.setItem(slot, named(Items.GLASS, " "));
        LolShopItem.Category category = LolShopItem.Category.values()[page];
        List<LolShopItem> items = Arrays.stream(LolShopItem.values()).filter(item -> item.category() == category).toList();
        for (int index = 0; index < PAGE_SIZE; index++) {
            int itemIndex = index;
            if (itemIndex >= items.size()) break;
            LolShopItem item = items.get(itemIndex);
            boolean owned = PlayerEconomy.account(customer).owns(item);
            ItemStack icon = new ItemStack(item.icon());
            icon.set(DataComponents.CUSTOM_NAME, Component.literal((owned ? "§a" : "§f") + item.displayName() + " §6" + item.price() + "G"));
            shop.setItem(ITEM_SLOTS[index], icon);
        }
        shop.setItem(45, named(Items.ARROW, "§e이전 페이지"));
        shop.setItem(4, named(Items.BOOK, "§6§l" + category.displayName()));
        shop.setItem(49, named(Items.GOLD_INGOT, "§6보유 골드: " + PlayerEconomy.account(customer).gold() + "G"));
        shop.setItem(53, named(Items.ARROW, "§e다음 페이지"));
    }

    @Override public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex == 45) { page = Math.max(0, page - 1); refresh(); return; }
        if (slotIndex == 53) { page = Math.min(LolShopItem.Category.values().length - 1, page + 1); refresh(); return; }
        for (int index = 0; index < ITEM_SLOTS.length; index++) {
            if (slotIndex != ITEM_SLOTS[index]) continue;
            List<LolShopItem> items = Arrays.stream(LolShopItem.values())
                    .filter(item -> item.category() == LolShopItem.Category.values()[page]).toList();
            if (index >= items.size()) return;
            LolShopItem item = items.get(index);
            PlayerEconomy.PurchaseResult result = PlayerEconomy.purchase(customer, item);
            customer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message(result))));
            refresh();
            return;
        }
        if (slotIndex >= 0 && slotIndex < 54) return;
        super.clicked(slotIndex, buttonNum, input, player);
    }

    private static String message(PlayerEconomy.PurchaseResult result) {
        return switch (result) {
            case SUCCESS -> "§a구매 완료";
            case OWNED -> "§e이미 보유 중입니다";
            case NOT_ENOUGH_GOLD -> "§c골드가 부족합니다";
            case STARTER_LOCKED -> "§c시작 아이템은 하나만 보유할 수 있습니다";
            case BOOTS_LOCKED -> "§c완성 장화는 하나만 보유할 수 있습니다";
        };
    }

    private static ItemStack named(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
