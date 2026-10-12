package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.client.DeceiverModel;
import com.simplebuilding.modules.simplemobs.client.DeceiverRenderer;
import com.simplebuilding.modules.simplemobs.client.HowlerModel;
import com.simplebuilding.modules.simplemobs.client.HowlerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

public final class FabricClientExample implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(DeceiverModel.LAYER, DeceiverModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(HowlerModel.LAYER, HowlerModel::createBodyLayer);
        EntityRendererRegistry.register(MobsRegistry.DECEIVER, DeceiverRenderer::new);
        EntityRendererRegistry.register(MobsRegistry.HOLLOW_HOWLER, HowlerRenderer::new);
        EntityRendererRegistry.register(MobsRegistry.HOWLER_RITUAL, NoopRenderer::new);
        EntityRendererRegistry.register(DisguiseRegistry.MARKER, NoopRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SHELLKER, com.simplebuilding.modules.simplemobs.client.ShellkerRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SHELLKER_BULLET, com.simplebuilding.modules.simplemobs.client.ShellkerBulletRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SOULFIRE_BLAZE, com.simplebuilding.modules.simplemobs.client.SoulfireBlazeRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SOUL_FIREBALL, ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 0.75F, true));
    }
}
