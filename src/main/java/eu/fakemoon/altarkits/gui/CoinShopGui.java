package eu.fakemoon.altarkits.gui;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.kit.Kit;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** /coinshop — buy price-tagged kits with coins. */
public final class CoinShopGui implements KitsHolder {

    private static final int SIZE = 54;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BALANCE = 49;
    private static final int SLOT_NEXT = 53;

    private final AltarKitsPlugin plugin;
    private final Player player;
    private final Inventory inv;
    private final Kit[] bySlot = new Kit[SIZE];
    private int page;
    private int pages = 1;

    private CoinShopGui(AltarKitsPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, page);
        this.inv = Bukkit.createInventory(this, SIZE, Messages.get("gui.shop-title"));
        build();
    }

    public static void open(AltarKitsPlugin plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(AltarKitsPlugin plugin, Player player, int page) {
        player.openInventory(new CoinShopGui(plugin, player, page).inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void build() {
        List<Kit> shop = plugin.kits().buyable();
        pages = Math.max(1, (shop.size() + KitsGui.PER_PAGE - 1) / KitsGui.PER_PAGE);
        if (page >= pages) page = pages - 1;

        ItemStack filler = Items.named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < SIZE; slot++) inv.setItem(slot, filler);
        for (int slot : KitsGui.KIT_SLOTS) inv.setItem(slot, null);
        Arrays.fill(bySlot, null);

        int start = page * KitsGui.PER_PAGE;
        for (int i = 0; i < KitsGui.PER_PAGE && start + i < shop.size(); i++) {
            Kit kit = shop.get(start + i);
            int slot = KitsGui.KIT_SLOTS[i];
            bySlot[slot] = kit;
            inv.setItem(slot, shopItem(kit));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, Items.named(Material.ARROW,
                    Messages.raw("gui.prev-button"), Messages.raw("gui.prev-button-lore")));
        }
        if (page < pages - 1) {
            inv.setItem(SLOT_NEXT, Items.named(Material.ARROW,
                    Messages.raw("gui.next-button"), Messages.raw("gui.next-button-lore")));
        }
        long coins = plugin.playerData().coins(player.getUniqueId());
        inv.setItem(SLOT_BALANCE, Items.named(Material.SUNFLOWER,
                Messages.raw("gui.shop-balance", "coins", String.valueOf(coins))));
    }

    private ItemStack shopItem(Kit kit) {
        boolean owned = plugin.playerData().hasPurchased(player.getUniqueId(), kit.name());
        long coins = plugin.playerData().coins(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(Messages.raw("gui.shop-price", "price", String.valueOf(kit.price())));
        lore.add("");
        if (owned) {
            lore.add(Messages.raw("gui.shop-owned"));
        } else if (coins >= kit.price()) {
            lore.add(Messages.raw("gui.shop-buy-hint"));
        } else {
            lore.add(Messages.raw("gui.shop-cant-afford", "coins", String.valueOf(coins)));
        }
        return Items.withDisplay(kit.icon(), Messages.raw("gui.kit-name", "kit", kit.displayName()), lore);
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();

        if (slot == SLOT_PREV && page > 0) {
            clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            plugin.sync(() -> open(plugin, clicker, page - 1));
            return;
        }
        if (slot == SLOT_NEXT && page < pages - 1) {
            clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            plugin.sync(() -> open(plugin, clicker, page + 1));
            return;
        }

        Kit kit = slot >= 0 && slot < bySlot.length ? bySlot[slot] : null;
        if (kit == null || !kit.isBuyable()) return;

        UUID id = clicker.getUniqueId();
        if (plugin.playerData().hasPurchased(id, kit.name())) {
            Messages.send(clicker, "coins.already-owned", "kit", kit.displayName());
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return;
        }
        long coins = plugin.playerData().coins(id);
        if (coins < kit.price()) {
            Messages.send(clicker, "coins.cant-afford",
                    "kit", kit.displayName(), "price", String.valueOf(kit.price()), "coins", String.valueOf(coins));
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }
        plugin.playerData().addCoins(id, -kit.price());
        plugin.playerData().addPurchase(id, kit.name());
        Messages.send(clicker, "coins.bought", "kit", kit.displayName(), "price", String.valueOf(kit.price()));
        clicker.playSound(clicker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        build();
    }
}
