package org.khmc.eventslib.collections.model;

import org.bukkit.Material;

import java.util.Locale;

public enum CollectionType {
    SWORD("Netherite Sword", "NETHERITE_SWORD", 0, CategoryGroup.TOOLS),
    PICKAXE("Netherite Pickaxe", "NETHERITE_PICKAXE", 1, CategoryGroup.TOOLS),
    AXE("Netherite Axe", "NETHERITE_AXE", 2, CategoryGroup.TOOLS),
    SHOVEL("Netherite Shovel", "NETHERITE_SHOVEL", 3, CategoryGroup.TOOLS),
    HOE("Netherite Hoe", "NETHERITE_HOE", 4, CategoryGroup.TOOLS),
    SPEAR("Netherite Spear", "NETHERITE_SPEAR", 5, CategoryGroup.TOOLS),
    MACE("Mace", "MACE", 6, CategoryGroup.TOOLS),
    BOW("Bow", "BOW", 7, CategoryGroup.TOOLS),
    CROSSBOW("Crossbow", "CROSSBOW", 8, CategoryGroup.TOOLS),
    FISHING_ROD("Fishing Rod", "FISHING_ROD", 9, CategoryGroup.TOOLS),
    SHIELD("Shield", "SHIELD", 10, CategoryGroup.TOOLS),

    HELMET("Netherite Helmet", "NETHERITE_HELMET", 11, CategoryGroup.ARMOR),
    CHESTPLATE("Netherite Chestplate", "NETHERITE_CHESTPLATE", 12, CategoryGroup.ARMOR),
    LEGGINGS("Netherite Leggings", "NETHERITE_LEGGINGS", 13, CategoryGroup.ARMOR),
    BOOTS("Netherite Boots", "NETHERITE_BOOTS", 14, CategoryGroup.ARMOR);

    public enum CategoryGroup {
        TOOLS,
        ARMOR
    }

    private final String displayName;
    private final String defaultMaterialName;
    private final int order;
    private final CategoryGroup group;

    CollectionType(String displayName, String defaultMaterialName, int order, CategoryGroup group) {
        this.displayName = displayName;
        this.defaultMaterialName = defaultMaterialName;
        this.order = order;
        this.group = group;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultMaterialName() {
        return defaultMaterialName;
    }

    public int getOrder() {
        return order;
    }

    public CategoryGroup getGroup() {
        return group;
    }

    public Material getBaseMaterial() {
        Material mat = Material.matchMaterial(defaultMaterialName);
        if (mat == null) {
            if (this == SPEAR) {
                mat = Material.TRIDENT;
            } else {
                mat = Material.NETHERITE_SWORD;
            }
        }
        return mat;
    }

    public static CollectionType fromString(String input) {
        if (input == null) return null;
        String clean = input.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        for (CollectionType type : values()) {
            if (type.name().equalsIgnoreCase(clean) || type.getDisplayName().equalsIgnoreCase(input.trim())) {
                return type;
            }
        }
        return null;
    }
}
