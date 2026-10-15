package com.simplebuilding.modules.simpleshops;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.client.DisplayCaseRenderer;
import com.simpleshops.client.ShopScreens;
import com.simpleshops.client.SalesBlockRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ShopsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(ShopsRegistry.CASE_BE, DisplayCaseRenderer::new);
        BlockEntityRendererRegistry.register(ShopsRegistry.SALES_BE, SalesBlockRenderer::new);
        MenuScreens.register(ShopsRegistry.OWNER_MENU, ShopScreens.Owner::new);
        MenuScreens.register(ShopsRegistry.BUYER_MENU, ShopScreens.Buyer::new);
        MenuScreens.register(ShopsRegistry.STOCK_MENU, ShopScreens.Stock::new);
        MenuScreens.register(ShopsRegistry.SHOP_MENU, ShopScreens.Owner::new);
        MenuScreens.register(ShopsRegistry.STORAGE_MENU, ShopScreens.Storage::new);
    }
}
