package lumien.chunkanimator.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CeleritasAsmHandler {
   private static final Logger LOGGER = LogManager.getLogger("ChunkAnimatorCore");

   public static void renderRegionBatch(Object renderer, Object emitter, Object commandList, Object region, Object storage, Object renderList, Object occlusionCamera, Object pass, Object shader, Object primitiveType, boolean useBlockFaceCulling, Object camera, long frameTime) {
      try {
         CeleritasHandler.renderRegionBatch(renderer, emitter, commandList, region, storage, renderList, occlusionCamera, pass, shader, primitiveType, useBlockFaceCulling, camera, frameTime);
      } catch (Throwable t) {
         LOGGER.error("Chunk Animator render hook failed, falling back to vanilla Celeritas draw for this region", t);

         try {
            CeleritasHandler.vanillaRegionDraw(renderer, emitter, commandList, region, storage, renderList, occlusionCamera, pass, shader, primitiveType, useBlockFaceCulling, camera, frameTime);
         } catch (Throwable fallbackError) {
            LOGGER.error("Chunk Animator vanilla fallback also failed for this region", fallbackError);
         }
      }
   }
}
