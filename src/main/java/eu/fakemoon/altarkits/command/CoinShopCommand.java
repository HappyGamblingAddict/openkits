package eu.fakemoon.altarkits.command;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.gui.CoinShopGui;
import eu.fakemoon.altarkits.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CoinShopCommand implements CommandExecutor {

    private final AltarKitsPlugin plugin;

    public CoinShopCommand(AltarKitsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<red>Players only.");
            return true;
        }
        CoinShopGui.open(plugin, player);
        return true;
    }
}
