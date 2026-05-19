package astryxion.chunkanimator.compat.embeddium.mixin;

import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSection.class, remap = false)
public abstract class EmbeddiumRenderSectionMixin {

    @Shadow
    private boolean built;

    @Inject(method = "setInfo", at = @At("HEAD"), remap = false)
    private void chunkanimator$markBuilt(BuiltSectionInfo info, CallbackInfo ci) {
        if (info == null || this.built) {
            return;
        }
        RenderSection self = (RenderSection) (Object) this;
        IndigoSectionAnimation.onSectionBuilt(self.getChunkX(), self.getChunkY(), self.getChunkZ());
    }

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    private void chunkanimator$clearAnimation(CallbackInfo ci) {
        RenderSection self = (RenderSection) (Object) this;
        IndigoSectionAnimation.onSectionRemoved(self.getChunkX(), self.getChunkY(), self.getChunkZ());
    }
}
