package com.simplebuilding.modules.simpleinterfaces.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** The edge-only spyglass overlay: one own texture (a soft dark rim, clear in the middle) stretched over the whole screen. */
public final class ScopeOverlay {
    private ScopeOverlay() {}

    public static void draw(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ZoomState.EDGE, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
    }
}
