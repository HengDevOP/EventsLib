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
import org.khmc.eventslib.util.ModelUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

public class CollectionSkinsGUI {

    private final EventsLibPlugin plugin;
    private final CollectionType type;
    private final int page;

    private static final int PAGE_SIZE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public CollectionSkinsGUI(EventsLibPlugin plugin, CollectionType type, int page) {
        this.plugin = plugin;
        this.type = type;
        this.page = Math.max(0, page);
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        CollectionManager manager = plugin.getCollectionManager();
        PlayerCollectionProfile profile = manager.getProfile(player);

        String title = "&f" + ColorUtil.toSmallCaps("skins - " + type.getDisplayName());
        MarketGUI gui = new MarketGUI(title, 6);

        ItemStack borderGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bMeta = borderGlass.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.empty());
            borderGlass.setItemMeta(bMeta);
        }

        // Fill border
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setButton(i, borderGlass, e -> e.setCancelled(true));
            }
        }

        // Slot 4: Category Info Header
        String currentlyEquipped = profile != null ? profile.getEquippedSkin(type) : null;
        CollectionSkin eqSkin = (currentlyEquipped != null) ? manager.getSkin(currentlyEquipped) : null;
        String eqName = (eqSkin != null) ? "&b" + eqSkin.getName() : "&7None (Default)";

        int ownedCount = 0;
        if (profile != null) {
            for (String sid : profile.getUnlockedSkins()) {
                CollectionSkin s = manager.getSkin(sid);
                if (s != null && s.getType() == type && s.isEnabled()) {
                    ownedCount++;
                }
            }
        }
        int totalAvailable = manager.getTotalSkinsCount(type);

        ItemStack catBanner = new ItemStack(type.getBaseMaterial());
        ItemMeta bnMeta = catBanner.getItemMeta();
        if (bnMeta != null) {
            bnMeta.displayName(ColorUtil.parse("&e✦ &f" + ColorUtil.toSmallCaps(type.getDisplayName())).decoration(TextDecoration.ITALIC, false));
            List<Component> bnLore = new ArrayList<>();
            bnLore.add(ColorUtil.parse("&7Category: &e" + type.getGroup().name()).decoration(TextDecoration.ITALIC, false));
            bnLore.add(ColorUtil.parse("&7Skins Owned: &a" + ownedCount + " &7/ &f" + totalAvailable).decoration(TextDecoration.ITALIC, false));
            bnLore.add(ColorUtil.parse("&7Active Skin: " + eqName).decoration(TextDecoration.ITALIC, false));
            bnLore.add(ColorUtil.parse("&8(Visual skins require Resource Pack)").decoration(TextDecoration.ITALIC, false));
            bnMeta.lore(bnLore);
            bnMeta.addItemFlags(ItemFlag.values());
            catBanner.setItemMeta(bnMeta);
        }
        gui.setButton(4, catBanner, e -> e.setCancelled(true));

        // Slot 48: Default / Unequip Skin Button
        boolean isDefaultEquipped = (currentlyEquipped == null || currentlyEquipped.isEmpty());
        ItemStack defaultItem = new ItemStack(isDefaultEquipped ? Material.NETHERITE_INGOT : Material.BARRIER);
        ItemMeta dMeta = defaultItem.getItemMeta();
        if (dMeta != null) {
            dMeta.displayName(ColorUtil.parse("&6✦ " + ColorUtil.toSmallCaps("default skin")).decoration(TextDecoration.ITALIC, false));
            List<Component> dLore = new ArrayList<>();
            dLore.add(ColorUtil.parse("&7Unequip cosmetic skins and revert to").decoration(TextDecoration.ITALIC, false));
            dLore.add(ColorUtil.parse("&7the standard vanilla appearance.").decoration(TextDecoration.ITALIC, false));
            dLore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
            if (isDefaultEquipped) {
                dLore.add(ColorUtil.parse("&a✔ EQUIPPED").decoration(TextDecoration.ITALIC, false));
                dLore.add(ColorUtil.parse("&7Currently active.").decoration(TextDecoration.ITALIC, false));
            } else {
                dLore.add(ColorUtil.parse("&e✦ Click to unequip cosmetic.").decoration(TextDecoration.ITALIC, false));
            }
            dMeta.lore(dLore);
            dMeta.addItemFlags(ItemFlag.values());
            defaultItem.setItemMeta(dMeta);
        }
        gui.setButton(48, defaultItem, e -> {
            e.setCancelled(true);
            if (isDefaultEquipped) {
                SoundUtil.playClick(player);
                return;
            }
            manager.equipSkin(player, type, null);
            SoundUtil.playSuccess(player);
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                    "&aUnequipped cosmetic skin for &e" + type.getDisplayName() + "&a. Reverted to default."));
            open(player);
        });

        // Skins Pagination
        List<CollectionSkin> allSkins = manager.getSkinsForType(type);
        int totalPages = Math.max(1, (int) Math.ceil((double) allSkins.size() / PAGE_SIZE));
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, allSkins.size());

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int slot = CONTENT_SLOTS[i];
            int skinIdx = startIndex + i;

            if (skinIdx < endIndex) {
                CollectionSkin skin = allSkins.get(skinIdx);
                boolean isOwned = (profile != null && profile.hasUnlocked(skin.getId())) || player.hasPermission("collections.admin");
                boolean isEquipped = skin.getId().equalsIgnoreCase(currentlyEquipped);

                ItemStack skinItem = new ItemStack(skin.getBaseMaterial());
                ItemMeta sMeta = skinItem.getItemMeta();
                if (sMeta != null) {
                    // Apply preview model on GUI item!
                    ModelUtil.applyModel(sMeta, skin.getCustomModel(), plugin);

                    sMeta.displayName(ColorUtil.parse("&e✦ &b" + skin.getName()).decoration(TextDecoration.ITALIC, false));
                    List<Component> sLore = new ArrayList<>();
                    if (skin.getDescription() != null && !skin.getDescription().isEmpty()) {
                        sLore.add(ColorUtil.parse("&7" + skin.getDescription()).decoration(TextDecoration.ITALIC, false));
                        sLore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
                    }

                    if (isEquipped) {
                        sLore.add(ColorUtil.parse("&a✔ EQUIPPED").decoration(TextDecoration.ITALIC, false));
                        sLore.add(ColorUtil.parse("&7Currently active.").decoration(TextDecoration.ITALIC, false));
                    } else if (isOwned) {
                        sLore.add(ColorUtil.parse("&b✦ UNLOCKED").decoration(TextDecoration.ITALIC, false));
                        sLore.add(ColorUtil.parse("&eClick to equip.").decoration(TextDecoration.ITALIC, false));
                    } else {
                        sLore.add(ColorUtil.parse("&c✕ LOCKED").decoration(TextDecoration.ITALIC, false));
                        sLore.add(ColorUtil.parse("&7You don't own this skin.").decoration(TextDecoration.ITALIC, false));
                    }

                    sLore.add(ColorUtil.parse("&8Skin ID: &7" + skin.getId()).decoration(TextDecoration.ITALIC, false));
                    sMeta.lore(sLore);
                    sMeta.addItemFlags(ItemFlag.values());
                    skinItem.setItemMeta(sMeta);
                }

                gui.setButton(slot, skinItem, e -> {
                    e.setCancelled(true);
                    if (isEquipped) {
                        SoundUtil.playClick(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&7You already have the &b" + skin.getName() + " &7skin equipped!"));
                        return;
                    }

                    if (!isOwned) {
                        SoundUtil.playError(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&cYou do not own the &e" + skin.getName() + " &cskin! Status: &cLOCKED&c."));
                        return;
                    }

                    boolean success = manager.equipSkin(player, type, skin.getId());
                    if (success) {
                        SoundUtil.playSuccess(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&aEquipped &b" + skin.getName() + " &afor &e" + type.getDisplayName() + "&a!"));
                        open(player);
                    } else {
                        SoundUtil.playError(player);
                    }
                });
            } else {
                gui.setButton(slot, null, null);
            }
        }

        // Navigation Row:
        // Slot 45: Previous Page
        if (page > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prevItem.getItemMeta();
            if (pMeta != null) {
                pMeta.displayName(ColorUtil.parse("&e« " + ColorUtil.toSmallCaps("previous page")).decoration(TextDecoration.ITALIC, false));
                prevItem.setItemMeta(pMeta);
            }
            gui.setButton(45, prevItem, e -> {
                e.setCancelled(true);
                SoundUtil.playClick(player);
                new CollectionSkinsGUI(plugin, type, page - 1).open(player);
            });
        }

        // Slot 49: Back to Categories
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta bcMeta = backItem.getItemMeta();
        if (bcMeta != null) {
            bcMeta.displayName(ColorUtil.parse("&c« " + ColorUtil.toSmallCaps("back to categories")).decoration(TextDecoration.ITALIC, false));
            backItem.setItemMeta(bcMeta);
        }
        gui.setButton(49, backItem, e -> {
            e.setCancelled(true);
            SoundUtil.playClick(player);
            new CollectionsCategoryGUI(plugin).open(player);
        });

        // Slot 53: Next Page
        if (page < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nMeta = nextItem.getItemMeta();
            if (nMeta != null) {
                nMeta.displayName(ColorUtil.parse("&e" + ColorUtil.toSmallCaps("next page") + " »").decoration(TextDecoration.ITALIC, false));
                nextItem.setItemMeta(nMeta);
            }
            gui.setButton(53, nextItem, e -> {
                e.setCancelled(true);
                SoundUtil.playClick(player);
                new CollectionSkinsGUI(plugin, type, page + 1).open(player);
            });
        }

        gui.open(player);
    }
}
