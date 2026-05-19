package astryxion.chunkanimator.compat.indigo;

import astryxion.chunkanimator.ChunkAnimator;
import astryxion.chunkanimator.config.AnimationMode;
import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationContext;
import astryxion.chunkanimator.handler.AnimationHandler;
import astryxion.chunkanimator.handler.Float3Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Applies chunk animation offsets for Sodium / Embeddium section rendering.
 */
public final class IndigoSectionAnimation {

    private IndigoSectionAnimation() {
    }

    public static void onSectionBuilt(int sectionX, int sectionY, int sectionZ) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            return;
        }

        final var handler = ChunkAnimator.instance.animationHandler;
        if (handler == null) {
            return;
        }

        handler.setOriginForSection(SectionPos.asLong(sectionX, sectionY, sectionZ), sectionOrigin(sectionX, sectionY, sectionZ));
    }

    public static void onSectionRemoved(int sectionX, int sectionY, int sectionZ) {
        final var handler = ChunkAnimator.instance.animationHandler;
        if (handler == null) {
            return;
        }

        handler.clearSection(SectionPos.asLong(sectionX, sectionY, sectionZ));
    }

    /**
     * @return additional translation to apply to {@code u_RegionOffset}, or {@code null} if no animation is active
     */
    public static @Nullable float[] getRegionOffsetDelta(int sectionX, int sectionY, int sectionZ, double cameraX, double cameraY, double cameraZ) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            return null;
        }

        final var handler = ChunkAnimator.instance.animationHandler;
        if (handler == null) {
            return null;
        }

        final long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        final var animationData = handler.getAnimationData(sectionKey);
        if (animationData == null) {
            return null;
        }

        final BlockPos origin = sectionOrigin(sectionX, sectionY, sectionZ);
        final float baseX = (float) (origin.getX() - cameraX);
        final float baseY = (float) (origin.getY() - cameraY);
        final float baseZ = (float) (origin.getZ() - cameraZ);

        final int animationDuration = ChunkAnimatorConfig.ANIMATION_DURATION.get();
        long time = animationData.timeStamp;

        if (time == -1L) {
            time = System.currentTimeMillis();
            animationData.timeStamp = time;
            handler.prepareSectionAnimation(sectionKey, origin, animationData);
        }

        final long timeDif = System.currentTimeMillis() - time;

        if (timeDif >= animationDuration) {
            handler.clearSection(sectionKey);
            return null;
        }

        final float[] animated = new float[3];
        animated[0] = baseX;
        animated[1] = baseY;
        animated[2] = baseZ;

        final var mode = ChunkAnimatorConfig.MODE.get();
        final Float3Setter offsetSink = (ox, oy, oz) -> {
            animated[0] = ox;
            animated[1] = oy;
            animated[2] = oz;
        };

        mode.contextConsumer().accept(new AnimationContext(
                null,
                offsetSink,
                animated[0],
                animated[1],
                animated[2],
                animationData,
                origin,
                timeDif,
                AnimationContext.LevelContext.from(Objects.requireNonNull(Minecraft.getInstance().level))
        ));

        return new float[]{
                animated[0] - baseX,
                animated[1] - baseY,
                animated[2] - baseZ
        };
    }

    public static boolean isAnimating(int sectionX, int sectionY, int sectionZ) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            return false;
        }

        final var handler = ChunkAnimator.instance.animationHandler;
        if (handler == null) {
            return false;
        }

        final var animationData = handler.getAnimationData(SectionPos.asLong(sectionX, sectionY, sectionZ));
        if (animationData == null) {
            return false;
        }

        if (animationData.timeStamp == -1L) {
            return true;
        }

        return System.currentTimeMillis() - animationData.timeStamp < ChunkAnimatorConfig.ANIMATION_DURATION.get();
    }

    private static BlockPos sectionOrigin(int sectionX, int sectionY, int sectionZ) {
        return SectionPos.of(sectionX, sectionY, sectionZ).origin();
    }
}
