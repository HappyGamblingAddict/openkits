package eu.fakemoon.altarkits.gui;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.kit.Kit;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import eu.fakemoon.altarkits.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Main /kits menu: paginated, left-click to claim, right-click to preview. */
public final class KitsGui implements KitsHolder {

    /** Content slots for kits inside the 54-slot menu (4 centered rows of 7 = 28 per page). */
    public static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    public static final int PER_PAGE = KIT_SLOTS.length;
    static final int SIZE = 54;

    /** The index (0..PER_PAGE-1) of a content slot, or -1 if it isn't one. */
    public static int contentIndex(int slot) {
        for (int i = 0; i < KIT_SLOTS.length; i++) {
            if (KIT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private static final int SLOT_PREV = 45;
    private static final int SLOT_LAYOUTS = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_PAGE = 50;
    private static final int SLOT_NEXT = 53;

    private final AltarKitsPlugin plugin;
    private final Player player;
    private final Inventory inv;
    private final Kit[] bySlot = new Kit[SIZE];
    private int page;
    private int pages = 1;

    private KitsGui(AltarKitsPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, page);
        this.inv = Bukkit.createInventory(this, SIZE, Messages.get("gui.kits-title"));
        build();
    }

    public static void open(AltarKitsPlugin plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(AltarKitsPlugin plugin, Player player, int page) {
        player.openInventory(new KitsGui(plugin, player, page).inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void build() {
        List<Kit> kits = plugin.kits().sorted();
        int maxPos = 0;
        for (Kit kit : kits) maxPos = Math.max(maxPos, kit.order());
        pages = Math.max(1, maxPos / PER_PAGE + 1);
        if (page >= pages) page = pages - 1;

        ItemStack filler = Items.named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < SIZE; slot++) inv.setItem(slot, filler);
        for (int slot : KIT_SLOTS) inv.setItem(slot, null);
        java.util.Arrays.fill(bySlot, null);

        // Each kit is shown at its stored position (page = order / PER_PAGE,
        // slot = order % PER_PAGE), so gaps set in the editor are preserved.
        for (Kit kit : kits) {
            if (kit.order() / PER_PAGE != page) continue;
            int idx = kit.order() % PER_PAGE;
            if (idx < 0 || idx >= KIT_SLOTS.length) continue;
            int slot = KIT_SLOTS[idx];
            bySlot[slot] = kit;
            inv.setItem(slot, kitItem(kit));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, Items.named(Material.ARROW,
                    Messages.raw("gui.prev-button"), Messages.raw("gui.prev-button-lore")));
        }
        if (page < pages - 1) {
            inv.setItem(SLOT_NEXT, Items.named(Material.ARROW,
                    Messages.raw("gui.next-button"), Messages.raw("gui.next-button-lore")));
        }
        inv.setItem(SLOT_PAGE, Items.named(Material.PAPER,
                Messages.raw("gui.page-info", "page", String.valueOf(page + 1), "pages", String.valueOf(pages))));
        inv.setItem(SLOT_CLOSE, Items.named(Material.BARRIER, Messages.raw("gui.close-button")));
        inv.setItem(SLOT_LAYOUTS, Items.withDisplay(new ItemStack(Material.COMPARATOR),
                Messages.raw("gui.layouts-button"), Messages.rawList("gui.layouts-button-lore")));
    }

    private ItemStack kitItem(Kit kit) {
        List<String> lore = new ArrayList<>();
        if (!plugin.kits().hasAccess(player, kit)) {
            if (kit.isBuyable()) {
                lore.add(Messages.raw("gui.shop-locked", "price", String.valueOf(kit.price())));
            } else {
                lore.add(Messages.raw("gui.locked"));
                lore.add("");
                lore.add(Messages.raw("gui.locked-lore"));
            }
            lore.add(Messages.raw("gui.preview-hint"));
            return Items.withDisplay(new ItemStack(Material.BARRIER),
                    Messages.raw("gui.kit-name-locked", "kit", kit.displayName()), lore);
        }
        long remaining = plugin.kits().remainingCooldown(player, kit);
        lore.add(remaining <= 0
                ? Messages.raw("gui.ready")
                : Messages.raw("gui.cooldown", "time", Text.duration((remaining + 999) / 1000)));
        lore.add("");
        lore.add(Messages.raw("gui.claim-hint"));
        lore.add(Messages.raw("gui.preview-hint"));
        return Items.withDisplay(kit.icon(), Messages.raw("gui.kit-name", "kit", kit.displayName()), lore);
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_CLOSE -> {
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                plugin.sync(clicker::closeInventory);
                return;
            }
            case SLOT_LAYOUTS -> {
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                plugin.sync(() -> LayoutsGui.open(plugin, clicker));
                return;
            }
            case SLOT_PREV -> {
                if (page > 0) {
                    clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                    plugin.sync(() -> open(plugin, clicker, page - 1));
                }
                return;
            }
            case SLOT_NEXT -> {
                if (page < pages - 1) {
                    clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                    plugin.sync(() -> open(plugin, clicker, page + 1));
                }
                return;
            }
            default -> {
            }
        }

        Kit kit = slot >= 0 && slot < bySlot.length ? bySlot[slot] : null;
        if (kit == null) return;

        ClickType click = event.getClick();
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            plugin.sync(() -> PreviewGui.open(plugin, clicker, kit));
            return;
        }
        if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
            plugin.sync(() -> {
                if (plugin.kits().claim(clicker, kit)) {
                    clicker.closeInventory();
                } else {
                    clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                    build();
                }
            });
        }
    }
}
