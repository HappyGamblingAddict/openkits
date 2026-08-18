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
import eu.fakemoon.altarkits.util.Messages;
import org.bukkit.Bukkit;
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

        getLogger().info("Kits enabled with " + kits.all().size() + " kit(s).");
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

    /** Runs a task on the next tick — used to switch GUIs safely from inside click events. */
    public void sync(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }
}
