package com.simplebuilding.modules.simplestorage;

import com.simplestorage.client.CrateRenderer;
import com.simplestorage.client.StorageCraftingScreen;
import com.simplestorage.client.StorageCraftingTableRenderer;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/** The crates draw their food, the table its grid, the chests and boxes their lids; the screens. */
public final class StorageFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(StorageBlockEntities.CRATE, CrateRenderer::new);
        BlockEntityRendererRegistry.register(StorageBlockEntities.STORAGE_CRAFTING_TABLE, StorageCraftingTableRenderer::new);
        MenuScreens.register(StorageMenus.STORAGE_CRAFTING_TABLE, StorageCraftingScreen::new);
        BlockEntityRendererRegistry.register(StorageBlockEntities.TIERED_CHEST, com.simplestorage.client.TieredChestRenderer::new);
        BlockEntityRendererRegistry.register(StorageBlockEntities.TRAPPED_COPPER_CHEST, com.simplestorage.client.TrappedCopperChestRenderer::new);
        BlockEntityRendererRegistry.register(StorageBlockEntities.TIERED_SHULKER_BOX, com.simplestorage.client.TieredShulkerBoxRenderer::new);
        for (var type : StorageMenus.tieredTypes()) MenuScreens.register(type, com.simplestorage.client.TieredChestScreen::new);
        // Split stage 8c-1: the bundle packets, the placed bundle (item above it, dyed leather layer), the tooltip, the quiver on the back.
        com.simplestorage.bundle.BundleNet.toServer = payload -> {
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(payload.type())) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        };
        BlockEntityRendererRegistry.register(StorageBlockEntities.PLACED_BUNDLE, com.simplestorage.client.PlacedBundleRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(com.simplestorage.client.PlacedBundleTint.INSTANCE),
                com.simplestorage.registry.StorageBlocks.PLACED_BUNDLE);
        net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof com.simplestorage.bundle.BundleTooltipData bundle ? com.simplestorage.client.BundleTooltips.create(bundle)
                        : data instanceof com.simplestorage.backpack.BackpackTooltipData backpack ? com.simplestorage.client.BackpackTooltip.create(backpack) : null);
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
                helper.register(new com.simplestorage.client.BackpackLayer(avatar));
                helper.register(new com.simplestorage.client.QuiverLayer(avatar));
            }
        });
        // Split stage 8c-2: the backpack screen, key (B), the dyed placed backpack.
        com.simplestorage.backpack.BackpackNet.toServer = payload -> {
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(payload.type())) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        };
        MenuScreens.register(StorageMenus.BACKPACK, com.simplestorage.client.BackpackScreen::new);
        com.simplestorage.backpack.BackpackKeyMigration.run(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir());
        com.simplestorage.client.BackpackKeyHandler.KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                com.simplestorage.client.BackpackKeyHandler.create());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.simplestorage.client.BackpackKeyHandler::tick);
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(com.simplestorage.client.BackpackBlockTint.INSTANCE),
                com.simplestorage.registry.StorageBlocks.BACKPACKS.values().toArray(new net.minecraft.world.level.block.Block[0]));
        // Split stage 8b-2: chest carts and chest boats.
        com.simplestorage.client.VehicleRenderers.register(new com.simplestorage.client.VehicleRenderers.Registrar() {
            @Override
            public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type, provider);
            }
        });
    }
}
