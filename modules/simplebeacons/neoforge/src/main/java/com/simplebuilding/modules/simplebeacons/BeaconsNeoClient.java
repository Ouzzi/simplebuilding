package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.client.StrongBeaconScreen;
import com.simplebeacons.client.StrongConduitRenderer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist. */
final class BeaconsNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(BeaconsRegistry.STRONG_BEACON_BE, context -> new BeaconRenderer<>());
            event.registerBlockEntityRenderer(BeaconsRegistry.STRONG_CONDUIT_BE, StrongConduitRenderer::new);
        });
        bus.addListener((RegisterMenuScreensEvent event) -> event.register(BeaconsRegistry.STRONG_BEACON_MENU, StrongBeaconScreen::new));
    }

    private BeaconsNeoClient() {}
}
