package com.simplebuilding.modules.simpleenddragonfight;

import com.simplebuilding.modules.simpleenddragonfight.client.DragonmiteModel;
import com.simplebuilding.modules.simpleenddragonfight.client.DragonmiteRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class DragonFightNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(DragonmiteModel.LAYER, DragonmiteModel::createBodyLayer));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(DragonFightRegistry.DRAGONMITE, DragonmiteRenderer::new));
    }
    private DragonFightNeoClient() {}
}
