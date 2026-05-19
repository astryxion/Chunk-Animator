package astryxion.chunkanimator.compat.indigo;

import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.util.iterator.ByteIterator;

/**
 * Skips sections that are currently animating so they are not drawn in the main multi-draw batch.
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
            if (!IndigoSectionAnimation.isAnimating(cx, cy, cz)) {
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
