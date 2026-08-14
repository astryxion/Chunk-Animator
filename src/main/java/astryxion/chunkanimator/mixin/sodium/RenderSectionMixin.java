package astryxion.chunkanimator.mixin.sodium;

import astryxion.chunkanimator.handler.SectionAnimationTracker;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts and clears section animations when Embeddium/Xenon builds or deletes a section.
 *
 * @author Sxilverr
 */
@Mixin(value = RenderSection.class, remap = false)
public abstract class RenderSectionMixin {

    @Shadow
    private boolean built;

    @Inject(method = "setInfo", at = @At("HEAD"), remap = false)
    private void chunkanimator$markBuilt(BuiltSectionInfo info, CallbackInfo ci) {
        if (info == null || this.built) {
            return;
        }
        RenderSection self = (RenderSection) (Object) this;
        SectionAnimationTracker.markBuilt(self.getChunkX(), self.getChunkY(), self.getChunkZ());
    }

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    private void chunkanimator$clearAnimation(CallbackInfo ci) {
        RenderSection self = (RenderSection) (Object) this;
        SectionAnimationTracker.clear(self.getChunkX(), self.getChunkY(), self.getChunkZ());
    }

}
