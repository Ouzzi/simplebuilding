package com.simplebuilding.modules.simplemobmanners;

/** Mod Menu entry: opens the config screen. */
public final class MobMannersModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MobMannersScreen::create;
    }
}
