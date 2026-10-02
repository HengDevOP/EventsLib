package org.khmc.eventslib.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.gui.EventEditorGUI;
import org.khmc.eventslib.gui.framework.GUIButton;
import org.khmc.eventslib.gui.framework.MarketGUI;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SchedulerUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.function.Consumer;

public class EventListener implements Listener {

    private final EventsLibPlugin plugin;

    public EventListener(EventsLibPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Track daily login and notify if daily event rewards are ready to claim (Singapore 3:00 PM cycle)
        SchedulerUtil.runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            int unclaimedCount = 0;
            for (EventModel ev : plugin.getEventManager().getPublicEvents()) {
                if (ev.getEventItem() != null && !plugin.getEventManager().hasClaimedToday(player.getUniqueId(), ev.getId())) {
                    unclaimedCount++;
                }
            }

            if (unclaimedCount > 0) {
                String prefix = plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ");
                player.sendMessage(ColorUtil.parse(prefix + "&a✦ You have &e" + unclaimedCount +
                        " &adaily event reward(s) available to claim! Type &e/el &ato claim your items."));
                SoundUtil.playToggle(player);
            }
        }, 30L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;

        // 1. Static MarketGUI (Directory, Viewer, Hub, Shop)
        if (event.getInventory().getHolder() instanceof MarketGUI gui) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                int slot = event.getSlot();
                GUIButton button = gui.getButton(slot);
                if (button != null && button.getClickAction() != null) {
                    try {
                        button.getClickAction().accept(event);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Error executing EventsLib button action: " + t.getMessage());
                    }
                }
            }
            return;
        }

        // 2. EventEditorGUI: Allow interaction across slots so admins can place & arrange items!
        if (event.getInventory().getHolder() instanceof EventEditorGUI) {
            // Unrestricted - admins can edit freely!
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == null) return;

        if (event.getInventory().getHolder() instanceof MarketGUI gui) {
            Consumer<InventoryCloseEvent> closeAction = gui.getCloseAction();
            if (closeAction != null) {
                try {
                    closeAction.accept(event);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Error executing EventsLib close action: " + t.getMessage());
                }
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof EventEditorGUI editor) {
            if (event.getPlayer() instanceof Player player) {
                editor.handleClose(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() == null) return;

        if (event.getInventory().getHolder() instanceof MarketGUI) {
            event.setCancelled(true);
        }
    }
}
