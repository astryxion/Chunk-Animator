package lumien.chunkanimator.config;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.util.List;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ChunkAnimatorConfig {
   Property propertyMode;
   int mode;
   Property propertyAnimationDuration;
   int animationDuration;
   Configuration config;

   public void preInit(FMLPreInitializationEvent event) {
      this.config = new Configuration(event.getSuggestedConfigurationFile());
      this.config.load();
      this.propertyMode = this.config.get("settings", "Mode", 0, "How should the chunks be animated?\n 0: Chunks always appear from below\n 1: Chunks always appear from above\n 2: Chunks appear from below if they are lower than the Horizon and from above if they are higher than the Horizon");
      this.propertyAnimationDuration = this.config.get("settings", "AnimationDuration", 1000, "How long should the animation last? (In milliseconds)");
      this.syncConfig();
   }

   public void syncConfig() {
      this.mode = this.propertyMode.getInt();
      this.animationDuration = this.propertyAnimationDuration.getInt();
      if (this.config.hasChanged()) {
         this.config.save();
      }

   }

   public int getMode() {
      return this.mode;
   }

   public int getAnimationDuration() {
      return this.animationDuration;
   }

   public String getString() {
      return this.config.toString();
   }

   public List<IConfigElement> getConfigElements() {
      return (new ConfigElement(this.config.getCategory("settings"))).getChildElements();
   }
}
