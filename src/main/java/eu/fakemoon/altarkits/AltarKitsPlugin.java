package eu.fakemoon.altarkits;

import eu.fakemoon.altarkits.coin.CoinListener;
import eu.fakemoon.altarkits.command.CoinShopCommand;
import eu.fakemoon.altarkits.command.CoinsCommand;
import eu.fakemoon.altarkits.command.KitAdminCommand;
import eu.fakemoon.altarkits.command.KitLayoutsCommand;
import eu.fakemoon.altarkits.command.KitsCommand;
import eu.fakemoon.altarkits.data.PlayerDataManager;
import eu.fakemoon.altarkits.gui.GuiListener;
import eu.fakemoon.altarkits.kit.KitManager;
import eu.fakemoon.altarkits.util.Folia;
import eu.fakemoon.altarkits.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class AltarKitsPlugin extends JavaPlugin {

    private PlayerDataManager playerData;
    private KitManager kits;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        Messages.init(this);

        playerData = new PlayerDataManager(this);
        kits = new KitManager(this, playerData);
        kits.load();

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(playerData, this);
        getServer().getPluginManager().registerEvents(new CoinListener(this), this);

        Objects.requireNonNull(getCommand("kits")).setExecutor(new KitsCommand(this));
        Objects.requireNonNull(getCommand("kitlayouts")).setExecutor(new KitLayoutsCommand(this));
        Objects.requireNonNull(getCommand("coinshop")).setExecutor(new CoinShopCommand(this));
        CoinsCommand coins = new CoinsCommand(this);
        Objects.requireNonNull(getCommand("coins")).setExecutor(coins);
        Objects.requireNonNull(getCommand("coins")).setTabCompleter(coins);
        KitAdminCommand admin = new KitAdminCommand(this);
        Objects.requireNonNull(getCommand("kit")).setExecutor(admin);
        Objects.requireNonNull(getCommand("kit")).setTabCompleter(admin);

        getLogger().info("Kits enabled with " + kits.all().size() + " kit(s) on "
                + (Folia.isFolia() ? "Folia" : "Paper") + ".");
    }

    @Override
    public void onDisable() {
        if (kits != null) kits.flushSync();
        if (playerData != null) playerData.flushSync();
    }

    public KitManager kits() {
        return kits;
    }

    public PlayerDataManager playerData() {
        return playerData;
    }

    /**
     * Runs a task on the player's own region thread next tick — used to switch GUIs
     * safely from inside click events, and to touch a player from another thread
     * (e.g. a console command or another player's region).
     *
     * <p>On Paper this is just the main thread, on Folia the thread that owns the
     * player, which is the only thread allowed to open/close their inventory.
     */
    public void sync(Player player, Runnable task) {
        player.getScheduler().run(this, scheduled -> task.run(), null);
    }

    /**
     * Runs a task on the global region thread next tick. For server-wide work that
     * touches no specific player (config reloads, onDisable, …).
     */
    public void sync(Runnable task) {
        getServer().getGlobalRegionScheduler().run(this, scheduled -> task.run());
    }

    /** Runs a task off the server threads — for file IO only. */
    public void async(Runnable task) {
        getServer().getAsyncScheduler().runNow(this, scheduled -> task.run());
    }
}
