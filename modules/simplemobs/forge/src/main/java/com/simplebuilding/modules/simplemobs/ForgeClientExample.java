package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.client.DeceiverModel;
import com.simplebuilding.modules.simplemobs.client.DeceiverRenderer;
import com.simplebuilding.modules.simplemobs.client.HowlerModel;
import com.simplebuilding.modules.simplemobs.client.HowlerRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class ForgeClientExample {
    static void init() {
        MobsConfig.loadClient(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event -> {
            event.registerLayerDefinition(DeceiverModel.LAYER, DeceiverModel::createBodyLayer);
            event.registerLayerDefinition(HowlerModel.LAYER, HowlerModel::createBodyLayer);
            event.registerLayerDefinition(com.simplebuilding.modules.simplemobs.client.DreadModel.LAYER, com.simplebuilding.modules.simplemobs.client.DreadModel::createBodyLayer);
            event.registerLayerDefinition(com.simplebuilding.modules.simplemobs.client.WatcherModel.LAYER, com.simplebuilding.modules.simplemobs.client.WatcherModel::createBodyLayer);
        });
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerEntityRenderer(MobsRegistry.DECEIVER, DeceiverRenderer::new);
            event.registerEntityRenderer(MobsRegistry.HOLLOW_HOWLER, HowlerRenderer::new);
            event.registerEntityRenderer(MobsRegistry.HOWLER_RITUAL, NoopRenderer::new);
            event.registerEntityRenderer(DisguiseRegistry.MARKER, NoopRenderer::new);
            event.registerEntityRenderer(EndNetherMobs.SHELLKER, com.simplebuilding.modules.simplemobs.client.ShellkerRenderer::new);
            event.registerEntityRenderer(EndNetherMobs.SHELLKER_BULLET, com.simplebuilding.modules.simplemobs.client.ShellkerBulletRenderer::new);
            event.registerEntityRenderer(EndNetherMobs.SOULFIRE_BLAZE, com.simplebuilding.modules.simplemobs.client.SoulfireBlazeRenderer::new);
            event.registerEntityRenderer(WitherMobs.BLIGHTED_WITHER, com.simplebuilding.modules.simplemobs.client.BlightedWitherRenderer::new);
            event.registerEntityRenderer(WitherMobs.ABYSSAL_WITHER, com.simplebuilding.modules.simplemobs.client.BlightedWitherRenderer::abyssal);
            event.registerEntityRenderer(FearMobs.DREAD, com.simplebuilding.modules.simplemobs.client.DreadRenderer::new);
            event.registerEntityRenderer(FearMobs.WATCHER, com.simplebuilding.modules.simplemobs.client.WatcherRenderer::new);
            event.registerEntityRenderer(EndNetherMobs.SOUL_FIREBALL, ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 0.75F, true));
        });
    }
    private ForgeClientExample() {}
}
