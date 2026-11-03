package com.simplebuilding.modules.simpleenchanting;

import com.simpleenchanting.EnchantingRegistry;
import com.simpleenchanting.client.EnchanterRenderer;
import com.simpleenchanting.client.EnchanterScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/** Fabric client entry: the enchanters' screen and their floating book. */
public final class EnchantingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(EnchantingRegistry.ENCHANTER_MENU, EnchanterScreen::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(EnchantingRegistry.ENCHANTER_BE, EnchanterRenderer::new);
    }
}
