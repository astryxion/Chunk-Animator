package astryxion.chunkanimator.compat.sodium;

import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;

/**
 * Skips sections that are currently animating so they are not drawn in the main multi-draw batch.
 */
public final class SodiumAnimatingSectionSkippingIterator implements ByteIterator {

    private final ByteIterator delegate;
    private final int regionChunkX;
    private final int regionChunkY;
    private final int regionChunkZ;
    private int next;
    private boolean hasNext;

    public SodiumAnimatingSectionSkippingIterator(ByteIterator delegate, RenderRegion region) {
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
