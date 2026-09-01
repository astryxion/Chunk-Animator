package astryxion.chunkanimator.compat.sodium.mixin;

import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import astryxion.chunkanimator.compat.sodium.ChunkAnimatorRegionOffset;
import astryxion.chunkanimator.compat.sodium.ChunkAnimatorShaderAccess;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
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
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class SodiumDefaultChunkRendererMixin {

    @Shadow
    @Final
    private SharedQuadIndexBuffer sharedIndexBuffer;

    @Unique
    private ChunkRenderList chunkanimator$currentRenderList;

    @Unique
    private MultiDrawBatch chunkanimator$animatingBatch;

    @Unique
    private final int[] chunkanimator$hidden = new int[RenderRegion.REGION_SIZE];

    @Unique
    private final byte[] chunkanimator$savedGeometry = new byte[RenderRegion.REGION_SIZE];

    @Unique
    private int chunkanimator$hiddenCount;

    @Unique
    private int chunkanimator$savedCount = -1;

    @Unique
    private boolean chunkanimator$compactThisList;

    @Shadow
    private GlTessellation prepareTessellation(CommandList commandList, RenderRegion.DeviceResources resources) {
        throw new AssertionError();
    }

    @Shadow
    private GlTessellation prepareIndexedTessellation(CommandList commandList, RenderRegion.DeviceResources resources) {
        throw new AssertionError();
    }

    @Invoker("addLocalIndexedDrawCommands")
    private static void chunkanimator$addLocalIndexedDrawCommands(MultiDrawBatch batch, long pMeshData, int mask) {
        throw new AssertionError();
    }

    @Invoker("addSharedIndexedDrawCommands")
    private static void chunkanimator$addSharedIndexedDrawCommands(MultiDrawBatch batch, long pMeshData, int mask) {
        throw new AssertionError();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Iterator;next()Ljava/lang/Object;"
            )
    )
    private Object chunkanimator$captureRenderList(Iterator<?> iterator) {
        this.chunkanimator$hiddenCount = 0;
        this.chunkanimator$compactThisList = false;
        Object next = iterator.next();
        if (next instanceof ChunkRenderList list) {
            this.chunkanimator$currentRenderList = list;
        }
        return next;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;getCachedBatch(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;"
            )
    )
    private MultiDrawBatch chunkanimator$invalidateCacheIfAnimating(RenderRegion region, TerrainRenderPass pass) {
        MultiDrawBatch batch = region.getCachedBatch(pass);
        // fillCommandBuffer appends from batch.size and never resets it. Setting
        // isFilled=false without clear() overflows Sodium's native command buffers.
        boolean animating = this.chunkanimator$currentRenderList != null
                && chunkanimator$hasAnimating(this.chunkanimator$currentRenderList, region);
        this.chunkanimator$compactThisList = animating;
        if (animating) {
            batch.clear();
        }
        return batch;
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;ZZ)V"
            )
    )
    private void chunkanimator$compactBeforeFill(CallbackInfo ci) {
        ChunkRenderList list = this.chunkanimator$currentRenderList;
        if (!this.chunkanimator$compactThisList || list == null) {
            return;
        }
        chunkanimator$compactAnimating(list, list.getRegion());
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;ZZ)V",
                    shift = At.Shift.AFTER
            )
    )
    private void chunkanimator$restoreAfterFill(CallbackInfo ci) {
        chunkanimator$restoreGeometryList();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;isEmpty()Z"
            )
    )
    private boolean chunkanimator$keepRegionIfAnimating(MultiDrawBatch batch) {
        return batch.isEmpty() && this.chunkanimator$hiddenCount == 0;
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlTessellation;Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void chunkanimator$drawAnimatingSections(
            ChunkRenderMatrices matrices,
            CommandList commandList,
            ChunkRenderListIterable renderLists,
            TerrainRenderPass renderPass,
            CameraTransform camera,
            FogParameters parameters,
            boolean indexedRenderingEnabled,
            GpuSampler terrainSampler,
            GpuBufferSlice uniformBuffer,
            GlTexelBuffer sectionTimeInfoTexture,
            CallbackInfo ci
    ) {
        int hiddenCount = this.chunkanimator$hiddenCount;
        if (hiddenCount == 0) {
            return;
        }

        ChunkRenderList renderList = this.chunkanimator$currentRenderList;
        if (renderList == null) {
            return;
        }

        RenderRegion region = renderList.getRegion();
        SectionRenderDataStorage storage = region.getStorage(renderPass);
        RenderRegion.DeviceResources resources = region.getResources();
        ChunkShaderInterface shader = ((ChunkAnimatorShaderAccess) this).chunkanimator$activeShader();
        if (storage == null || resources == null || shader == null) {
            this.chunkanimator$hiddenCount = 0;
            return;
        }

        boolean useIndexedTessellation = renderPass.isTranslucent() && indexedRenderingEnabled;
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;

        int regionChunkX = region.getChunkX();
        int regionChunkY = region.getChunkY();
        int regionChunkZ = region.getChunkZ();

        float baseX = (region.getOriginX() - camera.intX) - camera.fracX;
        float baseY = (region.getOriginY() - camera.intY) - camera.fracY;
        float baseZ = (region.getOriginZ() - camera.intZ) - camera.fracZ;

        MultiDrawBatch animatingBatch = this.chunkanimator$getAnimatingBatch();
        GlTessellation tessellation = useIndexedTessellation
                ? this.prepareIndexedTessellation(commandList, resources)
                : this.prepareTessellation(commandList, resources);
        boolean drewAny = false;

        for (int i = 0; i < hiddenCount; i++) {
            int sectionIndex = this.chunkanimator$hidden[i];
            int cx = regionChunkX + LocalSectionIndex.unpackX(sectionIndex);
            int cy = regionChunkY + LocalSectionIndex.unpackY(sectionIndex);
            int cz = regionChunkZ + LocalSectionIndex.unpackZ(sectionIndex);

            float[] delta = IndigoSectionAnimation.getRegionOffsetDelta(cx, cy, cz);
            if (delta == null) {
                continue;
            }

            long meshData = storage.getDataPointer(sectionIndex);
            int slices;
            if (useBlockFaceCulling) {
                slices = DefaultChunkRenderer.getVisibleFaces(camera.intX, camera.intY, camera.intZ, cx, cy, cz);
            } else {
                slices = ModelQuadFacing.ALL;
            }
            slices &= SectionRenderDataUnsafe.getSliceMask(meshData);
            if (slices == 0) {
                continue;
            }

            animatingBatch.clear();
            if (useIndexedTessellation && SectionRenderDataUnsafe.isLocalIndex(meshData)) {
                chunkanimator$addLocalIndexedDrawCommands(animatingBatch, meshData, slices);
            } else {
                chunkanimator$addSharedIndexedDrawCommands(animatingBatch, meshData, slices);
            }
            if (animatingBatch.isEmpty()) {
                continue;
            }

            if (!useIndexedTessellation) {
                this.sharedIndexBuffer.ensureCapacity(commandList, UInt32.downcast(animatingBatch.maxElementCount));
            }

            if (shader instanceof ChunkAnimatorRegionOffset regionOffset) {
                regionOffset.chunkanimator$setRegionOffset(baseX + delta[0], baseY + delta[1], baseZ + delta[2]);
            }

            try (DrawCommandList drawCmd = commandList.beginTessellating(tessellation)) {
                drawCmd.multiDrawElementsBaseVertex(animatingBatch, GlIndexType.UNSIGNED_INT);
            }

            drewAny = true;
        }

        if (drewAny) {
            shader.setRegionData(camera, region);
        }

        // Drop the static-only cache so the next frame refills with completed sections.
        region.getCachedBatch(renderPass).clear();
        this.chunkanimator$hiddenCount = 0;
    }

    @Unique
    private static boolean chunkanimator$hasAnimating(ChunkRenderList list, RenderRegion region) {
        SodiumChunkRenderListAccessor access = (SodiumChunkRenderListAccessor) list;
        int count = access.chunkanimator$getSectionsWithGeometryCount();
        byte[] sections = access.chunkanimator$getSectionsWithGeometry();
        int originX = region.getChunkX();
        int originY = region.getChunkY();
        int originZ = region.getChunkZ();
        for (int i = 0; i < count; i++) {
            int idx = sections[i] & 0xFF;
            if (IndigoSectionAnimation.isAnimating(
                    originX + LocalSectionIndex.unpackX(idx),
                    originY + LocalSectionIndex.unpackY(idx),
                    originZ + LocalSectionIndex.unpackZ(idx))) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void chunkanimator$compactAnimating(ChunkRenderList list, RenderRegion region) {
        this.chunkanimator$hiddenCount = 0;
        SodiumChunkRenderListAccessor access = (SodiumChunkRenderListAccessor) list;
        int count = access.chunkanimator$getSectionsWithGeometryCount();
        byte[] sections = access.chunkanimator$getSectionsWithGeometry();
        System.arraycopy(sections, 0, this.chunkanimator$savedGeometry, 0, count);
        this.chunkanimator$savedCount = count;

        int originX = region.getChunkX();
        int originY = region.getChunkY();
        int originZ = region.getChunkZ();
        int write = 0;
        for (int i = 0; i < count; i++) {
            int idx = sections[i] & 0xFF;
            if (IndigoSectionAnimation.isAnimating(
                    originX + LocalSectionIndex.unpackX(idx),
                    originY + LocalSectionIndex.unpackY(idx),
                    originZ + LocalSectionIndex.unpackZ(idx))) {
                this.chunkanimator$hidden[this.chunkanimator$hiddenCount++] = idx;
            } else {
                sections[write++] = (byte) idx;
            }
        }
        access.chunkanimator$setSectionsWithGeometryCount(write);
    }

    @Unique
    private void chunkanimator$restoreGeometryList() {
        ChunkRenderList list = this.chunkanimator$currentRenderList;
        if (list == null || this.chunkanimator$savedCount < 0) {
            return;
        }
        SodiumChunkRenderListAccessor access = (SodiumChunkRenderListAccessor) list;
        byte[] sections = access.chunkanimator$getSectionsWithGeometry();
        System.arraycopy(this.chunkanimator$savedGeometry, 0, sections, 0, this.chunkanimator$savedCount);
        access.chunkanimator$setSectionsWithGeometryCount(this.chunkanimator$savedCount);
        this.chunkanimator$savedCount = -1;
    }

    @Unique
    private MultiDrawBatch chunkanimator$getAnimatingBatch() {
        if (this.chunkanimator$animatingBatch == null) {
            this.chunkanimator$animatingBatch = new MultiDrawBatch((ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1);
        }
        return this.chunkanimator$animatingBatch;
    }
}
