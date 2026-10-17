package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;

/** Fabric client wiring of the admin screens (loaded only on the client). */
final class AdminFabricClient {
    static void init() {
        net.minecraft.client.gui.screens.MenuScreens.register(Invsee.INVSEE, com.simplebuilding.modules.simpletweaks.admin.invsee.client.InvseeScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(Invsee.ENDERSEE, com.simplebuilding.modules.simpletweaks.admin.invsee.client.InvseeScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(Invsee.DEATHS, com.simplebuilding.modules.simpletweaks.admin.invsee.client.InvseeScreen::new);
    }
    private AdminFabricClient() {}
}
