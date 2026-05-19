package astryxion.chunkanimator.handler;

import astryxion.chunkanimator.config.AnimationMode;
import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Objects;
import java.util.WeakHashMap;

/**
 * This class handles setting up and rendering the animations.
 *
 * @author lumien231
 */
@OnlyIn(Dist.CLIENT)
public final class AnimationHandler {

    private final Minecraft mc = Minecraft.getInstance();
    private final WeakHashMap<SectionRenderDispatcher.RenderSection, AnimationData> timeStamps = new WeakHashMap<>();
    private final Long2ObjectMap<AnimationData> sectionTimeStamps = new Long2ObjectOpenHashMap<>();

    public void preRender(PreRenderContext context) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            context.offset().set(context.x(), context.y(), context.z());
            return;
        }

        final var animationData = timeStamps.get(context.renderSection());

        if (animationData == null) {
            context.offset().set(context.x(), context.y(), context.z());
            return;
        }

        final var mode = ChunkAnimatorConfig.MODE.get();
        final int animationDuration = ChunkAnimatorConfig.ANIMATION_DURATION.get();

        long time = animationData.timeStamp;

        // If preRender hasn't been called on this chunk yet, prepare to start the animation.
        if (time == -1L) {
            time = System.currentTimeMillis();
            animationData.timeStamp = time;
            mode.prepareConsumer().accept(context, animationData);
        }

        final long timeDif = System.currentTimeMillis() - time;

        if (timeDif < animationDuration) {
            ChunkAnimatorConfig.MODE.get().contextConsumer().accept(new AnimationContext(
                    context.renderSection(),
                    context.offset(),
                    context.x(),
                    context.y(),
                    context.z(),
                    animationData,
                    context.renderSection().getOrigin(),
                    timeDif,
                    AnimationContext.LevelContext.from(Objects.requireNonNull(this.mc.level))
            ));
        } else {
            context.offset().set(context.x(), context.y(), context.z());
            this.timeStamps.remove(context.renderSection());
        }
    }

    public AnimationData getAnimationData(long sectionPos) {
        return this.sectionTimeStamps.get(sectionPos);
    }

    public void clearSection(long sectionPos) {
        this.sectionTimeStamps.remove(sectionPos);
    }

    public void setOriginForSection(long sectionPos, BlockPos pos) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            this.sectionTimeStamps.remove(sectionPos);
            return;
        }

        if (this.mc.player == null) {
            return;
        }

        final BlockPos zeroedPlayerPos = getZeroedPlayerPos(this.mc.player);
        final BlockPos zeroedCenteredChunkPos = getZeroedCenteredChunkPos(pos);

        if (!ChunkAnimatorConfig.DISABLE_AROUND_PLAYER.get() || zeroedPlayerPos.distSqr(zeroedCenteredChunkPos) > (64 * 64)) {
            final var mode = ChunkAnimatorConfig.MODE.get();
            this.sectionTimeStamps.put(sectionPos, new AnimationData(-1L, mode == AnimationMode.HORIZONTAL_SLIDE || mode == AnimationMode.HORIZONTAL_SLIDE_ALTERNATE ?
                    getChunkFacing(zeroedPlayerPos.subtract(zeroedCenteredChunkPos)) : null));
        } else {
            this.sectionTimeStamps.remove(sectionPos);
        }
    }

    public void prepareSectionAnimation(long sectionPos, BlockPos origin, AnimationData animationData) {
        if (ChunkAnimatorConfig.MODE.get() == AnimationMode.HORIZONTAL_SLIDE_ALTERNATE) {
            animationData.chunkFacing = getChunkFacing(getZeroedPlayerPos(Objects.requireNonNull(this.mc.player))
                    .subtract(getZeroedCenteredChunkPos(origin)));
        }
    }

    public void setOrigin(final SectionRenderDispatcher.RenderSection renderSection, final BlockPos pos) {
        if (!ChunkAnimatorConfig.areAnimationsEnabled()) {
            this.timeStamps.remove(renderSection);
            return;
        }

        if (this.mc.player == null)
            return;

        final BlockPos zeroedPlayerPos = getZeroedPlayerPos(this.mc.player);
        final BlockPos zeroedCenteredChunkPos = getZeroedCenteredChunkPos(pos);

        if (!ChunkAnimatorConfig.DISABLE_AROUND_PLAYER.get() || zeroedPlayerPos.distSqr(zeroedCenteredChunkPos) > (64 * 64)) {
            final var mode = ChunkAnimatorConfig.MODE.get();
            timeStamps.put(renderSection, new AnimationData(-1L, mode == AnimationMode.HORIZONTAL_SLIDE || mode == AnimationMode.HORIZONTAL_SLIDE_ALTERNATE ?
                    getChunkFacing(zeroedPlayerPos.subtract(zeroedCenteredChunkPos)) : null));
        } else {
            timeStamps.remove(renderSection);
        }
    }

    /**
     * Gets the given player's position, setting their {@code y-coordinate} to {@code 0}.
     *
     * @param player The {@link LocalPlayer} instance.
     * @return The zeroed {@link BlockPos}.
     */
    public static BlockPos getZeroedPlayerPos(final LocalPlayer player) {
        final BlockPos playerPos = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        return playerPos.offset(0, -player.getBlockY(), 0);
    }

    /**
     * Gets the given {@link BlockPos} for the chunk, setting its {@code y-coordinate} to
     * {@code 0} and offsetting its {@code x} and {@code y-coordinate} to by {@code 8}.
     *
     * @param position The {@link BlockPos} of the chunk.
     * @return The zeroed, centered {@link BlockPos}.
     */
    public static BlockPos getZeroedCenteredChunkPos(final BlockPos position) {
        return position.offset(8, -position.getY(), 8);
    }

    /**
     * Gets the direction the chunk is facing based on the given {@link Vec3i}
     * from the relevant position to the chunk.
     *
     * @param dif The {@link Vec3i} distance from the relevant position to the chunk.
     * @return The {@link Direction} of the chunk relative to the {@code dif}.
     */
    public static Direction getChunkFacing(final Vec3i dif) {
        final int difX = Math.abs(dif.getX());
        final int difZ = Math.abs(dif.getZ());

        return difX > difZ ? dif.getX() > 0 ? Direction.EAST : Direction.WEST : dif.getZ() > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public void clear() {
        // These should be cleared by GC, but just in case.
        this.timeStamps.clear();
        this.sectionTimeStamps.clear();
    }

    public static class AnimationData {
        public long timeStamp;
        public Direction chunkFacing;

        public AnimationData(final long timeStamp, final Direction chunkFacing) {
            this.timeStamp = timeStamp;
            this.chunkFacing = chunkFacing;
        }
    }

}

