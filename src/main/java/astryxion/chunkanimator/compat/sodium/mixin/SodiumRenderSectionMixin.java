package astryxion.chunkanimator.compat.sodium.mixin;

import astryxion.chunkanimator.compat.indigo.IndigoSectionAnimation;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderSection.class, remap = false)
public abstract class SodiumRenderSectionMixin {

    @Unique
    private boolean chunkanimator$seen;

    @Inject(method = "setInfo", at = @At("HEAD"), remap = false)
    private void chunkanimator$markBuilt(BuiltSectionInfo info, CallbackInfoReturnable<Integer> cir) {
        if (info == null || this.chunkanimator$seen) {
            return;
        }
        this.chunkanimator$seen = true;
        RenderSection self = (RenderSection) (Object) this;
        IndigoSectionAnimation.onSectionBuilt(self.getChunkX(), self.getChunkY(), self.getChunkZ());
    }

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    private void chunkanimator$clearAnimation(CallbackInfo ci) {
        RenderSection self = (RenderSection) (Object) this;
        IndigoSectionAnimation.onSectionRemoved(self.getChunkX(), self.getChunkY(), self.getChunkZ());
    }
}
