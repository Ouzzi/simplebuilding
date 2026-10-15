package com.simplebuilding.modules.simpleshops;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.client.DisplayCaseRenderer;
import com.simpleshops.client.SalesBlockRenderer;
import com.simpleshops.client.ShopScreens;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist. */
final class ShopsNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(ShopsRegistry.CASE_BE, DisplayCaseRenderer::new);
            event.registerBlockEntityRenderer(ShopsRegistry.SALES_BE, SalesBlockRenderer::new);
        });
        bus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(ShopsRegistry.OWNER_MENU, ShopScreens.Owner::new);
            event.register(ShopsRegistry.BUYER_MENU, ShopScreens.Buyer::new);
            event.register(ShopsRegistry.STOCK_MENU, ShopScreens.Stock::new);
        });
    }

    private ShopsNeoClient() {}
}
