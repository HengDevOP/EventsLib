package org.khmc.eventslib.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.gui.framework.MarketGUI;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

public class EventViewerGUI {

    private final EventsLibPlugin plugin;
    private final EventModel event;

    public EventViewerGUI(EventsLibPlugin plugin, EventModel event) {
        this.plugin = plugin;
        this.event = event;
    }

    public void open(Player player) {
        if (player == null || event == null) return;

        // Check privacy access
        if (!plugin.getEventManager().canAccess(player, event)) {
            String msg = plugin.getConfig().getString("messages.event-private", "&cThis event is currently private.");
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") + msg));
            SoundUtil.playError(player);
            return;
        }

        String title = "&f" + ColorUtil.toSmallCaps("server events - " + event.getId());
        MarketGUI gui = new MarketGUI(title, 6);

        ItemStack[] slots = event.getSlots();
        for (int i = 0; i < slots.length; i++) {
            ItemStack item = slots[i];
            if (item != null && !item.getType().isAir()) {
                gui.setButton(i, item.clone(), e -> e.setCancelled(true));
            }
        }

        gui.open(player);
    }
}
