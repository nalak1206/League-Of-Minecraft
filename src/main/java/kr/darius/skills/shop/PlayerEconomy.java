package kr.darius.skills.shop;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.darius.skills.LolPlayerDataStore;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class PlayerEconomy {
    private static final Identifier DAMAGE_ID = id("shop_attack_damage");
    private static final Identifier HEALTH_ID = id("shop_max_health");
    private static final Identifier SPEED_ID = id("shop_attack_speed");
    private static final Identifier ARMOR_ID = id("shop_armor");
    private static final Identifier MOVE_ID = id("shop_move_speed");
    private static final Map<UUID, Account> ACCOUNTS = new HashMap<>();

    private PlayerEconomy() {}

    public static Account account(ServerPlayer player) {
        return ACCOUNTS.computeIfAbsent(player.getUUID(), id -> new Account());
    }

    public static PurchaseResult purchase(ServerPlayer player, LolShopItem item) {
        Account account = account(player);
        if (account.items.contains(item)) return PurchaseResult.OWNED;
        if (item.category() == LolShopItem.Category.STARTER && account.items.stream().anyMatch(owned -> owned.category() == LolShopItem.Category.STARTER))
            return PurchaseResult.STARTER_LOCKED;
        int price = item.price();
        if (item.isFinishedBoots() && account.items.contains(LolShopItem.BOOTS)) price -= LolShopItem.BOOTS.price();
        if (item.category() == LolShopItem.Category.BOOTS && account.items.stream().anyMatch(LolShopItem::isFinishedBoots))
            return PurchaseResult.BOOTS_LOCKED;
        if (account.gold < price) return PurchaseResult.NOT_ENOUGH_GOLD;
        account.gold -= price;
        if (item.isFinishedBoots()) account.items.remove(LolShopItem.BOOTS);
        account.items.add(item);
        applyAttributes(player);
        LolPlayerDataStore.save(player.level().getServer());
        return PurchaseResult.SUCCESS;
    }

    public static void addGold(ServerPlayer player, int amount) {
        account(player).gold = Math.max(0, account(player).gold + amount);
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static void setGold(ServerPlayer player, int amount) {
        account(player).gold = Math.max(0, amount);
        LolPlayerDataStore.save(player.level().getServer());
    }

    public static void applyAttributes(ServerPlayer player) {
        Account account = account(player);
        double damage = 0, health = 0, attackSpeed = 0, armor = 0, move = 0;
        for (LolShopItem item : account.items) {
            damage += item.attackDamage(); health += item.maxHealth(); attackSpeed += item.attackSpeed();
            armor += item.armor(); move += item.movementSpeed();
        }
        modifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.MAX_HEALTH, HEALTH_ID, health, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.ATTACK_SPEED, SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player, Attributes.ARMOR, ARMOR_ID, armor, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.MOVEMENT_SPEED, MOVE_ID, move, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void modifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                 Identifier id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    public static double abilityPower(ServerPlayer player) {
        return account(player).items.stream().mapToDouble(LolShopItem::abilityPower).sum();
    }

    public static double attackDamage(ServerPlayer player) {
        return account(player).items.stream().mapToDouble(LolShopItem::attackDamage).sum();
    }

    public static double attackSpeed(ServerPlayer player) {
        return account(player).items.stream().mapToDouble(LolShopItem::attackSpeed).sum();
    }

    public static boolean owns(ServerPlayer player, LolShopItem item) {
        return account(player).items.contains(item);
    }

    public static Set<UUID> playerIds() { return Set.copyOf(ACCOUNTS.keySet()); }
    public static AccountSnapshot snapshot(UUID id) {
        Account account = ACCOUNTS.getOrDefault(id, new Account());
        return new AccountSnapshot(account.gold, account.items.stream().map(Enum::name).toArray(String[]::new));
    }
    public static void load(UUID id, AccountSnapshot snapshot) {
        Account account = new Account();
        account.gold = Math.max(0, snapshot.gold());
        if (snapshot.items() != null) for (String item : snapshot.items()) {
            try { account.items.add(LolShopItem.valueOf(item)); } catch (IllegalArgumentException ignored) { }
        }
        ACCOUNTS.put(id, account);
    }
    public static void clear() { ACCOUNTS.clear(); }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("darius_skills", path); }

    public enum PurchaseResult { SUCCESS, OWNED, NOT_ENOUGH_GOLD, STARTER_LOCKED, BOOTS_LOCKED }
    public static final class Account {
        private int gold = 500;
        private final EnumSet<LolShopItem> items = EnumSet.noneOf(LolShopItem.class);
        public int gold() { return gold; }
        public boolean owns(LolShopItem item) { return items.contains(item); }
        public Set<LolShopItem> items() { return Set.copyOf(items); }
    }
    public record AccountSnapshot(int gold, String[] items) {}
}
