package com.simplebuilding.modules.simplecrucibles;

import com.simplecrucibles.client.CrucibleRenderer;
import com.simplecrucibles.client.CrucibleScreen;
import com.simplecrucibles.registry.CrucibleBlockEntities;
import com.simplecrucibles.registry.CrucibleMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class CruciblesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CrucibleMenus.CRUCIBLES.values().forEach(type -> MenuScreens.register(type, CrucibleScreen::new));
        CrucibleMenus.BARRELS.values().forEach(type -> MenuScreens.register(type, net.minecraft.client.gui.screens.inventory.ContainerScreen::new));
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(CrucibleBlockEntities.CRUCIBLE, CrucibleRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(
                java.util.List.of(com.simplecrucibles.client.ReinforcedCauldronTint.INSTANCE), com.simplecrucibles.registry.CrucibleBlocks.REINFORCED_CAULDRON);
        // Split stage 5b: soul lava rendering (turquoise lava textures) and the soul burn screen filter.
        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(com.simplecrucibles.fluid.CrucibleFluids.SOUL_LAVA,
                com.simplecrucibles.fluid.CrucibleFluids.FLOWING_SOUL_LAVA, com.simplecrucibles.client.SoulLavaModel.unbaked());
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT, com.simplecrucibles.SimpleCrucibles.id("soul_burn_filter"),
                (context, tickCounter) -> com.simplecrucibles.client.SoulBurnOverlay.render(context));
    }
}
