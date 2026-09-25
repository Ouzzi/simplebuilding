package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;

/** MC 26.4 snapshot side of {@code SpriteRecordingGraphics}: RenderPipeline is back in com.mojang.blaze3d.pipeline. */
abstract class SpriteRecordingGraphics extends GuiGraphicsExtractor {

    SpriteRecordingGraphics(Minecraft client) {
        super(client, new GuiRenderState(), 0, 0);
    }

    protected abstract void recordSprite(Identifier sprite, int x, int y, int width, int height);

    @Override
    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        recordSprite(sprite, x, y, width, height);
    }

    @Override
    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int textureWidth, int textureHeight,
                           int u, int v, int x, int y, int width, int height) {
        recordSprite(sprite, x, y, width, height);
    }
}
