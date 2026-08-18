package eu.fakemoon.altarkits.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms])");

    private Text() {
    }

    /** Parses MiniMessage, forcing italics off so item names/lore aren't slanted by default. */
    public static Component mm(String miniMessage) {
        return MM.deserialize(miniMessage).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static void msg(CommandSender to, String miniMessage) {
        to.sendMessage(Messages.prefix().append(mm(miniMessage)));
    }

    public static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Formats seconds as e.g. "2d 5h", "3h 12m", "45s". */
    public static String duration(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder out = new StringBuilder();
        if (days > 0) out.append(days).append("d ");
        if (hours > 0) out.append(hours).append("h ");
        if (minutes > 0) out.append(minutes).append("m ");
        if (seconds > 0 && days == 0) out.append(seconds).append("s");
        return out.toString().trim();
    }

    /** Parses "1d2h30m", "90m", "45s", plain seconds, or "none"/"0". Returns -1 if invalid. */
    public static long parseDuration(String input) {
        String s = input.toLowerCase(Locale.ROOT).trim();
        if (s.equals("none") || s.equals("0")) return 0;
        Matcher m = DURATION_PART.matcher(s);
        long total = 0;
        boolean any = false;
        while (m.find()) {
            any = true;
            long value = Long.parseLong(m.group(1));
            total += switch (m.group(2)) {
                case "d" -> value * 86400;
                case "h" -> value * 3600;
                case "m" -> value * 60;
                default -> value;
            };
        }
        if (any) return total;
        try {
            long plain = Long.parseLong(s);
            return plain >= 0 ? plain : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
