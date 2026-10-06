package org.khmc.eventslib.collections.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.collections.manager.CollectionManager;
import org.khmc.eventslib.collections.model.CollectionSkin;
import org.khmc.eventslib.collections.model.CollectionType;
import org.khmc.eventslib.collections.model.PlayerCollectionProfile;
import org.khmc.eventslib.gui.framework.MarketGUI;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

public class CollectionsCategoryGUI {

    private final EventsLibPlugin plugin;

    public CollectionsCategoryGUI(EventsLibPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        CollectionManager manager = plugin.getCollectionManager();
        PlayerCollectionProfile profile = manager.getProfile(player);

        String title = "&f" + ColorUtil.toSmallCaps("skin collections");
        MarketGUI gui = new MarketGUI(title, 6);

        ItemStack borderGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bMeta = borderGlass.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.empty());
            borderGlass.setItemMeta(bMeta);
        }

        // Fill top row & bottom row
        for (int i = 0; i < 9; i++) {
            gui.setButton(i, borderGlass, e -> e.setCancelled(true));
            gui.setButton(45 + i, borderGlass, e -> e.setCancelled(true));
        }

        // Header at slot 4
        int totalSkins = manager.getTotalSkinsCount();
        int totalUnlocked = 0;
        if (profile != null) {
            for (String sid : profile.getUnlockedSkins()) {
                CollectionSkin s = manager.getSkin(sid);
                if (s != null && s.isEnabled()) {
                    totalUnlocked++;
                }
            }
        }
        int percent = totalSkins > 0 ? (int) Math.round(((double) totalUnlocked / totalSkins) * 100.0) : 0;

        ItemStack headerItem = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta hMeta = headerItem.getItemMeta();
        if (hMeta != null) {
            hMeta.displayName(ColorUtil.parse("&6✦ &e" + ColorUtil.toSmallCaps("cosmetic collections")).decoration(TextDecoration.ITALIC, false));
            List<Component> hLore = new ArrayList<>();
            hLore.add(ColorUtil.parse("&7Unlock and equip exclusive visual skins").decoration(TextDecoration.ITALIC, false));
            hLore.add(ColorUtil.parse("&7for your weapons, tools, and armor!").decoration(TextDecoration.ITALIC, false));
            hLore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
            hLore.add(ColorUtil.parse("&7Total Collection: &e" + totalUnlocked + " &7/ &b" + totalSkins + " &8(&a" + percent + "%&8)").decoration(TextDecoration.ITALIC, false));
            hLore.add(ColorUtil.parse("&8(Visual skins require Resource Pack)").decoration(TextDecoration.ITALIC, false));
            hMeta.lore(hLore);
            hMeta.addItemFlags(ItemFlag.values());
            headerItem.setItemMeta(hMeta);
        }
        gui.setButton(4, headerItem, e -> e.setCancelled(true));

        // Category Slots Mapping:
        // Tools: row 1 & 2 (slots 10..16, 19..22)
        // Armor: row 3 & 4 (slots 28..31)
        int[] toolSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
        int[] armorSlots = {28, 29, 30, 31};

        int toolIdx = 0;
        int armorIdx = 0;

        for (CollectionType type : CollectionType.values()) {
            int slot;
            if (type.getGroup() == CollectionType.CategoryGroup.TOOLS) {
                if (toolIdx < toolSlots.length) {
                    slot = toolSlots[toolIdx++];
                } else {
                    continue;
                }
            } else {
                if (armorIdx < armorSlots.length) {
                    slot = armorSlots[armorIdx++];
                } else {
                    continue;
                }
            }

            int catTotal = manager.getTotalSkinsCount(type);
            int catOwned = profile != null ? profile.getUnlockedCount(type, (manager.getAllSkins() instanceof java.util.Map<?,?> ? null : null)) : 0;
            // Count manually to ensure exact accuracy
            if (profile != null) {
                catOwned = 0;
                for (String sid : profile.getUnlockedSkins()) {
                    CollectionSkin s = manager.getSkin(sid);
                    if (s != null && s.getType() == type && s.isEnabled()) {
                        catOwned++;
                    }
                }
            }

            String equippedSkinId = profile != null ? profile.getEquippedSkin(type) : null;
            CollectionSkin equippedSkin = (equippedSkinId != null) ? manager.getSkin(equippedSkinId) : null;
            String equippedDisplay = (equippedSkin != null) ? "&b" + equippedSkin.getName() : "&7None (Default)";

            ItemStack catItem = new ItemStack(type.getBaseMaterial());
            ItemMeta cMeta = catItem.getItemMeta();
            if (cMeta != null) {
                cMeta.displayName(ColorUtil.parse("&e✦ &f" + ColorUtil.toSmallCaps(type.getDisplayName())).decoration(TextDecoration.ITALIC, false));
                List<Component> cLore = new ArrayList<>();
                cLore.add(ColorUtil.parse("&7Category: &e" + type.getGroup().name()).decoration(TextDecoration.ITALIC, false));
                cLore.add(ColorUtil.parse("&7Skins Collected: &a" + catOwned + " &7/ &f" + catTotal).decoration(TextDecoration.ITALIC, false));
                cLore.add(ColorUtil.parse("&7Equipped Skin: " + equippedDisplay).decoration(TextDecoration.ITALIC, false));
                cLore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
                cLore.add(ColorUtil.parse("&e✦ Click to browse skins!").decoration(TextDecoration.ITALIC, false));
                cMeta.lore(cLore);
                cMeta.addItemFlags(ItemFlag.values());
                catItem.setItemMeta(cMeta);
            }

            final CollectionType targetType = type;
            gui.setButton(slot, catItem, e -> {
                e.setCancelled(true);
                SoundUtil.playClick(player);
                new CollectionSkinsGUI(plugin, targetType, 0).open(player);
            });
        }

        // Close button at slot 49
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta clMeta = closeItem.getItemMeta();
        if (clMeta != null) {
            clMeta.displayName(ColorUtil.parse("&c✕ " + ColorUtil.toSmallCaps("close")).decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(clMeta);
        }
        gui.setButton(49, closeItem, e -> {
            e.setCancelled(true);
            player.closeInventory();
        });

        gui.open(player);
    }
}
