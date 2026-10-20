package com.simplebuilding.modules.simplestorage;

import com.simplestorage.client.CrateRenderer;
import com.simplestorage.client.StorageCraftingScreen;
import com.simplestorage.client.StorageCraftingTableRenderer;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist. */
final class StorageNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(StorageBlockEntities.CRATE, CrateRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.STORAGE_CRAFTING_TABLE, StorageCraftingTableRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.TIERED_CHEST, com.simplestorage.client.TieredChestRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.TRAPPED_COPPER_CHEST, com.simplestorage.client.TrappedCopperChestRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.TIERED_SHULKER_BOX, com.simplestorage.client.TieredShulkerBoxRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.PLACED_BUNDLE, com.simplestorage.client.PlacedBundleRenderer::new);
            // Split stage 8b-2: chest carts and chest boats.
            com.simplestorage.client.VehicleRenderers.register(new com.simplestorage.client.VehicleRenderers.Registrar() {
                @Override
                public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                        net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                    event.registerEntityRenderer(type, provider);
                }
            });
        });
        // Split stage 8c-1: bundle packets, dyed placed bundle, tooltip, the quiver on the back (both player models, mannequins).
        com.simplestorage.bundle.BundleNet.toServer = net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer;
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) ->
                event.register(java.util.List.of(com.simplestorage.client.PlacedBundleTint.INSTANCE), com.simplestorage.registry.StorageBlocks.PLACED_BUNDLE));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) ->
                event.register(com.simplestorage.bundle.BundleTooltipData.class, com.simplestorage.client.BundleTooltips::create));
        bus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (net.minecraft.world.entity.player.PlayerModelType skin : event.getSkins()) {
                net.minecraft.client.renderer.entity.player.AvatarRenderer<?> player = event.getPlayerRenderer(skin);
                if (player != null) player.addLayer(new com.simplestorage.client.QuiverLayer(player));
                net.minecraft.client.renderer.entity.player.AvatarRenderer<?> mannequin = event.getMannequinRenderer(skin);
                if (mannequin != null) mannequin.addLayer(new com.simplestorage.client.QuiverLayer(mannequin));
            }
        });
        bus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(StorageMenus.STORAGE_CRAFTING_TABLE, StorageCraftingScreen::new);
            for (var type : StorageMenus.tieredTypes()) event.register(type, com.simplestorage.client.TieredChestScreen::new);
        });
    }

    private StorageNeoClient() {}
}
