package eu.fakemoon.altarkits.command;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.util.Messages;
import eu.fakemoon.altarkits.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/** /coins — check your balance, or (admin) give/take/set another player's coins. */
public final class CoinsCommand implements CommandExecutor, TabCompleter {

    private final AltarKitsPlugin plugin;

    public CoinsCommand(AltarKitsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                Text.msg(sender, "<red>Usage: /coins <give|take|set|show> <player> [amount]");
                return true;
            }
            Messages.send(player, "coins.balance",
                    "coins", String.valueOf(plugin.playerData().coins(player.getUniqueId())));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give") || sub.equals("take") || sub.equals("set") || sub.equals("show")) {
            if (!sender.hasPermission("kits.admin")) {
                Text.msg(sender, "<red>You don't have permission.");
                return true;
            }
            admin(sender, sub, args);
            return true;
        }

        // /coins <player> — admin quick view
        if (sender.hasPermission("kits.admin")) {
            OfflinePlayer target = resolve(args[0]);
            Messages.send(sender, "coins.other-balance",
                    "player", name(target), "coins", String.valueOf(plugin.playerData().coins(target.getUniqueId())));
        } else {
            Text.msg(sender, "<red>Usage: /coins");
        }
        return true;
    }

    private void admin(CommandSender sender, String sub, String[] args) {
        if (args.length < 2) {
            Text.msg(sender, "<red>Usage: /coins " + sub + " <player>" + (sub.equals("show") ? "" : " <amount>"));
            return;
        }
        OfflinePlayer target = resolve(args[1]);
        if (sub.equals("show")) {
            Messages.send(sender, "coins.other-balance",
                    "player", name(target), "coins", String.valueOf(plugin.playerData().coins(target.getUniqueId())));
            return;
        }
        if (args.length < 3) {
            Text.msg(sender, "<red>Usage: /coins " + sub + " <player> <amount>");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException ex) {
            Text.msg(sender, "<red>Amount must be a number.");
            return;
        }
        switch (sub) {
            case "give" -> plugin.playerData().addCoins(target.getUniqueId(), amount);
            case "take" -> plugin.playerData().addCoins(target.getUniqueId(), -amount);
            case "set" -> plugin.playerData().setCoins(target.getUniqueId(), amount);
            default -> {
            }
        }
        Messages.send(sender, "coins.admin-updated",
                "player", name(target), "coins", String.valueOf(plugin.playerData().coins(target.getUniqueId())));
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayer(name);
    }

    private String name(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("kits.admin")) return List.of();
        if (args.length == 1) {
            return List.of("give", "take", "set", "show").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }
}
