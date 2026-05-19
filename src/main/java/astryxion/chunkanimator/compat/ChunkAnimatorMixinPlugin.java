package astryxion.chunkanimator.compat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ChunkAnimatorMixinPlugin implements IMixinConfigPlugin {

    private static boolean hasClass(String className) {
        return ChunkAnimatorMixinPlugin.class.getClassLoader().getResource(className.replace('.', '/') + ".class") != null;
    }

    public static boolean isEmbeddiumLoaded() {
        return hasClass("org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer");
    }

    public static boolean isSodiumLoaded() {
        return hasClass("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
    }

    public static boolean usesIndigoRenderer() {
        return isEmbeddiumLoaded() || isSodiumLoaded();
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
        final boolean embeddium = isEmbeddiumLoaded();
        final boolean sodium = isSodiumLoaded();

        if (mixinClassName.endsWith(".LevelRendererMixin")) {
            return !usesIndigoRenderer();
        }

        if (mixinClassName.contains(".compat.embeddium.")) {
            return embeddium;
        }

        if (mixinClassName.contains(".compat.sodium.")) {
            return sodium && !embeddium;
        }

        if (mixinClassName.contains("ConfigurationSectionScreenMixin") || mixinClassName.contains("OptionsSubScreenAccessor")) {
            return FMLEnvironment.dist == Dist.CLIENT;
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
