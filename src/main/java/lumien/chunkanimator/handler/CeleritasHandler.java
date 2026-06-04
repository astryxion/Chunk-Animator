package lumien.chunkanimator.handler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Applies chunk load animations when Angelica/Celeritas replaces vanilla display-list rendering.
 * Loaded via reflection only; no compile-time dependency on Angelica or Celeritas.
 */
public final class CeleritasHandler {
   private static final Logger LOGGER = LogManager.getLogger("ChunkAnimatorCore");
   /** Limit how many new section animations may start each frame to avoid render meltdown at high RD. */
   private static final int MAX_NEW_ANIMATIONS_PER_FRAME = 48;
   private static final HashMap<Long, AnimationHandler.SectionAnimInfo> SECTIONS = new HashMap<Long, AnimationHandler.SectionAnimInfo>();

   private static boolean available;
   private static boolean initAttempted;
   private static int newAnimationsThisFrame;
   private static long animationFrameId = -1L;

   private static Method fillCommandBuffer;
   private static Method prepareTessellation;
   private static Method getSharedIndexBuffer;
   private static Method setModelMatrixUniforms;
   private static Method getVisibleFaces;
   private static Method unpackX;
   private static Method unpackY;
   private static Method unpackZ;
   private static Method getSliceMask;
   private static Method emitterClear;
   private static Method emitterAddDrawCommands;
   private static Method emitterIsEmpty;
   private static Method emitterExecuteBatch;
   private static Method emitterGetIndexBufferSize;
   private static Method passIsSorted;
   private static Method passIsReverseOrder;
   private static Method listSectionsIterator;
   private static Method regionGetChunkX;
   private static Method regionGetChunkY;
   private static Method regionGetChunkZ;
   private static Method regionGetOriginX;
   private static Method regionGetOriginY;
   private static Method regionGetOriginZ;
   private static Method regionGetSectionLoadTimes;
   private static Method storageGetDataPointer;
   private static Method shaderSetRegionOffset;
   private static Method shaderSetSectionAges;
   private static Method configGetPrimitiveTypeForPass;
   private static Method ensureIndexCapacity;
   private static Method iteratorHasNext;
   private static Method iteratorNextByteAsInt;

   private CeleritasHandler() {
   }

   public static boolean isAvailable() {
      ensureReflection();
      return available;
   }

   public static void initReflection() {
      ensureReflection();
   }

