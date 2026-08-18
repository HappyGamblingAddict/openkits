package eu.fakemoon.altarkits.kit;

import eu.fakemoon.altarkits.util.Text;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Designs one PvP kit per Unstable SMP character, themed to that character.
 * Built as real ItemStacks at runtime (the server serializes them), so no
 * hand-authored item data is needed. Each kit is locked to kits.kit.&lt;id&gt;.
 *
 * Slot map (player-inventory indices): 0-8 hotbar, 9-35 storage,
 * 36 boots, 37 leggings, 38 chestplate, 39 helmet, 40 offhand.
 */
public final class UnstableKits {

    private UnstableKits() {
    }

    public static List<Kit> build() {
        List<Kit> kits = new ArrayList<>();
        int[] order = {3};

        add(kits, order, "parrotx2", "ParrotX2", Material.ELYTRA, c -> {
            c.put(0, sword("<aqua><bold>Parrot's Edge"));
            c.put(38, e(e(new ItemStack(Material.ELYTRA), Enchantment.UNBREAKING, 3), Enchantment.MENDING, 1));
            c.put(9, new ItemStack(Material.FIREWORK_ROCKET, 64));
            c.put(10, new ItemStack(Material.FIREWORK_ROCKET, 64));
            c.put(11, new ItemStack(Material.LIME_CONCRETE, 64));
        });

        add(kits, order, "wemmbu", "Wemmbu", Material.NETHERITE_SWORD, c -> {
            c.put(0, sword("<blue><bold>Host's Blade"));
            c.put(9, e(e(new ItemStack(Material.CROSSBOW), Enchantment.MULTISHOT, 1), Enchantment.QUICK_CHARGE, 3));
            c.put(10, new ItemStack(Material.ARROW, 32));
        });

        add(kits, order, "spoke", "Spoke", Material.NETHERITE_SWORD, c -> {
            c.put(0, sword("<gold><bold>Spoke's Blade"));
            c.put(9, potion(Material.POTION, PotionType.STRONG_STRENGTH));
        });

        add(kits, order, "flamefrags", "FlameFrags", Material.FIRE_CHARGE, c -> {
            c.put(0, e(sword("<red><bold>Inferno"), Enchantment.FIRE_ASPECT, 2));
            c.put(39, fireArmor(Material.NETHERITE_HELMET));
            c.put(38, fireArmor(Material.NETHERITE_CHESTPLATE));
            c.put(37, fireArmor(Material.NETHERITE_LEGGINGS));
            c.put(36, fireArmor(Material.NETHERITE_BOOTS));
            c.put(9, potion(Material.POTION, PotionType.LONG_FIRE_RESISTANCE));
            c.put(10, potion(Material.SPLASH_POTION, PotionType.LONG_FIRE_RESISTANCE));
            c.put(11, new ItemStack(Material.FLINT_AND_STEEL));
            c.put(12, new ItemStack(Material.TNT, 16));
        });

        add(kits, order, "manepear", "ManePear", Material.APPLE, c -> {
            c.put(0, axe("<green><bold>Pear Cleaver"));
            c.put(9, e(e(new ItemStack(Material.BOW), Enchantment.POWER, 5), Enchantment.PUNCH, 2));
            c.put(10, new ItemStack(Material.ARROW, 64));
            c.put(11, new ItemStack(Material.GOLDEN_APPLE, 16));
        });

        add(kits, order, "wifies", "Wifies", Material.SUGAR, c -> {
            c.put(0, sword("<light_purple><bold>Swift Strike"));
            c.put(9, potion(Material.POTION, PotionType.LONG_SWIFTNESS));
            c.put(10, potion(Material.POTION, PotionType.STRONG_LEAPING));
        });

        add(kits, order, "eggchan", "Eggchan", Material.EGG, c -> {
            c.put(0, e(sword("<yellow><bold>Yolk Breaker"), Enchantment.KNOCKBACK, 2));
            c.put(9, new ItemStack(Material.EGG, 16));
            c.put(10, new ItemStack(Material.SNOWBALL, 16));
        });

        add(kits, order, "jadenman", "Jaden_MAN", Material.NETHERITE_SWORD, c ->
                c.put(0, sword("<aqua><bold>Jaden's Blade")));

        add(kits, order, "v3n0m", "V3N0M", Material.SPIDER_EYE, c -> {
            c.put(0, sword("<green><bold>Venomfang"));
            c.put(9, potion(Material.SPLASH_POTION, PotionType.POISON));
            c.put(10, potion(Material.SPLASH_POTION, PotionType.POISON));
            c.put(11, e(new ItemStack(Material.BOW), Enchantment.POWER, 4));
            c.put(12, tipped(PotionType.POISON, 32));
        });

        add(kits, order, "salvationism", "Salvationism", Material.TOTEM_OF_UNDYING, c -> {
            c.put(0, e(sword("<gold><bold>Redeemer"), Enchantment.SMITE, 5));
            c.put(9, new ItemStack(Material.TOTEM_OF_UNDYING, 2));
            c.put(10, potion(Material.SPLASH_POTION, PotionType.STRONG_REGENERATION));
            c.put(11, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8));
        });

