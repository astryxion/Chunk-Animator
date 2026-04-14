package astryxion.chunkanimator.handler;

import astryxion.chunkanimator.ChunkAnimator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Handles {@link LevelEvent}s, updating {@link AnimationHandler} properties when the world
 * loads/unloads.
 *
 * @author Harley O'Connor
 */
public final class LevelEventHandler {

    private static final AnimationHandler HANDLER = ChunkAnimator.instance.animationHandler;

    @SubscribeEvent
    public void worldUnload (final LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel)) {
            return;
        }

        HANDLER.clear();
    }

}

