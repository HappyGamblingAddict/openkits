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

import java.util.Map;

/** Read-only view of a kit's default contents. */
public final class PreviewGui implements KitsHolder {

    private static final int SLOT_BACK = 49;

    private final AltarKitsPlugin plugin;
    private final Inventory inv;

    private PreviewGui(AltarKitsPlugin plugin, Kit kit) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, 54,
                Messages.get("gui.preview-title", "kit", kit.displayName()));

        for (Map.Entry<Integer, ItemStack> entry : kit.contents().entrySet()) {
            int gui = LayoutEditorGui.mapSlot(entry.getKey());
            if (gui >= 0 && gui <= 40) inv.setItem(gui, entry.getValue().clone());
        }
        ItemStack filler = Items.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 41; slot < 54; slot++) inv.setItem(slot, filler);
        inv.setItem(SLOT_BACK, Items.named(Material.ARROW,
                Messages.raw("gui.back-button"), Messages.raw("gui.back-button-lore")));
    }

    public static void open(AltarKitsPlugin plugin, Player player, Kit kit) {
        player.openInventory(new PreviewGui(plugin, kit).inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (event.getRawSlot() == SLOT_BACK) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
            plugin.sync(() -> KitsGui.open(plugin, clicker));
        }
    }
}
