package com.simplebuilding.modules.simpleweather;

import net.fabricmc.api.ModInitializer;

public final class WeatherFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleWeather.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleWeather.load();
        // CMD24: /simpleweather config for every option; save() normalises, writes and syncs the clients (Fabric entry: SimpleLib is bundled here).
        com.simplelib.api.config.ConfigCommands.server(SimpleWeather.MOD_ID).gson(WeatherConfig.class, () -> SimpleWeather.CONFIG, c -> { c.normalize(); SimpleWeather.CONFIG = c; },
                () -> { WeatherConfig d = new WeatherConfig(); d.normalize(); return d; }).save(SimpleWeather::save).register();
    }
}
