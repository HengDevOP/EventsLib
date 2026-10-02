package org.khmc.eventslib.model;

import org.bukkit.inventory.ItemStack;

public class EventShopItem {

    private int id;
    private final String eventId;
    private int price;
    private final ItemStack item;
    private final long createdAt;

    public EventShopItem(int id, String eventId, int price, ItemStack item, long createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.price = Math.max(1, price);
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = Math.max(1, price);
    }

    public ItemStack getItem() {
        return item != null ? item.clone() : null;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
