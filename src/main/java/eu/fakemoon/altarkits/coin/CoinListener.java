package eu.fakemoon.altarkits.coin;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Awards coins to the killer on a player kill (amount configurable). */
public final class CoinListener implements Listener {

    private final AltarKitsPlugin plugin;

    public CoinListener(AltarKitsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("coins.enabled", true)) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;

        long amount = plugin.getConfig().getLong("coins.per-kill", 10);
        if (amount <= 0) return;
        plugin.playerData().addCoins(killer.getUniqueId(), amount);
        killer.sendActionBar(Messages.get("coins.earned",
                "amount", String.valueOf(amount),
                "coins", String.valueOf(plugin.playerData().coins(killer.getUniqueId()))));
    }
}
