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

/** Kit picker for the per-player layout editor. Paginated to match the kits menu. */
public final class LayoutsGui implements KitsHolder {

    private static final int SIZE = 54;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_PAGE = 50;
    private static final int SLOT_NEXT = 53;

    private final AltarKitsPlugin plugin;
    private final Player player;
    private final Inventory inv;
    private final Kit[] bySlot = new Kit[SIZE];
    private int page;
    private int pages = 1;

    private LayoutsGui(AltarKitsPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, page);
        this.inv = Bukkit.createInventory(this, SIZE, Messages.get("gui.layouts-title"));
        build();
    }

    public static void open(AltarKitsPlugin plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(AltarKitsPlugin plugin, Player player, int page) {
        player.openInventory(new LayoutsGui(plugin, player, page).inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void build() {
        List<Kit> accessible = plugin.kits().sorted().stream()
                .filter(kit -> plugin.kits().hasAccess(player, kit))
                .toList();
        pages = Math.max(1, (accessible.size() + KitsGui.PER_PAGE - 1) / KitsGui.PER_PAGE);
        if (page >= pages) page = pages - 1;

        ItemStack filler = Items.named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < SIZE; slot++) inv.setItem(slot, filler);
        for (int slot : KitsGui.KIT_SLOTS) inv.setItem(slot, null);
        Arrays.fill(bySlot, null);

        int start = page * KitsGui.PER_PAGE;
        for (int i = 0; i < KitsGui.PER_PAGE && start + i < accessible.size(); i++) {
            Kit kit = accessible.get(start + i);
            int slot = KitsGui.KIT_SLOTS[i];
            bySlot[slot] = kit;

            boolean custom = plugin.playerData().hasLayout(player.getUniqueId(), kit.name());
            List<String> lore = new ArrayList<>(Messages.rawList("gui.layout-entry-lore"));
            lore.add("");
            lore.add(Messages.raw(custom ? "gui.layout-custom" : "gui.layout-default"));
            lore.add("");
            lore.add(Messages.raw("gui.edit-hint"));
            inv.setItem(slot, Items.withDisplay(kit.icon(),
                    Messages.raw("gui.layout-entry-name", "kit", kit.displayName()), lore));
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
        inv.setItem(SLOT_BACK, Items.named(Material.ARROW,
                Messages.raw("gui.back-button"), Messages.raw("gui.back-button-lore")));
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_BACK -> {
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                plugin.sync(() -> KitsGui.open(plugin, clicker));
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
        clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        plugin.sync(() -> LayoutEditorGui.open(plugin, clicker, kit, LayoutEditorGui.Mode.PLAYER));
    }
}
