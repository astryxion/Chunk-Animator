package astryxion.chunkanimator.compat.indigo;

import astryxion.chunkanimator.ChunkAnimator;
import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationHandler;
import astryxion.chunkanimator.handler.PreRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

/**
 * Applies chunk animation offsets for Sodium section rendering.
 */
public final class IndigoSectionAnimation {

    private IndigoSectionAnimation() {
    }

    public static void onSectionBuilt(int sectionX, int sectionY, int sectionZ) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            return;
        }
        final var handler = handler();
        if (handler == null) {
            return;
        }
        handler.setOrigin(sectionOrigin(sectionX, sectionY, sectionZ));
    }

    public static void onSectionRemoved(int sectionX, int sectionY, int sectionZ) {
        final var handler = handler();
        if (handler == null) {
            return;
        }
        handler.clearOrigin(sectionOrigin(sectionX, sectionY, sectionZ));
    }

    /**
     * @return additional translation to apply to {@code u_RegionOffset}, or {@code null} if no animation is active
     */
    public static @Nullable float[] getRegionOffsetDelta(int sectionX, int sectionY, int sectionZ) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            return null;
        }
        final var handler = handler();
        if (handler == null) {
            return null;
        }

        final AnimationHandler.Offset offset = handler.preRender(new PreRenderContext(sectionOrigin(sectionX, sectionY, sectionZ)));
        if (offset.x() == 0.0f && offset.y() == 0.0f && offset.z() == 0.0f) {
            return null;
        }

        return new float[]{offset.x(), offset.y(), offset.z()};
    }

    public static boolean isAnimating(int sectionX, int sectionY, int sectionZ) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            return false;
        }
        final var handler = handler();
        if (handler == null) {
            return false;
        }
        return handler.isAnimating(sectionOrigin(sectionX, sectionY, sectionZ));
    }

    private static AnimationHandler handler() {
        if (ChunkAnimator.instance == null) {
            return null;
        }
        return ChunkAnimator.instance.animationHandler;
    }

    private static BlockPos sectionOrigin(int sectionX, int sectionY, int sectionZ) {
        return SectionPos.of(sectionX, sectionY, sectionZ).origin();
    }
}
