package kr.leagueofminecraft.shop;

import java.util.Comparator;
import java.util.List;
import kr.leagueofminecraft.core.ChampionProgression;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Eight-page LoL shop with fixed page, skill, XP and gold panels. */
public final class LolShopMenu extends ChestMenu {
    private static final int[] PAGE_SLOTS = {9, 10, 18, 19, 27, 28, 36, 37};
    private static final int[] THREE_COLUMN_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32, 39, 40, 41};
    private static final int[] FOUR_COLUMN_SLOTS = {12, 13, 14, 15, 21, 22, 23, 24, 30, 31, 32, 33, 39, 40, 41, 42};
    private static final int[] SIX_ITEM_SLOTS = {21, 22, 23, 30, 31, 32};
    private static final int[] EQUIPMENT_SLOTS = {45, 46, 47, 48, 49, 50};
    private static final int[] SKILL_SLOTS = {17, 26, 35, 44};

    private static final LolShopItem[][] PAGE_ITEMS = {
            {LolShopItem.DORANS_BLADE, LolShopItem.DORANS_SHIELD, LolShopItem.DORANS_RING,
             LolShopItem.DORANS_BOW, LolShopItem.DORANS_HELM, LolShopItem.CULL,
             LolShopItem.TEAR, LolShopItem.DARK_SEAL, LolShopItem.WORLD_ATLAS,
             LolShopItem.MOSSTOMPER, LolShopItem.GUSTWALKER, LolShopItem.SCORCHCLAW},
            {LolShopItem.BOOTS, LolShopItem.SLIGHTLY_MAGICAL_BOOTS, LolShopItem.PLATED, LolShopItem.ARMORED_ADVANCE,
             LolShopItem.BERSERKERS, LolShopItem.GUNMETAL_GREAVES, LolShopItem.SWIFTNESS, LolShopItem.SWIFTMARCH,
             LolShopItem.SORCERERS, LolShopItem.SPELLSLINGERS_SHOES, LolShopItem.MERCURYS, LolShopItem.CHAINLACED_CRUSHERS,
             LolShopItem.IONIAN, LolShopItem.CRIMSON_LUCIDITY, LolShopItem.GREEDY_GREAVES, LolShopItem.FOREVER_FORWARD},
            {LolShopItem.TRINITY_FORCE, LolShopItem.STERAKS_GAGE, LolShopItem.BLACK_CLEAVER,
             LolShopItem.SUNDERED_SKY, LolShopItem.DEATHS_DANCE, LolShopItem.BLADE_OF_THE_RUINED_KING},
            {LolShopItem.INFINITY_EDGE, LolShopItem.KRAKEN_SLAYER, LolShopItem.THE_COLLECTOR,
             LolShopItem.LORD_DOMINIKS_REGARDS, LolShopItem.BLOODTHIRSTER, LolShopItem.STATIKK_SHIV},
            {LolShopItem.YOUMUUS_GHOSTBLADE, LolShopItem.EDGE_OF_NIGHT, LolShopItem.SERYLDAS_GRUDGE,
             LolShopItem.AXIOM_ARC, LolShopItem.OPPORTUNITY, LolShopItem.PROFANE_HYDRA},
            {LolShopItem.RABADONS_DEATHCAP, LolShopItem.ZHONYAS_HOURGLASS, LolShopItem.VOID_STAFF,
             LolShopItem.LIANDRYS_TORMENT, LolShopItem.RYLAIS_CRYSTAL_SCEPTER, LolShopItem.SHADOWFLAME},
            {LolShopItem.HEARTSTEEL, LolShopItem.SUNFIRE_AEGIS, LolShopItem.THORNMAIL,
             LolShopItem.WARMOGS_ARMOR, LolShopItem.KAENIC_ROOKERN, LolShopItem.JAKSHO},
            {LolShopItem.LOCKET_OF_THE_IRON_SOLARI, LolShopItem.REDEMPTION, LolShopItem.SHURELYAS_BATTLESONG,
             LolShopItem.KNIGHTS_VOW, LolShopItem.ARDENT_CENSER, LolShopItem.IMPERIAL_MANDATE}
    };

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
        Item background = pageBackground(page);
        String pageName = LolShopItem.Category.values()[page].displayName();
        for (int slot = 0; slot < shop.getContainerSize(); slot++)
            shop.setItem(slot, named(background, "§8" + pageName));

        for (int index = 0; index < PAGE_SLOTS.length; index++) {
            LolShopItem.Category category = LolShopItem.Category.values()[index];
            shop.setItem(PAGE_SLOTS[index], named(index == page ? Items.EMERALD : Items.PAPER,
                    (index == page ? "§a§l" : "§f") + (index + 1) + ". " + category.displayName()));
        }

        LolShopItem[] items = PAGE_ITEMS[page];
        int[] slots = productSlots(page);
        for (int index = 0; index < items.length; index++) {
            LolShopItem item = items[index];
            boolean owned = PlayerEconomy.account(customer).owns(item);
            ItemStack icon = item.createIcon();
            icon.set(DataComponents.CUSTOM_NAME, Component.literal((owned ? "§a§l" : "§f") + item.displayName()
                    + " §6" + item.price() + "G §8[" + item.statSummary() + "]"));
            shop.setItem(slots[index], icon);
        }

        ChampionProgression.Progress progress = ChampionProgression.get(customer);
        int needed = ChampionProgression.xpToNext(customer);
        shop.setItem(8, named(Items.NETHER_STAR, "§b§lLv." + progress.level() + " §f경험치 "
                + progress.xp() + (needed == 0 ? " §6MAX" : "/" + needed)
                + " §d스킬 포인트 " + progress.skillPoints()));
        for (int skill = 1; skill <= 4; skill++)
            shop.setItem(SKILL_SLOTS[skill - 1], skillIcon(progress, skill));
        shop.setItem(53, named(Items.GOLD_INGOT,
                "§6§l보유 골드: " + PlayerEconomy.account(customer).gold() + "G"));

        List<LolShopItem> equipment = equipment();
        for (int index = 0; index < EQUIPMENT_SLOTS.length; index++) {
            if (index >= equipment.size()) {
                shop.setItem(EQUIPMENT_SLOTS[index], named(background, "§8빈 LoL 아이템 칸"));
                continue;
            }
            LolShopItem item = equipment.get(index);
            ItemStack icon = item.createIcon();
            icon.set(DataComponents.CUSTOM_NAME, Component.literal("§b" + item.displayName()
                    + " §7(클릭 판매 " + (int) Math.floor(item.price() * 0.70) + "G)"));
            shop.setItem(EQUIPMENT_SLOTS[index], icon);
        }
    }

    @Override public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        for (int index = 0; index < PAGE_SLOTS.length; index++) {
            if (slotIndex != PAGE_SLOTS[index]) continue;
            page = index;
            refresh();
            return;
        }
        for (int skill = 1; skill <= 4; skill++) {
            if (slotIndex != SKILL_SLOTS[skill - 1]) continue;
            boolean success = ChampionProgression.rankUp(customer, skill);
            customer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                    success ? "§a스킬 랭크 상승" : "§c현재는 올릴 수 없습니다")));
            refresh();
            return;
        }
        int[] slots = productSlots(page);
        LolShopItem[] items = PAGE_ITEMS[page];
        for (int index = 0; index < items.length; index++) {
            if (slotIndex != slots[index]) continue;
            PlayerEconomy.PurchaseResult result = PlayerEconomy.purchase(customer, items[index]);
            customer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message(result))));
            refresh();
            return;
        }
        for (int index = 0; index < EQUIPMENT_SLOTS.length; index++) {
            if (slotIndex != EQUIPMENT_SLOTS[index]) continue;
            List<LolShopItem> equipment = equipment();
            if (index >= equipment.size()) return;
            int refund = PlayerEconomy.sell(customer, equipment.get(index));
            customer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§6판매 +" + refund + "G")));
            refresh();
            return;
        }
        if (slotIndex >= 0 && slotIndex < 54) return;
        super.clicked(slotIndex, buttonNum, input, player);
    }

    private List<LolShopItem> equipment() {
        return PlayerEconomy.account(customer).items().stream()
                .sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }

    private static int[] productSlots(int page) {
        if (page == 0) return THREE_COLUMN_SLOTS;
        if (page == 1) return FOUR_COLUMN_SLOTS;
        return SIX_ITEM_SLOTS;
    }

    private static Item pageBackground(int page) {
        return switch (page) {
            case 2 -> vanillaItem("yellow_stained_glass_pane");
            case 3 -> vanillaItem("green_stained_glass_pane");
            case 4 -> vanillaItem("red_stained_glass_pane");
            case 5 -> vanillaItem("light_blue_stained_glass_pane");
            case 6 -> vanillaItem("blue_stained_glass_pane");
            case 7 -> vanillaItem("lime_stained_glass_pane");
            case 1 -> vanillaItem("black_stained_glass_pane");
            default -> vanillaItem("light_gray_stained_glass_pane");
        };
    }

    private static Item vanillaItem(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }

    private static ItemStack skillIcon(ChampionProgression.Progress progress, int skill) {
        int rank = progress.rank(skill);
        int max = skill == 4 ? 3 : 5;
        Item icon = skillArmor(skill, rank);
        String key = switch (skill) { case 1 -> "Q"; case 2 -> "W"; case 3 -> "E"; default -> "R"; };
        return named(icon, "§d§l" + key + " §f스킬 레벨 " + rank + "/" + max + " §7(클릭 투자)");
    }

    private static Item skillArmor(int skill, int rank) {
        if (skill == 4) return switch (rank) {
            case 1 -> Items.GOLDEN_BOOTS;
            case 2 -> Items.DIAMOND_BOOTS;
            case 3 -> Items.NETHERITE_BOOTS;
            default -> Items.LEATHER_BOOTS;
        };
        return switch (skill) {
            case 1 -> switch (rank) {
                case 1 -> Items.COPPER_HELMET; case 2 -> Items.IRON_HELMET; case 3 -> Items.GOLDEN_HELMET;
                case 4 -> Items.DIAMOND_HELMET; case 5 -> Items.NETHERITE_HELMET; default -> Items.LEATHER_HELMET;
            };
            case 2 -> switch (rank) {
                case 1 -> Items.COPPER_CHESTPLATE; case 2 -> Items.IRON_CHESTPLATE; case 3 -> Items.GOLDEN_CHESTPLATE;
                case 4 -> Items.DIAMOND_CHESTPLATE; case 5 -> Items.NETHERITE_CHESTPLATE; default -> Items.LEATHER_CHESTPLATE;
            };
            default -> switch (rank) {
                case 1 -> Items.COPPER_LEGGINGS; case 2 -> Items.IRON_LEGGINGS; case 3 -> Items.GOLDEN_LEGGINGS;
                case 4 -> Items.DIAMOND_LEGGINGS; case 5 -> Items.NETHERITE_LEGGINGS; default -> Items.LEATHER_LEGGINGS;
            };
        };
    }

    private static String message(PlayerEconomy.PurchaseResult result) {
        return switch (result) {
            case SUCCESS -> "§a구매 완료";
            case OWNED -> "§e이미 보유 중입니다";
            case NOT_ENOUGH_GOLD -> "§c골드가 부족합니다";
            case STARTER_LOCKED -> "§c시작 아이템은 하나만 보유할 수 있습니다";
            case BOOTS_LOCKED -> "§c장화는 한 종류만 보유할 수 있습니다";
            case INVENTORY_FULL -> "§cLoL 아이템 인벤토리 6칸이 가득 찼습니다";
        };
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
