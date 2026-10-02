package org.khmc.eventslib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

public class EventEditorGUI implements InventoryHolder {

    private final EventsLibPlugin plugin;
    private final EventModel event;
    private final Inventory inventory;

    public EventEditorGUI(EventsLibPlugin plugin, EventModel event) {
        this.plugin = plugin;
        this.event = event;
        String titleStr = "&f" + ColorUtil.toSmallCaps("server events - " + event.getId());
        this.inventory = Bukkit.createInventory(this, EventModel.TOTAL_SLOTS, ColorUtil.parse(titleStr));

        // Populate existing items
        ItemStack[] slots = event.getSlots();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && !slots[i].getType().isAir()) {
                this.inventory.setItem(i, slots[i].clone());
            }
        }
    }

    public EventModel getEvent() {
        return event;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void open(Player player) {
        if (player == null) return;
        player.openInventory(this.inventory);
        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                plugin.getConfig().getString("messages.editor-opened", "&7Opening 54-slot editor. Place your items and close the inventory to save.")));
    }

    public void handleClose(Player player) {
        if (player == null) return;

        // Extract all 54 slots
        for (int i = 0; i < EventModel.TOTAL_SLOTS; i++) {
            ItemStack is = inventory.getItem(i);
            event.setItem(i, (is != null && !is.getType().isAir()) ? is.clone() : null);
        }

        plugin.getEventManager().saveEventAsync(event);

        String msg = plugin.getConfig().getString("messages.event-saved", "&aSaved 54-slot layout for event &e{id}&a.")
                .replace("{id}", event.getId());
        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") + msg));
        SoundUtil.playSuccess(player);
    }
}
