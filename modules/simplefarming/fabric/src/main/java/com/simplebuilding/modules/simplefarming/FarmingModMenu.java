package com.simplebuilding.modules.simplefarming;

/** Mod Menu entry: opens the config screen. */
public final class FarmingModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FarmingScreen::create;
    }
}
