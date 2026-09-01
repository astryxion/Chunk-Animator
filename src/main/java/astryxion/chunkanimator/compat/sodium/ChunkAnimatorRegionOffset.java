package astryxion.chunkanimator.compat.sodium;

/**
 * Extra uniform access for applying a per-section animation offset on Sodium's terrain shader.
 */
public interface ChunkAnimatorRegionOffset {

    void chunkanimator$setRegionOffset(float x, float y, float z);
}
