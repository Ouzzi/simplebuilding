package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;

/** Client-only Forge setup (dist guard in the entry point): the Shaping Saw screen. */
final class BlocksForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.<ShapingSawMenu, ShapingSawScreen>register(Sawing.MENU, ShapingSawScreen::new)));
    }

    private BlocksForgeClient() {}
}
