package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.client.EndBoatRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class SimpleEndForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            for (EndWood w : EndWood.ALL) {
                event.registerEntityRenderer(w.boatType(), c -> new EndBoatRenderer(c, w, false));
                event.registerEntityRenderer(w.chestBoatType(), c -> new EndBoatRenderer(c, w, true));
            }
        });
    }
    private SimpleEndForgeClient() {}
}
