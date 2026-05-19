package astryxion.chunkanimator.compat.embeddium.mixin;

import astryxion.chunkanimator.compat.indigo.AnimatingSectionSkippingIterator;
import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.SharedQuadIndexBuffer;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderListIterable;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.util.iterator.ByteIterator;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class EmbeddiumDefaultChunkRendererMixin {

    @Shadow
    @Final
    private MultiDrawBatch batch;

    @Shadow
    @Final
    private SharedQuadIndexBuffer sharedIndexBuffer;

    @Shadow
    private boolean isIndexedPass;

    @Unique
    private MultiDrawBatch chunkanimator$animatingBatch;

    @Shadow
    private GlTessellation prepareTessellation(CommandList commandList, RenderRegion region) {
        throw new AssertionError();
    }

    @Redirect(
            method = "fillCommandBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/embeddedt/embeddium/impl/render/chunk/lists/ChunkRenderList;sectionsWithGeometryIterator(Z)Lorg/embeddedt/embeddium/impl/util/iterator/ByteIterator;"
            )
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
                    target = "Lorg/embeddedt/embeddium/impl/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lorg/embeddedt/embeddium/impl/gl/device/MultiDrawBatch;Lorg/embeddedt/embeddium/impl/render/chunk/region/RenderRegion;Lorg/embeddedt/embeddium/impl/render/chunk/data/SectionRenderDataStorage;Lorg/embeddedt/embeddium/impl/render/chunk/lists/ChunkRenderList;Lorg/embeddedt/embeddium/impl/render/viewport/CameraTransform;Lorg/embeddedt/embeddium/impl/render/chunk/terrain/TerrainRenderPass;Z)V",
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
            SectionRenderDataStorage storage
    ) {
        if (renderList == null || region == null || storage == null) {
            return;
        }

        ByteIterator sectionIter = renderList.sectionsWithGeometryIterator(renderPass.isReverseOrder());
        if (sectionIter == null) {
            return;
        }

        int regionChunkX = region.getChunkX();
        int regionChunkY = region.getChunkY();
        int regionChunkZ = region.getChunkZ();

        int indexPointerMask = renderPass.isSorted() ? 0xFFFFFFFF : 0;

        float baseX = (region.getOriginX() - camera.intX) - camera.fracX;
        float baseY = (region.getOriginY() - camera.intY) - camera.fracY;
        float baseZ = (region.getOriginZ() - camera.intZ) - camera.fracZ;

        MultiDrawBatch animatingBatch = this.chunkanimator$getAnimatingBatch();
        GlTessellation tessellation = null;
        boolean drewAny = false;

        while (sectionIter.hasNext()) {
            int sectionIndex = sectionIter.nextByteAsInt();
            int cx = regionChunkX + LocalSectionIndex.unpackX(sectionIndex);
            int cy = regionChunkY + LocalSectionIndex.unpackY(sectionIndex);
            int cz = regionChunkZ + LocalSectionIndex.unpackZ(sectionIndex);

            if (!IndigoSectionAnimation.isAnimating(cx, cy, cz)) {
                continue;
            }

            float[] delta = IndigoSectionAnimation.getRegionOffsetDelta(cx, cy, cz, camera.x, camera.y, camera.z);
            if (delta == null) {
                continue;
            }

            long meshData = storage.getDataPointer(sectionIndex);
            int sliceMask = SectionRenderDataUnsafe.getSliceMask(meshData);
            if (sliceMask == 0) {
                continue;
            }

            animatingBatch.clear();
            addAllFaces(animatingBatch, meshData, sliceMask, indexPointerMask);
            if (animatingBatch.isEmpty()) {
                continue;
            }

            if (!this.isIndexedPass) {
                this.sharedIndexBuffer.ensureCapacity(commandList, animatingBatch.getIndexBufferSize());
            }

            if (tessellation == null) {
                tessellation = this.prepareTessellation(commandList, region);
            }

            shader.setRegionOffset(baseX + delta[0], baseY + delta[1], baseZ + delta[2]);

            try (DrawCommandList drawCmd = commandList.beginTessellating(tessellation)) {
                drawCmd.multiDrawElementsBaseVertex(animatingBatch, GlIndexType.UNSIGNED_INT);
            }

            drewAny = true;
        }

        if (drewAny) {
            shader.setRegionOffset(baseX, baseY, baseZ);
        }
    }

    @Unique
    private MultiDrawBatch chunkanimator$getAnimatingBatch() {
        if (this.chunkanimator$animatingBatch == null) {
            this.chunkanimator$animatingBatch = new MultiDrawBatch((ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1);
        }
        return this.chunkanimator$animatingBatch;
    }

    private static void addAllFaces(MultiDrawBatch batch, long meshData, int mask, int indexPointerMask) {
        long pBaseVertex = batch.pBaseVertex;
        long pElementCount = batch.pElementCount;
        long pElementPointer = batch.pElementPointer;

        int size = batch.size;
        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            MemoryUtil.memPutInt(pBaseVertex + (size << 2), SectionRenderDataUnsafe.getVertexOffset(meshData, facing));
            MemoryUtil.memPutInt(pElementCount + (size << 2), SectionRenderDataUnsafe.getElementCount(meshData, facing));
            MemoryUtil.memPutAddress(pElementPointer + (size << 3), SectionRenderDataUnsafe.getIndexOffset(meshData, facing) & indexPointerMask);
            size += (mask >> facing) & 1;
        }
        batch.size = size;
    }
}
