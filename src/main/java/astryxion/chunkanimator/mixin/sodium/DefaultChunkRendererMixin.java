package astryxion.chunkanimator.mixin.sodium;

import astryxion.chunkanimator.compat.sodium.AnimatingSectionSkippingIterator;
import astryxion.chunkanimator.compat.sodium.TerrainPassAccess;
import astryxion.chunkanimator.handler.SectionAnimationTracker;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

/**
 * Draws animating sections with a per-section offset after the batched region draw.
 *
 * @author Sxilverr
 */
@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererMixin {

    @Shadow(remap = false)
    private MultiDrawBatch batch;

    @Redirect(
            method = "fillCommandBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/lists/ChunkRenderList;sectionsWithGeometryIterator(Z)Lme/jellysquid/mods/sodium/client/util/iterator/ByteIterator;"
            ),
            remap = false
    )
    private static ByteIterator chunkanimator$filterAnimating(ChunkRenderList list, boolean reverse) {
        ByteIterator base = list.sectionsWithGeometryIterator(reverse);
        if (base == null) {
            return null;
        }
        return new AnimatingSectionSkippingIterator(base, list.getRegion());
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Lme/jellysquid/mods/sodium/client/gl/tessellation/GlTessellation;Lme/jellysquid/mods/sodium/client/gl/device/MultiDrawBatch;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            remap = false
    )
    private void chunkanimator$drawAnimatingSections(
            ChunkRenderMatrices matrices,
            CommandList commandList,
            ChunkRenderListIterable renderLists,
            TerrainRenderPass renderPass,
            CameraTransform camera,
            CallbackInfo ci,
            boolean useBlockFaceCulling,
            ChunkShaderInterface shader,
            Iterator<ChunkRenderList> iterator,
            ChunkRenderList renderList,
            RenderRegion region,
            SectionRenderDataStorage storage,
            GlTessellation tessellation
    ) {
        ByteIterator sectionIter = renderList.sectionsWithGeometryIterator(renderPass.isReverseOrder());
        if (sectionIter == null) {
            return;
        }

        int regionChunkX = region.getChunkX();
        int regionChunkY = region.getChunkY();
        int regionChunkZ = region.getChunkZ();

        int indexPointerMask = TerrainPassAccess.indexPointerMask(renderPass);

        float baseX = (region.getOriginX() - camera.intX) - camera.fracX;
        float baseY = (region.getOriginY() - camera.intY) - camera.fracY;
        float baseZ = (region.getOriginZ() - camera.intZ) - camera.fracZ;

        while (sectionIter.hasNext()) {
            int sectionIndex = sectionIter.nextByteAsInt();
            int cx = regionChunkX + LocalSectionIndex.unpackX(sectionIndex);
            int cy = regionChunkY + LocalSectionIndex.unpackY(sectionIndex);
            int cz = regionChunkZ + LocalSectionIndex.unpackZ(sectionIndex);

            float offsetX = SectionAnimationTracker.getOffsetX(cx, cy, cz);
            float offsetY = SectionAnimationTracker.getOffsetY(cx, cy, cz);
            float offsetZ = SectionAnimationTracker.getOffsetZ(cx, cy, cz);
            if (offsetX == 0.0f && offsetY == 0.0f && offsetZ == 0.0f) {
                continue;
            }

            long pMeshData = storage.getDataPointer(sectionIndex);
            int sliceMask = SectionRenderDataUnsafe.getSliceMask(pMeshData);
            if (sliceMask == 0) {
                continue;
            }

            this.batch.clear();
            addAllFaces(this.batch, pMeshData, sliceMask, indexPointerMask);
            if (this.batch.size == 0) {
                continue;
            }

            shader.setRegionOffset(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);

            try (DrawCommandList drawCmd = commandList.beginTessellating(tessellation)) {
                drawCmd.multiDrawElementsBaseVertex(this.batch, GlIndexType.UNSIGNED_INT);
            }
        }

        shader.setRegionOffset(baseX, baseY, baseZ);
    }

    private static void addAllFaces(MultiDrawBatch batch, long pMeshData, int mask, int indexPointerMask) {
        long pBaseVertex = batch.pBaseVertex;
        long pElementCount = batch.pElementCount;
        long pElementPointer = batch.pElementPointer;

        int size = batch.size;
        boolean writeIndexOffset = TerrainPassAccess.hasIndexOffset();
        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            MemoryUtil.memPutInt(pBaseVertex + (size << 2), SectionRenderDataUnsafe.getVertexOffset(pMeshData, facing));
            MemoryUtil.memPutInt(pElementCount + (size << 2), SectionRenderDataUnsafe.getElementCount(pMeshData, facing));
            if (writeIndexOffset) {
                MemoryUtil.memPutAddress(pElementPointer + (size << 3), TerrainPassAccess.getIndexOffset(pMeshData, facing) & indexPointerMask);
            }
            size += (mask >> facing) & 1;
        }
        batch.size = size;
    }

}
