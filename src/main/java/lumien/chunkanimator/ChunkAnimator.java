package lumien.chunkanimator;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.handler.AnimationHandler;
import lumien.chunkanimator.handler.CeleritasHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

@Mod(
   modid = "ChunkAnimator",
   name = "Chunk Animator",
   version = "@VERSION@",
   guiFactory = "lumien.chunkanimator.config.ChunkAnimatorGuiFactory"
)
public class ChunkAnimator {
   @Instance("ChunkAnimator")
   public static ChunkAnimator INSTANCE;
   public AnimationHandler animationHandler;
   public ChunkAnimatorConfig config;

   @EventHandler
   public void preInit(FMLPreInitializationEvent event) {
      this.animationHandler = new AnimationHandler();
      FMLCommonHandler.instance().bus().register(this);
      MinecraftForge.EVENT_BUS.register(this);
      this.config = new ChunkAnimatorConfig();
      this.config.preInit(event);
   }

   @SubscribeEvent
   public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
      if (event.modID.equals("ChunkAnimator")) {
         this.config.syncConfig();
      }

   }

   @SubscribeEvent
   public void onWorldUnload(WorldEvent.Unload event) {
      if (event.world.isRemote) {
         CeleritasHandler.clearSections();
      }
   }
}
