package astryxion.chunkanimator.mixin.client;

import astryxion.chunkanimator.ChunkAnimator;
import astryxion.chunkanimator.client.gui.ChunkAnimatorConfigUi;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ConfigurationScreen.ConfigurationSectionScreen.class, remap = false)
public class ConfigurationSectionScreenMixin {

    @Shadow
    protected ConfigurationScreen.ConfigurationSectionScreen.Context context;

    @Inject(method = "rebuild", at = @At("RETURN"), remap = false)
    private void chunkanimator$centerAnimationsToggle(CallbackInfoReturnable<ConfigurationScreen.ConfigurationSectionScreen> cir) {
        if (!ChunkAnimator.MOD_ID.equals(this.context.modId())) {
            return;
        }

        final OptionsList optionsList = ((OptionsSubScreenAccessor) (Object) this).chunkanimator$getList();
        if (optionsList == null) {
            return;
        }

        final List<?> rows = optionsList.children();
        for (int i = rows.size() - 1; i >= 0; i--) {
            if (rowHasAnimationsToggle(rows.get(i))) {
                rows.remove(i);
                break;
            }
        }

        optionsList.addSmall(ChunkAnimatorConfigUi.createAnimationsToggle(), null);
        @SuppressWarnings("unchecked")
        final List<Object> mutableRows = (List<Object>) rows;
        final Object centeredRow = mutableRows.remove(mutableRows.size() - 1);
        mutableRows.add(0, centeredRow);
    }

    private static boolean rowHasAnimationsToggle(Object row) {
        try {
            @SuppressWarnings("unchecked")
            final List<AbstractWidget> widgets = (List<AbstractWidget>) row.getClass().getMethod("children").invoke(row);
            for (AbstractWidget widget : widgets) {
                if (widget instanceof Button button && ChunkAnimatorConfigUi.isAnimationsToggle(button)) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }
}
