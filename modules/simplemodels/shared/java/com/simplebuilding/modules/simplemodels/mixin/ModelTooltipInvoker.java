package com.simplebuilding.modules.simplemodels.mixin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;
/** Vanilla tooltip from ready-made components (text plus the model preview image), without a loader tooltip factory. */
@Mixin(GuiGraphicsExtractor.class)
public interface ModelTooltipInvoker {
    @Invoker("setTooltipForNextFrameInternal")
    void simplemodels$tooltip(Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner, Identifier style, boolean replaceExisting);
}
