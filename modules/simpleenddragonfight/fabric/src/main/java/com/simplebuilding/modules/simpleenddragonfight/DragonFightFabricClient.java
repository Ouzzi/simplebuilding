package com.simplebuilding.modules.simpleenddragonfight;

import com.simplebuilding.modules.simpleenddragonfight.client.DragonmiteModel;
import com.simplebuilding.modules.simpleenddragonfight.client.DragonmiteRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public final class DragonFightFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(DragonmiteModel.LAYER, DragonmiteModel::createBodyLayer);
        EntityRendererRegistry.register(DragonFightRegistry.DRAGONMITE, DragonmiteRenderer::new);
    }
}
