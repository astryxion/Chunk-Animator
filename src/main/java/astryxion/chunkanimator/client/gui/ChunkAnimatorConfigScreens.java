package astryxion.chunkanimator.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * NeoForge generic config UI. With a single client spec this opens the options
 * list directly instead of an empty type-index screen.
 */
public final class ChunkAnimatorConfigScreens {

    private ChunkAnimatorConfigScreens() {
    }

    public static Screen create(ModContainer container, Screen parent) {
        return new ConfigurationScreen(container, parent);
    }
}
