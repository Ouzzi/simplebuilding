package com.simplebuilding.modules.simplestorage;

import com.simplestorage.client.CrateRenderer;
import com.simplestorage.client.StorageCraftingScreen;
import com.simplestorage.client.StorageCraftingTableRenderer;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge setup (dist guard in the entry point). */
final class StorageForgeClient {
    static void init(FMLJavaModLoadingContext context) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerBlockEntityRenderer(StorageBlockEntities.CRATE, CrateRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.STORAGE_CRAFTING_TABLE, StorageCraftingTableRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.TIERED_CHEST, com.simplestorage.client.TieredChestRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.TRAPPED_COPPER_CHEST, com.simplestorage.client.TrappedCopperChestRenderer::new);
            event.registerBlockEntityRenderer(StorageBlockEntities.TIERED_SHULKER_BOX, com.simplestorage.client.TieredShulkerBoxRenderer::new);
            // Split stage 8b-2: chest carts and chest boats.
            com.simplestorage.client.VehicleRenderers.register(new com.simplestorage.client.VehicleRenderers.Registrar() {
                @Override
                public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                        net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                    event.registerEntityRenderer(type, provider);
                }
            });
        });
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> event.enqueueWork(() -> {
            MenuScreens.register(StorageMenus.STORAGE_CRAFTING_TABLE, StorageCraftingScreen::new);
            for (var type : StorageMenus.tieredTypes()) MenuScreens.register(type, com.simplestorage.client.TieredChestScreen::new);
        }));
    }

    private StorageForgeClient() {}
}
