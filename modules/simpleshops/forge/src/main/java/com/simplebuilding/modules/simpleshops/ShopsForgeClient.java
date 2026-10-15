package com.simplebuilding.modules.simpleshops;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.client.DisplayCaseRenderer;
import com.simpleshops.client.SalesBlockRenderer;
import com.simpleshops.client.ShopScreens;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class ShopsForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerBlockEntityRenderer(ShopsRegistry.CASE_BE, DisplayCaseRenderer::new);
            event.registerBlockEntityRenderer(ShopsRegistry.SALES_BE, SalesBlockRenderer::new);
        });
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> event.enqueueWork(() -> {
            MenuScreens.register(ShopsRegistry.OWNER_MENU, ShopScreens.Owner::new);
            MenuScreens.register(ShopsRegistry.BUYER_MENU, ShopScreens.Buyer::new);
            MenuScreens.register(ShopsRegistry.STOCK_MENU, ShopScreens.Stock::new);
        }));
    }

    private ShopsForgeClient() {}
}