        add(kits, order, "zam", "Zam", Material.NETHERITE_SWORD, c -> {
            c.put(0, sword("<red><bold>Zam's Blade"));
            c.put(9, potion(Material.POTION, PotionType.STRONG_STRENGTH));
        });

        add(kits, order, "boosfer", "Boosfer", Material.NETHERITE_SWORD, c ->
                c.put(0, sword("<light_purple><bold>Boosfer's Blade")));

        add(kits, order, "clownpierce", "ClownPierce", Material.NETHERITE_SWORD, c -> {
            c.put(0, sword("<dark_purple><bold>Clown's Blade"));
            c.put(9, potion(Material.POTION, PotionType.STRONG_STRENGTH));
            c.put(10, new ItemStack(Material.PURPLE_CONCRETE, 64));
        });

        add(kits, order, "minutetech", "MinuteTech", Material.REDSTONE, c -> {
            c.put(0, sword("<aqua><bold>Circuit Breaker"));
            c.put(9, e(new ItemStack(Material.CROSSBOW), Enchantment.QUICK_CHARGE, 3));
            c.put(10, new ItemStack(Material.TNT, 16));
            c.put(11, new ItemStack(Material.FLINT_AND_STEEL));
            c.put(12, new ItemStack(Material.REDSTONE_BLOCK, 16));
        });

        add(kits, order, "shoebilly", "ShoeBilly_", Material.NETHERITE_SWORD, c ->
                c.put(0, sword("<gold><bold>Billy's Blade")));

        add(kits, order, "itzrealme", "ItzRealMe", Material.NETHERITE_SWORD, c ->
                c.put(0, sword("<red><bold>Real Blade")));

        add(kits, order, "theobaldthebird", "TheobaldTheBird", Material.FEATHER, c -> {
            c.put(0, sword("<aqua><bold>Skytalon"));
            c.put(38, e(e(new ItemStack(Material.ELYTRA), Enchantment.UNBREAKING, 3), Enchantment.MENDING, 1));
            c.put(9, new ItemStack(Material.FIREWORK_ROCKET, 64));
            c.put(10, e(e(new ItemStack(Material.BOW), Enchantment.POWER, 5), Enchantment.FLAME, 1));
            c.put(11, new ItemStack(Material.ARROW, 64));
        });

        add(kits, order, "flowtives", "Flowtives", Material.TRIDENT, c -> {
            c.put(0, e(e(e(new ItemStack(Material.TRIDENT), Enchantment.LOYALTY, 3),
                    Enchantment.IMPALING, 5), Enchantment.UNBREAKING, 3));
            c.put(36, e(waterBoots(), Enchantment.DEPTH_STRIDER, 3));
            c.put(9, new ItemStack(Material.WATER_BUCKET));
            c.put(10, new ItemStack(Material.WATER_BUCKET));
            c.put(11, potion(Material.POTION, PotionType.LONG_WATER_BREATHING));
        });

