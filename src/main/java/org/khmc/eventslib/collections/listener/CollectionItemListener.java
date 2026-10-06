package org.khmc.eventslib.collections.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.collections.manager.CollectionManager;
import org.khmc.eventslib.collections.model.PlayerCollectionProfile;
import org.khmc.eventslib.util.SchedulerUtil;

public class CollectionItemListener implements Listener {

    private final EventsLibPlugin plugin;
    private final CollectionManager manager;

    public CollectionItemListener(EventsLibPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCollectionManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manager.loadProfileAsync(player.getUniqueId(), profile -> {
            SchedulerUtil.runTask(plugin, () -> {
                if (player.isOnline()) {
                    manager.syncPlayerItems(player);
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.unloadProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerCollectionProfile profile = manager.getProfile(player);
        if (profile == null) return;

        ItemStack item = event.getItem().getItemStack();
        if (manager.syncItem(item, profile)) {
            event.getItem().setItemStack(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerCollectionProfile profile = manager.getProfile(player);
        if (profile == null) return;

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null && !item.getType().isAir()) {
            if (manager.syncItem(item, profile)) {
                player.getInventory().setItem(event.getNewSlot(), item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        PlayerCollectionProfile profile = manager.getProfile(player);
        if (profile == null) return;

        ItemStack main = event.getMainHandItem();
        if (main != null && !main.getType().isAir()) {
            manager.syncItem(main, profile);
        }

        ItemStack off = event.getOffHandItem();
        if (off != null && !off.getType().isAir()) {
            manager.syncItem(off, profile);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerCollectionProfile profile = manager.getProfile(player);
        if (profile == null) return;

        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            manager.syncItem(current, profile);
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            manager.syncItem(cursor, profile);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            SchedulerUtil.runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    manager.syncPlayerItems(player);
                }
            }, 1L);
        }
    }
}
