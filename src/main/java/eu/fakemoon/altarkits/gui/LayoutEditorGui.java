package eu.fakemoon.altarkits.gui;

import eu.fakemoon.altarkits.AltarKitsPlugin;
import eu.fakemoon.altarkits.kit.Kit;
import eu.fakemoon.altarkits.util.Items;
import eu.fakemoon.altarkits.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 54-slot inventory editor used two ways:
 * <ul>
 *   <li>ADMIN — freely edit a kit's contents/default layout (/kit edit). Closing saves the kit.</li>
 *   <li>PLAYER — rearrange (only) the kit's items into a personal layout. Closing saves the layout;
 *       items can never be added, removed or split, so nothing can be duped or stolen.</li>
 * </ul>
 * GUI slots 0-8 = hotbar, 9-35 = storage, 36-40 = helmet/chest/legs/boots/offhand, 49 = button.
 */
public final class LayoutEditorGui implements KitsHolder {

    public enum Mode { ADMIN, PLAYER }

    private static final int SLOT_BUTTON = 49;

    private final AltarKitsPlugin plugin;
    private final Player player;
    private final Kit kit;
    private final Mode mode;
    private final Inventory inv;

    private LayoutEditorGui(AltarKitsPlugin plugin, Player player, Kit kit, Mode mode) {
        this.plugin = plugin;
        this.player = player;
        this.kit = kit;
        this.mode = mode;
        this.inv = Bukkit.createInventory(this, 54, Messages.get(
                mode == Mode.ADMIN ? "gui.editor-title" : "gui.layout-title", "kit", kit.displayName()));
        build();
    }

    public static void open(AltarKitsPlugin plugin, Player player, Kit kit, Mode mode) {
        player.openInventory(new LayoutEditorGui(plugin, player, kit, mode).inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    /**
     * Maps player-inventory slots to GUI slots and back (the mapping is its own inverse).
     * Armor (inv 36-39 = boots..helmet) is shown left-to-right as helmet, chest, legs, boots.
     */
    static int mapSlot(int slot) {
        return switch (slot) {
            case 36 -> 39;
            case 37 -> 38;
            case 38 -> 37;
            case 39 -> 36;
            default -> slot;
        };
    }

    private void build() {
        for (int gui = 0; gui <= 40; gui++) inv.setItem(gui, null);
        Map<Integer, ItemStack> contents = mode == Mode.PLAYER
                ? plugin.kits().resolveLayout(player, kit)
                : kit.contents();
        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            int gui = mapSlot(entry.getKey());
            if (gui >= 0 && gui <= 40) inv.setItem(gui, entry.getValue().clone());
        }

        ItemStack filler = Items.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 41; slot < 54; slot++) inv.setItem(slot, filler);

        if (mode == Mode.PLAYER) {
            inv.setItem(SLOT_BUTTON, Items.withDisplay(new ItemStack(Material.BARRIER),
                    Messages.raw("gui.reset-button"), Messages.rawList("gui.reset-button-lore")));
        } else {
            inv.setItem(SLOT_BUTTON, Items.withDisplay(new ItemStack(Material.WRITABLE_BOOK),
                    Messages.raw("gui.editor-info", "kit", kit.displayName()),
                    Messages.rawList("gui.editor-info-lore")));
        }
    }

    @Override
    public void click(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        int raw = event.getRawSlot();

        if (mode == Mode.ADMIN) {
            // Admins edit freely; only the filler/button row is off limits.
            if (raw >= 41 && raw < 54) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (raw == SLOT_BUTTON && event.getClickedInventory() == top) {
            plugin.playerData().clearLayout(player.getUniqueId(), kit.name());
            build();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
            Messages.send(player, "messages.layout-reset");
            return;
        }
        if (event.getClickedInventory() != top || raw < 0 || raw > 40) return;

        // Only whole-stack moves between content slots. No splitting, merging,
        // shift-clicks, hotbar swaps, drops or double-click collects — this keeps
        // the layout's items exactly equal to the kit's items.
        switch (event.getAction()) {
            case PICKUP_ALL, NOTHING -> event.setCancelled(false);
            case SWAP_WITH_CURSOR -> event.setCancelled(false);
            case PLACE_ALL -> {
                if (Items.isEmpty(event.getCurrentItem())) event.setCancelled(false);
            }
            default -> {
            }
        }
    }

    @Override
    public void drag(InventoryDragEvent event) {
        Set<Integer> raws = event.getRawSlots();
        if (mode == Mode.ADMIN) {
            for (int raw : raws) {
                if (raw >= 41 && raw < 54) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        // Player mode: a single-slot EVEN drag onto an empty content slot is just a place.
        if (event.getType() == DragType.EVEN && raws.size() == 1) {
            int raw = raws.iterator().next();
            if (raw >= 0 && raw <= 40 && Items.isEmpty(event.getView().getTopInventory().getItem(raw))) return;
        }
        event.setCancelled(true);
    }

    @Override
    public void closed(InventoryCloseEvent event) {
        if (mode == Mode.ADMIN) {
            kit.setContents(readContents(null));
            plugin.kits().saveAll();
            Messages.send(player, "messages.kit-saved", "kit", kit.displayName());
            return;
        }

        // If something is still on the cursor, count it toward the layout and void the
        // cursor so the copy is never handed to the player.
        ItemStack cursor = event.getView().getCursor();
        ItemStack loose = Items.isEmpty(cursor) ? null : cursor.clone();
        if (loose != null) event.getView().setCursor(null);
        Map<Integer, ItemStack> layout = readContents(loose);

        if (!Items.sameItems(layout.values(), kit.contents().values())) {
            Messages.send(player, "messages.layout-not-saved");
            return;
        }
        if (sameLayout(layout, kit.contents())) {
            plugin.playerData().clearLayout(player.getUniqueId(), kit.name());
        } else {
            plugin.playerData().setLayout(player.getUniqueId(), kit.name(), layout);
            Messages.send(player, "messages.layout-saved");
        }
    }

    /** Reads GUI content slots back into player-inventory slot keys; the loose cursor item (if any) fills the first free slot. */
    private Map<Integer, ItemStack> readContents(ItemStack loose) {
        Map<Integer, ItemStack> out = new HashMap<>();
        for (int gui = 0; gui <= 40; gui++) {
            ItemStack item = inv.getItem(gui);
            if (!Items.isEmpty(item)) out.put(mapSlot(gui), item.clone());
        }
        if (loose != null) {
            for (int slot = 0; slot < Kit.CONTENT_SLOTS; slot++) {
                if (!out.containsKey(slot)) {
                    out.put(slot, loose);
                    break;
                }
            }
        }
        return out;
    }

    private static boolean sameLayout(Map<Integer, ItemStack> a, Map<Integer, ItemStack> b) {
        if (!a.keySet().equals(b.keySet())) return false;
        for (Map.Entry<Integer, ItemStack> entry : a.entrySet()) {
            if (!entry.getValue().equals(b.get(entry.getKey()))) return false;
        }
        return true;
    }
}
