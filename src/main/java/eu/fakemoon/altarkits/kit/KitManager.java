package eu.fakemoon.altarkits.kit;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.data.PlayerDataManager;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import eu.fakemoon.altarkits.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class KitManager {

    private final AltarKitsPlugin plugin;
    private final PlayerDataManager playerData;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private final AtomicReference<String> pendingSave = new AtomicReference<>();
    private final Object ioLock = new Object();

    public KitManager(AltarKitsPlugin plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    private File file() {
        return new File(plugin.getDataFolder(), "kits.yml");
    }

    public void load() {
        kits.clear();
        File file = file();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("kits");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) continue;
            Kit kit = new Kit(name.toLowerCase(Locale.ROOT));
            kit.setDisplayName(section.getString("display-name", name));
            ItemStack icon = Items.fromBase64(section.getString("icon"));
            if (icon != null) kit.setIcon(icon);
            kit.setCooldownSeconds(section.getLong("cooldown", 0));
            kit.setPermission(section.getString("permission", ""));
            kit.setOrder(section.getInt("order", kits.size()));
            kit.setPrice(section.getInt("price", 0));
            Map<Integer, ItemStack> contents = new HashMap<>();
            ConfigurationSection contentsSection = section.getConfigurationSection("contents");
            if (contentsSection != null) {
                for (String key : contentsSection.getKeys(false)) {
                    ItemStack item = Items.fromBase64(contentsSection.getString(key));
                    int slot;
                    try {
                        slot = Integer.parseInt(key);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    if (item != null && slot >= 0 && slot < Kit.CONTENT_SLOTS) contents.put(slot, item);
                }
            }
            kit.setContents(contents);
            kits.put(kit.name(), kit);
        }
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String path = "kits." + kit.name() + ".";
            yaml.set(path + "display-name", kit.displayName());
            yaml.set(path + "icon", Items.toBase64(kit.icon()));
            yaml.set(path + "cooldown", kit.cooldownSeconds());
            yaml.set(path + "permission", kit.permission());
            yaml.set(path + "order", kit.order());
            yaml.set(path + "price", kit.price());
            for (Map.Entry<Integer, ItemStack> entry : kit.contents().entrySet()) {
                yaml.set(path + "contents." + entry.getKey(), Items.toBase64(entry.getValue()));
            }
        }
        // Serialize on the main thread, write async — sync writes can stall the
        // server when the folder is being synced (e.g. OneDrive).
        pendingSave.set(yaml.saveToString());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String data = pendingSave.getAndSet(null);
            if (data == null) return;
            synchronized (ioLock) {
                write(data);
            }
        });
    }

    private void write(String data) {
        try {
            Files.writeString(file().toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save kits.yml: " + ex.getMessage());
        }
    }

    /** Writes a still-queued save, synchronously — called on plugin disable. */
    public void flushSync() {
        String data = pendingSave.getAndSet(null);
        if (data == null) return;
        synchronized (ioLock) {
            write(data);
        }
    }

    public Kit get(String name) {
        return kits.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Kit> all() {
        return kits.values();
    }

    /** Kits in GUI display order. */
    public List<Kit> sorted() {
        return kits.values().stream().sorted(Comparator.comparingInt(Kit::order)).toList();
    }

    /** Creates a kit from the creator's current inventory (hotbar, storage, armor, offhand). */
    public Kit create(String name, Player from) {
        Kit kit = new Kit(name);
        kit.setDisplayName(Text.capitalize(name));
        Map<Integer, ItemStack> contents = new HashMap<>();
        PlayerInventory inv = from.getInventory();
        for (int slot = 0; slot < Kit.CONTENT_SLOTS; slot++) {
            ItemStack item = inv.getItem(slot);
            if (!Items.isEmpty(item)) contents.put(slot, item.clone());
        }
        kit.setContents(contents);
        for (int slot = 0; slot < Kit.CONTENT_SLOTS; slot++) {
            ItemStack item = contents.get(slot);
            if (item != null) {
                ItemStack icon = item.clone();
                icon.setAmount(1);
                kit.setIcon(icon);
                break;
            }
        }
        kit.setOrder(kits.values().stream().mapToInt(Kit::order).max().orElse(-1) + 1);
        kits.put(kit.name(), kit);
        saveAll();
        return kit;
    }

    public void delete(Kit kit) {
        kits.remove(kit.name());
        saveAll();
    }

    /** Registers a pre-built kit (used by the Unstable character-kit generator). */
    public void define(Kit kit) {
        kits.put(kit.name(), kit);
    }

    public long remainingCooldown(Player player, Kit kit) {
        if (player.hasPermission("kits.cooldown.bypass")) return 0;
        return playerData.cooldownExpiry(player.getUniqueId(), kit.name()) - System.currentTimeMillis();
    }

    /**
     * Whether the player may claim this kit. A buyable kit (price &gt; 0) is locked
     * until purchased, unless the player holds its permission node.
     */
    public boolean hasAccess(Player player, Kit kit) {
        if (playerData.hasPurchased(player.getUniqueId(), kit.name())) return true;
        if (kit.isBuyable()) {
            return !kit.permission().isEmpty() && player.hasPermission(kit.permission());
        }
        return kit.hasAccess(player);
    }

    /** Buyable kits (price &gt; 0), in GUI order — used by the coin shop. */
    public List<Kit> buyable() {
        return sorted().stream().filter(Kit::isBuyable).toList();
    }

    /** Claims a kit with permission + cooldown checks; messages the player. */
    public boolean claim(Player player, Kit kit) {
        if (!hasAccess(player, kit)) {
            Messages.send(player, "messages.no-access", "kit", kit.displayName());
            return false;
        }
        long remaining = remainingCooldown(player, kit);
        if (remaining > 0) {
            Messages.send(player, "messages.on-cooldown",
                    "kit", kit.displayName(), "time", Text.duration((remaining + 999) / 1000));
            return false;
        }
        Map<Integer, ItemStack> layout = resolveLayout(player, kit);
        if (!wouldFit(player, layout)) {
            Messages.send(player, "messages.inventory-full", "kit", kit.displayName());
            return false;
        }
        apply(player, layout);
        if (kit.cooldownSeconds() > 0) {
            playerData.setCooldownExpiry(player.getUniqueId(), kit.name(),
                    System.currentTimeMillis() + kit.cooldownSeconds() * 1000L);
        }
        Messages.send(player, "messages.claimed", "kit", kit.displayName());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        return true;
    }

    /** Gives a kit ignoring permission and cooldown (admin /kit give). */
    public void give(Player target, Kit kit) {
        apply(target, resolveLayout(target, kit));
        Messages.send(target, "messages.received", "kit", kit.displayName());
    }

    /**
     * The layout to place items with: the player's saved layout if it still contains
     * exactly the kit's items, otherwise the kit's default. Stale layouts (kit was
     * edited since) silently fall back to the default.
     */
    public Map<Integer, ItemStack> resolveLayout(Player player, Kit kit) {
        Map<Integer, ItemStack> saved = playerData.layout(player.getUniqueId(), kit.name());
        if (saved == null) return kit.contents();
        if (Items.sameItems(saved.values(), kit.contents().values())) return saved;
        // The kit's items changed since this layout was saved — drop the stale layout.
        playerData.clearLayout(player.getUniqueId(), kit.name());
        return kit.contents();
    }

    /**
     * Whether the kit fits entirely without anything dropping on the ground —
     * a dry run of {@link #apply} against a copy of the inventory. Mirrors its
     * logic: direct placement into empty content slots, then overflow via the
     * storage slots (0-35, what {@link PlayerInventory#addItem} uses).
     */
    private boolean wouldFit(Player player, Map<Integer, ItemStack> layout) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] sim = new ItemStack[Kit.CONTENT_SLOTS];
        for (int slot = 0; slot < Kit.CONTENT_SLOTS; slot++) {
            ItemStack current = inv.getItem(slot);
            sim[slot] = Items.isEmpty(current) ? null : current.clone();
        }
        List<ItemStack> leftover = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= Kit.CONTENT_SLOTS) continue;
            ItemStack item = entry.getValue().clone();
            if (sim[slot] == null) {
                sim[slot] = item;
            } else {
                leftover.add(item);
            }
        }
        for (ItemStack item : leftover) {
            int remaining = item.getAmount();
            int max = item.getMaxStackSize();
            for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
                ItemStack s = sim[slot];
                if (s == null || !s.isSimilar(item)) continue;
                int moved = Math.min(max - s.getAmount(), remaining);
                if (moved > 0) {
                    s.setAmount(s.getAmount() + moved);
                    remaining -= moved;
                }
            }
            for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
                if (sim[slot] != null) continue;
                int moved = Math.min(max, remaining);
                ItemStack placed = item.clone();
                placed.setAmount(moved);
                sim[slot] = placed;
                remaining -= moved;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private void apply(Player player, Map<Integer, ItemStack> layout) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> leftover = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= Kit.CONTENT_SLOTS) continue;
            ItemStack item = entry.getValue().clone();
            if (Items.isEmpty(inv.getItem(slot))) {
                inv.setItem(slot, item);
            } else {
                leftover.add(item);
            }
        }
        for (ItemStack item : leftover) {
            for (ItemStack overflow : inv.addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
    }
}
