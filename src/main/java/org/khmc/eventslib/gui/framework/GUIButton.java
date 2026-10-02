package org.khmc.eventslib.gui.framework;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class GUIButton {
    private ItemStack itemStack;
    private Consumer<InventoryClickEvent> clickAction;

    public GUIButton(ItemStack itemStack, Consumer<InventoryClickEvent> clickAction) {
        this.itemStack = itemStack;
        this.clickAction = clickAction;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public Consumer<InventoryClickEvent> getClickAction() {
        return this.clickAction;
    }

    public void setClickAction(Consumer<InventoryClickEvent> clickAction) {
        this.clickAction = clickAction;
    }
}
