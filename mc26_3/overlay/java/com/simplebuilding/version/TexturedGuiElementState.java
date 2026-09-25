package com.simplebuilding.version;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** 26.3 side of {@code TexturedGuiElementState} (RenderPipeline lives in renderpearl now). */
public interface TexturedGuiElementState extends GuiElementRenderState {

    @Override
    default RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }
}
