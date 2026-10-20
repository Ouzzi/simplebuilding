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
