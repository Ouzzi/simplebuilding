package com.simplebuilding.version;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** 26.4-snapshot side of {@code TexturedGuiElementState}: RenderPipeline is back in com.mojang.blaze3d.pipeline. */
public interface TexturedGuiElementState extends GuiElementRenderState {

    @Override
    default RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }
}
