package astryxion.chunkanimator.client.gui;

import astryxion.chunkanimator.ChunkAnimator;
import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
/**
 * UI helpers for the Chunk Animator config screen.
 */
public final class ChunkAnimatorConfigUi {

    private static final Component ANIMATIONS_LABEL = Component.translatable("chunkanimator.configuration.animations");
    private static final int TOGGLE_BUTTON_WIDTH = 310;

    private ChunkAnimatorConfigUi() {
    }

    public static Button createAnimationsToggle() {
        return Button.builder(animationsToggleMessage(), ChunkAnimatorConfigUi::onAnimationsToggle)
                .width(TOGGLE_BUTTON_WIDTH)
                .build();
    }

    private static void onAnimationsToggle(Button button) {
        final boolean enabled = !ChunkAnimatorConfig.ANIMATIONS_ENABLED.get();
        ChunkAnimatorConfig.ANIMATIONS_ENABLED.set(enabled);
        ChunkAnimatorConfig.SPEC.save();
        button.setMessage(animationsToggleMessage());

        if (!enabled && ChunkAnimator.instance != null && ChunkAnimator.instance.animationHandler != null) {
            ChunkAnimator.instance.animationHandler.clear();
        }
    }

    private static MutableComponent animationsToggleMessage() {
        final boolean enabled = ChunkAnimatorConfig.ANIMATIONS_ENABLED.get();
        return ANIMATIONS_LABEL.copy()
                .append(" ")
                .append(enabled
                        ? Component.literal("On").withStyle(ChatFormatting.GREEN)
                        : Component.literal("Off").withStyle(ChatFormatting.RED));
    }

    public static boolean isAnimationsToggle(Button button) {
        return button.getMessage().getString().startsWith("Chunk Animations");
    }
}