   private static synchronized void ensureReflection() {
      if (initAttempted) {
         return;
      }

      initAttempted = true;

      try {
         Class<?> defaultRenderer = Class.forName("org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer", false, CeleritasHandler.class.getClassLoader());
         Class<?> emitterClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.multidraw.MultiDrawEmitter");
         Class<?> regionClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion");
         Class<?> storageClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage");
         Class<?> renderListClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList");
         Class<?> cameraClass = Class.forName("org.embeddedt.embeddium.impl.render.viewport.CameraTransform");
         Class<?> passClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass");
         Class<?> shaderClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface");
         Class<?> primitiveClass = Class.forName("org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType");
         Class<?> commandListClass = Class.forName("org.embeddedt.embeddium.impl.gl.device.CommandList");
         Class<?> tessClass = Class.forName("org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation");
         Class<?> localIndexClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex");
         Class<?> unsafeClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe");
         Class<?> configClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration");
         Class<?> sharedIndexClass = Class.forName("org.embeddedt.embeddium.impl.render.chunk.SharedQuadIndexBuffer");

         fillCommandBuffer = defaultRenderer.getDeclaredMethod("fillCommandBuffer", emitterClass, regionClass, storageClass, renderListClass, cameraClass, passClass, Boolean.TYPE);
         fillCommandBuffer.setAccessible(true);
         prepareTessellation = defaultRenderer.getDeclaredMethod("prepareTessellation", commandListClass, regionClass);
         prepareTessellation.setAccessible(true);
         getSharedIndexBuffer = defaultRenderer.getDeclaredMethod("getSharedIndexBuffer", Class.forName("org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType"), commandListClass);
         getSharedIndexBuffer.setAccessible(true);
         setModelMatrixUniforms = defaultRenderer.getDeclaredMethod("setModelMatrixUniforms", shaderClass, regionClass, cameraClass);
         setModelMatrixUniforms.setAccessible(true);
         getVisibleFaces = defaultRenderer.getDeclaredMethod("getVisibleFaces", Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
         getVisibleFaces.setAccessible(true);

         unpackX = localIndexClass.getDeclaredMethod("unpackX", Integer.TYPE);
         unpackY = localIndexClass.getDeclaredMethod("unpackY", Integer.TYPE);
         unpackZ = localIndexClass.getDeclaredMethod("unpackZ", Integer.TYPE);
         getSliceMask = unsafeClass.getDeclaredMethod("getSliceMask", Long.TYPE);

         emitterClear = emitterClass.getMethod("clear");
         emitterAddDrawCommands = emitterClass.getMethod("addDrawCommands", Long.TYPE, Integer.TYPE, Integer.TYPE);
         emitterIsEmpty = emitterClass.getMethod("isEmpty");
         emitterExecuteBatch = emitterClass.getMethod("executeBatch", commandListClass, tessClass, primitiveClass);
         emitterGetIndexBufferSize = emitterClass.getMethod("getIndexBufferSize");

         passIsSorted = passClass.getMethod("isSorted");
         passIsReverseOrder = passClass.getMethod("isReverseOrder");
         listSectionsIterator = renderListClass.getMethod("sectionsWithGeometryIterator", Boolean.TYPE);

         regionGetChunkX = regionClass.getMethod("getChunkX");
         regionGetChunkY = regionClass.getMethod("getChunkY");
         regionGetChunkZ = regionClass.getMethod("getChunkZ");
         regionGetOriginX = regionClass.getMethod("getOriginX");
         regionGetOriginY = regionClass.getMethod("getOriginY");
         regionGetOriginZ = regionClass.getMethod("getOriginZ");
         regionGetSectionLoadTimes = regionClass.getMethod("getSectionLoadTimes");

         storageGetDataPointer = storageClass.getMethod("getDataPointer", Integer.TYPE);
         shaderSetRegionOffset = shaderClass.getMethod("setRegionOffset", Float.TYPE, Float.TYPE, Float.TYPE);
         shaderSetSectionAges = shaderClass.getMethod("setSectionAges", Long.TYPE, long[].class);

         configGetPrimitiveTypeForPass = configClass.getMethod("getPrimitiveTypeForPass", passClass);
         ensureIndexCapacity = sharedIndexClass.getMethod("ensureCapacity", commandListClass, Integer.TYPE);

         iteratorHasNext = Class.forName("org.embeddedt.embeddium.impl.util.iterator.ByteIterator").getMethod("hasNext");
         iteratorNextByteAsInt = Class.forName("org.embeddedt.embeddium.impl.util.iterator.ByteIterator").getMethod("nextByteAsInt");

         available = true;
         LOGGER.info("Angelica/Celeritas chunk animation support enabled");
      } catch (Throwable t) {
         available = false;
         LOGGER.warn("Failed to initialize Angelica/Celeritas chunk animation support", t);
      }
   }

   public static void renderRegionBatch(Object renderer, Object emitter, Object commandList, Object region, Object storage, Object renderList, Object occlusionCamera, Object pass, Object shader, Object primitiveType, boolean useBlockFaceCulling, Object camera, long frameTime) throws Throwable {
      ensureReflection();
      if (!available) {
         vanillaRegionDraw(renderer, emitter, commandList, region, storage, renderList, occlusionCamera, pass, shader, primitiveType, useBlockFaceCulling, camera, frameTime);
         return;
      }

      beginAnimationFrame(frameTime);

      Object sectionIterator = listSectionsIterator.invoke(renderList, passIsReverseOrder.invoke(pass));
      if (sectionIterator == null) {
         return;
      }

      int regionChunkX = (Integer)regionGetChunkX.invoke(region);
      int regionChunkY = (Integer)regionGetChunkY.invoke(region);
      int regionChunkZ = (Integer)regionGetChunkZ.invoke(region);
      int indexPointerMask = (Boolean)passIsSorted.invoke(pass) ? -1 : 0;

      boolean needsCustomDraw = false;
      while ((Boolean)iteratorHasNext.invoke(sectionIterator)) {
         int sectionIndex = (Integer)iteratorNextByteAsInt.invoke(sectionIterator);
         SectionDrawInfo info = buildSectionDrawInfo(regionChunkX, regionChunkY, regionChunkZ, sectionIndex, storage, occlusionCamera, pass, useBlockFaceCulling);
         if (info == null) {
            continue;
         }

         if (info.modY != 0.0F) {
            needsCustomDraw = true;
            break;
         }
      }

      if (!needsCustomDraw) {
         vanillaRegionDraw(renderer, emitter, commandList, region, storage, renderList, occlusionCamera, pass, shader, primitiveType, useBlockFaceCulling, camera, frameTime);
         return;
      }

      emitterClear.invoke(emitter);
      sectionIterator = listSectionsIterator.invoke(renderList, passIsReverseOrder.invoke(pass));
      if (sectionIterator == null) {
         return;
      }

      Object tessellation = prepareTessellation.invoke(renderer, commandList, region);

      while ((Boolean)iteratorHasNext.invoke(sectionIterator)) {
         int sectionIndex = (Integer)iteratorNextByteAsInt.invoke(sectionIterator);
         SectionDrawInfo info = buildSectionDrawInfo(regionChunkX, regionChunkY, regionChunkZ, sectionIndex, storage, occlusionCamera, pass, useBlockFaceCulling);
         if (info == null) {
            continue;
         }

         if (info.modY == 0.0F) {
            emitterAddDrawCommands.invoke(emitter, info.meshData, info.visibleFaces, indexPointerMask);
         } else {
            if (!(Boolean)emitterIsEmpty.invoke(emitter)) {
               executeRegionDraw(renderer, emitter, commandList, region, pass, shader, primitiveType, camera, frameTime, tessellation, 0.0F);
               emitterClear.invoke(emitter);
            }

            emitterAddDrawCommands.invoke(emitter, info.meshData, info.visibleFaces, indexPointerMask);
            executeRegionDraw(renderer, emitter, commandList, region, pass, shader, primitiveType, camera, frameTime, tessellation, info.modY);
            emitterClear.invoke(emitter);
         }
      }

      if (!(Boolean)emitterIsEmpty.invoke(emitter)) {
         executeRegionDraw(renderer, emitter, commandList, region, pass, shader, primitiveType, camera, frameTime, tessellation, 0.0F);
      }
   }

   public static void vanillaRegionDraw(Object renderer, Object emitter, Object commandList, Object region, Object storage, Object renderList, Object occlusionCamera, Object pass, Object shader, Object primitiveType, boolean useBlockFaceCulling, Object camera, long frameTime) throws Throwable {
      fillCommandBuffer.invoke(null, emitter, region, storage, renderList, occlusionCamera, pass, useBlockFaceCulling);
      if ((Boolean)emitterIsEmpty.invoke(emitter)) {
         return;
      }

      Object tessellation = prepareTessellation.invoke(renderer, commandList, region);
      executeRegionDraw(renderer, emitter, commandList, region, pass, shader, primitiveType, camera, frameTime, tessellation, 0.0F);
   }

   private static void executeRegionDraw(Object renderer, Object emitter, Object commandList, Object region, Object pass, Object shader, Object primitiveType, Object camera, long frameTime, Object tessellation, float modY) throws Throwable {
      if (!(Boolean)passIsSorted.invoke(pass)) {
         Object config = getField(renderer, "renderPassConfiguration");
         Object primitiveTypeForPass = configGetPrimitiveTypeForPass.invoke(config, pass);
         Object sharedIndex = getSharedIndexBuffer.invoke(renderer, primitiveTypeForPass, commandList);
         ensureIndexCapacity.invoke(sharedIndex, commandList, (Integer)emitterGetIndexBufferSize.invoke(emitter));
      }

      setModelMatrixUniforms.invoke(null, shader, region, camera);

      if (modY != 0.0F) {
         shaderSetRegionOffset.invoke(shader, getRegionOffset(region, camera, 'x'), getRegionOffset(region, camera, 'y') + modY, getRegionOffset(region, camera, 'z'));
      }

      shaderSetSectionAges.invoke(shader, frameTime, (long[])regionGetSectionLoadTimes.invoke(region));
      emitterExecuteBatch.invoke(emitter, commandList, tessellation, primitiveType);

      if (modY != 0.0F) {
         setModelMatrixUniforms.invoke(null, shader, region, camera);
      }
   }

   private static SectionDrawInfo buildSectionDrawInfo(int regionChunkX, int regionChunkY, int regionChunkZ, int sectionIndex, Object storage, Object occlusionCamera, Object pass, boolean useBlockFaceCulling) throws Throwable {
      int chunkX = regionChunkX + (Integer)unpackX.invoke(null, sectionIndex);
      int chunkY = regionChunkY + (Integer)unpackY.invoke(null, sectionIndex);
      int chunkZ = regionChunkZ + (Integer)unpackZ.invoke(null, sectionIndex);
      long meshData = (Long)storageGetDataPointer.invoke(storage, sectionIndex);

      int visibleFaces = getVisibleFaceMask(occlusionCamera, pass, useBlockFaceCulling, chunkX, chunkY, chunkZ, meshData);
      if (visibleFaces == 0) {
         return null;
      }

      long sectionKey = packSection(chunkX, chunkY, chunkZ);
      float modY = registerSectionAndGetModY(sectionKey, chunkY << 4);
      return new SectionDrawInfo(meshData, visibleFaces, modY);
   }

   private static float registerSectionAndGetModY(long sectionKey, int blockY) {
      if (!SECTIONS.containsKey(sectionKey)) {
         if (newAnimationsThisFrame >= MAX_NEW_ANIMATIONS_PER_FRAME) {
            SECTIONS.put(sectionKey, new AnimationHandler.SectionAnimInfo(blockY, AnimationHandler.ANIMATION_FINISHED));
            return 0.0F;
         }

         SECTIONS.put(sectionKey, new AnimationHandler.SectionAnimInfo(blockY, -1L));
         newAnimationsThisFrame++;
      }

      return (float)AnimationHandler.getModY(sectionKey, SECTIONS);
   }

   private static void beginAnimationFrame(long frameTime) {
      if (frameTime != animationFrameId) {
         animationFrameId = frameTime;
         newAnimationsThisFrame = 0;
      }
   }

   public static void clearSections() {
      SECTIONS.clear();
      newAnimationsThisFrame = 0;
      animationFrameId = -1L;
   }

   private static int getVisibleFaceMask(Object occlusionCamera, Object pass, boolean useBlockFaceCulling, int chunkX, int chunkY, int chunkZ, long meshData) throws Throwable {
      int visibleFaces;
      if (useBlockFaceCulling && !(Boolean)passIsSorted.invoke(pass)) {
         visibleFaces = (Integer)getVisibleFaces.invoke(null, getCameraInt(occlusionCamera, 'x'), getCameraInt(occlusionCamera, 'y'), getCameraInt(occlusionCamera, 'z'), chunkX, chunkY, chunkZ);
      } else {
         visibleFaces = Integer.MAX_VALUE;
      }

      return visibleFaces & (Integer)getSliceMask.invoke(null, meshData);
   }

   private static float getRegionOffset(Object region, Object camera, char axis) throws Throwable {
      int origin;
      switch (axis) {
         case 'x':
            origin = (Integer)regionGetOriginX.invoke(region);
            break;
         case 'y':
            origin = (Integer)regionGetOriginY.invoke(region);
            break;
         case 'z':
            origin = (Integer)regionGetOriginZ.invoke(region);
            break;
         default:
            return 0.0F;
      }

      return (float)(origin - getCameraInt(camera, axis)) - getCameraFrac(camera, axis);
   }

   private static int getCameraInt(Object camera, char axis) throws Exception {
      Field field;
      switch (axis) {
         case 'x':
            field = camera.getClass().getField("intX");
            break;
         case 'y':
            field = camera.getClass().getField("intY");
            break;
         case 'z':
            field = camera.getClass().getField("intZ");
            break;
         default:
            return 0;
      }

      return field.getInt(camera);
   }

   private static float getCameraFrac(Object camera, char axis) throws Exception {
      Field field;
      switch (axis) {
         case 'x':
            field = camera.getClass().getField("fracX");
            break;
         case 'y':
            field = camera.getClass().getField("fracY");
            break;
         case 'z':
            field = camera.getClass().getField("fracZ");
            break;
         default:
            return 0.0F;
      }

      return field.getFloat(camera);
   }

   private static Object getField(Object target, String name) throws Exception {
      Class<?> type = target.getClass();

      while (type != null) {
         try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
         } catch (NoSuchFieldException ignored) {
            type = type.getSuperclass();
         }
      }

      throw new NoSuchFieldException(name);
   }

   public static long packSection(int chunkX, int chunkY, int chunkZ) {
      return ((long)chunkX & 4194303L) << 42 | ((long)chunkY & 1023L) << 32 | (long)chunkZ & 4294967295L;
   }

   private static final class SectionDrawInfo {
      private final long meshData;
      private final int visibleFaces;
      private final float modY;

      private SectionDrawInfo(long meshData, int visibleFaces, float modY) {
         this.meshData = meshData;
         this.visibleFaces = visibleFaces;
         this.modY = modY;
      }
   }
}
