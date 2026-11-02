package com.simplebuilding.modules.simplefoundry;

import com.simplefoundry.block.FoundryMenu;
import com.simplefoundry.client.FoundryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client entry: the desk window. */
public final class FoundryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(FoundryMenu.TYPE, FoundryScreen::new);
    }
}
