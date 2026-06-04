package lumien.chunkanimator.config;

import cpw.mods.fml.client.IModGuiFactory;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ChunkAnimatorGuiFactory implements IModGuiFactory {
   public void initialize(Minecraft minecraftInstance) {
   }

   public Class<? extends GuiScreen> mainConfigGuiClass() {
      return ChunkAnimatorConfigGui.class;
   }

   public Set<IModGuiFactory.RuntimeOptionCategoryElement> runtimeGuiCategories() {
      return null;
   }

   public IModGuiFactory.RuntimeOptionGuiHandler getHandlerFor(IModGuiFactory.RuntimeOptionCategoryElement element) {
      return null;
   }
}
