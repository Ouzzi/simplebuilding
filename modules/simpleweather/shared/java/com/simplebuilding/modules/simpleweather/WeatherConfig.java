package com.simplebuilding.modules.simpleweather;

/** {@code config/simpleweather.json}: the server weather switch and the local rain density (formerly {@code qOL.*} in Simple QoL). */
public final class WeatherConfig {
    public static final int DEFAULT_RAIN_PARTICLE_DENSITY = 20;
    /** Server: stops rain and thunder and clears active weather. */
    public boolean disableWeather = false;
    /** Client look only, 0-100 % of rain particles and sounds. */
    public int clientRainParticleDensity = DEFAULT_RAIN_PARTICLE_DENSITY;

    public static final float MIN_FACTOR = 0.25f, MAX_FACTOR = 4f;
    /** Server: how often rain starts. 1 = Vanilla; 2 = the dry spell between rains is half as long; 0.25 = four times as long. */
    public float rainFrequencyFactor = 1f;
    /** Server: the same for thunderstorms (their own countdown). */
    public float thunderFrequencyFactor = 1f;

    public void normalize() {
        rainFrequencyFactor = clampFactor(rainFrequencyFactor);
        thunderFrequencyFactor = clampFactor(thunderFrequencyFactor);
        clientRainParticleDensity = Math.clamp(clientRainParticleDensity, 0, 100);
    }

    private static float clampFactor(float v) {
        return Float.isNaN(v) ? 1f : Math.clamp(v, MIN_FACTOR, MAX_FACTOR);
    }
}
