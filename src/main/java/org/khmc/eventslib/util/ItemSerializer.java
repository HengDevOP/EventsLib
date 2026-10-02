package org.khmc.eventslib.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;

public final class ItemSerializer {

    private ItemSerializer() {}

    public static byte[] toBytes(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        try {
            return item.serializeAsBytes();
        } catch (Throwable t) {
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.set("item", item);
                return config.saveToString().getBytes(StandardCharsets.UTF_8);
            } catch (Throwable fallbackErr) {
                return null;
            }
        }
    }

    public static ItemStack fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Throwable t) {
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.loadFromString(new String(bytes, StandardCharsets.UTF_8));
                return config.getItemStack("item");
            } catch (Throwable fallbackErr) {
                return null;
            }
        }
    }
}
