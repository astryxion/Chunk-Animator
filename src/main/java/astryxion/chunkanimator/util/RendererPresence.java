package astryxion.chunkanimator.util;

import net.minecraftforge.fml.loading.LoadingModList;

/**
 * Detects Sodium-based terrain renderers (Embeddium / Xenon) that replace vanilla
 * chunk rendering and therefore need a different animation path.
 *
 * @author Sxilverr
 */
public final class RendererPresence {

    private static Boolean sodiumBased;

    private RendererPresence() {}

    public static boolean isSodiumBased() {
        if (sodiumBased == null) {
            sodiumBased = detect();
        }
        return sodiumBased;
    }

    public static String describe() {
        try {
            LoadingModList list = LoadingModList.get();
            if (list != null) {
                if (list.getModFileById("xenon") != null) {
                    return "Xenon";
                }
                if (list.getModFileById("embeddium") != null) {
                    return "Embeddium";
                }
                if (list.getModFileById("rubidium") != null) {
                    return "Rubidium";
                }
            }
        } catch (Throwable ignored) {
        }
        return isSodiumBased() ? "Embeddium/Xenon" : null;
    }

    private static boolean detect() {
        try {
            LoadingModList list = LoadingModList.get();
            if (list != null) {
                if (list.getModFileById("xenon") != null
                        || list.getModFileById("embeddium") != null
                        || list.getModFileById("rubidium") != null) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return RendererPresence.class.getClassLoader()
                    .getResource("me/jellysquid/mods/sodium/client/SodiumClientMod.class") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

}
