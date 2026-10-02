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
import org.khmc.eventslib.hook.ShardsHook;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.ModelUtil;
import org.khmc.eventslib.util.SoundUtil;
import org.khmc.eventslib.util.TimeResetUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class EventHubGUI {

    private final EventsLibPlugin plugin;
    private final EventModel event;

    public EventHubGUI(EventsLibPlugin plugin, EventModel event) {
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
        MarketGUI gui = new MarketGUI(title, 3);

        ItemStack borderGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bMeta = borderGlass.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.empty());
            borderGlass.setItemMeta(bMeta);
        }

        // Fill border
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i == 9 || i == 11 || i == 13 || i == 15 || i == 17) {
                gui.setButton(i, borderGlass, e -> e.setCancelled(true));
            }
        }

        // Slot 4: Event Icon Header
        ItemStack bannerItem = new ItemStack(Material.PAPER);
        ItemMeta bnMeta = bannerItem.getItemMeta();
        if (bnMeta != null) {
            String cmdStr = event.getCustomModelData();
            if (cmdStr.isEmpty() || cmdStr.equals("0")) {
                String configKey = event.getPrivacy().isPublic() ? "icon.public-custom-model-data" : "icon.private-custom-model-data";
                cmdStr = plugin.getConfig().getString(configKey, plugin.getConfig().getString("icon.default-custom-model-data", ""));
            }
            if (cmdStr != null && !cmdStr.isEmpty() && !cmdStr.equals("0")) {
                ModelUtil.applyModel(bnMeta, cmdStr, plugin);
            }

            bnMeta.displayName(ColorUtil.parse("&e✦ &d" + event.getId()).decoration(TextDecoration.ITALIC, false));
            List<Component> bnLore = new ArrayList<>();
            bnLore.add(ColorUtil.parse("&7Title: " + event.getTitle()).decoration(TextDecoration.ITALIC, false));
            bnLore.add(ColorUtil.parse("&7Privacy: " + event.getPrivacy().getFormattedDisplay()).decoration(TextDecoration.ITALIC, false));
            if (event.hasCustomModelData()) {
                bnLore.add(ColorUtil.parse("&7Model Data: &b" + event.getCustomModelData()).decoration(TextDecoration.ITALIC, false));
            }
            bnLore.add(ColorUtil.parse("&7Creator: &f" + event.getCreatorName()).decoration(TextDecoration.ITALIC, false));
            bnMeta.lore(bnLore);
            bnMeta.addItemFlags(ItemFlag.values());
            bannerItem.setItemMeta(bnMeta);
        }
        gui.setButton(4, bannerItem, e -> e.setCancelled(true));

        // 1. Slot 10: Events Item & Daily Login Reward
        boolean hasClaimed = plugin.getEventManager().hasClaimedToday(player.getUniqueId(), event.getId());
        Duration untilReset = TimeResetUtil.getTimeUntilNextReset();
        ItemStack eventItemDisplay;

        if (event.getEventItem() != null) {
            eventItemDisplay = event.getEventItem().clone();
            eventItemDisplay.setAmount(1);
            ItemMeta eMeta = eventItemDisplay.getItemMeta();
            if (eMeta != null) {
                Component currentName = eMeta.hasDisplayName() ? eMeta.displayName() :
                        ColorUtil.parse("&e✦ " + ColorUtil.toSmallCaps("events item"));
                eMeta.displayName(currentName);

                List<Component> lore = eMeta.hasLore() && eMeta.lore() != null ? new ArrayList<>(eMeta.lore()) : new ArrayList<>();
                lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Daily Login Reward: &f" + event.getDailyClaimAmount() + "x").decoration(TextDecoration.ITALIC, false));

                if (hasClaimed) {
                    lore.add(ColorUtil.parse("&c✕ Daily reward already claimed!").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&7Next claim in: &e" + TimeResetUtil.formatDuration(untilReset)).decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&8(Resets daily at 3:00 PM SGT)").decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(ColorUtil.parse("&a✔ Daily reward ready to claim!").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&e✦ Click to claim daily reward!").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&8(Resets daily at 3:00 PM SGT)").decoration(TextDecoration.ITALIC, false));
                }
                eMeta.lore(lore);
                eMeta.addItemFlags(ItemFlag.values());
                eventItemDisplay.setItemMeta(eMeta);
            }
        } else {
            eventItemDisplay = new ItemStack(Material.PAPER);
            ItemMeta eMeta = eventItemDisplay.getItemMeta();
            if (eMeta != null) {
                eMeta.displayName(ColorUtil.parse("&c✦ " + ColorUtil.toSmallCaps("events item")).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7No daily event item has been set yet.").decoration(TextDecoration.ITALIC, false));
                if (isAdmin) {
                    lore.add(ColorUtil.parse("&eHold an item and run:").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&6/el setitem " + event.getId() + " [daily_amount]").decoration(TextDecoration.ITALIC, false));
                }
                eMeta.lore(lore);
                eventItemDisplay.setItemMeta(eMeta);
            }
        }

        gui.setButton(10, eventItemDisplay, e -> {
            e.setCancelled(true);
            if (event.getEventItem() == null) {
                SoundUtil.playError(player);
                if (isAdmin) {
                    player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                            "&cNo event item set yet. Hold an item and use &e/el setitem " + event.getId() + " [amount]&c."));
                } else {
                    player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                            "&cNo daily login item is configured for this event yet."));
                }
                return;
            }

            if (plugin.getEventManager().hasClaimedToday(player.getUniqueId(), event.getId())) {
                SoundUtil.playError(player);
                Duration left = TimeResetUtil.getTimeUntilNextReset();
                player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                        "&cYou already claimed today's reward! Next reset in &e" + TimeResetUtil.formatDuration(left) + " &7(3:00 PM SGT)&c."));
                return;
            }

            boolean claimed = plugin.getEventManager().claimDailyReward(player, event);
            if (claimed) {
                SoundUtil.playSuccess(player);
                String itemName = event.getEventItem().hasItemMeta() && event.getEventItem().getItemMeta().hasDisplayName() ?
                        LegacyComponentSerializer.legacyAmpersand().serialize(event.getEventItem().getItemMeta().displayName()) :
                        event.getEventItem().getType().name();
                player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                        "&aClaimed daily reward: &e" + event.getDailyClaimAmount() + "x &f" + itemName + "&a!"));
                open(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        // 2. Slot 12: Event Spin (Wheel / Roulette with Shards)
        ItemStack spinItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta spMeta = spinItem.getItemMeta();
        if (spMeta != null) {
            spMeta.displayName(ColorUtil.parse("&e✦ &6&l" + ColorUtil.toSmallCaps("event spin") + " &e✦").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7Spin the wheel using shards to win exclusive prizes!").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&7Spin Cost: &d" + ShardsHook.format(event.getSpinCost())).decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&7Rewards in pool: &e" + event.getSpinRewards().size()).decoration(TextDecoration.ITALIC, false));
            double shardsBal = ShardsHook.getBalance(player);
            lore.add(ColorUtil.parse("&7Your Shards: &d" + ShardsHook.format(shardsBal)).decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&e✦ Click to open Spin Wheel!").decoration(TextDecoration.ITALIC, false));
            spMeta.lore(lore);
            spinItem.setItemMeta(spMeta);
        }

        gui.setButton(12, spinItem, e -> {
            e.setCancelled(true);
            new EventSpinGUI(plugin, event).open(player);
        });

        // 3. Slot 14: Event Shop
        ItemStack shopItem = new ItemStack(Material.EMERALD);
        ItemMeta sMeta = shopItem.getItemMeta();
        if (sMeta != null) {
            sMeta.displayName(ColorUtil.parse("&6✦ " + ColorUtil.toSmallCaps("event shop")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7Exchange your event items for exclusive rewards.").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&7Available Rewards: &e" + event.getShopItems().size()).decoration(TextDecoration.ITALIC, false));
            if (event.getEventItem() != null) {
                int bal = plugin.getEventManager().countPlayerEventItems(player, event);
                lore.add(ColorUtil.parse("&7Your Balance: &a" + bal + "x").decoration(TextDecoration.ITALIC, false));
            }
            lore.add(ColorUtil.parse("&e✦ Click to open shop!").decoration(TextDecoration.ITALIC, false));
            sMeta.lore(lore);
            shopItem.setItemMeta(sMeta);
        }

        gui.setButton(14, shopItem, e -> {
            e.setCancelled(true);
            new EventShopGUI(plugin, event).open(player);
        });

        // 4. Slot 16: View 54-Slot Layout / Showcase
        ItemStack showcaseItem = new ItemStack(Material.BOOK);
        ItemMeta scMeta = showcaseItem.getItemMeta();
        if (scMeta != null) {
            scMeta.displayName(ColorUtil.parse("&b✦ " + ColorUtil.toSmallCaps("event layout")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7View the 54-slot showcase layout for this event.").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&7Occupied Slots: &f" + event.getOccupiedSlotsCount() + "/54").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&e✦ Click to view layout!").decoration(TextDecoration.ITALIC, false));
            scMeta.lore(lore);
            showcaseItem.setItemMeta(scMeta);
        }

        gui.setButton(16, showcaseItem, e -> {
            e.setCancelled(true);
            new EventViewerGUI(plugin, event).open(player);
        });

        // Slot 22: Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta bckMeta = backItem.getItemMeta();
        if (bckMeta != null) {
            bckMeta.displayName(ColorUtil.parse("&c« " + ColorUtil.toSmallCaps("back to directory")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7Return to the main server events list.").decoration(TextDecoration.ITALIC, false));
            bckMeta.lore(lore);
            backItem.setItemMeta(bckMeta);
        }
        gui.setButton(22, backItem, e -> {
            e.setCancelled(true);
            new EventsDirectoryGUI(plugin).open(player);
        });

        // Admin Extra Buttons:
        if (isAdmin) {
            // Slot 21: Edit 54-slot layout
            ItemStack editItem = new ItemStack(Material.ANVIL);
            ItemMeta eMeta = editItem.getItemMeta();
            if (eMeta != null) {
                eMeta.displayName(ColorUtil.parse("&a✦ " + ColorUtil.toSmallCaps("edit layout")).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7Open 54-slot drag-and-drop editor.").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&eClick to edit layout!").decoration(TextDecoration.ITALIC, false));
                eMeta.lore(lore);
                editItem.setItemMeta(eMeta);
            }
            gui.setButton(21, editItem, e -> {
                e.setCancelled(true);
                new EventEditorGUI(plugin, event).open(player);
            });

            // Slot 23: Quick Admin Helper
            ItemStack adminItem = new ItemStack(Material.COMPARATOR);
            ItemMeta aMeta = adminItem.getItemMeta();
            if (aMeta != null) {
                aMeta.displayName(ColorUtil.parse("&d✦ " + ColorUtil.toSmallCaps("admin helper")).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7Set Event Item: &e/el setitem " + event.getId() + " [amt]").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Add Spin Reward: &e/el spin add " + event.getId() + " [weight]").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Set Spin Cost: &e/el spincost " + event.getId() + " <shards>").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Add Shop Item: &e/el shop add " + event.getId() + " <price>").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Set Model: &e/el model " + event.getId() + " <string>").decoration(TextDecoration.ITALIC, false));
                aMeta.lore(lore);
                adminItem.setItemMeta(aMeta);
            }
            gui.setButton(23, adminItem, e -> e.setCancelled(true));
        }

        gui.open(player);
    }
}
