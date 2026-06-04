package lumien.chunkanimator.config;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import java.util.ArrayList;
import java.util.List;
import lumien.chunkanimator.ChunkAnimator;
import net.minecraft.client.gui.GuiScreen;

public class ChunkAnimatorConfigGui extends GuiConfig {
   public ChunkAnimatorConfigGui(GuiScreen parent) {
      super(parent, getConfigElements(), "ChunkAnimator", false, false, GuiConfig.getAbridgedConfigPath(ChunkAnimator.INSTANCE.config.getString()));
   }

   private static List<IConfigElement> getConfigElements() {
      List<IConfigElement> list = new ArrayList();
      list.addAll(ChunkAnimator.INSTANCE.config.getConfigElements());
      return list;
   }
}
