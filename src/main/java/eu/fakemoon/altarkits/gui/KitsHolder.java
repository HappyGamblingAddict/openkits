package eu.fakemoon.altarkits.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/** Marker + dispatch interface for all AltarKits GUIs. */
public interface KitsHolder extends InventoryHolder {

    void click(InventoryClickEvent event);

    default void drag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    default void closed(InventoryCloseEvent event) {
    }
}
