package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.client.StrongBeaconScreen;
import com.simplebeacons.client.StrongConduitRenderer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class BeaconsForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerBlockEntityRenderer(BeaconsRegistry.STRONG_BEACON_BE, context -> new BeaconRenderer<>());
            event.registerBlockEntityRenderer(BeaconsRegistry.STRONG_CONDUIT_BE, StrongConduitRenderer::new);
        });
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(BeaconsRegistry.STRONG_BEACON_MENU, StrongBeaconScreen::new)));
    }

    private BeaconsForgeClient() {}
}
