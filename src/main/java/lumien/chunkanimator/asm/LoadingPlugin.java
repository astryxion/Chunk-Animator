package lumien.chunkanimator.asm;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import java.util.Map;

@SortingIndex(10001)
public class LoadingPlugin implements IFMLLoadingPlugin {
   public static boolean IN_MCP = false;

   public String[] getASMTransformerClass() {
      return new String[]{ClassTransformer.class.getName()};
   }

   public String getModContainerClass() {
      return null;
   }

   public String getSetupClass() {
      return null;
   }

   public void injectData(Map<String, Object> data) {
      IN_MCP = !(Boolean)data.get("runtimeDeobfuscationEnabled");
   }

   public String getAccessTransformerClass() {
      return null;
   }
}
