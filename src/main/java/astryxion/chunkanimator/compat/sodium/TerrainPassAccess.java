package astryxion.chunkanimator.compat.sodium;

import java.lang.reflect.Method;

/**
 * Embeddium/Xenon APIs that Rubidium 0.7 does not share, resolved reflectively.
 */
public final class TerrainPassAccess {

    private static final Method IS_SORTED = find(
            "me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass",
            "isSorted"
    );
    private static final Method GET_INDEX_OFFSET = find(
            "me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe",
            "getIndexOffset",
            long.class,
            int.class
    );

    private TerrainPassAccess() {}

    public static int indexPointerMask(Object renderPass) {
        if (IS_SORTED == null) {
            return 0;
        }
        try {
            return Boolean.TRUE.equals(IS_SORTED.invoke(renderPass)) ? 0xFFFFFFFF : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean hasIndexOffset() {
        return GET_INDEX_OFFSET != null;
    }

    public static long getIndexOffset(long pMeshData, int facing) {
        if (GET_INDEX_OFFSET == null) {
            return 0L;
        }
        try {
            return ((Number) GET_INDEX_OFFSET.invoke(null, pMeshData, facing)).longValue();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static Method find(String className, String method, Class<?>... params) {
        try {
            return Class.forName(className).getMethod(method, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

}
