package astryxion.chunkanimator.client.gui;

import astryxion.chunkanimator.ChunkAnimator;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the Chunk Animator client config screen directly (skips the NeoForge config index).
 */
public final class ChunkAnimatorConfigScreens {

    private static final ConfigurationScreen.ConfigurationSectionScreen.Filter FILTER = ChunkAnimatorConfigScreens::filterEntry;

    private ChunkAnimatorConfigScreens() {
    }

    public static Screen create(ModContainer container, Screen parent) {
        for (ModConfig modConfig : ModConfigs.getConfigSet(ModConfig.Type.CLIENT)) {
            if (container.getModId().equals(modConfig.getModId())) {
                return new ConfigurationScreen.ConfigurationSectionScreen(
                        parent,
                        ModConfig.Type.CLIENT,
                        modConfig,
                        sectionTitle(),
                        FILTER
                );
            }
        }

        return new ConfigurationScreen(container, parent);
    }

    private static Component sectionTitle() {
        return Component.translatable("chunkanimator.configuration.title.client");
    }

    @Nullable
    private static ConfigurationScreen.ConfigurationSectionScreen.Element filterEntry(
            ConfigurationScreen.ConfigurationSectionScreen.Context context,
            String key,
            @Nullable ConfigurationScreen.ConfigurationSectionScreen.Element original
    ) {
        if (!ChunkAnimator.MOD_ID.equals(context.modId()) || !"animationsEnabled".equals(key)) {
            return original;
        }

        // Rendered as a centered synthetic row in ChunkAnimatorConfigurationSectionScreen.
        return new ConfigurationScreen.ConfigurationSectionScreen.Element(
                null,
                null,
                ChunkAnimatorConfigUi.createAnimationsToggle(),
                false
        );
    }
}
