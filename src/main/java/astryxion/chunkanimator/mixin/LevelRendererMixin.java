package astryxion.chunkanimator.mixin;

import astryxion.chunkanimator.ChunkAnimator;
import astryxion.chunkanimator.handler.AnimationHandler;
import astryxion.chunkanimator.handler.PreRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Consumer;

/**
 * @author Harley O'Connor
 */
@Mixin(LevelRenderer.class)
public final class LevelRendererMixin {

    // 26.2: addRecentlyCompiledSection removed; callback is now SectionRenderDispatcher onSectionMeshUpdate
    @ModifyArg(
            method = "invalidateCompiledGeometry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;<init>(Lnet/minecraft/TracingExecutor;Lnet/minecraft/client/renderer/RenderBuffers;Lnet/minecraft/client/renderer/chunk/SectionCompiler;Ljava/util/function/Consumer;)V"
            ),
            index = 3
    )
    private Consumer<SectionRenderDispatcher.RenderSection> chunkAnimator$onSectionCompiled(
            Consumer<SectionRenderDispatcher.RenderSection> onSectionMeshUpdate
    ) {
        return renderSection -> {
            onSectionMeshUpdate.accept(renderSection);
            if (ChunkAnimator.instance == null || ChunkAnimator.instance.animationHandler == null) {
                return;
            }
            ChunkAnimator.instance.animationHandler.setOrigin(renderSection.getRenderOrigin());
        };
    }

    @ModifyArgs(
            method = "prepareChunkRenders",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DynamicUniforms$ChunkSectionInfo;<init>(Lorg/joml/Matrix4fc;IIIFII)V"
            )
    )
    private void chunkAnimator$animateChunkSection(Args args) {
        if (ChunkAnimator.instance == null || ChunkAnimator.instance.animationHandler == null) {
            return;
        }
        final Matrix4fc modelView = args.get(0);
        final int x = args.get(1);
        final int y = args.get(2);
        final int z = args.get(3);

        final AnimationHandler.Offset offset = ChunkAnimator.instance.animationHandler.preRender(
                new PreRenderContext(new BlockPos(x, y, z))
        );

        if (offset.x() != 0 || offset.y() != 0 || offset.z() != 0) {
            final Matrix4f translated = new Matrix4f(modelView).translate(offset.x(), offset.y(), offset.z());
            args.set(0, (Matrix4fc) translated);
        }
    }
}
