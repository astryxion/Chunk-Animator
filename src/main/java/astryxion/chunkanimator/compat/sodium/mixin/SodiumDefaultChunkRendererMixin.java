package astryxion.chunkanimator.compat.sodium.mixin;

import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import astryxion.chunkanimator.compat.sodium.SodiumAnimatingSectionSkippingIterator;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.DrawCommandList;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
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
public abstract class SodiumDefaultChunkRendererMixin {

    @Shadow
    @Final
    private MultiDrawBatch batch;

    @Shadow
    @Final
    private SharedQuadIndexBuffer sharedIndexBuffer;

    @Unique
    private MultiDrawBatch chunkanimator$animatingBatch;

    @Shadow
    private GlTessellation prepareTessellation(CommandList commandList, RenderRegion region) {
        throw new AssertionError();
    }

    @Shadow
    private GlTessellation prepareIndexedTessellation(CommandList commandList, RenderRegion region) {
        throw new AssertionError();
    }

    @Redirect(
            method = "fillCommandBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;sectionsWithGeometryIterator(Z)Lnet/caffeinemc/mods/sodium/client/util/iterator/ByteIterator;"
            )
    )
    private static ByteIterator chunkanimator$filterAnimating(ChunkRenderList list, boolean reverse) {
        ByteIterator base = list.sectionsWithGeometryIterator(reverse);
        if (base == null) {
            return null;
        }
        return new SodiumAnimatingSectionSkippingIterator(base, list.getRegion());
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Z)V",
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
            boolean useIndexedTessellation,
            ChunkShaderInterface shader,
            Iterator<ChunkRenderList> iterator,
            ChunkRenderList renderList,
            RenderRegion region,
            SectionRenderDataStorage storage
    ) {
        if (renderList == null || region == null || storage == null || shader == null) {
            return;
        }

        ByteIterator sectionIter = renderList.sectionsWithGeometryIterator(renderPass.isTranslucent());
        if (sectionIter == null) {
            return;
        }

        int regionChunkX = region.getChunkX();
        int regionChunkY = region.getChunkY();
        int regionChunkZ = region.getChunkZ();

        boolean indexedPass = useIndexedTessellation;

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
            if (indexedPass) {
                addIndexedFaces(animatingBatch, meshData, sliceMask);
            } else {
                addNonIndexedFaces(animatingBatch, meshData, sliceMask);
            }
            if (animatingBatch.isEmpty()) {
                continue;
            }

            if (!indexedPass) {
                this.sharedIndexBuffer.ensureCapacity(commandList, animatingBatch.getIndexBufferSize());
            }

            if (tessellation == null) {
                tessellation = indexedPass
                        ? this.prepareIndexedTessellation(commandList, region)
                        : this.prepareTessellation(commandList, region);
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

    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    private static void addNonIndexedFaces(MultiDrawBatch batch, long meshData, int mask) {
        long pElementPointer = batch.pElementPointer;
        long pBaseVertex = batch.pBaseVertex;
        long pElementCount = batch.pElementCount;

        int size = batch.size;
        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            MemoryUtil.memPutInt(pBaseVertex + (size << 2), (int) SectionRenderDataUnsafe.getVertexOffset(meshData, facing));
            MemoryUtil.memPutInt(pElementCount + (size << 2), (int) SectionRenderDataUnsafe.getElementCount(meshData, facing));
            MemoryUtil.memPutAddress(pElementPointer + (size << Pointer.POINTER_SHIFT), 0);

            size += (mask >> facing) & 1;
        }
        batch.size = size;
    }

    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    private static void addIndexedFaces(MultiDrawBatch batch, long meshData, int mask) {
        long pElementPointer = batch.pElementPointer;
        long pBaseVertex = batch.pBaseVertex;
        long pElementCount = batch.pElementCount;

        int size = batch.size;
        long elementOffset = SectionRenderDataUnsafe.getBaseElement(meshData);

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            long vertexOffset = SectionRenderDataUnsafe.getVertexOffset(meshData, facing);
            long elementCount = SectionRenderDataUnsafe.getElementCount(meshData, facing);

            MemoryUtil.memPutInt(pBaseVertex + (size << 2), UInt32.uncheckedDowncast(vertexOffset));
            MemoryUtil.memPutInt(pElementCount + (size << 2), UInt32.uncheckedDowncast(elementCount));
            MemoryUtil.memPutAddress(pElementPointer + (size << Pointer.POINTER_SHIFT), elementOffset << 2);

            elementOffset += elementCount;
            size += (mask >> facing) & 1;
        }
        batch.size = size;
    }
}
