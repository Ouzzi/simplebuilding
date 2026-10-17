package com.simplebuilding.modules.simpletweaks.forge;

import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;
import com.simplebuilding.modules.simpletweaks.admin.invsee.client.InvseeScreen;
import net.minecraft.client.gui.screens.MenuScreens;

/** Forge client: the admin screens (dist guard in {@link AdminForge}). */
final class AdminForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> event.enqueueWork(() -> {
            MenuScreens.register(Invsee.INVSEE, InvseeScreen::new);
            MenuScreens.register(Invsee.ENDERSEE, InvseeScreen::new);
            MenuScreens.register(Invsee.DEATHS, InvseeScreen::new);
        }));
    }
    private AdminForgeClient() {}
}
