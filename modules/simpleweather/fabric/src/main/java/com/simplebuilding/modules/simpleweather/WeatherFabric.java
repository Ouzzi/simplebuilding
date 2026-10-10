package com.simplebuilding.modules.simpleweather;

import net.fabricmc.api.ModInitializer;

public final class WeatherFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleWeather.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleWeather.load();
    }
}
