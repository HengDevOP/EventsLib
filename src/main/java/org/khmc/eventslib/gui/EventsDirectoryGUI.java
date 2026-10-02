package org.khmc.eventslib.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.gui.framework.MarketGUI;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.model.EventPrivacy;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.ModelUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

public class EventsDirectoryGUI {

    private final EventsLibPlugin plugin;

    public EventsDirectoryGUI(EventsLibPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player == null) return;

        boolean isAdmin = player.isOp() || player.hasPermission("eventslib.admin");
        List<EventModel> eventList = isAdmin ?
                new ArrayList<>(plugin.getEventManager().getAllEvents()) :
                new ArrayList<>(plugin.getEventManager().getPublicEvents());

        MarketGUI gui = new MarketGUI("&f" + ColorUtil.toSmallCaps("server events"), 6);

        ItemStack borderGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bMeta = borderGlass.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.empty());
            borderGlass.setItemMeta(bMeta);
        }

        // Fill bottom row with dark border
        for (int i = 45; i < 54; i++) {
            gui.setButton(i, borderGlass, e -> e.setCancelled(true));
        }

        // Info button at slot 49
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(ColorUtil.parse("&e✦ " + ColorUtil.toSmallCaps("events management")).decoration(TextDecoration.ITALIC, false));
            List<Component> iLore = new ArrayList<>();
            iLore.add(ColorUtil.parse("&7Total Events: &e" + eventList.size()).decoration(TextDecoration.ITALIC, false));
            if (isAdmin) {
                iLore.add(ColorUtil.parse("&7Create new: &e/el create <id>").decoration(TextDecoration.ITALIC, false));
                iLore.add(ColorUtil.parse("&7Set privacy: &e/el privacy <id> <private/public>").decoration(TextDecoration.ITALIC, false));
                iLore.add(ColorUtil.parse("&7Set model: &e/el model <id> <string>").decoration(TextDecoration.ITALIC, false));
            }
            infoMeta.lore(iLore);
            infoItem.setItemMeta(infoMeta);
        }
        gui.setButton(49, infoItem, e -> e.setCancelled(true));

        if (eventList.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta eMeta = emptyItem.getItemMeta();
            if (eMeta != null) {
                eMeta.displayName(ColorUtil.parse("&c" + ColorUtil.toSmallCaps("no events available")).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7There are currently no events to display.").decoration(TextDecoration.ITALIC, false));
                if (isAdmin) {
                    lore.add(ColorUtil.parse("&7Use &e/el create <id> &7to create your first event!").decoration(TextDecoration.ITALIC, false));
                }
                eMeta.lore(lore);
                emptyItem.setItemMeta(eMeta);
            }
            gui.setButton(22, emptyItem, e -> e.setCancelled(true));
            gui.open(player);
            return;
        }

        int slot = 0;
        for (EventModel ev : eventList) {
            if (slot >= 45) break;

            ItemStack displayStack = new ItemStack(Material.PAPER);
            ItemMeta meta = displayStack.getItemMeta();

            if (meta != null) {
                String cmdStr = ev.getCustomModelData();
                if (cmdStr.isEmpty() || cmdStr.equals("0")) {
                    String configKey = ev.getPrivacy().isPublic() ? "icon.public-custom-model-data" : "icon.private-custom-model-data";
                    cmdStr = plugin.getConfig().getString(configKey, plugin.getConfig().getString("icon.default-custom-model-data", ""));
                }

                if (cmdStr != null && !cmdStr.isEmpty() && !cmdStr.equals("0")) {
                    ModelUtil.applyModel(meta, cmdStr, plugin);
                }

                meta.displayName(ColorUtil.parse("&e✦ &d" + ev.getId()).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7Title: " + ev.getTitle()).decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Privacy: " + ev.getPrivacy().getFormattedDisplay()).decoration(TextDecoration.ITALIC, false));
                if (ev.hasCustomModelData()) {
                    lore.add(ColorUtil.parse("&7Model Data: &b" + ev.getCustomModelData()).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(ColorUtil.parse("&7Items: &f" + ev.getOccupiedSlotsCount() + "/54").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Shop Items: &e" + ev.getShopItems().size()).decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Spin Rewards: &b" + ev.getSpinRewards().size() + " &8(&d" + Math.round(ev.getSpinCost()) + " ★&8)").decoration(TextDecoration.ITALIC, false));
                lore.add(ColorUtil.parse("&7Creator: &f" + ev.getCreatorName()).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());

                if (isAdmin) {
                    lore.add(ColorUtil.parse("&eLeft-Click: &7Open Event Hub").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&aRight-Click: &7Edit 54-slot layout").decoration(TextDecoration.ITALIC, false));
                    lore.add(ColorUtil.parse("&bShift-Click: &7Toggle Privacy (&cPrivate&7/&aPublic&7)").decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(ColorUtil.parse("&eClick to open event").decoration(TextDecoration.ITALIC, false));
                }

                meta.lore(lore);
                meta.addItemFlags(ItemFlag.values());
                displayStack.setItemMeta(meta);
            }

            final EventModel targetEvent = ev;
            gui.setButton(slot, displayStack, e -> {
                e.setCancelled(true);
                SoundUtil.playClick(player);

                if (isAdmin) {
                    if (e.isShiftClick()) {
                        // Toggle privacy
                        EventPrivacy nextPrivacy = targetEvent.getPrivacy().isPrivate() ? EventPrivacy.PUBLIC : EventPrivacy.PRIVATE;
                        plugin.getEventManager().setEventPrivacy(targetEvent.getId(), nextPrivacy);
                        SoundUtil.playToggle(player);
                        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                                "&aEvent &e" + targetEvent.getId() + " &aprivacy updated to " + nextPrivacy.getFormattedDisplay() + "&a."));
                        open(player);
                    } else if (e.isRightClick()) {
                        // Open 54-slot editor
                        new EventEditorGUI(plugin, targetEvent).open(player);
                    } else {
                        // Open event hub menu
                        new EventHubGUI(plugin, targetEvent).open(player);
                    }
                } else {
                    new EventHubGUI(plugin, targetEvent).open(player);
                }
            });

            slot++;
        }

        gui.open(player);
    }
}
