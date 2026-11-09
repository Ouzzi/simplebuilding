package com.simplebuilding.modules.simplemodels.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Editor viewport (b-models13): hands its self-projected mesh to the GUI render state as one element (like BlueprintView). */
@Mixin(GuiGraphicsExtractor.class)
public interface ModelGuiStateAccessor {
    @Accessor("guiRenderState")
    GuiRenderState simplemodels$guiRenderState();
}
