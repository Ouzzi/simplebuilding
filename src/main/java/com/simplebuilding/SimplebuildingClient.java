package com.simplebuilding;

import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class SimplebuildingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new RangefinderHudOverlay());
        WorldRenderEvents.END_MAIN.register(new BlockHighlightRenderer());
    }
    
}
