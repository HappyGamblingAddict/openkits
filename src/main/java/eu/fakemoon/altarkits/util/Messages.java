package eu.fakemoon.altarkits.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads messages.yml (created from the bundled default on first run). Keys the
 * admin deleted or that are new in an update fall back to the bundled defaults.
 * Placeholders are given as key/value pairs: raw("gui.kit-name", "kit", "Mino").
 */
public final class Messages {

    private static YamlConfiguration yaml = new YamlConfiguration();

    private Messages() {
    }

    public static void init(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        yaml = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource("messages.yml");
        if (bundled != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
    }

    /** Raw MiniMessage string with {placeholders} applied. */
    public static String raw(String key, String... placeholders) {
        // Single-arg getString consults the bundled defaults, so keys the admin
        // deleted (or new keys added in an update) fall back instead of showing
        // the raw key text.
        String value = yaml.getString(key);
        if (value == null) value = key;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            value = value.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return value;
    }

    public static List<String> rawList(String key, String... placeholders) {
        List<String> lines = yaml.getStringList(key);
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                line = line.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
            out.add(line);
        }
        return out;
    }

    public static Component get(String key, String... placeholders) {
        return Text.mm(raw(key, placeholders));
    }

    public static Component prefix() {
        return get("prefix");
    }

    /** Sends prefix + the configured message. */
    public static void send(CommandSender to, String key, String... placeholders) {
        to.sendMessage(prefix().append(get(key, placeholders)));
    }
}
