package astryxion.chunkanimator.compat.sodium.mixin;

import astryxion.chunkanimator.compat.sodium.ChunkAnimatorShaderAccess;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
public abstract class SodiumShaderChunkRendererMixin implements ChunkAnimatorShaderAccess {

    @Shadow
    protected GlProgram<ChunkShaderInterface> activeProgram;

    @Override
    public ChunkShaderInterface chunkanimator$activeShader() {
        return this.activeProgram == null ? null : this.activeProgram.getInterface();
    }
}
