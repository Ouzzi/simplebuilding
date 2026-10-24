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
        MobsConfig.loadClient(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        FearNet.toServer = payload -> { if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(payload.type())) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload); };
        ModelLayerRegistry.registerModelLayer(DeceiverModel.LAYER, DeceiverModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(HowlerModel.LAYER, HowlerModel::createBodyLayer);
        EntityRendererRegistry.register(MobsRegistry.DECEIVER, DeceiverRenderer::new);
        EntityRendererRegistry.register(MobsRegistry.HOLLOW_HOWLER, HowlerRenderer::new);
        EntityRendererRegistry.register(MobsRegistry.HOWLER_RITUAL, NoopRenderer::new);
        EntityRendererRegistry.register(DisguiseRegistry.MARKER, NoopRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SHELLKER, com.simplebuilding.modules.simplemobs.client.ShellkerRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SHELLKER_BULLET, com.simplebuilding.modules.simplemobs.client.ShellkerBulletRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SOULFIRE_BLAZE, com.simplebuilding.modules.simplemobs.client.SoulfireBlazeRenderer::new);
        ModelLayerRegistry.registerModelLayer(com.simplebuilding.modules.simplemobs.client.DreadModel.LAYER, com.simplebuilding.modules.simplemobs.client.DreadModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(com.simplebuilding.modules.simplemobs.client.WatcherModel.LAYER, com.simplebuilding.modules.simplemobs.client.WatcherModel::createBodyLayer);
        EntityRendererRegistry.register(FearMobs.DREAD, com.simplebuilding.modules.simplemobs.client.DreadRenderer::new);
        EntityRendererRegistry.register(WitherMobs.BLIGHTED_WITHER, com.simplebuilding.modules.simplemobs.client.BlightedWitherRenderer::new);
        EntityRendererRegistry.register(WitherMobs.ABYSSAL_WITHER, com.simplebuilding.modules.simplemobs.client.BlightedWitherRenderer::abyssal);
        EntityRendererRegistry.register(FearMobs.WATCHER, com.simplebuilding.modules.simplemobs.client.WatcherRenderer::new);
        EntityRendererRegistry.register(EndNetherMobs.SOUL_FIREBALL, ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 0.75F, true));
        // REV2-B: the disguise caches hold entities of the client level; leaving must not keep that world in memory.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            com.simplebuilding.modules.simplemobs.client.DisguiseLooks.clear();
            com.simplebuilding.modules.simplemobs.client.DeceiverLooks.clear();
            DisguiseMarker.clearClient();
        }));
    }
}
