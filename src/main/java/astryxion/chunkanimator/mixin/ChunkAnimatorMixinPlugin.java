package astryxion.chunkanimator.mixin;

import astryxion.chunkanimator.util.RendererPresence;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies vanilla/OptiFine mixins unless Embeddium or Xenon is present, in which
 * case only the Sodium-based mixins are applied.
 *
 * @author Sxilverr
 */
public final class ChunkAnimatorMixinPlugin implements IMixinConfigPlugin {

    private static final String LOG_PREFIX = "[ChunkAnimator] ";

    @Override
    public void onLoad(String mixinPackage) {
        if (RendererPresence.isSodiumBased()) {
            System.out.println(LOG_PREFIX + RendererPresence.describe()
                    + " detected; using Sodium-based chunk animation mixins.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean sodiumMixin = mixinClassName.contains(".sodium.");
        return sodiumMixin == RendererPresence.isSodiumBased();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
