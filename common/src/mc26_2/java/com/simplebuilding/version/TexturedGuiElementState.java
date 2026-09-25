package com.simplebuilding.version;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/**
 * A GUI element drawn with the textured GUI pipeline. Exists only because RenderPipeline moved
 * package in 26.3 (com.mojang.renderpearl.api.pipeline), so shared code must not name it. 26.2 side
 * of the version shim, see {@link McVersion}.
 */
public interface TexturedGuiElementState extends GuiElementRenderState {

    @Override
    default RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }
}
