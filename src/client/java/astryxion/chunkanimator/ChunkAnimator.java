package astryxion.chunkanimator;

import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Client-only Fabric entrypoint.
 *
 * @author lumien231
 */
public final class ChunkAnimator implements ClientModInitializer {

	public static final String MOD_ID = "chunkanimator";

	public static ChunkAnimator instance;

	public AnimationHandler animationHandler;
	private ClientLevel lastLevel;

	@Override
	public void onInitializeClient() {
		instance = this;

		ChunkAnimatorConfig.load();
		this.animationHandler = new AnimationHandler();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			final ClientLevel level = client.level;
			if (level != this.lastLevel) {
				this.lastLevel = level;
				this.animationHandler.clear();
			}
		});
	}
}

