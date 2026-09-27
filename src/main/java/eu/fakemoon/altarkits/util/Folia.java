package eu.fakemoon.altarkits.util;

/**
 * Folia detection. Kits needs no branching behaviour: the region schedulers from
 * Paper's API (see {@code AltarKitsPlugin#sync}) run on the main thread on Paper
 * and on the owning region thread on Folia, so the same calls are correct on both.
 * This is only used to report the active platform in the startup log.
 */
public final class Folia {

    private static final boolean FOLIA = isClassPresent("io.papermc.paper.threadedregions.RegionizedServer");

    private Folia() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
