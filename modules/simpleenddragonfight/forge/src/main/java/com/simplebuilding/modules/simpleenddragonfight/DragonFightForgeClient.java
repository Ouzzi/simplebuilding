package com.simplebuilding.modules.simpleenddragonfight;

import com.simplebuilding.modules.simpleenddragonfight.client.DragonmiteModel;
import com.simplebuilding.modules.simpleenddragonfight.client.DragonmiteRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class DragonFightForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event -> event.registerLayerDefinition(DragonmiteModel.LAYER, DragonmiteModel::createBodyLayer));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> event.registerEntityRenderer(DragonFightRegistry.DRAGONMITE, DragonmiteRenderer::new));
    }
    private DragonFightForgeClient() {}
}
