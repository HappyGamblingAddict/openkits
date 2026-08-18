package eu.fakemoon.altarkits.command;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.gui.KitEditorGui;
import eu.fakemoon.altarkits.gui.LayoutEditorGui;
import eu.fakemoon.altarkits.kit.Kit;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import eu.fakemoon.altarkits.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class KitAdminCommand implements CommandExecutor, TabCompleter {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,24}");
    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "edit", "editor", "icon", "cooldown", "permission", "price", "displayname", "give", "list", "reload", "unstable");

    private final AltarKitsPlugin plugin;

    public KitAdminCommand(AltarKitsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "edit" -> edit(sender, args);
            case "editor" -> editor(sender);
            case "icon" -> icon(sender, args);
            case "cooldown" -> cooldown(sender, args);
            case "permission" -> permission(sender, args);
            case "price" -> price(sender, args);
            case "displayname" -> displayName(sender, args);
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                plugin.reloadConfig();
                Messages.init(plugin);
                Text.msg(sender, "<green>config.yml + messages.yml reloaded. Open menus update when reopened.");
            }
            case "unstable" -> unstable(sender);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        Text.msg(sender, "<gold>Kit admin commands:");
        sender.sendMessage(Text.mm("<gray> /kit create <name> <dark_gray>- snapshot your inventory as a new kit"));
        sender.sendMessage(Text.mm("<gray> /kit edit <name> <dark_gray>- edit a kit's contents in a GUI"));
        sender.sendMessage(Text.mm("<gray> /kit editor <dark_gray>- arrange where kits show in the /kits menu"));
        sender.sendMessage(Text.mm("<gray> /kit delete <name>"));
        sender.sendMessage(Text.mm("<gray> /kit icon <name> <dark_gray>- set the GUI icon to your held item"));
        sender.sendMessage(Text.mm("<gray> /kit cooldown <name> <12h30m|none>"));
        sender.sendMessage(Text.mm("<gray> /kit permission <name> <node|none>"));
        sender.sendMessage(Text.mm("<gray> /kit price <name> <coins|none> <dark_gray>- sell in /coinshop"));
        sender.sendMessage(Text.mm("<gray> /kit displayname <name> <text...>"));
        sender.sendMessage(Text.mm("<gray> /kit give <player> <name>"));
        sender.sendMessage(Text.mm("<gray> /kit list"));
        sender.sendMessage(Text.mm("<gray> /kit reload <dark_gray>- reload messages.yml"));
        sender.sendMessage(Text.mm("<gray> /kit unstable <dark_gray>- generate the 30 Unstable character kits"));
    }

    private Kit kitArg(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.msg(sender, "<red>Usage: /kit " + args[0].toLowerCase(Locale.ROOT) + " <name> ...");
            return null;
        }
        Kit kit = plugin.kits().get(args[1]);
        if (kit == null) Text.msg(sender, "<red>No kit named <white>" + args[1] + "</white>.");
        return kit;
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<red>Players only — the kit is created from your inventory.");
            return;
        }
        if (args.length < 2) {
            Text.msg(sender, "<red>Usage: /kit create <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (!NAME.matcher(name).matches()) {
            Text.msg(sender, "<red>Kit names may only use a-z, 0-9, _ and - (max 24 chars).");
            return;
        }
        if (plugin.kits().get(name) != null) {
            Text.msg(sender, "<red>A kit named <white>" + name + "</white> already exists.");
            return;
        }
        boolean empty = true;
        for (int slot = 0; slot < Kit.CONTENT_SLOTS && empty; slot++) {
            empty = Items.isEmpty(player.getInventory().getItem(slot));
        }
        if (empty) {
            Text.msg(sender, "<red>Your inventory is empty — hold the kit's items first.");
            return;
        }
        Kit kit = plugin.kits().create(name, player);
        Text.msg(sender, "<green>Kit <yellow>" + kit.displayName() + "</yellow> created from your inventory — it now shows in <white>/kits</white>.");
        Text.msg(sender, "<gray>Cooldown: <white>none</white>. Set one with <white>/kit cooldown " + name + " 12h</white>.");
    }

    private void delete(CommandSender sender, String[] args) {
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        plugin.kits().delete(kit);
        Text.msg(sender, "<green>Kit <yellow>" + kit.displayName() + "</yellow> deleted.");
    }

    private void edit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<red>Players only.");
            return;
        }
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        LayoutEditorGui.open(plugin, player, kit, LayoutEditorGui.Mode.ADMIN);
    }

    private void unstable(CommandSender sender) {
        List<Kit> generated = eu.fakemoon.altarkits.kit.UnstableKits.build();
        for (Kit kit : generated) plugin.kits().define(kit);
        plugin.kits().saveAll();
        Text.msg(sender, "<green>Generated <yellow>" + generated.size() + "</yellow> Unstable character kits — "
                + "each locked to <white>kits.kit.&lt;name&gt;</white> and visible in <white>/kits</white>.");
        Text.msg(sender, "<gray>Tweak any with <white>/kit edit <name></white>, rearrange with <white>/kit editor</white>.");
    }

    private void editor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<red>Players only.");
            return;
        }
        if (plugin.kits().all().isEmpty()) {
            Text.msg(sender, "<red>No kits to arrange yet — create one first.");
            return;
        }
        KitEditorGui.open(plugin, player);
    }

    private void icon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<red>Players only.");
            return;
        }
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (Items.isEmpty(held)) {
            Text.msg(sender, "<red>Hold the item you want as the kit's icon.");
            return;
        }
        ItemStack icon = held.clone();
        icon.setAmount(1);
        kit.setIcon(icon);
        plugin.kits().saveAll();
        Text.msg(sender, "<green>Icon of <yellow>" + kit.displayName() + "</yellow> updated.");
    }

    private void cooldown(CommandSender sender, String[] args) {
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        if (args.length < 3) {
            Text.msg(sender, "<red>Usage: /kit cooldown " + kit.name() + " <12h30m|none>");
            return;
        }
        long seconds = Text.parseDuration(args[2]);
        if (seconds < 0) {
            Text.msg(sender, "<red>Invalid duration. Examples: <white>12h</white>, <white>1d6h</white>, <white>90m</white>, <white>none</white>.");
            return;
        }
        kit.setCooldownSeconds(seconds);
        plugin.kits().saveAll();
        Text.msg(sender, "<green>Cooldown of <yellow>" + kit.displayName() + "</yellow> set to <white>"
                + (seconds == 0 ? "none" : Text.duration(seconds)) + "</white>.");
    }

    private void permission(CommandSender sender, String[] args) {
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        if (args.length < 3) {
            Text.msg(sender, "<red>Usage: /kit permission " + kit.name() + " <node|none>");
            return;
        }
        String node = args[2].equalsIgnoreCase("none") ? "" : args[2];
        kit.setPermission(node);
        plugin.kits().saveAll();
        Text.msg(sender, node.isEmpty()
                ? "<green>Kit <yellow>" + kit.displayName() + "</yellow> is now usable by everyone."
                : "<green>Kit <yellow>" + kit.displayName() + "</yellow> now requires <white>" + node + "</white>.");
    }

    private void price(CommandSender sender, String[] args) {
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        if (args.length < 3) {
            Text.msg(sender, "<red>Usage: /kit price " + kit.name() + " <coins|none>");
            return;
        }
        int price;
        if (args[2].equalsIgnoreCase("none") || args[2].equals("0")) {
            price = 0;
        } else {
            try {
                price = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                Text.msg(sender, "<red>Price must be a whole number of coins (or <white>none</white>).");
                return;
            }
            if (price < 0) {
                Text.msg(sender, "<red>Price can't be negative.");
                return;
            }
        }
        kit.setPrice(price);
        plugin.kits().saveAll();
        Text.msg(sender, price == 0
                ? "<green>Kit <yellow>" + kit.displayName() + "</yellow> removed from the coin shop."
                : "<green>Kit <yellow>" + kit.displayName() + "</yellow> now costs <gold>" + price + "</gold> coins in <white>/coinshop</white>.");
        if (price > 0 && kit.permission().isEmpty()) {
            Text.msg(sender, "<gray>It's now locked until bought (no permission set).");
        }
    }

    private void displayName(CommandSender sender, String[] args) {
        Kit kit = kitArg(sender, args);
        if (kit == null) return;
        if (args.length < 3) {
            Text.msg(sender, "<red>Usage: /kit displayname " + kit.name() + " <text...> <dark_gray>(MiniMessage supported)");
            return;
        }
        String name = String.join(" ", List.of(args).subList(2, args.length));
        kit.setDisplayName(name);
        plugin.kits().saveAll();
        Text.msg(sender, "<green>Display name set to: <reset>" + name);
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Text.msg(sender, "<red>Usage: /kit give <player> <kit>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Text.msg(sender, "<red>Player <white>" + args[1] + "</white> is not online.");
            return;
        }
        Kit kit = plugin.kits().get(args[2]);
        if (kit == null) {
            Text.msg(sender, "<red>No kit named <white>" + args[2] + "</white>.");
            return;
        }
        plugin.kits().give(target, kit);
        Text.msg(sender, "<green>Gave kit <yellow>" + kit.displayName() + "</yellow> to <white>" + target.getName() + "</white>.");
    }

    private void list(CommandSender sender) {
        if (plugin.kits().all().isEmpty()) {
            Text.msg(sender, "<gray>No kits yet. Create one with <white>/kit create <name></white>.");
            return;
        }
        Text.msg(sender, "<gold>Kits (" + plugin.kits().all().size() + "):");
        for (Kit kit : plugin.kits().sorted()) {
            sender.sendMessage(Text.mm(" <yellow>" + kit.name() + "</yellow> <dark_gray>-</dark_gray> <gray>cooldown: <white>"
                    + (kit.cooldownSeconds() == 0 ? "none" : Text.duration(kit.cooldownSeconds()))
                    + "</white>, permission: <white>" + (kit.permission().isEmpty() ? "everyone" : kit.permission())
                    + "</white>, price: <white>" + (kit.price() == 0 ? "-" : kit.price() + " coins")
                    + "</white>, items: <white>" + kit.contents().size() + "</white>"));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String @NotNull [] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("give")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
            }
            if (sub.equals("create")) return List.of();
            return kitNames(args[1]);
        }
        if (args.length == 3) {
            if (sub.equals("give")) return kitNames(args[2]);
            if (sub.equals("cooldown")) return Stream.of("12h", "1d", "30m", "none")
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
            if (sub.equals("permission")) return List.of("none", "kits.kit." + args[1].toLowerCase(Locale.ROOT));
            if (sub.equals("price")) return Stream.of("none", "100", "500", "1000")
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    private List<String> kitNames(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return plugin.kits().all().stream().map(Kit::name).filter(n -> n.startsWith(p)).toList();
    }
}
