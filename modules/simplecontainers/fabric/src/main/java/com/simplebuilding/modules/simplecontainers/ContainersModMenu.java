package com.simplebuilding.modules.simplecontainers;

/** Mod Menu entry: opens the config screen. */
public final class ContainersModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ContainersScreen::create;
    }
}
