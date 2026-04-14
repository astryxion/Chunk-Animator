package astryxion.chunkanimator.mixin;

import com.mojang.blaze3d.shaders.Uniform;
import astryxion.chunkanimator.ChunkAnimator;
import astryxion.chunkanimator.handler.PreRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Harley O'Connor
 */
@Mixin(LevelRenderer.class)
public final class LevelRendererMixin {

	private static final ThreadLocal<ChunkRenderDispatcher.RenderChunk> CURRENT_RENDER_CHUNK = new ThreadLocal<>();

	@Redirect(method = "renderChunkLayer", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;getOrigin()Lnet/minecraft/core/BlockPos;"
	))
	private BlockPos captureRenderChunkOrigin(ChunkRenderDispatcher.RenderChunk renderChunk) {
		CURRENT_RENDER_CHUNK.set(renderChunk);
		return renderChunk.getOrigin();
	}

	@Redirect(method = "renderChunkLayer", at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/shaders/Uniform;set(FFF)V"
	))
	private void applyAnimatedOffset(Uniform chunkOffset, float x, float y, float z) {
		final ChunkRenderDispatcher.RenderChunk renderChunk = CURRENT_RENDER_CHUNK.get();
		if (renderChunk != null && ChunkAnimator.instance != null && ChunkAnimator.instance.animationHandler != null) {
			try {
				ChunkAnimator.instance.animationHandler.preRender(new PreRenderContext(renderChunk, chunkOffset, x, y, z));
			} finally {
				// Ensure we never accidentally apply the last chunk to later Uniform#set calls (e.g. final reset).
				CURRENT_RENDER_CHUNK.remove();
			}
			return;
		}

		chunkOffset.set(x, y, z);
	}
}