        add(kits, order, "sharpness", "Sharpness", Material.NETHERITE_SWORD, c -> {
            c.put(0, sword("<aqua><bold>Sharpness"));
            c.put(9, sword("<gray>Backup"));
        });

        add(kits, order, "swight", "Swight", Material.IRON_SWORD, c ->
                c.put(0, sword("<white><bold>Swight's Blade")));

        add(kits, order, "ferremc", "FerreMC", Material.NETHERITE_SWORD, c ->
                c.put(0, sword("<gold><bold>Ferre's Blade")));

        add(kits, order, "leow0ok", "Leow0ok", Material.GOLDEN_APPLE, c -> {
            c.put(0, e(sword("<gold><bold>Lion's Fang"), Enchantment.FIRE_ASPECT, 2));
            c.put(9, potion(Material.POTION, PotionType.STRONG_STRENGTH));
            c.put(10, potion(Material.POTION, PotionType.LONG_STRENGTH));
        });

        add(kits, order, "purpled", "Purpled", Material.RED_BED, c -> {
            c.put(0, sword("<dark_purple><bold>Purpled's Blade"));
            c.put(9, new ItemStack(Material.WHITE_BED, 8));
            c.put(10, new ItemStack(Material.ENDER_PEARL, 16));
            c.put(11, new ItemStack(Material.END_STONE, 64));
        });

        add(kits, order, "mapicc", "Mapicc", Material.END_CRYSTAL, c -> {
            c.put(0, sword("<red><bold>Mapicc's Blade"));
            c.put(9, new ItemStack(Material.END_CRYSTAL, 16));
            c.put(10, new ItemStack(Material.OBSIDIAN, 32));
            c.put(11, new ItemStack(Material.RESPAWN_ANCHOR, 4));
            c.put(12, new ItemStack(Material.GLOWSTONE, 16));
            c.put(40, new ItemStack(Material.TOTEM_OF_UNDYING));
            c.put(13, new ItemStack(Material.TOTEM_OF_UNDYING, 2));
        });

        add(kits, order, "venthatguy", "__venthatguy", Material.COAL, c -> {
            c.put(0, sword("<dark_gray><bold>Vent's Blade"));
            c.put(9, potion(Material.SPLASH_POTION, PotionType.LONG_INVISIBILITY));
        });

        add(kits, order, "lomedy", "Lomedy", Material.NETHERITE_SWORD, c ->
                c.put(0, sword("<aqua><bold>Lomedy's Blade")));

        add(kits, order, "lettucek", "LettuceK", Material.KELP, c -> {
            c.put(0, axe("<green><bold>Lettuce Cleaver"));
            c.put(9, e(new ItemStack(Material.BOW), Enchantment.POWER, 4));
            c.put(10, new ItemStack(Material.ARROW, 64));
        });

        add(kits, order, "nitenly", "Nitenly", Material.ENDER_EYE, c -> {
            c.put(0, sword("<dark_blue><bold>Nightfall"));
            c.put(2, new ItemStack(Material.ENDER_PEARL, 32));
            c.put(9, potion(Material.POTION, PotionType.LONG_NIGHT_VISION));
            c.put(10, potion(Material.SPLASH_POTION, PotionType.LONG_INVISIBILITY));
        });

        add(kits, order, "mistrul", "Mistrul", Material.SPLASH_POTION, c -> {
            c.put(0, sword("<blue><bold>Mist Blade"));
            c.put(9, potion(Material.SPLASH_POTION, PotionType.LONG_INVISIBILITY));
            c.put(10, potion(Material.SPLASH_POTION, PotionType.STRONG_HARMING));
            c.put(11, potion(Material.SPLASH_POTION, PotionType.SLOWNESS));
        });

        add(kits, order, "cytharam", "Cytharam", Material.SHIELD, c -> {
            c.put(0, sword("<gray><bold>Knight's Blade"));
            c.put(40, e(new ItemStack(Material.SHIELD), Enchantment.UNBREAKING, 3));
            c.put(9, potion(Material.SPLASH_POTION, PotionType.LONG_INVISIBILITY));
            c.put(10, potion(Material.SPLASH_POTION, PotionType.LONG_INVISIBILITY));
        });

