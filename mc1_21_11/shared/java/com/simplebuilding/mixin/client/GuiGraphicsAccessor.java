package com.simplebuilding.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Zugriff auf den GUI-Renderzustand, damit die Blaupausen-Ansicht ihr selbst projiziertes
 * 3D-Modell als ein einziges {@code GuiElementRenderState} einreichen kann (BlueprintView).
 */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Accessor("guiRenderState")
    GuiRenderState simplebuilding$guiRenderState();
}
