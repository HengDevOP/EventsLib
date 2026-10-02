package org.khmc.eventslib.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.gui.framework.MarketGUI;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.model.EventShopItem;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventShopGUI {

    private final EventsLibPlugin plugin;
    private final EventModel event;

    public EventShopGUI(EventsLibPlugin plugin, EventModel event) {
        this.plugin = plugin;
        this.event = event;
    }

    public void open(Player player) {
        if (player == null || event == null) return;

        if (!plugin.getEventManager().canAccess(player, event)) {
            String msg = plugin.getConfig().getString("messages.event-private", "&cThis event is currently private.");
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") + msg));
            SoundUtil.playError(player);
            return;
        }

        boolean isAdmin = player.isOp() || player.hasPermission("eventslib.admin");
        String title = "&f" + ColorUtil.toSmallCaps("server events - " + event.getId());
        MarketGUI gui = new MarketGUI(title, 6);

        ItemStack borderGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bMeta = borderGlass.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.empty());
            borderGlass.setItemMeta(bMeta);
        }

        // Fill bottom row
        for (int i = 45; i < 54; i++) {
            gui.setButton(i, borderGlass, e -> e.setCancelled(true));
        }

        int playerBalance = plugin.getEventManager().countPlayerEventItems(player, event);
        String eventItemName = getEventItemDisplayName(event.getEventItem());

        // Slot 45: Back button to EventHubGUI
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta bckMeta = backItem.getItemMeta();
        if (bckMeta != null) {
            bckMeta.displayName(ColorUtil.parse("&c« " + ColorUtil.toSmallCaps("back to event")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7Return to event menu.").decoration(TextDecoration.ITALIC, false));
            bckMeta.lore(lore);
            backItem.setItemMeta(bckMeta);
        }
        gui.setButton(45, backItem, e -> {
            e.setCancelled(true);
            new EventHubGUI(plugin, event).open(player);
        });

        // Slot 49: Balance Indicator
        ItemStack balanceDisplay;
        if (event.getEventItem() != null) {
            balanceDisplay = event.getEventItem().clone();
            balanceDisplay.setAmount(1);
            ItemMeta balMeta = balanceDisplay.getItemMeta();
            if (balMeta != null) {
                balMeta.displayName(ColorUtil.parse("&e✦ " + ColorUtil.toSmallCaps("your event balance")).decoration(TextDecoration.ITALIC, false));
                List<Component> bLore = new ArrayList<>();
                bLore.add(ColorUtil.parse("&7Currency: &f" + eventItemName).decoration(TextDecoration.ITALIC, false));
                bLore.add(ColorUtil.parse("&7Current Balance: &a" + playerBalance + "x").decoration(TextDecoration.ITALIC, false));
                bLore.add(ColorUtil.parse("&8(Claim more daily at 3:00 PM SGT)").decoration(TextDecoration.ITALIC, false));
                if (isAdmin) {
                    bLore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
                    bLore.add(ColorUtil.parse("&dAdmin: &7Hold item and run &e/el shop add " + event.getId() + " <price>").decoration(TextDecoration.ITALIC, false));
                }
                balMeta.lore(bLore);
                balanceDisplay.setItemMeta(balMeta);
            }
        } else {
            balanceDisplay = new ItemStack(Material.BARRIER);
            ItemMeta balMeta = balanceDisplay.getItemMeta();
            if (balMeta != null) {
                balMeta.displayName(ColorUtil.parse("&c✦ " + ColorUtil.toSmallCaps("no currency set")).decoration(TextDecoration.ITALIC, false));
                List<Component> bLore = new ArrayList<>();
                bLore.add(ColorUtil.parse("&7Admin has not set an event item yet.").decoration(TextDecoration.ITALIC, false));
                if (isAdmin) {
                    bLore.add(ColorUtil.parse("&eUse /el setitem " + event.getId() + " [amt] to configure.").decoration(TextDecoration.ITALIC, false));
                }
                balMeta.lore(bLore);
                balanceDisplay.setItemMeta(balMeta);
            }
        }
        gui.setButton(49, balanceDisplay, e -> e.setCancelled(true));

        // Shop items (slots 0 to 44)
        List<EventShopItem> items = event.getShopItems();
        if (items.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta eMeta = emptyItem.getItemMeta();
            if (eMeta != null) {
                eMeta.displayName(ColorUtil.parse("&7" + ColorUtil.toSmallCaps("shop is empty")).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7No items have been added to this event shop yet.").decoration(TextDecoration.ITALIC, false));
                if (isAdmin) {
                    lore.add(ColorUtil.parse("&eHold an item and type:").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&6/el shop add " + event.getId() + " <price>").decoration(TextDecoration.ITALIC, false));
                }
                eMeta.lore(lore);
                emptyItem.setItemMeta(eMeta);
            }
            gui.setButton(22, emptyItem, e -> e.setCancelled(true));
        } else {
            int slot = 0;
            for (EventShopItem shopItem : items) {
                if (slot >= 45) break;

                ItemStack displayStack = shopItem.getItem().clone();
                ItemMeta meta = displayStack.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&7Price: &e" + shopItem.getPrice() + "x " + eventItemName).decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&7Your Balance: " + (playerBalance >= shopItem.getPrice() ? "&a" : "&c") + playerBalance + "x").decoration(TextDecoration.ITALIC, false));

                    if (playerBalance >= shopItem.getPrice()) {
                        lore.add(ColorUtil.parse("&a✦ Click to purchase!").decoration(TextDecoration.ITALIC, false));
                    } else {
                        lore.add(ColorUtil.parse("&c✕ Not enough event items!").decoration(TextDecoration.ITALIC, false));
                    }

                    if (isAdmin) {
                        lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
                        lore.add(ColorUtil.parse("&cRight-Click to remove from shop.").decoration(TextDecoration.ITALIC, false));
                    }

                    meta.lore(lore);
                    meta.addItemFlags(ItemFlag.values());
                    displayStack.setItemMeta(meta);
                }

                final EventShopItem targetShopItem = shopItem;
                gui.setButton(slot, displayStack, e -> {
                    e.setCancelled(true);

                    // Admin remove
                    if (isAdmin && e.isRightClick()) {
                        plugin.getEventManager().removeShopItem(event.getId(), targetShopItem.getId());
                        SoundUtil.playToggle(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&cRemoved item from &e" + event.getId() + " &cshop."));
                        open(player);
                        return;
                    }

                    // Purchase
                    if (event.getEventItem() == null) {
                        SoundUtil.playError(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&cThis event does not have an event currency configured."));
                        return;
                    }

                    int currentBal = plugin.getEventManager().countPlayerEventItems(player, event);
                    if (currentBal < targetShopItem.getPrice()) {
                        SoundUtil.playError(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&cYou do not have enough " + eventItemName + "&c! Required: &e" + targetShopItem.getPrice() + "x&c, You have: &e" + currentBal + "x&c."));
                        return;
                    }

                    boolean deducted = plugin.getEventManager().deductEventItems(player, event, targetShopItem.getPrice());
                    if (!deducted) {
                        SoundUtil.playError(player);
                        return;
                    }

                    ItemStack reward = targetShopItem.getItem().clone();
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
                    if (!leftover.isEmpty()) {
                        for (ItemStack left : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), left);
                        }
                    }

                    SoundUtil.playSuccess(player);
                    String rewardName = getItemDisplayName(targetShopItem.getItem());
                    player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                            "&aPurchased &f" + rewardName + " &afor &e" + targetShopItem.getPrice() + "x " + eventItemName + "&a!"));
                    open(player);
                });

                slot++;
            }
        }

        gui.open(player);
    }

    private String getEventItemDisplayName(ItemStack item) {
        if (item == null) return "Event Items";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return LegacyComponentSerializer.legacyAmpersand().serialize(item.getItemMeta().displayName());
        }
        String name = item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Item";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return LegacyComponentSerializer.legacyAmpersand().serialize(item.getItemMeta().displayName());
        }
        String name = item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