        return kits;
    }

    // ------------------------------------------------------------ builders

    private static void add(List<Kit> kits, int[] order, String id, String display,
                            Material icon, Consumer<Map<Integer, ItemStack>> extra) {
        Kit kit = new Kit(id);
        kit.setDisplayName(display);
        kit.setPermission("kits.kit." + id);
        kit.setCooldownSeconds(0);
        kit.setOrder(order[0]++);
        Map<Integer, ItemStack> contents = base();
        extra.accept(contents);
        kit.setContents(contents);
        kit.setIcon(new ItemStack(icon));
        kits.add(kit);
    }

    /** Shared strong PvP staples; the weapon at slot 0 is overridden per character. */
    private static Map<Integer, ItemStack> base() {
        Map<Integer, ItemStack> c = new HashMap<>();
        c.put(39, prot(new ItemStack(Material.NETHERITE_HELMET)));
        c.put(38, prot(new ItemStack(Material.NETHERITE_CHESTPLATE)));
        c.put(37, prot(new ItemStack(Material.NETHERITE_LEGGINGS)));
        c.put(36, e(prot(new ItemStack(Material.NETHERITE_BOOTS)), Enchantment.FEATHER_FALLING, 4));
        c.put(40, new ItemStack(Material.TOTEM_OF_UNDYING));
        c.put(0, sword("<gray>Blade"));
        c.put(1, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8));
        c.put(2, new ItemStack(Material.ENDER_PEARL, 16));
        c.put(3, potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING));
        c.put(4, potion(Material.POTION, PotionType.STRONG_STRENGTH));
        c.put(5, potion(Material.POTION, PotionType.LONG_SWIFTNESS));
        c.put(6, new ItemStack(Material.GOLDEN_CARROT, 64));
        c.put(7, new ItemStack(Material.OBSIDIAN, 16));
        c.put(8, new ItemStack(Material.COBBLESTONE, 64));
        return c;
    }

    private static ItemStack e(ItemStack s, Enchantment ench, int level) {
        s.addUnsafeEnchantment(ench, level);
        return s;
    }

    private static ItemStack prot(ItemStack s) {
        e(s, Enchantment.PROTECTION, 4);
        e(s, Enchantment.UNBREAKING, 3);
        e(s, Enchantment.MENDING, 1);
        return s;
    }

    private static ItemStack fireArmor(Material m) {
        ItemStack s = new ItemStack(m);
        e(s, Enchantment.FIRE_PROTECTION, 4);
        e(s, Enchantment.UNBREAKING, 3);
        e(s, Enchantment.MENDING, 1);
        return s;
    }

    private static ItemStack waterBoots() {
        return prot(new ItemStack(Material.NETHERITE_BOOTS));
    }

    private static ItemStack sword(String nameMm) {
        ItemStack s = new ItemStack(Material.NETHERITE_SWORD);
        e(s, Enchantment.SHARPNESS, 5);
        e(s, Enchantment.UNBREAKING, 3);
        e(s, Enchantment.MENDING, 1);
        return rename(s, nameMm);
    }

    private static ItemStack axe(String nameMm) {
        ItemStack s = new ItemStack(Material.NETHERITE_AXE);
        e(s, Enchantment.SHARPNESS, 5);
        e(s, Enchantment.UNBREAKING, 3);
        e(s, Enchantment.MENDING, 1);
        return rename(s, nameMm);
    }

    private static ItemStack rename(ItemStack s, String nameMm) {
        s.editMeta(meta -> meta.displayName(Text.mm(nameMm)));
        return s;
    }

    private static ItemStack potion(Material mat, PotionType type) {
        ItemStack s = new ItemStack(mat);
        s.editMeta(PotionMeta.class, m -> m.setBasePotionType(type));
        return s;
    }

    private static ItemStack tipped(PotionType type, int amount) {
        ItemStack s = new ItemStack(Material.TIPPED_ARROW, amount);
        s.editMeta(PotionMeta.class, m -> m.setBasePotionType(type));
        return s;
    }
}
