package eu.fakemoon.altarkits.kit;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.data.PlayerDataManager;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import eu.fakemoon.altarkits.util.Text;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class KitManager {

    private final AltarKitsPlugin plugin;
    private final PlayerDataManager playerData;
    /** Concurrent because on Folia admin commands and player claims run on different threads. */
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final AtomicReference<List<Kit>> pendingSnapshot = new AtomicReference<>();
    private final AtomicBoolean writeRunning = new AtomicBoolean();
    private volatile boolean stopped;
    private final Object ioLock = new Object();

    public KitManager(AltarKitsPlugin plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    private File file() {
        return new File(plugin.getDataFolder(), "kits.yml");
    }

    public synchronized void load() {
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

    /**
     * Snapshots every kit (cheap) here, then serializes and writes on the async
     * scheduler. The snapshot is what keeps the YAML/base64 work — by far the most
     * expensive part — off the caller's thread, which on Folia is a region thread.
     * A burst of edits (dragging kits around the arranger) collapses into one write.
     */
    public synchronized void saveAll() {
        List<Kit> snapshot = new ArrayList<>(kits.size());
        for (Kit kit : kits.values()) snapshot.add(kit.copy());
        pendingSnapshot.set(snapshot);
        scheduleWrite();
    }

    /** Arms a single background write; a write already in flight picks the new snapshot up. */
    private void scheduleWrite() {
        if (!writeRunning.compareAndSet(false, true)) return;
        plugin.async(() -> {
            try {
                List<Kit> snapshot = pendingSnapshot.getAndSet(null);
                if (snapshot == null) return;
                String data = serialize(snapshot);
                synchronized (ioLock) {
                    // If the plugin started disabling, flushSync owns the final write —
                    // letting this one land after it would revert kits.yml to an older state.
                    if (stopped) return;
                    write(data);
                }
            } finally {
                writeRunning.set(false);
                if (!stopped && pendingSnapshot.get() != null) scheduleWrite();
            }
        });
    }

    private static String serialize(List<Kit> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Kit kit : snapshot) {
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
        return yaml.saveToString();
    }

    private void write(String data) {
        try {
            Files.writeString(file().toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save kits.yml: " + ex.getMessage());
        }
    }

    /** Writes a still-queued snapshot, synchronously — called on plugin disable. */
    public void flushSync() {
        stopped = true;
        List<Kit> snapshot = pendingSnapshot.getAndSet(null);
        if (snapshot == null) return;
        synchronized (ioLock) {
            write(serialize(snapshot));
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
    public synchronized Kit create(String name, Player from) {
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

    public synchronized void delete(Kit kit) {
        kits.remove(kit.name());
        saveAll();
    }

    /** Registers a pre-built kit (used by the Unstable character-kit generator). */
    public synchronized void define(Kit kit) {
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

    /**
     * Claims a kit with permission + cooldown checks; messages the player.
     * Must run on the player's own thread (see {@link #give}, which defers for you).
     */
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

    /**
     * Gives a kit ignoring permission and cooldown (admin /kit give).
     * Safe from any thread: the inventory write is deferred to the player's thread,
     * which on Folia may be a different region than the caller's.
     */
    public void give(Player target, Kit kit) {
        plugin.sync(target, () -> {
            apply(target, resolveLayout(target, kit));
            Messages.send(target, "messages.received", "kit", kit.displayName());
        });
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
     * Whether the kit fits entirely without anything dropping on the ground — a dry run
     * of {@link #apply} against a copy of the inventory. Both go through
     * {@link #place} so the check can never be more permissive than the placement
     * (a drift there would let a claim dump items on the ground for anyone to grab).
     */
    private boolean wouldFit(Player player, Map<Integer, ItemStack> layout) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] sim = new ItemStack[Kit.CONTENT_SLOTS];
        for (int slot = 0; slot < Kit.CONTENT_SLOTS; slot++) {
            ItemStack current = inv.getItem(slot);
            sim[slot] = Items.isEmpty(current) ? null : current.clone();
        }
        return place(new ArraySlots(sim), layout).isEmpty();
    }

    /** Places a resolved layout. Must run on the player's own thread. */
    private void apply(Player player, Map<Integer, ItemStack> layout) {
        for (ItemStack left : place(new InventorySlots(player.getInventory()), layout)) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    /**
     * Places a layout into the given slots and returns whatever could not be stored:
     * direct placement into empty content slots, then overflow into storage (0-35,
     * what {@link PlayerInventory#addItem} uses).
     */
    private static List<ItemStack> place(Slots slots, Map<Integer, ItemStack> layout) {
        List<ItemStack> displaced = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= Kit.CONTENT_SLOTS) continue;
            ItemStack item = entry.getValue().clone();
            if (Items.isEmpty(slots.get(slot))) {
                slots.set(slot, item);
            } else {
                displaced.add(item);
            }
        }
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack item : displaced) {
            overflow.addAll(slots.addAll(item));
        }
        return overflow;
    }

    /** Slot access, so the fit-check can run against a copy without touching the real inventory. */
    private interface Slots {
        ItemStack get(int slot);

        void set(int slot, ItemStack item);

        /** Adds the way {@link PlayerInventory#addItem} does, returning what did not fit. */
        Collection<ItemStack> addAll(ItemStack item);
    }

    /** Backing store for the dry run: a plain array copy of the player's inventory. */
    private record ArraySlots(ItemStack[] slots) implements Slots {

        @Override
        public ItemStack get(int slot) {
            return slots[slot];
        }

        @Override
        public void set(int slot, ItemStack item) {
            slots[slot] = item;
        }

        @Override
        public Collection<ItemStack> addAll(ItemStack item) {
            List<ItemStack> left = new ArrayList<>();
            int remaining = item.getAmount();
            int max = item.getMaxStackSize();
            for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
                ItemStack stack = slots[slot];
                if (stack == null || !stack.isSimilar(item)) continue;
                int moved = Math.min(max - stack.getAmount(), remaining);
                if (moved > 0) {
                    stack.setAmount(stack.getAmount() + moved);
                    remaining -= moved;
                }
            }
            for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
                if (slots[slot] != null) continue;
                int moved = Math.min(max, remaining);
                ItemStack placed = item.clone();
                placed.setAmount(moved);
                slots[slot] = placed;
                remaining -= moved;
            }
            if (remaining > 0) left.add(item);
            return left;
        }
    }

    /** Backing store for the real thing. */
    private record InventorySlots(PlayerInventory inv) implements Slots {

        @Override
        public ItemStack get(int slot) {
            return inv.getItem(slot);
        }

        @Override
        public void set(int slot, ItemStack item) {
            inv.setItem(slot, item);
        }

        @Override
        public Collection<ItemStack> addAll(ItemStack item) {
            return inv.addItem(item).values();
        }
    }
}
