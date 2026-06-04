package lumien.chunkanimator.asm;

import lumien.chunkanimator.handler.CeleritasAsmHandler;
import lumien.chunkanimator.handler.CeleritasHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassTransformer implements IClassTransformer {
   private static final Logger LOGGER = LogManager.getLogger("ChunkAnimatorCore");
   private static final String ASM_HANDLER = "lumien/chunkanimator/handler/AsmHandler";
   private static final String CELERITAS_ASM_HANDLER = "lumien/chunkanimator/handler/CeleritasAsmHandler";
   private static final String DEFAULT_CHUNK_RENDERER = "org/embeddedt/embeddium/impl/render/chunk/DefaultChunkRenderer";

   public ClassTransformer() {
      LOGGER.log(Level.DEBUG, "Starting Class Transformation");
   }

   public byte[] transform(String name, String transformedName, byte[] basicClass) {
      if (transformedName.equals("net.minecraft.client.renderer.RenderList")) {
         return this.patchRenderList(basicClass);
      }

      if (transformedName.equals("net.minecraft.client.renderer.WorldRenderer")) {
         return this.patchWorldRenderer(basicClass);
      }

      if (transformedName.equals("org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer")) {
         return this.patchDefaultChunkRenderer(basicClass);
      }

      return basicClass;
   }

   private byte[] patchWorldRenderer(byte[] basicClass) {
      ClassNode classNode = new ClassNode();
      ClassReader classReader = new ClassReader(basicClass);
      classReader.accept(classNode, 0);
      LOGGER.log(Level.DEBUG, "Found WorldRenderer Class: " + classNode.name);
      MethodNode setPosition = null;

      for (MethodNode mn : classNode.methods) {
         if (mn.name.equals(MCPNames.method("func_78913_a"))) {
            setPosition = mn;
            break;
         }
      }

      if (setPosition != null) {
         LOGGER.log(Level.DEBUG, "- Found setPosition");

         for (int i = 0; i < setPosition.instructions.size(); ++i) {
            AbstractInsnNode ain = setPosition.instructions.get(i);
            if (ain instanceof MethodInsnNode) {
               MethodInsnNode min = (MethodInsnNode)ain;
               if (min.name.equals("glEndList")) {
                  InsnList toInsert = new InsnList();
                  toInsert.add(new VarInsnNode(Opcodes.ALOAD, 0));
                  toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HANDLER, "setPosition", "(Lnet/minecraft/client/renderer/WorldRenderer;)V", false));
                  setPosition.instructions.insertBefore(ain, toInsert);
                  LOGGER.log(Level.DEBUG, "- Patched Vanilla setPosition");
                  break;
               }
            } else if (ain instanceof FieldInsnNode) {
               FieldInsnNode fin = (FieldInsnNode)ain;
               if (fin.name.equals("needsBoxUpdate")) {
                  InsnList toInsert = new InsnList();
                  toInsert.add(new VarInsnNode(Opcodes.ALOAD, 0));
                  toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HANDLER, "setPosition", "(Lnet/minecraft/client/renderer/WorldRenderer;)V", false));
                  setPosition.instructions.insertBefore(ain, toInsert);
                  LOGGER.log(Level.DEBUG, "- Patched Optifine setPosition");
                  break;
               }
            }
         }
      }

      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      classNode.accept(writer);
      return writer.toByteArray();
   }

   private byte[] patchRenderList(byte[] basicClass) {
      ClassNode classNode = new ClassNode();
      ClassReader classReader = new ClassReader(basicClass);
      classReader.accept(classNode, 0);
      LOGGER.log(Level.DEBUG, "Found RenderList Class: " + classNode.name);
      MethodNode callLists = null;

      for (MethodNode mn : classNode.methods) {
         if (mn.name.equals(MCPNames.method("func_78419_a"))) {
            callLists = mn;
         }
      }

      if (callLists != null) {
         LOGGER.log(Level.DEBUG, "- Found callLists");

         for (int i = 0; i < callLists.instructions.size(); ++i) {
            AbstractInsnNode ain = callLists.instructions.get(i);
            if (ain instanceof MethodInsnNode) {
               MethodInsnNode min = (MethodInsnNode)ain;
               if (min.name.equals("glCallLists")) {
                  LOGGER.log(Level.DEBUG, "- Patched callLists");
                  InsnList toInsert = new InsnList();
                  toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HANDLER, "callLists", "(Ljava/nio/IntBuffer;)V", false));
                  callLists.instructions.insert(min, toInsert);
                  callLists.instructions.remove(min);
                  break;
               }
            }
         }
      }

      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      classNode.accept(writer);
      return writer.toByteArray();
   }

   private byte[] patchDefaultChunkRenderer(byte[] basicClass) {
      ClassNode classNode = new ClassNode();
      ClassReader classReader = new ClassReader(basicClass);
      classReader.accept(classNode, 0);
      LOGGER.info("Patching DefaultChunkRenderer for Angelica/Celeritas chunk animation");

      MethodNode renderMethod = null;
      for (MethodNode mn : classNode.methods) {
         if ("render".equals(mn.name) && mn.desc.startsWith("(Lorg/embeddedt/embeddium/impl/render/chunk/ChunkRenderMatrices;")) {
            renderMethod = mn;
            break;
         }
      }

      if (renderMethod == null) {
         LOGGER.warn("Could not find DefaultChunkRenderer.render to patch");
         ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
         classNode.accept(writer);
         return writer.toByteArray();
      }

      MethodInsnNode fillCommandBufferCall = null;
      MethodInsnNode executeBatchCall = null;

      for (AbstractInsnNode insn : renderMethod.instructions.toArray()) {
         if (!(insn instanceof MethodInsnNode)) {
            continue;
         }

         MethodInsnNode methodInsn = (MethodInsnNode)insn;
         if ("fillCommandBuffer".equals(methodInsn.name) && methodInsn.owner.endsWith("DefaultChunkRenderer")) {
            fillCommandBufferCall = methodInsn;
         } else if ("executeBatch".equals(methodInsn.name) && fillCommandBufferCall != null && executeBatchCall == null) {
            executeBatchCall = methodInsn;
         }
      }

      if (fillCommandBufferCall == null || executeBatchCall == null) {
         LOGGER.warn("Could not locate fillCommandBuffer/executeBatch in DefaultChunkRenderer.render");
         ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
         classNode.accept(writer);
         return writer.toByteArray();
      }

      AbstractInsnNode removeStart = findEmitterLoadStart(fillCommandBufferCall);
      AbstractInsnNode insertPoint = executeBatchCall.getNext();
      JumpInsnNode loopJump = insertPoint instanceof JumpInsnNode ? (JumpInsnNode)insertPoint : null;

      AbstractInsnNode current = removeStart;
      while (current != null) {
         AbstractInsnNode next = current.getNext();
         renderMethod.instructions.remove(current);
         if (current == executeBatchCall) {
            break;
         }
         current = next;
      }

      InsnList replacement = new InsnList();
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
      replacement.add(new FieldInsnNode(Opcodes.GETFIELD, DEFAULT_CHUNK_RENDERER, "emitter", "Lorg/embeddedt/embeddium/impl/render/chunk/multidraw/MultiDrawEmitter;"));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 2));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 14));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 15));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 13));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 5));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 4));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 8));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 9));
      replacement.add(new VarInsnNode(Opcodes.ILOAD, 7));
      replacement.add(new VarInsnNode(Opcodes.ALOAD, 6));
      replacement.add(new VarInsnNode(Opcodes.LLOAD, 11));
      replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CELERITAS_ASM_HANDLER, "renderRegionBatch", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ZLjava/lang/Object;J)V", false));

      if (loopJump != null) {
         replacement.add(new JumpInsnNode(Opcodes.GOTO, loopJump.label));
      }

      renderMethod.instructions.insertBefore(insertPoint, replacement);
      LOGGER.info("Applied Celeritas chunk animation hook to DefaultChunkRenderer.render");

      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      classNode.accept(writer);
      return writer.toByteArray();
   }

   private AbstractInsnNode findEmitterLoadStart(MethodInsnNode fillCommandBufferCall) {
      AbstractInsnNode node = fillCommandBufferCall.getPrevious();
      while (node != null) {
         if (node instanceof FieldInsnNode) {
            FieldInsnNode fieldInsn = (FieldInsnNode)node;
            if (fieldInsn.getOpcode() == Opcodes.GETFIELD && "emitter".equals(fieldInsn.name)) {
               AbstractInsnNode previous = node.getPrevious();
               if (previous instanceof VarInsnNode && ((VarInsnNode)previous).getOpcode() == Opcodes.ALOAD && ((VarInsnNode)previous).var == 0) {
                  return previous;
               }
            }
         }
         node = node.getPrevious();
      }

      return fillCommandBufferCall;
   }
}
