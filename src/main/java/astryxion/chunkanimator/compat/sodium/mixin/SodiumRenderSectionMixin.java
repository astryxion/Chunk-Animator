package astryxion.chunkanimator.compat.sodium.mixin;

import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderSection.class, remap = false)
public abstract class SodiumRenderSectionMixin {

    @Shadow
    private boolean built;

    @Inject(method = "setInfo", at = @At("HEAD"), remap = false)
    private void chunkanimator$markBuilt(BuiltSectionInfo info, CallbackInfoReturnable<Boolean> cir) {
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
