package astryxion.chunkanimator;

import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationHandler;
import astryxion.chunkanimator.handler.LevelEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Chunk Animator for NeoForge 26.2.
 *
 * @author Astryxion
 */
@Mod(value = ChunkAnimator.MOD_ID, dist = Dist.CLIENT)
public final class ChunkAnimator {

	public static final String MOD_ID = "chunkanimator";

	public static ChunkAnimator instance;

	public AnimationHandler animationHandler;

	public ChunkAnimator(IEventBus modBus, ModContainer modContainer) {
		instance = this;
		// Must exist before any RenderSection#setSectionNode (often before FMLClientSetupEvent), or mixins never register chunks.
		this.animationHandler = new AnimationHandler();

		modContainer.registerConfig(ModConfig.Type.CLIENT, ChunkAnimatorConfig.SPEC);

        modBus.addListener(this::setupClient);
	}

	/**
	 * Performs setup tasks that should only be run on the client. {@link net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection#setSectionNode(long)}
	 *
	 * @param event The {@link FMLClientSetupEvent} instance.
	 */
	private void setupClient(final FMLClientSetupEvent event) {
		NeoForge.EVENT_BUS.register(new LevelEventHandler());
	}

}

