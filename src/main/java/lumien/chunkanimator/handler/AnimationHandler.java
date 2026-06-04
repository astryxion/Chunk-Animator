package lumien.chunkanimator.handler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.asm.MCPNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import org.lwjgl.opengl.GL11;

public class AnimationHandler {
   static Field glRenderList;
   /** Section has finished animating and must not restart. */
   static final long ANIMATION_FINISHED = Long.MIN_VALUE;
   private final HashMap<Integer, SectionAnimInfo> displayListTimestamps = new HashMap<Integer, SectionAnimInfo>();

   public void callLists(java.nio.IntBuffer glLists) {
      while (glLists.hasRemaining()) {
         int renderListID = glLists.get();
         double modY = getModY(renderListID, this.displayListTimestamps);

         if (modY != 0.0D) {
            GL11.glTranslated(0.0D, modY, 0.0D);
         }

         GL11.glCallList(renderListID);

         if (modY != 0.0D) {
            GL11.glTranslated(0.0D, -modY, 0.0D);
         }
      }
   }

   public void setPosition(WorldRenderer worldRenderer) {
      int renderID = -1;

      try {
         renderID = glRenderList.getInt(worldRenderer);
      } catch (Exception ignored) {
      }

      int posY = getWorldRendererPosY(worldRenderer);
      this.displayListTimestamps.put(renderID, new SectionAnimInfo(posY, -1L));
      this.displayListTimestamps.put(renderID + 1, new SectionAnimInfo(posY, -1L));
   }

   static double getModY(long sectionKey, Map<Long, SectionAnimInfo> sections) {
      if (!sections.containsKey(sectionKey)) {
         return 0.0D;
      }

      SectionAnimInfo info = sections.get(sectionKey);
      if (info.timeStamp == ANIMATION_FINISHED) {
         return 0.0D;
      }

      long time = info.timeStamp;
      if (time == -1L) {
         time = info.timeStamp = System.currentTimeMillis();
      }

      long timeDif = System.currentTimeMillis() - time;
      int animationDuration = getAnimationDuration();
      if (animationDuration <= 0 || timeDif >= (long)animationDuration) {
         info.timeStamp = ANIMATION_FINISHED;
         return 0.0D;
      }

      return calculateModY(info.blockY, timeDif, animationDuration);
   }

   static boolean shouldAnimate(long sectionKey, Map<Long, SectionAnimInfo> sections) {
      if (!sections.containsKey(sectionKey)) {
         return true;
      }

      SectionAnimInfo info = sections.get(sectionKey);
      if (info.timeStamp == ANIMATION_FINISHED) {
         return false;
      }

      if (info.timeStamp == -1L) {
         return true;
      }

      return System.currentTimeMillis() - info.timeStamp < (long)getAnimationDuration();
   }

   private double getModY(int renderListID, Map<Integer, SectionAnimInfo> sections) {
      if (!sections.containsKey(renderListID)) {
         return 0.0D;
      }

      SectionAnimInfo info = sections.get(renderListID);
      if (info.timeStamp == ANIMATION_FINISHED) {
         return 0.0D;
      }

      long time = info.timeStamp;
      if (time == -1L) {
         time = info.timeStamp = System.currentTimeMillis();
      }

      long timeDif = System.currentTimeMillis() - time;
      int animationDuration = getAnimationDuration();
      if (animationDuration <= 0 || timeDif >= (long)animationDuration) {
         info.timeStamp = ANIMATION_FINISHED;
         return 0.0D;
      }

      return calculateModY(info.blockY, timeDif, animationDuration);
   }

   static int getAnimationDuration() {
      if (ChunkAnimator.INSTANCE != null && ChunkAnimator.INSTANCE.config != null) {
         return ChunkAnimator.INSTANCE.config.getAnimationDuration();
      }

      return 1000;
   }

   static int getAnimationMode() {
      if (ChunkAnimator.INSTANCE != null && ChunkAnimator.INSTANCE.config != null) {
         return ChunkAnimator.INSTANCE.config.getMode();
      }

      return 0;
   }

   static double calculateModY(int blockY, long timeDif, int animationDuration) {
      int mode = getAnimationMode();
      if (mode == 2) {
         if (Minecraft.getMinecraft().theWorld != null && blockY < Minecraft.getMinecraft().theWorld.provider.getHorizon()) {
            mode = 0;
         } else {
            mode = 1;
         }
      }

      switch (mode) {
         case 0:
            return (double)(-blockY) + (double)blockY / (double)animationDuration * (double)timeDif;
         case 1:
            return 256.0D - (double)blockY - (256.0D - (double)blockY) / (double)animationDuration * (double)timeDif;
         default:
            return 0.0D;
      }
   }

   private static int getWorldRendererPosY(WorldRenderer worldRenderer) {
      try {
         Field posY = WorldRenderer.class.getDeclaredField(MCPNames.field("field_78920_d"));
         posY.setAccessible(true);
         return posY.getInt(worldRenderer);
      } catch (Exception e) {
         return 0;
      }
   }

   static {
      try {
         glRenderList = WorldRenderer.class.getDeclaredField(MCPNames.field("field_78942_y"));
         glRenderList.setAccessible(true);
      } catch (Exception ignored) {
      }
   }

   public static final class SectionAnimInfo {
      public final int blockY;
      public long timeStamp;

      public SectionAnimInfo(int blockY, long timeStamp) {
         this.blockY = blockY;
         this.timeStamp = timeStamp;
      }
   }
}
