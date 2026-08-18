package eu.fakemoon.altarkits.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A kit definition. Content slots use player-inventory indices:
 * 0-8 hotbar, 9-35 storage, 36-39 armor (boots, leggings, chestplate, helmet), 40 offhand.
 */
public final class Kit {

    public static final int CONTENT_SLOTS = 41;

    private final String name;
    private String displayName;
    private ItemStack icon = new ItemStack(Material.CHEST);
    private long cooldownSeconds;
    private String permission = "";
    private int order;
    private int price;
    private Map<Integer, ItemStack> contents = new HashMap<>();

    public Kit(String name) {
        this.name = name;
        this.displayName = name;
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack icon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public long cooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public String permission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null ? "" : permission;
    }

    public int order() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /** Coin price; 0 = not sold in the coin shop (free / permission-gated). */
    public int price() {
        return price;
    }

    public void setPrice(int price) {
        this.price = Math.max(0, price);
    }

    public boolean isBuyable() {
        return price > 0;
    }

    /** Default contents/layout, keyed by player-inventory slot. */
    public Map<Integer, ItemStack> contents() {
        return contents;
    }

    public void setContents(Map<Integer, ItemStack> contents) {
        this.contents = contents;
    }

    public boolean hasAccess(Player player) {
        return permission.isEmpty() || player.hasPermission(permission);
    }
}
