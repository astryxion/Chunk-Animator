package astryxion.chunkanimator.handler;

import astryxion.chunkanimator.config.AnimationMode;
import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks newly built Embeddium/Xenon sections and computes the same animation
 * offsets as {@link AnimationHandler}, using {@link ChunkAnimatorConfig}.
 *
 * @author Sxilverr
 */
public final class SectionAnimationTracker {

    private static final ConcurrentHashMap<Long, AnimationState> animations = new ConcurrentHashMap<>();

    private SectionAnimationTracker() {}

    public static void markBuilt(int chunkX, int chunkY, int chunkZ) {
        if (!isConfigReady() || ChunkAnimatorConfig.ANIMATION_DURATION.get() <= 0) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        BlockPos origin = new BlockPos(chunkX << 4, chunkY << 4, chunkZ << 4);
        BlockPos zeroedPlayerPos = AnimationHandler.getZeroedPlayerPos(player);
        BlockPos zeroedCenteredChunkPos = AnimationHandler.getZeroedCenteredChunkPos(origin);

        if (ChunkAnimatorConfig.DISABLE_AROUND_PLAYER.get()
                && zeroedPlayerPos.distSqr(zeroedCenteredChunkPos) <= (64 * 64)) {
            return;
        }

        Direction facing = null;
        AnimationMode mode = ChunkAnimatorConfig.MODE.get();
        if (mode == AnimationMode.HORIZONTAL_SLIDE) {
            facing = AnimationHandler.getChunkFacing(zeroedPlayerPos.subtract(zeroedCenteredChunkPos));
        }

        animations.putIfAbsent(SectionPos.asLong(chunkX, chunkY, chunkZ),
                new AnimationState(System.currentTimeMillis(), facing));
    }

    public static void clear(int chunkX, int chunkY, int chunkZ) {
        animations.remove(SectionPos.asLong(chunkX, chunkY, chunkZ));
    }

    public static void clearAll() {
        animations.clear();
    }

    public static boolean isAnimating(int chunkX, int chunkY, int chunkZ) {
        AnimationState state = animations.get(SectionPos.asLong(chunkX, chunkY, chunkZ));
        if (state == null || !isConfigReady()) {
            return false;
        }
        return System.currentTimeMillis() - state.startTime < ChunkAnimatorConfig.ANIMATION_DURATION.get();
    }

    public static float getOffsetX(int chunkX, int chunkY, int chunkZ) {
        return computeOffset(chunkX, chunkY, chunkZ).x;
    }

    public static float getOffsetY(int chunkX, int chunkY, int chunkZ) {
        return computeOffset(chunkX, chunkY, chunkZ).y;
    }

    public static float getOffsetZ(int chunkX, int chunkY, int chunkZ) {
        return computeOffset(chunkX, chunkY, chunkZ).z;
    }

    private static Offset computeOffset(int chunkX, int chunkY, int chunkZ) {
        long key = SectionPos.asLong(chunkX, chunkY, chunkZ);
        AnimationState state = animations.get(key);
        if (state == null || !isConfigReady()) {
            return Offset.ZERO;
        }

        int duration = ChunkAnimatorConfig.ANIMATION_DURATION.get();
        long elapsed = System.currentTimeMillis() - state.startTime;
        if (duration <= 0 || elapsed >= duration) {
            animations.remove(key);
            return Offset.ZERO;
        }

        float remaining = 1.0f - easedProgress(elapsed, duration);
        int originY = chunkY << 4;
        AnimationMode mode = ChunkAnimatorConfig.MODE.get();

        return switch (mode) {
            case BELOW -> new Offset(0.0f, -Math.abs(originY) * remaining, 0.0f);
            case ABOVE -> new Offset(0.0f, aboveDistance(originY) * remaining, 0.0f);
            case HYBRID -> originY < horizonHeight()
                    ? new Offset(0.0f, -Math.abs(originY) * remaining, 0.0f)
                    : new Offset(0.0f, aboveDistance(originY) * remaining, 0.0f);
            case HORIZONTAL_SLIDE, HORIZONTAL_SLIDE_ALTERNATE -> horizontalOffset(chunkX, chunkY, chunkZ, state, remaining);
        };
    }

    private static Offset horizontalOffset(int chunkX, int chunkY, int chunkZ, AnimationState state, float remaining) {
        if (state.chunkFacing == null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return Offset.ZERO;
            }
            BlockPos origin = new BlockPos(chunkX << 4, chunkY << 4, chunkZ << 4);
            state.chunkFacing = AnimationHandler.getChunkFacing(
                    AnimationHandler.getZeroedPlayerPos(player)
                            .subtract(AnimationHandler.getZeroedCenteredChunkPos(origin))
            );
        }
        var normal = state.chunkFacing.getNormal();
        float mod = -200.0f * remaining;
        return new Offset(normal.getX() * mod, 0.0f, normal.getZ() * mod);
    }

    private static float easedProgress(long elapsed, int duration) {
        return ChunkAnimatorConfig.EASING_FUNCTION.get().easeOutFunc()
                .apply((float) elapsed, 0.0f, 1.0f, (float) duration);
    }

    private static float aboveDistance(int originY) {
        return maxY() - Math.abs(originY);
    }

    private static int maxY() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 256;
        }
        return level.dimensionType().minY() + level.dimensionType().height();
    }

    private static double horizonHeight() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 63.0;
        }
        return level.getLevelData().getHorizonHeight(level);
    }

    private static boolean isConfigReady() {
        return ChunkAnimatorConfig.SPEC.isLoaded();
    }

    private static final class AnimationState {
        private final long startTime;
        private Direction chunkFacing;

        private AnimationState(long startTime, Direction chunkFacing) {
            this.startTime = startTime;
            this.chunkFacing = chunkFacing;
        }
    }

    private record Offset(float x, float y, float z) {
        private static final Offset ZERO = new Offset(0.0f, 0.0f, 0.0f);
    }

}
