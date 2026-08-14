package kr.leagueofminecraft.shop;

import kr.leagueofminecraft.ModConstants;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import kr.leagueofminecraft.core.ChampionManager;
import kr.leagueofminecraft.core.ChampionProgression;
import kr.leagueofminecraft.core.LolPlayerDataStore;
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
        LolShopItem ownedBoots = account.items.stream()
                .filter(owned -> owned.category() == LolShopItem.Category.BOOTS).findFirst().orElse(null);
        int price = item.price();
        boolean replacesBoots = item.category() == LolShopItem.Category.BOOTS && ownedBoots != null;
        if (replacesBoots) price = Math.max(0, item.price() - ownedBoots.price());
        if (!replacesBoots && account.items.size() >= 6) return PurchaseResult.INVENTORY_FULL;
        if (account.gold < price) return PurchaseResult.NOT_ENOUGH_GOLD;
        account.gold -= price;
        if (replacesBoots) account.items.remove(ownedBoots);
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
        int level = ChampionProgression.get(player).level();
        damage += excessCriticalStrikeAttackDamage(player, account) + (level - 1) * 0.35;
        health += (level - 1) * 1.25;
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
        double value = account(player).items.stream().mapToDouble(LolShopItem::abilityPower).sum();
        return owns(player, LolShopItem.RABADONS_DEATHCAP) ? value * 1.30 : value;
    }

    public static double attackDamage(ServerPlayer player) {
        Account account = account(player);
        return account.items.stream().mapToDouble(LolShopItem::attackDamage).sum()
                + excessCriticalStrikeAttackDamage(player, account);
    }

    public static int sell(ServerPlayer player, LolShopItem item) {
        Account account = account(player);
        if (!account.items.remove(item)) return 0;
        int refund = Math.max(1, (int) Math.floor(item.price() * 0.70));
        account.gold += refund;
        applyAttributes(player);
        LolPlayerDataStore.save(player.level().getServer());
        return refund;
    }

    public static double attackSpeed(ServerPlayer player) {
        return account(player).items.stream().mapToDouble(LolShopItem::attackSpeed).sum();
    }

    public static double criticalStrikeChance(ServerPlayer player) {
        return account(player).items.stream().mapToDouble(LolShopItem::criticalStrikeChance).sum();
    }

    public static double bonusCriticalStrikeDamage(ServerPlayer player) {
        return account(player).items.stream().mapToDouble(LolShopItem::bonusCriticalStrikeDamage).sum();
    }

    public static double magicResistance(ServerPlayer player) {
        return 30.0 + sum(player, LolShopItem::magicResistance);
    }
    public static double abilityHaste(ServerPlayer player) { return sum(player, LolShopItem::abilityHaste); }
    public static double armorPenetrationPercent(ServerPlayer player) {
        return Math.min(1.0, sum(player, LolShopItem::armorPenetrationPercent));
    }
    public static double armorPenetrationFlat(ServerPlayer player) { return sum(player, LolShopItem::armorPenetrationFlat); }
    public static double magicPenetrationPercent(ServerPlayer player) {
        return Math.min(1.0, sum(player, LolShopItem::magicPenetrationPercent));
    }
    public static double magicPenetrationFlat(ServerPlayer player) { return sum(player, LolShopItem::magicPenetrationFlat); }
    public static double healthRegenPerFive(ServerPlayer player) { return sum(player, LolShopItem::healthRegenPerFive); }
    public static double lifeSteal(ServerPlayer player) { return Math.min(1.0, sum(player, LolShopItem::lifeSteal)); }
    public static double tenacity(ServerPlayer player) { return Math.min(0.60, sum(player, LolShopItem::tenacity)); }
    public static double healAndShieldPower(ServerPlayer player) { return sum(player, LolShopItem::healAndShieldPower); }
    public static long cooldownMillis(ServerPlayer player, long baseMillis) {
        return Math.max(1L, Math.round(baseMillis * 100.0 / (100.0 + abilityHaste(player))));
    }

    public static void tickRegen(ServerPlayer player, long serverTicks) {
        if (serverTicks % 100 != 0 || !player.isAlive() || player.getHealth() >= player.getMaxHealth()) return;
        double amount = healthRegenPerFive(player);
        if (amount > 0) player.heal((float) amount);
    }

    private static double sum(ServerPlayer player, java.util.function.ToDoubleFunction<LolShopItem> stat) {
        return account(player).items.stream().mapToDouble(stat).sum();
    }

    private static double excessCriticalStrikeAttackDamage(ServerPlayer player, Account account) {
        if (!ChampionManager.isYone(player)) return 0.0;
        double rawChance = account.items.stream().mapToDouble(LolShopItem::criticalStrikeChance).sum();
        // PC Yone converts each 1% critical chance above 100% into 0.5 LoL AD.
        // Combat stats use one tenth of LoL's scale, so 50% excess becomes 2.5 damage.
        return Math.max(0.0, rawChance * 2.0 - 1.0) * 5.0;
    }

    public static boolean owns(ServerPlayer player, LolShopItem item) {
        return account(player).items.contains(item);
    }

    /** Stable virtual inventory order shared by the M GUI and Alt item keys. */
    public static List<LolShopItem> equipment(ServerPlayer player) {
        return account(player).items.stream().sorted(java.util.Comparator.comparingInt(Enum::ordinal)).toList();
    }

    public static LolShopItem equipmentAt(ServerPlayer player, int slot) {
        List<LolShopItem> items = equipment(player);
        return slot >= 0 && slot < items.size() ? items.get(slot) : null;
    }

    public static LolTrinket trinket(ServerPlayer player) { return account(player).trinket; }

    public static LolTrinket cycleTrinket(ServerPlayer player) {
        Account account = account(player);
        account.trinket = account.trinket == LolTrinket.STEALTH_WARD
                ? LolTrinket.ORACLE_LENS : LolTrinket.STEALTH_WARD;
        LolPlayerDataStore.save(player.level().getServer());
        return account.trinket;
    }

    public static Set<UUID> playerIds() { return Set.copyOf(ACCOUNTS.keySet()); }
    public static AccountSnapshot snapshot(UUID id) {
        Account account = ACCOUNTS.getOrDefault(id, new Account());
        return new AccountSnapshot(account.gold, account.items.stream().map(Enum::name).toArray(String[]::new),
                account.trinket.name());
    }
    public static void load(UUID id, AccountSnapshot snapshot) {
        Account account = new Account();
        account.gold = Math.max(0, snapshot.gold());
        if (snapshot.items() != null) for (String item : snapshot.items()) {
            try { account.items.add(LolShopItem.valueOf(item)); } catch (IllegalArgumentException ignored) { }
        }
        try { account.trinket = LolTrinket.valueOf(snapshot.trinket()); }
        catch (IllegalArgumentException | NullPointerException ignored) { }
        ACCOUNTS.put(id, account);
    }
    public static void clear() { ACCOUNTS.clear(); }

    private static Identifier id(String path) { return ModConstants.id(path); }

    public enum PurchaseResult { SUCCESS, OWNED, NOT_ENOUGH_GOLD, STARTER_LOCKED, BOOTS_LOCKED, INVENTORY_FULL }
    public static final class Account {
        private int gold = 500;
        private final EnumSet<LolShopItem> items = EnumSet.noneOf(LolShopItem.class);
        private LolTrinket trinket = LolTrinket.STEALTH_WARD;
        public int gold() { return gold; }
        public boolean owns(LolShopItem item) { return items.contains(item); }
        public Set<LolShopItem> items() { return Set.copyOf(items); }
    }
    public record AccountSnapshot(int gold, String[] items, String trinket) {}
}
