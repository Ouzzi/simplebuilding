package com.simplebuilding.modules.simplehammers;

import com.simplehammers.client.HammerAreaClient;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge client side: the hammer's area outline and extra breaking cracks (Queue 738). */
public final class HammersNeoClient {
    private HammersNeoClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event) -> HammerAreaClient.submitOutline(
                event.getSubmitNodeCollector(), event.getPoseStack(), event.getLevelRenderState().cameraRenderState.pos));
        // Fires after vanilla extracted its breaking states.
        NeoForge.EVENT_BUS.addListener((ExtractLevelRenderStateEvent event) -> HammerAreaClient.extractCracks(event.getRenderState(), event.getLevel()));
    }
}
