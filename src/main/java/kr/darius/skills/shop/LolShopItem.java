package kr.darius.skills.shop;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum LolShopItem {
    DORANS_BLADE("도란의 검", Category.STARTER, 450, Items.IRON_SWORD, 1.0, 1.6, 0, 0, 0, 0),
    DORANS_RING("도란의 반지", Category.STARTER, 400, Items.ENDER_EYE, 0, 1.8, 18, 0, 0, 0),
    DORANS_SHIELD("도란의 방패", Category.STARTER, 450, Items.SHIELD, 0, 2.2, 0, 0, 0, 0),
    DORANS_BOW("도란의 활", Category.STARTER, 400, Items.BOW, 0.8, 0, 0, 0.10, 0, 0),
    DORANS_HELM("도란의 투구", Category.STARTER, 450, Items.IRON_HELMET, 0, 3.0, 0, 0, 1.0, 1.0),
    MOSSTOMPER("새끼 이끼쿵쿵이", Category.STARTER, 450, Items.MOSS_BLOCK, 0, 1.0, 0, 0, 0, 0),
    SCORCHCLAW("새끼 화염발톱", Category.STARTER, 450, Items.MAGMA_CREAM, 0.5, 0, 0, 0, 0, 0),
    GUSTWALKER("새끼 바람돌이", Category.STARTER, 450, Items.FEATHER, 0, 0, 0, 0, 0, 0.04),
    WORLD_ATLAS("세계 지도집", Category.STARTER, 400, Items.BOOK, 0, 0.6, 0, 0, 0, 0),
    CULL("수확의 낫", Category.STARTER, 450, Items.IRON_HOE, 0.7, 0, 0, 0, 0, 0),
    DARK_SEAL("암흑의 인장", Category.STARTER, 350, Items.ECHO_SHARD, 0, 1.0, 15, 0, 0, 0),
    TEAR("여신의 눈물", Category.STARTER, 400, Items.PRISMARINE_CRYSTALS, 0, 0, 0, 0, 0, 0),

    BOOTS("장화", Category.BOOTS, 300, Items.LEATHER_BOOTS, 0, 0, 0, 0, 0, 0.08),
    BERSERKERS("광전사의 군화", Category.BOOTS, 1100, Items.GOLDEN_BOOTS, 0, 0, 0, 0.30, 0, 0.14),
    SORCERERS("마법사의 신발", Category.BOOTS, 1100, Items.CHAINMAIL_BOOTS, 0, 0, 0, 0, 0, 0.14),
    IONIAN("명석함의 아이오니아 장화", Category.BOOTS, 900, Items.IRON_BOOTS, 0, 0, 0, 0, 0, 0.14),
    SWIFTNESS("신속의 장화", Category.BOOTS, 1000, Items.RABBIT_FOOT, 0, 0, 0, 0, 0, 0.18),
    PLATED("판금 장화", Category.BOOTS, 1200, Items.NETHERITE_BOOTS, 0, 0, 0, 0, 2.5, 0.14),
    MERCURYS("헤르메스의 발걸음", Category.BOOTS, 1250, Items.DIAMOND_BOOTS, 0, 0, 0, 0, 0, 0.14),

    TRINITY_FORCE("삼위일체", Category.LEGENDARY, 3333, Items.TRIDENT, 3.6, 6.66, 0, 0.30, 0, 0),
    BLADE_OF_THE_RUINED_KING("몰락한 왕의 검", Category.LEGENDARY, 3200, Items.NETHERITE_SWORD, 4.0, 0, 0, 0.25, 0, 0),
    BLACK_CLEAVER("칠흑의 양날 도끼", Category.LEGENDARY, 3000, Items.NETHERITE_AXE, 4.0, 8.0, 0, 0, 0, 0);

    public enum Category { STARTER, BOOTS, LEGENDARY }

    private final String displayName;
    private final Category category;
    private final int price;
    private final Item icon;
    private final double attackDamage;
    private final double maxHealth;
    private final double abilityPower;
    private final double attackSpeed;
    private final double armor;
    private final double movementSpeed;

    LolShopItem(String displayName, Category category, int price, Item icon, double attackDamage,
                double maxHealth, double abilityPower, double attackSpeed, double armor, double movementSpeed) {
        this.displayName = displayName;
        this.category = category;
        this.price = price;
        this.icon = icon;
        this.attackDamage = attackDamage;
        this.maxHealth = maxHealth;
        this.abilityPower = abilityPower;
        this.attackSpeed = attackSpeed;
        this.armor = armor;
        this.movementSpeed = movementSpeed;
    }

    public String displayName() { return displayName; }
    public Category category() { return category; }
    public int price() { return price; }
    public Item icon() { return icon; }
    public double attackDamage() { return attackDamage; }
    public double maxHealth() { return maxHealth; }
    public double abilityPower() { return abilityPower; }
    public double attackSpeed() { return attackSpeed; }
    public double armor() { return armor; }
    public double movementSpeed() { return movementSpeed; }
    public boolean isFinishedBoots() { return category == Category.BOOTS && this != BOOTS; }
}
