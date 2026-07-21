package astryxion.chunkanimator.mixin;

import astryxion.chunkanimator.ChunkAnimator;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Harley O'Connor
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public final class RenderSectionMixin {

    // 26.2: setOrigin(III) replaced by setSectionNode(J); still inject at reset() invoke
    @Inject(method = "setSectionNode", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;reset()V"
    ))
    public void setOrigin(long sectionNode, CallbackInfo ci) {
        final int x = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
        final int y = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
        final int z = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
        ChunkAnimator.instance.animationHandler.setOrigin(new BlockPos(x, y, z));
    }

}
