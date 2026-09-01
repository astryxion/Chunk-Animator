package astryxion.chunkanimator.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Skips the vanilla {@code LevelRenderer} animation mixin when Sodium is present,
 * and only applies Sodium mixins when that renderer is actually loaded.
 */
public final class ChunkAnimatorMixinPlugin implements IMixinConfigPlugin {

    private static boolean hasClass(String className) {
        return ChunkAnimatorMixinPlugin.class.getClassLoader().getResource(className.replace('.', '/') + ".class") != null;
    }

    public static boolean isSodiumLoaded() {
        return hasClass("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        final boolean sodium = isSodiumLoaded();

        if (mixinClassName.endsWith(".LevelRendererMixin")) {
            return !sodium;
        }

        if (mixinClassName.contains(".compat.sodium.")) {
            return sodium;
        }

        return true;
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
