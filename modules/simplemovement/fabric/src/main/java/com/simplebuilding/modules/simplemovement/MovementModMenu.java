package com.simplebuilding.modules.simplemovement;

/** Mod Menu entry: opens the config screen. */
public final class MovementModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MovementScreen::create;
    }
}
