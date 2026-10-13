package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.client.StrongBeaconScreen;
import com.simplebeacons.client.StrongConduitRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;

public final class BeaconsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(BeaconsRegistry.STRONG_BEACON_BE, context -> new BeaconRenderer<>());
        BlockEntityRendererRegistry.register(BeaconsRegistry.STRONG_CONDUIT_BE, StrongConduitRenderer::new);
        MenuScreens.register(BeaconsRegistry.STRONG_BEACON_MENU, StrongBeaconScreen::new);
    }
}
