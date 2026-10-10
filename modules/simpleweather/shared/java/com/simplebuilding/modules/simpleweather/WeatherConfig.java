package com.simplebuilding.modules.simpleweather;

/** {@code config/simpleweather.json}: the server weather switch and the local rain density (formerly {@code qOL.*} in Simple QoL). */
public final class WeatherConfig {
    public static final int DEFAULT_RAIN_PARTICLE_DENSITY = 20;
    /** Server: stops rain and thunder and clears active weather. */
    public boolean disableWeather = false;
    /** Client look only, 0-100 % of rain particles and sounds. */
    public int clientRainParticleDensity = DEFAULT_RAIN_PARTICLE_DENSITY;

    public void normalize() {
        clientRainParticleDensity = Math.clamp(clientRainParticleDensity, 0, 100);
    }
}
