package com.simplebuilding.modules.simpleweather;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simpleweather")
public final class WeatherForge {
    public WeatherForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simpleweather.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleWeather.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleWeather.load();
    }
}
