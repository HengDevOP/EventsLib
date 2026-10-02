package org.khmc.eventslib.manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.khmc.eventslib.database.EventDatabaseManager;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.model.EventPrivacy;
import org.khmc.eventslib.model.EventShopItem;
import org.khmc.eventslib.util.SchedulerUtil;
import org.khmc.eventslib.util.TimeResetUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    private final JavaPlugin plugin;
    private final EventDatabaseManager databaseManager;
    private final Map<String, EventModel> events = new ConcurrentHashMap<>();

    public EventManager(JavaPlugin plugin, EventDatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        loadAll();
    }

    public void loadAll() {
        events.clear();
        Map<String, EventModel> loaded = databaseManager.loadAllEvents();
        events.putAll(loaded);
        plugin.getLogger().info("Loaded " + events.size() + " event(s) from SQLite database.");
    }

    public boolean hasEvent(String id) {
        if (id == null) return false;
        return events.containsKey(id.trim().toLowerCase(Locale.ROOT));
    }

    public EventModel getEvent(String id) {
        if (id == null) return null;
        return events.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public EventModel createEvent(String rawId, Player creator) {
        if (rawId == null || rawId.trim().isEmpty()) return null;
        String cleanId = rawId.trim().toLowerCase(Locale.ROOT);
        if (events.containsKey(cleanId)) return null;

        UUID creatorUuid = creator != null ? creator.getUniqueId() : null;
        String creatorName = creator != null ? creator.getName() : "Console";
        long now = System.currentTimeMillis();

        EventModel event = new EventModel(cleanId, "&eEvent: &d" + rawId.trim(), EventPrivacy.PRIVATE, creatorUuid, creatorName, now, now);
        events.put(cleanId, event);

        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.saveEvent(event));
        return event;
    }

    public boolean setEventPrivacy(String id, EventPrivacy privacy) {
        EventModel event = getEvent(id);
        if (event == null || privacy == null) return false;

        event.setPrivacy(privacy);
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.updateEventPrivacy(event.getId(), privacy));
        return true;
    }

    public boolean setEventCustomModelData(String id, String customModelData) {
        EventModel event = getEvent(id);
        if (event == null) return false;

        String clean = customModelData != null ? customModelData.trim() : "";
        if (clean.equalsIgnoreCase("reset") || clean.equalsIgnoreCase("none")) {
            clean = "";
        }

        event.setCustomModelData(clean);
        String finalClean = clean;
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.updateEventCustomModelData(event.getId(), finalClean));
        return true;
    }

    public boolean setEventItem(String id, ItemStack item, int dailyAmount) {
        EventModel event = getEvent(id);
        if (event == null) return false;

        ItemStack toSet = (item != null && !item.getType().isAir()) ? item.clone() : null;
        if (toSet != null) {
            toSet.setAmount(1);
        }
        event.setEventItem(toSet, dailyAmount);
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.saveEventItem(event.getId(), toSet, dailyAmount));
        return true;
    }

    public boolean addShopItem(String id, int price, ItemStack item) {
        EventModel event = getEvent(id);
        if (event == null || item == null || item.getType().isAir()) return false;

        ItemStack toAdd = item.clone();
        int newId = databaseManager.addShopItem(event.getId(), price, toAdd);
        if (newId > 0) {
            event.addShopItem(new EventShopItem(newId, event.getId(), price, toAdd, System.currentTimeMillis()));
            return true;
        }
        return false;
    }

    public boolean removeShopItem(String id, int shopItemId) {
        EventModel event = getEvent(id);
        if (event == null) return false;

        boolean removed = event.removeShopItem(shopItemId);
        if (removed) {
            SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.deleteShopItem(shopItemId));
        }
        return removed;
    }

    public boolean setEventSpinCost(String id, double spinCost) {
        EventModel event = getEvent(id);
        if (event == null) return false;

        event.setSpinCost(spinCost);
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.updateEventSpinCost(event.getId(), spinCost));
        return true;
    }

    public boolean addSpinReward(String id, int weight, ItemStack item) {
        EventModel event = getEvent(id);
        if (event == null || item == null || item.getType().isAir()) return false;

        ItemStack toAdd = item.clone();
        int newId = databaseManager.addSpinReward(event.getId(), weight, toAdd);
        if (newId > 0) {
            event.addSpinReward(new org.khmc.eventslib.model.EventSpinReward(newId, event.getId(), weight, toAdd, System.currentTimeMillis()));
            return true;
        }
        return false;
    }

    public boolean removeSpinReward(String id, int rewardId) {
        EventModel event = getEvent(id);
        if (event == null) return false;

        boolean removed = event.removeSpinReward(rewardId);
        if (removed) {
            SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.deleteSpinReward(rewardId));
        }
        return removed;
    }

    public boolean hasClaimedToday(UUID playerUuid, String eventId) {
        if (playerUuid == null || eventId == null) return false;
        String cycleKey = TimeResetUtil.getCurrentCycleKey();
        return databaseManager.hasClaimedDaily(playerUuid, eventId.toLowerCase(Locale.ROOT), cycleKey);
    }

    public boolean claimDailyReward(Player player, EventModel event) {
        if (player == null || event == null) return false;
        if (event.getEventItem() == null) return false;

        String cycleKey = TimeResetUtil.getCurrentCycleKey();
        if (databaseManager.hasClaimedDaily(player.getUniqueId(), event.getId(), cycleKey)) {
            return false;
        }

        boolean recorded = databaseManager.recordDailyClaim(player.getUniqueId(), event.getId(), cycleKey);
        if (!recorded) {
            return false;
        }

        ItemStack claimStack = event.getEventItem().clone();
        claimStack.setAmount(event.getDailyClaimAmount());

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(claimStack);
        if (!leftover.isEmpty()) {
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
        return true;
    }

    public int countPlayerEventItems(Player player, EventModel event) {
        if (player == null || event == null || event.getEventItem() == null) return 0;
        int count = 0;
        ItemStack template = event.getEventItem();
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.isSimilar(template)) {
                count += is.getAmount();
            }
        }
        return count;
    }

    public boolean deductEventItems(Player player, EventModel event, int amountToRemove) {
        if (player == null || event == null || event.getEventItem() == null || amountToRemove <= 0) {
            return false;
        }
        if (countPlayerEventItems(player, event) < amountToRemove) {
            return false;
        }

        int remaining = amountToRemove;
        ItemStack template = event.getEventItem();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.isSimilar(template)) {
                if (is.getAmount() <= remaining) {
                    remaining -= is.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - remaining);
                    remaining = 0;
                    break;
                }
                if (remaining <= 0) break;
            }
        }
        player.updateInventory();
        return true;
    }

    public void saveEventAsync(EventModel event) {
        if (event == null) return;
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.saveEvent(event));
    }

    public void saveEventSync(EventModel event) {
        if (event == null) return;
        databaseManager.saveEvent(event);
    }

    public boolean deleteEvent(String id) {
        if (id == null) return false;
        String cleanId = id.trim().toLowerCase(Locale.ROOT);
        EventModel removed = events.remove(cleanId);
        if (removed != null) {
            SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.deleteEvent(cleanId));
            return true;
        }
        return false;
    }

    public Collection<EventModel> getAllEvents() {
        return Collections.unmodifiableCollection(events.values());
    }

    public List<EventModel> getPublicEvents() {
        List<EventModel> list = new ArrayList<>();
        for (EventModel e : events.values()) {
            if (e.getPrivacy().isPublic()) {
                list.add(e);
            }
        }
        return list;
    }

    public boolean canAccess(Player player, EventModel event) {
        if (event == null) return false;
        if (player == null) return true;
        if (player.isOp() || player.hasPermission("eventslib.admin")) {
            return true;
        }
        return event.getPrivacy().isPublic();
    }
}
