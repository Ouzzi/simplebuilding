package com.simplebuilding.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Zugriff auf den GUI-Renderzustand, damit die Blaupausen-Ansicht ihr selbst projiziertes
 * 3D-Modell als ein einziges {@code GuiElementRenderState} einreichen kann (BlueprintView).
 */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor("guiRenderState")
    GuiRenderState simplebuilding$guiRenderState();
}
