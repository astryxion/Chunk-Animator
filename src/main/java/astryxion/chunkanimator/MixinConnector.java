package astryxion.chunkanimator;

import astryxion.chunkanimator.util.RendererPresence;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

/**
 * @author Harley O'Connor
 */
public final class MixinConnector implements IMixinConnector {

    @Override
    public void connect() {
        if (RendererPresence.isSodiumBased()) {
            Mixins.addConfiguration("chunkanimator.embeddium.mixins.json");
        } else if (this.isOptiFinePresent()) {
            Mixins.addConfiguration("chunkanimator.optifine.mixins.json");
        } else {
            Mixins.addConfiguration("chunkanimator.mixins.json");
        }
    }

    private boolean isOptiFinePresent() {
        try {
            Class.forName("optifine.OptiFineTransformationService");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
