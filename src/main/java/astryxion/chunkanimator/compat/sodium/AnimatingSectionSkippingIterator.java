package astryxion.chunkanimator.compat.sodium;

import astryxion.chunkanimator.handler.SectionAnimationTracker;
import me.jellysquid.mods.sodium.client.render.chunk.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;

/**
 * Skips sections that are currently animating so the fast batched draw can
 * still run for every static section.
 *
 * @author Sxilverr
 */
public final class AnimatingSectionSkippingIterator implements ByteIterator {

    private final ByteIterator delegate;
    private final int regionChunkX;
    private final int regionChunkY;
    private final int regionChunkZ;
    private int next;
    private boolean hasNext;

    public AnimatingSectionSkippingIterator(ByteIterator delegate, RenderRegion region) {
        this.delegate = delegate;
        this.regionChunkX = region.getChunkX();
        this.regionChunkY = region.getChunkY();
        this.regionChunkZ = region.getChunkZ();
        advance();
    }

    private void advance() {
        while (delegate.hasNext()) {
            int idx = delegate.nextByteAsInt();
            int cx = regionChunkX + LocalSectionIndex.unpackX(idx);
            int cy = regionChunkY + LocalSectionIndex.unpackY(idx);
            int cz = regionChunkZ + LocalSectionIndex.unpackZ(idx);
            if (!SectionAnimationTracker.isAnimating(cx, cy, cz)) {
                next = idx;
                hasNext = true;
                return;
            }
        }
        hasNext = false;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public int nextByteAsInt() {
        int result = next;
        advance();
        return result;
    }

}
