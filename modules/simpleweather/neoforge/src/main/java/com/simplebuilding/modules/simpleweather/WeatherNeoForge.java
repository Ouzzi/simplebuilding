package com.simplebuilding.modules.simpleweather;

import net.neoforged.fml.common.Mod;

@Mod("simpleweather")
public final class WeatherNeoForge {
    public WeatherNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleWeather.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleWeather.load();
        ModuleNeoTests.register(bus);
    }
}
