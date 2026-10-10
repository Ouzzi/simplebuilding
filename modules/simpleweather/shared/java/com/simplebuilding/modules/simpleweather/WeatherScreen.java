package com.simplebuilding.modules.simpleweather;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class WeatherScreen {
    private WeatherScreen() {}

    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(SimpleWeather.CONFIG), WeatherConfig.class);
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simpleweather.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simpleweather.tab.general"));
        general.addEntry(e.startBooleanToggle(name("disableWeather"), c.disableWeather).setDefaultValue(false)
                .setTooltip(tip("disableWeather")).setSaveConsumer(v -> c.disableWeather = v).build());
        general.addEntry(e.startIntField(name("clientRainParticleDensity"), c.clientRainParticleDensity).setMin(0).setMax(100)
                .setDefaultValue(WeatherConfig.DEFAULT_RAIN_PARTICLE_DENSITY).setTooltip(tip("clientRainParticleDensity"))
                .setSaveConsumer(v -> c.clientRainParticleDensity = v).build());
        b.setSavingRunnable(() -> {
            SimpleWeather.CONFIG = c;
            SimpleWeather.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simpleweather.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simpleweather.option." + key + ".tooltip");
    }
}
