package astryxion.chunkanimator.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;

/**
 * Access to the currently bound Sodium terrain shader.
 */
public interface ChunkAnimatorShaderAccess {

    ChunkShaderInterface chunkanimator$activeShader();
}
