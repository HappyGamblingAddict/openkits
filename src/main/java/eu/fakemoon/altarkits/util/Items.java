package eu.fakemoon.altarkits.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class Items {

    private Items() {
    }

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    public static String toBase64(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack fromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (Exception ex) {
            return null;
        }
    }

    public static ItemStack named(Material material, String nameMm, String... loreMm) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Text.mm(nameMm));
            if (loreMm.length > 0) meta.lore(Arrays.stream(loreMm).map(Text::mm).toList());
        });
        return item;
    }

    /** Clones an item and applies a display name + lore (used for kit icons in menus). */
    public static ItemStack withDisplay(ItemStack base, String nameMm, List<String> loreMm) {
        ItemStack item = base.clone();
        item.setAmount(1);
        item.editMeta(meta -> {
            meta.displayName(Text.mm(nameMm));
            meta.lore(loreMm.stream().map(Text::mm).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    /**
     * Whether two collections contain exactly the same stacks, order-independent —
     * used to verify a player's custom layout still holds exactly the kit's items.
     * Compares semantically via ItemStack#equals: serialized bytes are NOT stable
     * across a save/load round-trip (component order can differ), so byte
     * comparison would wrongly reject layouts loaded from disk.
     */
    public static boolean sameItems(Collection<ItemStack> a, Collection<ItemStack> b) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : b) {
            if (!isEmpty(item)) remaining.add(item);
        }
        for (ItemStack item : a) {
            if (isEmpty(item)) continue;
            boolean matched = false;
            for (Iterator<ItemStack> it = remaining.iterator(); it.hasNext(); ) {
                if (item.equals(it.next())) {
                    it.remove();
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return remaining.isEmpty();
    }
}
