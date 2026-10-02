package org.khmc.eventslib.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EventModel {

    public static final int TOTAL_SLOTS = 54;

    private String id;
    private String title;
    private EventPrivacy privacy;
    private UUID creatorUuid;
    private String creatorName;
    private long createdAt;
    private long updatedAt;
    private String customModelData;
    private final ItemStack[] slots;

    // Event Collectible Item & Daily Login Claim
    private ItemStack eventItem;
    private int dailyClaimAmount = 1;

    // Event Shop Items
    private final List<EventShopItem> shopItems = new ArrayList<>();

    // Event Spin System (integrated with Shards)
    private double spinCost = 10.0;
    private final List<EventSpinReward> spinRewards = new ArrayList<>();

    public EventModel(String id) {
        this(id, "&eEvent: &d" + id, EventPrivacy.PRIVATE, null, "Console", System.currentTimeMillis(), System.currentTimeMillis(), "");
    }

    public EventModel(String id, String title, EventPrivacy privacy, UUID creatorUuid, String creatorName, long createdAt, long updatedAt) {
        this(id, title, privacy, creatorUuid, creatorName, createdAt, updatedAt, "");
    }

    public EventModel(String id, String title, EventPrivacy privacy, UUID creatorUuid, String creatorName, long createdAt, long updatedAt, String customModelData) {
        this(id, title, privacy, creatorUuid, creatorName, createdAt, updatedAt, customModelData, 10.0);
    }

    public EventModel(String id, String title, EventPrivacy privacy, UUID creatorUuid, String creatorName, long createdAt, long updatedAt, String customModelData, double spinCost) {
        this.id = id;
        this.title = title != null ? title : "&eEvent: &d" + id;
        this.privacy = privacy != null ? privacy : EventPrivacy.PRIVATE;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName != null ? creatorName : "Unknown";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : System.currentTimeMillis();
        this.customModelData = customModelData != null ? customModelData.trim() : "";
        this.spinCost = Math.max(0.0, spinCost);
        this.slots = new ItemStack[TOTAL_SLOTS];
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "&eEvent: &d" + id;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }

    public EventPrivacy getPrivacy() {
        return privacy != null ? privacy : EventPrivacy.PRIVATE;
    }

    public void setPrivacy(EventPrivacy privacy) {
        this.privacy = privacy != null ? privacy : EventPrivacy.PRIVATE;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCustomModelData() {
        return customModelData != null ? customModelData : "";
    }

    public void setCustomModelData(String customModelData) {
        this.customModelData = customModelData != null ? customModelData.trim() : "";
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean hasCustomModelData() {
        return customModelData != null && !customModelData.isEmpty() && !customModelData.equals("0");
    }

    public int getCustomModelDataInt() {
        if (!hasCustomModelData()) return 0;
        try {
            return Integer.parseInt(customModelData);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public void setCreatorUuid(UUID creatorUuid) {
        this.creatorUuid = creatorUuid;
    }

    public String getCreatorName() {
        return creatorName != null ? creatorName : "Unknown";
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            return slots[slot];
        }
        return null;
    }

    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            slots[slot] = (item != null && !item.getType().isAir()) ? item.clone() : null;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public ItemStack[] getSlots() {
        return Arrays.copyOf(slots, TOTAL_SLOTS);
    }

    public void setSlots(ItemStack[] newSlots) {
        Arrays.fill(this.slots, null);
        if (newSlots != null) {
            for (int i = 0; i < Math.min(TOTAL_SLOTS, newSlots.length); i++) {
                if (newSlots[i] != null && !newSlots[i].getType().isAir()) {
                    this.slots[i] = newSlots[i].clone();
                }
            }
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public int getOccupiedSlotsCount() {
        int count = 0;
        for (ItemStack is : slots) {
            if (is != null && !is.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    // --- Event Item & Daily Claim ---
    public ItemStack getEventItem() {
        return eventItem != null ? eventItem.clone() : null;
    }

    public void setEventItem(ItemStack eventItem, int dailyClaimAmount) {
        this.eventItem = (eventItem != null && !eventItem.getType().isAir()) ? eventItem.clone() : null;
        this.dailyClaimAmount = Math.max(1, dailyClaimAmount);
        this.updatedAt = System.currentTimeMillis();
    }

    public int getDailyClaimAmount() {
        return Math.max(1, dailyClaimAmount);
    }

    public void setDailyClaimAmount(int dailyClaimAmount) {
        this.dailyClaimAmount = Math.max(1, dailyClaimAmount);
        this.updatedAt = System.currentTimeMillis();
    }

    // --- Event Shop Items ---
    public List<EventShopItem> getShopItems() {
        return Collections.unmodifiableList(shopItems);
    }

    public void setShopItems(List<EventShopItem> items) {
        this.shopItems.clear();
        if (items != null) {
            this.shopItems.addAll(items);
        }
    }

    public void addShopItem(EventShopItem item) {
        if (item != null) {
            this.shopItems.add(item);
        }
    }

    public boolean removeShopItem(int shopItemId) {
        return this.shopItems.removeIf(it -> it.getId() == shopItemId);
    }

    // --- Event Spin System ---
    public double getSpinCost() {
        return Math.max(0.0, spinCost);
    }

    public void setSpinCost(double spinCost) {
        this.spinCost = Math.max(0.0, spinCost);
        this.updatedAt = System.currentTimeMillis();
    }

    public List<EventSpinReward> getSpinRewards() {
        return Collections.unmodifiableList(spinRewards);
    }

    public void setSpinRewards(List<EventSpinReward> rewards) {
        this.spinRewards.clear();
        if (rewards != null) {
            this.spinRewards.addAll(rewards);
        }
    }

    public void addSpinReward(EventSpinReward reward) {
        if (reward != null) {
            this.spinRewards.add(reward);
        }
    }

    public boolean removeSpinReward(int rewardId) {
        return this.spinRewards.removeIf(r -> r.getId() == rewardId);
    }

    public int getTotalSpinWeight() {
        int total = 0;
        for (EventSpinReward r : spinRewards) {
            total += r.getWeight();
        }
        return total;
    }

    public EventSpinReward getRandomSpinReward() {
        if (spinRewards.isEmpty()) return null;
        int totalWeight = getTotalSpinWeight();
        if (totalWeight <= 0) {
            return spinRewards.get(ThreadLocalRandom.current().nextInt(spinRewards.size()));
        }

        int randomVal = ThreadLocalRandom.current().nextInt(totalWeight);
        int running = 0;
        for (EventSpinReward r : spinRewards) {
            running += r.getWeight();
            if (randomVal < running) {
                return r;
            }
        }
        return spinRewards.get(spinRewards.size() - 1);
    }
}
