package org.khmc.eventslib.model;

import org.bukkit.inventory.ItemStack;

public class EventSpinReward {

    private int id;
    private final String eventId;
    private int weight;
    private final ItemStack item;
    private final long createdAt;

    public EventSpinReward(int id, String eventId, int weight, ItemStack item, long createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.weight = Math.max(1, weight);
        this.item = item != null ? item.clone() : null;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public int getWeight() {
        return Math.max(1, weight);
    }

    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
    }

    public ItemStack getItem() {
        return item != null ? item.clone() : null;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public double getChancePercent(int totalWeight) {
        if (totalWeight <= 0) return 0.0;
        return (weight * 100.0) / totalWeight;
    }
}
