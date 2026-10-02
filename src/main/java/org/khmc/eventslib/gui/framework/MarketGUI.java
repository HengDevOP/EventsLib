package org.khmc.eventslib.gui.framework;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.khmc.eventslib.util.ColorUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MarketGUI implements InventoryHolder {
    private final String titleLegacy;
    private final Component title;
    private final int rows;
    private final Map<Integer, GUIButton> buttons = new HashMap<>();
    private final Inventory inventory;
    private Consumer<InventoryCloseEvent> closeAction;

    public MarketGUI(String title, int rows) {
        this.titleLegacy = title;
        this.title = ColorUtil.parse(title);
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, this.title);
    }

    public void setButton(int slot, GUIButton button) {
        if (slot >= 0 && slot < this.rows * 9) {
            this.buttons.put(slot, button);
            this.inventory.setItem(slot, button != null ? button.getItemStack() : null);
        }
    }

    public void setButton(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        this.setButton(slot, new GUIButton(item, action));
    }

    public GUIButton getButton(int slot) {
        return this.buttons.get(slot);
    }

    public void fillVacant(ItemStack fillerItem) {
        for (int i = 0; i < this.rows * 9; ++i) {
            if (this.buttons.containsKey(i)) continue;
            this.setButton(i, new GUIButton(fillerItem, e -> e.setCancelled(true)));
        }
    }

    public void clear() {
        this.buttons.clear();
        this.inventory.clear();
    }

    public void setCloseAction(Consumer<InventoryCloseEvent> closeAction) {
        this.closeAction = closeAction;
    }

    public Consumer<InventoryCloseEvent> getCloseAction() {
        return this.closeAction;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void open(Player player) {
        if (player == null) return;
        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() == this.inventory) {
            return;
        }
        player.openInventory(this.inventory);
    }

    public int getRows() {
        return this.rows;
    }

    public String getTitleLegacy() {
        return this.titleLegacy;
    }
}
