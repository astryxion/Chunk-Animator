package lumien.chunkanimator.handler;

import java.nio.IntBuffer;
import lumien.chunkanimator.ChunkAnimator;
import net.minecraft.client.renderer.WorldRenderer;

public class AsmHandler {
   public static void callLists(IntBuffer glLists) {
      ChunkAnimator.INSTANCE.animationHandler.callLists(glLists);
   }

   public static void setPosition(WorldRenderer worldRenderer) {
      ChunkAnimator.INSTANCE.animationHandler.setPosition(worldRenderer);
   }
}
