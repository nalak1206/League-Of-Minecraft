package kr.leagueofminecraft.registry;

import kr.leagueofminecraft.ModConstants;
import kr.leagueofminecraft.champion.yone.SteelSwordItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;

/** Central registry for champion equipment. Add future champion weapons here. */
public final class ModItems {
    public static final Item NOXIAN_POWER = register("noxian_power",
            new Item.Properties().axe(ToolMaterial.DIAMOND, -1.0f, -3.0f).rarity(Rarity.RARE).fireResistant());
    public static final Item CRIPPLING_STRIKE = register("crippling_strike",
            new Item.Properties().axe(ToolMaterial.NETHERITE, -2.0f, -3.0f).rarity(Rarity.EPIC).fireResistant());
    public static final Item NOXIAN_GUILLOTINE = register("noxian_guillotine",
            new Item.Properties().axe(ToolMaterial.NETHERITE, -2.0f, -3.2f).rarity(Rarity.EPIC).fireResistant());
    public static final Item YONE_STEEL_SWORD = registerSteelSword();
    public static final Item YONE_AZAKANA_SWORD = register("yone_azakana_sword",
            new Item.Properties().sword(ToolMaterial.NETHERITE, -2.0f, -2.4f).rarity(Rarity.EPIC).fireResistant());

    // Compatibility registrations keep pre-0.14.9 worlds loadable long enough for the
    // champion loadout manager to remove the old stacks and equip canonical items.
    public static final Item LEGACY_NOXIAN_POWER = registerLegacy("noxian_power",
            new Item.Properties().axe(ToolMaterial.DIAMOND, -1.0f, -3.0f).rarity(Rarity.RARE).fireResistant());
    public static final Item LEGACY_CRIPPLING_STRIKE = registerLegacy("crippling_strike",
            new Item.Properties().axe(ToolMaterial.NETHERITE, -2.0f, -3.0f).rarity(Rarity.EPIC).fireResistant());
    public static final Item LEGACY_NOXIAN_GUILLOTINE = registerLegacy("noxian_guillotine",
            new Item.Properties().axe(ToolMaterial.NETHERITE, -2.0f, -3.2f).rarity(Rarity.EPIC).fireResistant());
    public static final Item LEGACY_YONE_STEEL_SWORD = registerLegacySteelSword();
    public static final Item LEGACY_YONE_AZAKANA_SWORD = registerLegacy("yone_azakana_sword",
            new Item.Properties().sword(ToolMaterial.NETHERITE, -2.0f, -2.4f).rarity(Rarity.EPIC).fireResistant());

    private ModItems() {}

    public static void initialize() {
        // Class loading performs registration. The method makes bootstrap intent explicit.
    }

    private static Item register(String path, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModConstants.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
    }

    private static Item registerSteelSword() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModConstants.id("steel_sword"));
        Item.Properties properties = new Item.Properties().sword(ToolMaterial.NETHERITE, -2.0f, -2.4f)
                .rarity(Rarity.RARE).fireResistant().setId(key);
        return Registry.register(BuiltInRegistries.ITEM, key, new SteelSwordItem(properties));
    }

    private static Item registerLegacy(String path, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModConstants.legacyId(path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
    }

    private static Item registerLegacySteelSword() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModConstants.legacyId("steel_sword"));
        Item.Properties properties = new Item.Properties().sword(ToolMaterial.NETHERITE, -2.0f, -2.4f)
                .rarity(Rarity.RARE).fireResistant().setId(key);
        return Registry.register(BuiltInRegistries.ITEM, key, new SteelSwordItem(properties));
    }
}
