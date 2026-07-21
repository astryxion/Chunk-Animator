package astryxion.chunkanimator.mixin;

import astryxion.chunkanimator.ChunkAnimator;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Harley O'Connor
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public final class RenderSectionMixin {

	@Inject(
			method = "setSectionMesh",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V",
					shift = At.Shift.AFTER
			)
	)
	private void setSectionMesh(SectionMesh sectionMesh, CallbackInfoReturnable<SectionMesh> cir) {
		final SectionRenderDispatcher.RenderSection renderSection = (SectionRenderDispatcher.RenderSection) (Object) this;
		ChunkAnimator.instance.animationHandler.setOrigin(renderSection.getRenderOrigin());
	}
}

