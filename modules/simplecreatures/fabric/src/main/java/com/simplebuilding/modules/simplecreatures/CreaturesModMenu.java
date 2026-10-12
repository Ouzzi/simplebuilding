package com.simplebuilding.modules.simplecreatures;

/** Mod Menu entry: opens the config screen. */
public final class CreaturesModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CreaturesScreen::create;
    }
}
