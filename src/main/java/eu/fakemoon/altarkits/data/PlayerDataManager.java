package eu.fakemoon.altarkits.data;

import eu.fakemoon.altarkits.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player cooldown expiries and custom kit layouts, stored in playerdata/&lt;uuid&gt;.yml. */
public final class PlayerDataManager implements Listener {

    private final JavaPlugin plugin;
    private final File dir;
    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();
    private final Map<UUID, String> pendingWrites = new ConcurrentHashMap<>();
    private final Object ioLock = new Object();

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "playerdata");
        this.dir.mkdirs();
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    private YamlConfiguration data(UUID id) {
        return cache.computeIfAbsent(id, key -> YamlConfiguration.loadConfiguration(file(key)));
    }

    /**
     * Serializes on the main thread (fast, in-memory) but writes to disk async —
     * a synchronous write can stall for seconds when the folder is being synced
     * (e.g. OneDrive), which froze the server long enough to trip the watchdog.
     */
    private void save(UUID id) {
        YamlConfiguration yaml = cache.get(id);
        if (yaml == null) return;
        pendingWrites.put(id, yaml.saveToString());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String data = pendingWrites.get(id);
            if (data == null) return; // an earlier task already wrote fresher data
            synchronized (ioLock) {
                write(id, data);
            }
            pendingWrites.remove(id, data); // keep it queued if newer data arrived meanwhile
        });
    }

    private void write(UUID id, String data) {
        try {
            Files.writeString(file(id).toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save player data for " + id + ": " + ex.getMessage());
        }
    }

    /** Writes anything still queued, synchronously — called on plugin disable. */
    public void flushSync() {
        synchronized (ioLock) {
            for (Map.Entry<UUID, String> entry : pendingWrites.entrySet()) {
                write(entry.getKey(), entry.getValue());
            }
            pendingWrites.clear();
        }
    }

    /** Epoch millis at which the kit becomes claimable again; 0 = never claimed / ready. */
    public long cooldownExpiry(UUID id, String kit) {
        return data(id).getLong("cooldowns." + kit, 0L);
    }

    public void setCooldownExpiry(UUID id, String kit, long expiryMillis) {
        data(id).set("cooldowns." + kit, expiryMillis);
        save(id);
    }

    /** The player's saved layout for a kit, or null if they use the default. */
    public Map<Integer, ItemStack> layout(UUID id, String kit) {
        ConfigurationSection section = data(id).getConfigurationSection("layouts." + kit);
        if (section == null) return null;
        Map<Integer, ItemStack> out = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ItemStack item = Items.fromBase64(section.getString(key));
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException ex) {
                continue;
            }
            if (item != null && slot >= 0 && slot < 41) out.put(slot, item);
        }
        return out.isEmpty() ? null : out;
    }

    public void setLayout(UUID id, String kit, Map<Integer, ItemStack> layout) {
        YamlConfiguration yaml = data(id);
        yaml.set("layouts." + kit, null);
        for (Map.Entry<Integer, ItemStack> entry : layout.entrySet()) {
            yaml.set("layouts." + kit + "." + entry.getKey(), Items.toBase64(entry.getValue()));
        }
        save(id);
    }

    public boolean hasLayout(UUID id, String kit) {
        return data(id).isConfigurationSection("layouts." + kit);
    }

    // ---------------------------------------------------------------- coins & purchases

    public long coins(UUID id) {
        return data(id).getLong("coins", 0L);
    }

    public void setCoins(UUID id, long amount) {
        data(id).set("coins", Math.max(0, amount));
        save(id);
    }

    public void addCoins(UUID id, long delta) {
        setCoins(id, coins(id) + delta);
    }

    public boolean hasPurchased(UUID id, String kit) {
        return data(id).getStringList("purchases").contains(kit.toLowerCase(java.util.Locale.ROOT));
    }

    public void addPurchase(UUID id, String kit) {
        String key = kit.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> owned = data(id).getStringList("purchases");
        if (!owned.contains(key)) {
            owned.add(key);
            data(id).set("purchases", owned);
            save(id);
        }
    }

    public void clearLayout(UUID id, String kit) {
        data(id).set("layouts." + kit, null);
        save(id);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }
}
