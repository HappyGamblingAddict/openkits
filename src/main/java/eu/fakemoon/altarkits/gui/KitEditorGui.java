package eu.fakemoon.altarkits.gui;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.kit.Kit;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Admin arranger (/kit editor): a live mirror of the /kits menu. Click a kit to
 * pick it up, then click any content slot to move it there (swapping if another
 * kit is already in that slot). Each move updates the kit's position instantly.
 */
public final class KitEditorGui implements KitsHolder {

    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_PAGE = 50;
    private static final int SLOT_NEXT = 53;

    private final AltarKitsPlugin plugin;
    private final Inventory inv;
    private final Kit[] bySlot = new Kit[KitsGui.SIZE];
    private int page;
    private int pages = 1;
    /** Name of the kit currently picked up, or null. */
    private String selected;

    private KitEditorGui(AltarKitsPlugin plugin) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, KitsGui.SIZE, Messages.get("gui.arrange-title"));
        build();
    }

    public static void open(AltarKitsPlugin plugin, Player player) {
        player.openInventory(new KitEditorGui(plugin).inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    private void build() {
        List<Kit> kits = plugin.kits().sorted();
        int maxPos = 0;
        for (Kit kit : kits) maxPos = Math.max(maxPos, kit.order());
        pages = Math.max(1, maxPos / KitsGui.PER_PAGE + 1);
        page = Math.max(0, Math.min(page, pages - 1));

        ItemStack filler = Items.named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < KitsGui.SIZE; slot++) inv.setItem(slot, filler);
        for (int slot : KitsGui.KIT_SLOTS) inv.setItem(slot, null);
        Arrays.fill(bySlot, null);

        boolean picking = selected != null && plugin.kits().get(selected) != null;
        for (Kit kit : kits) {
            if (kit.order() / KitsGui.PER_PAGE != page) continue;
            int idx = kit.order() % KitsGui.PER_PAGE;
            if (idx < 0 || idx >= KitsGui.KIT_SLOTS.length) continue;
            int slot = KitsGui.KIT_SLOTS[idx];
            bySlot[slot] = kit;
            inv.setItem(slot, icon(kit, kit.name().equals(selected)));
        }

        // While holding a kit, mark empty content slots as drop targets.
        if (picking) {
            for (int slot : KitsGui.KIT_SLOTS) {
                if (bySlot[slot] == null) {
                    inv.setItem(slot, Items.named(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                            Messages.raw("gui.arrange-place-here"), Messages.raw("gui.arrange-place-here-lore")));
                }
            }
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
        inv.setItem(SLOT_INFO, Items.withDisplay(new ItemStack(Material.WRITABLE_BOOK),
                Messages.raw("gui.arrange-info"), Messages.rawList("gui.arrange-info-lore")));
        inv.setItem(SLOT_CLOSE, Items.named(Material.BARRIER, Messages.raw("gui.close-button")));
    }

    private ItemStack icon(Kit kit, boolean selectedKit) {
        List<String> lore = new ArrayList<>();
        lore.add(selectedKit ? Messages.raw("gui.arrange-selected") : Messages.raw("gui.arrange-move-hint"));
        ItemStack item = Items.withDisplay(kit.icon(),
                Messages.raw("gui.kit-name", "kit", kit.displayName()), lore);
        if (selectedKit) {
            item.editMeta(meta -> {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
        }
        return item;
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
            case SLOT_PREV -> {
                if (page > 0) {
                    page--;
                    clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                    build();
                }
                return;
            }
            case SLOT_NEXT -> {
                if (page < pages - 1) {
                    page++;
                    clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                    build();
                }
                return;
            }
            default -> {
            }
        }

        int idx = KitsGui.contentIndex(slot);
        if (idx < 0) return;
        int position = page * KitsGui.PER_PAGE + idx;
        Kit here = bySlot[slot];

        if (selected == null) {
            // Nothing held: pick up the kit in this slot (if any).
            if (here != null) {
                selected = here.name();
                clicker.playSound(clicker.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.7f, 1.4f);
                build();
            }
            return;
        }

        Kit held = plugin.kits().get(selected);
        if (held == null) {
            selected = null;
            build();
            return;
        }
        if (here != null && here.name().equals(selected)) {
            // Clicked the held kit again — drop it back (deselect).
            selected = null;
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            build();
            return;
        }

        // Place the held kit here; if a kit already sits here, swap positions.
        int from = held.order();
        if (here != null) here.setOrder(from);
        held.setOrder(position);
        plugin.kits().saveAll();
        selected = null;
        clicker.playSound(clicker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);
        build();
    }
}
