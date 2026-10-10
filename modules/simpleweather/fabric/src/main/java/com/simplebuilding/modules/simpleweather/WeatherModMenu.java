package com.simplebuilding.modules.simpleweather;

/** Mod Menu entry: opens the config screen. */
public final class WeatherModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WeatherScreen::create;
    }
}
