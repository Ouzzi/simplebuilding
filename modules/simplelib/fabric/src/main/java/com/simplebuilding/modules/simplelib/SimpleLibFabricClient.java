package com.simplebuilding.modules.simplelib;

import com.simplelib.client.CrucibleRenderer;
import com.simplelib.client.CrucibleScreen;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class SimpleLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LibMenus.CRUCIBLES.values().forEach(type -> MenuScreens.register(type, CrucibleScreen::new));
        LibMenus.BARRELS.values().forEach(type -> MenuScreens.register(type, net.minecraft.client.gui.screens.inventory.ContainerScreen::new));
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(LibBlockEntities.CRUCIBLE, CrucibleRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(
                java.util.List.of(com.simplelib.client.ReinforcedCauldronTint.INSTANCE), com.simplelib.registry.LibBlocks.REINFORCED_CAULDRON);
    }
}
