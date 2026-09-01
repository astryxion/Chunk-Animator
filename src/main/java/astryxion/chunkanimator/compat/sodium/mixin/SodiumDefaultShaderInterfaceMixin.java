package astryxion.chunkanimator.compat.sodium.mixin;

import astryxion.chunkanimator.compat.sodium.ChunkAnimatorRegionOffset;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DefaultShaderInterface.class, remap = false)
public abstract class SodiumDefaultShaderInterfaceMixin implements ChunkAnimatorRegionOffset {

    @Shadow
    @Final
    private GlUniformFloat3v regionUniform;

    @Override
    public void chunkanimator$setRegionOffset(float x, float y, float z) {
        this.regionUniform.set(x, y, z);
    }
}
