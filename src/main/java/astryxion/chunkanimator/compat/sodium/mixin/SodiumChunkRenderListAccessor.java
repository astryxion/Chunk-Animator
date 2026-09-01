package astryxion.chunkanimator.compat.sodium.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChunkRenderList.class, remap = false)
public interface SodiumChunkRenderListAccessor {

    @Accessor("sectionsWithGeometry")
    byte[] chunkanimator$getSectionsWithGeometry();

    @Accessor("sectionsWithGeometryCount")
    int chunkanimator$getSectionsWithGeometryCount();

    @Accessor("sectionsWithGeometryCount")
    void chunkanimator$setSectionsWithGeometryCount(int count);
}
