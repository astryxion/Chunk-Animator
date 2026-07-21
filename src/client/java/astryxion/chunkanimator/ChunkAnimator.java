package astryxion.chunkanimator;

import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;

/**
 * Client-only Fabric entrypoint.
 *
 * @author lumien231
 */
public final class ChunkAnimator implements ClientModInitializer {

	public static final String MOD_ID = "chunkanimator";

	public static ChunkAnimator instance;

	public AnimationHandler animationHandler;

	@Override
	public void onInitializeClient() {
		instance = this;

		ChunkAnimatorConfig.load();
		this.animationHandler = new AnimationHandler();

		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> this.animationHandler.clear());
	}
}

