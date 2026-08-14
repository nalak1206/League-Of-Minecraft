package kr.darius.skills.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    SLIGHTLY_MAGICAL_BOOTS("약간 신비한 신발", Category.BOOTS, 300, Items.AMETHYST_SHARD, 0, 0, 0, 0, 0, 0.10),
    BERSERKERS("광전사의 군화", Category.BOOTS, 1100, Items.GOLDEN_BOOTS, 0, 0, 0, 0.30, 0, 0.14),
    SORCERERS("마법사의 신발", Category.BOOTS, 1100, Items.CHAINMAIL_BOOTS, 0, 0, 0, 0, 0, 0.14),
    IONIAN("명석함의 아이오니아 장화", Category.BOOTS, 900, Items.IRON_BOOTS, 0, 0, 0, 0, 0, 0.14),
    SWIFTNESS("신속의 장화", Category.BOOTS, 1000, Items.RABBIT_FOOT, 0, 0, 0, 0, 0, 0.18),
    PLATED("판금 장화", Category.BOOTS, 1200, Items.NETHERITE_BOOTS, 0, 0, 0, 0, 2.5, 0.14),
    MERCURYS("헤르메스의 발걸음", Category.BOOTS, 1250, Items.DIAMOND_BOOTS, 0, 0, 0, 0, 0, 0.14),
    ARMORED_ADVANCE("무장 진격", Category.BOOTS, 1500, Items.NETHERITE_BOOTS, 0, 0, 0, 0, 4.0, 0.16),
    GUNMETAL_GREAVES("건메탈 군화", Category.BOOTS, 1500, Items.IRON_BOOTS, 0, 0, 0, 0.45, 0, 0.16),
    SWIFTMARCH("신속행진", Category.BOOTS, 1500, Items.RABBIT_FOOT, 0, 0, 0, 0, 0, 0.23),
    SPELLSLINGERS_SHOES("주문투척자의 신발", Category.BOOTS, 1500, Items.AMETHYST_SHARD, 0, 0, 25, 0, 0, 0.16),
    CHAINLACED_CRUSHERS("사슬꾼 분쇄자", Category.BOOTS, 1500, Items.CHAINMAIL_BOOTS, 0, 0, 0, 0, 0, 0.16),
    CRIMSON_LUCIDITY("핏빛 명석함", Category.BOOTS, 1500, Items.REDSTONE, 0, 0, 0, 0, 0, 0.16),
    GREEDY_GREAVES("탐욕의 군화", Category.BOOTS, 1500, Items.GOLD_INGOT, 1.5, 0, 0, 0.20, 0, 0.16),
    FOREVER_FORWARD("불멸의 길", Category.BOOTS, 1500, Items.TOTEM_OF_UNDYING, 0, 4.0, 0, 0, 0, 0.16),

    TRINITY_FORCE("삼위일체", Category.FIGHTER, 3333, Items.MUSIC_DISC_13, 3.6, 6.66, 0, 0.30, 0, 0),
    BLACK_CLEAVER("칠흑의 양날 도끼", Category.FIGHTER, 3000, Items.NETHERITE_AXE, 4.5, 8.0, 0, 0, 0, 0),
    BLADE_OF_THE_RUINED_KING("몰락한 왕의 검", Category.FIGHTER, 3200, Items.IRON_SWORD, 4.0, 0, 0, 0.25, 0, 0),
    SUNDERED_SKY("갈라진 하늘", Category.FIGHTER, 3100, Items.NETHERITE_SPEAR, 4.5, 9.0, 0, 0, 0, 0),
    STERAKS_GAGE("스테락의 도전", Category.FIGHTER, 3200, Items.GOAT_HORN, 0, 8.0, 0, 0, 0, 0),
    DEATHS_DANCE("죽음의 무도", Category.FIGHTER, 3300, Items.COPPER_SPEAR, 6.0, 0, 0, 0, 5.0, 0),

    INFINITY_EDGE("무한의 대검", Category.MARKSMAN, 3500, Items.GOLDEN_SWORD, 7.5, 0, 0, 0, 0, 0),
    LORD_DOMINIKS_REGARDS("도미닉 경의 인사", Category.MARKSMAN, 3300, Items.CROSSBOW, 3.5, 0, 0, 0, 0, 0),
    THE_COLLECTOR("징수의 총", Category.MARKSMAN, 3000, Items.GOLDEN_HOE, 5.0, 0, 0, 0, 0, 0),
    KRAKEN_SLAYER("크라켄 학살자", Category.MARKSMAN, 3100, Items.NETHERITE_PICKAXE, 4.5, 0, 0, 0.40, 0, 0.04),
    BLOODTHIRSTER("피바라기", Category.MARKSMAN, 3400, Items.REDSTONE, 8.0, 0, 0, 0, 0, 0),
    STATIKK_SHIV("스태틱의 단검", Category.MARKSMAN, 2800, Items.BREEZE_ROD, 4.5, 0, 0, 0.30, 0, 0.04),

    YOUMUUS_GHOSTBLADE("요우무의 유령검", Category.ASSASSIN, 2800, Items.NETHERITE_SWORD, 5.5, 0, 0, 0, 0, 0.06),
    EDGE_OF_NIGHT("밤의 끝자락", Category.ASSASSIN, 3000, Items.DYED_BUNDLE.pick(DyeColor.PURPLE), 5.0, 5.0, 0, 0, 0, 0),
    SERYLDAS_GRUDGE("세릴다의 원한", Category.ASSASSIN, 3000, Items.IRON_SPEAR, 4.5, 0, 0, 0, 0, 0),
    OPPORTUNITY("벼락폭풍검", Category.ASSASSIN, 2700, Items.COPPER_SWORD, 5.5, 0, 0, 0, 0, 0.04),
    AXIOM_ARC("원칙의 원형낫", Category.ASSASSIN, 3000, Items.ENDER_EYE, 5.5, 0, 0, 0, 0, 0),
    PROFANE_HYDRA("불경한 히드라", Category.ASSASSIN, 3200, Items.BONE, 6.0, 0, 0, 0, 0, 0),

    RABADONS_DEATHCAP("라바돈의 죽음모자", Category.MAGE, 3600, Items.DRAGON_EGG, 0, 0, 130, 0, 0, 0),
    ZHONYAS_HOURGLASS("존야의 모래시계", Category.MAGE, 3250, Items.CLOCK, 0, 0, 105, 0, 5.0, 0),
    VOID_STAFF("공허의 지팡이", Category.MAGE, 3000, Items.BREEZE_ROD, 0, 0, 95, 0, 0, 0),
    LIANDRYS_TORMENT("리안드리의 고통", Category.MAGE, 3000, Items.SKELETON_SKULL, 0, 6.0, 70, 0, 0, 0),
    RYLAIS_CRYSTAL_SCEPTER("라일라이의 수정홀", Category.MAGE, 2600, Items.DIAMOND_SPEAR, 0, 8.0, 65, 0, 0, 0),
    SHADOWFLAME("그림자불꽃", Category.MAGE, 3200, Items.NETHERITE_HELMET, 0, 0, 110, 0, 0, 0),

    HEARTSTEEL("강철심장", Category.TANK, 3000, Items.CHISELED_COPPER.weathering().unaffected(), 0, 18.0, 0, 0, 0, 0),
    SUNFIRE_AEGIS("태양불꽃 방패", Category.TANK, 2700, Items.GOLDEN_APPLE, 0, 7.0, 0, 0, 5.0, 0),
    THORNMAIL("가시 갑옷", Category.TANK, 2450, Items.CHAINMAIL_CHESTPLATE, 0, 7.0, 0, 0, 7.5, 0),
    WARMOGS_ARMOR("워모그의 갑옷", Category.TANK, 3100, Items.LEATHER_CHESTPLATE, 0, 20.0, 0, 0, 0, 0.05),
    KAENIC_ROOKERN("케이닉 루컨", Category.TANK, 2900, Items.RECOVERY_COMPASS, 0, 8.0, 0, 0, 0, 0),
    JAKSHO("해신 작쇼", Category.TANK, 3200, Items.NETHERITE_CHESTPLATE, 0, 7.0, 0, 0, 5.0, 0),

    LOCKET_OF_THE_IRON_SOLARI("강철의 솔라리 펜던트", Category.SUPPORT, 2200, Items.GOLDEN_CHESTPLATE, 0, 4.0, 0, 0, 2.5, 0),
    REDEMPTION("구원", Category.SUPPORT, 2300, Items.MAGMA_CREAM, 0, 4.0, 60, 0, 0, 0),
    SHURELYAS_BATTLESONG("슈렐리아의 군가", Category.SUPPORT, 2200, Items.GOLDEN_HELMET, 0, 4.0, 55, 0, 0, 0.05),
    KNIGHTS_VOW("기사의 맹세", Category.SUPPORT, 2300, Items.NETHERITE_HELMET, 0, 8.0, 0, 0, 3.0, 0),
    ARDENT_CENSER("불타는 향로", Category.SUPPORT, 2200, Items.DIAMOND_SHOVEL, 0, 0, 60, 0, 0, 0.04),
    IMPERIAL_MANDATE("제국의 명령", Category.SUPPORT, 2250, Items.NETHERITE_SHOVEL, 0, 4.0, 60, 0, 0, 0);

    public enum Category {
        STARTER("시작 아이템"), BOOTS("장화"), FIGHTER("전사"), MARKSMAN("원거리 딜러"),
        ASSASSIN("암살자"), MAGE("마법사"), TANK("탱커"), SUPPORT("서포터");
        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

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
    public ItemStack createIcon() {
        ItemStack stack = new ItemStack(icon);
        if (this == BLADE_OF_THE_RUINED_KING) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }
    public double attackDamage() { return attackDamage; }
    public double maxHealth() { return maxHealth; }
    public double abilityPower() { return abilityPower; }
    public double attackSpeed() { return attackSpeed; }
    public double armor() { return armor; }
    public double movementSpeed() { return movementSpeed; }
    public double criticalStrikeChance() {
        return switch (this) {
            case INFINITY_EDGE, LORD_DOMINIKS_REGARDS, THE_COLLECTOR -> 0.25;
            default -> 0.0;
        };
    }
    public double bonusCriticalStrikeDamage() { return this == INFINITY_EDGE ? 0.30 : 0.0; }
    /** Values below use League's display scale (for example 18 means 18 lethality). */
    public double magicResistance() {
        return switch (this) {
            case MERCURYS, CHAINLACED_CRUSHERS, LOCKET_OF_THE_IRON_SOLARI -> 25;
            case KAENIC_ROOKERN -> 80;
            case JAKSHO -> 50;
            default -> 0;
        };
    }
    public double abilityHaste() {
        return switch (this) {
            case IONIAN -> 15;
            case CRIMSON_LUCIDITY -> 25;
            case BLACK_CLEAVER, AXIOM_ARC, LIANDRYS_TORMENT, REDEMPTION, SHURELYAS_BATTLESONG -> 20;
            case SUNDERED_SKY, DEATHS_DANCE, SERYLDAS_GRUDGE, PROFANE_HYDRA,
                    ZHONYAS_HOURGLASS, RYLAIS_CRYSTAL_SCEPTER, SUNFIRE_AEGIS,
                    WARMOGS_ARMOR, KAENIC_ROOKERN, JAKSHO, LOCKET_OF_THE_IRON_SOLARI,
                    KNIGHTS_VOW, ARDENT_CENSER, IMPERIAL_MANDATE -> 15;
            case TRINITY_FORCE, STERAKS_GAGE, EDGE_OF_NIGHT -> 10;
            default -> 0;
        };
    }
    public double armorPenetrationPercent() {
        return switch (this) {
            case LORD_DOMINIKS_REGARDS -> 0.35;
            case SERYLDAS_GRUDGE -> 0.30;
            default -> 0;
        };
    }
    public double armorPenetrationFlat() {
        return switch (this) {
            case THE_COLLECTOR -> 10;
            case EDGE_OF_NIGHT -> 15;
            case YOUMUUS_GHOSTBLADE, OPPORTUNITY, AXIOM_ARC, PROFANE_HYDRA -> 18;
            default -> 0;
        };
    }
    public double magicPenetrationPercent() { return this == VOID_STAFF ? 0.40 : 0; }
    public double magicPenetrationFlat() {
        return switch (this) {
            case SORCERERS -> 12;
            case SPELLSLINGERS_SHOES -> 18;
            case SHADOWFLAME -> 15;
            default -> 0;
        };
    }
    /** Minecraft health points restored per five seconds. */
    public double healthRegenPerFive() {
        return switch (this) {
            case DORANS_SHIELD -> 1.2;
            case DORANS_HELM -> 0.8;
            case WARMOGS_ARMOR -> 5.0;
            case WORLD_ATLAS, HEARTSTEEL -> 0.5;
            default -> 0;
        };
    }
    public double lifeSteal() {
        return switch (this) {
            case DORANS_BLADE -> 0.025;
            case BLADE_OF_THE_RUINED_KING -> 0.10;
            case BLOODTHIRSTER -> 0.15;
            default -> 0;
        };
    }
    public double tenacity() {
        return switch (this) {
            case MERCURYS, CHAINLACED_CRUSHERS -> 0.30;
            case STERAKS_GAGE -> 0.20;
            default -> 0;
        };
    }
    public double healAndShieldPower() {
        return switch (this) {
            case REDEMPTION -> 0.15;
            case ARDENT_CENSER -> 0.10;
            default -> 0;
        };
    }
    public String statSummary() {
        StringBuilder text = new StringBuilder();
        add(text, attackDamage, "AD"); add(text, abilityPower, "AP"); add(text, maxHealth, "HP");
        add(text, armor * 10, "방어"); add(text, magicResistance(), "마저");
        add(text, abilityHaste(), "가속"); add(text, armorPenetrationFlat(), "물관");
        add(text, armorPenetrationPercent() * 100, "%물관"); add(text, magicPenetrationFlat(), "마관");
        add(text, magicPenetrationPercent() * 100, "%마관"); add(text, attackSpeed * 100, "%공속");
        add(text, criticalStrikeChance() * 100, "%치명타"); add(text, lifeSteal() * 100, "%흡혈");
        add(text, tenacity() * 100, "%강인함"); add(text, healthRegenPerFive(), "5초재생");
        return text.toString();
    }
    private static void add(StringBuilder text, double value, String label) {
        if (value == 0) return;
        if (!text.isEmpty()) text.append(" ");
        text.append(value == Math.rint(value) ? Integer.toString((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value))
                .append(label);
    }
    public boolean isFinishedBoots() { return category == Category.BOOTS && this != BOOTS && this != SLIGHTLY_MAGICAL_BOOTS; }
}
