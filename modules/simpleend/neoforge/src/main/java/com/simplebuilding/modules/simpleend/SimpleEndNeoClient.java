package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.client.EndBoatRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class SimpleEndNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            for (EndWood w : EndWood.ALL) {
                event.registerEntityRenderer(w.boatType(), c -> new EndBoatRenderer(c, w, false));
                event.registerEntityRenderer(w.chestBoatType(), c -> new EndBoatRenderer(c, w, true));
            }
        });
    }
    private SimpleEndNeoClient() {}
}
