package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;

/**
 * A GUI extractor that reports every sprite blit instead of drawing it - MC 26.2 side (twin in
 * mc26_3/overlay/clientgametest/java). Split off the shared HUD recorder only because the overridden
 * blitSprite methods name RenderPipeline, which moved package in 26.3.
 */
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
