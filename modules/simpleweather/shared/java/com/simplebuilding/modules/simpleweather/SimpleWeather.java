package com.simplebuilding.modules.simpleweather;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.SubMods;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Weather: weather switch and rain density, a sub-mod of Simple QoL (also playable alone). The loader entry sets
 * {@link #configDir} and calls {@link #load()}; on the first start the old Simple QoL values are taken over once.
 */
public final class SimpleWeather {
    public static final String MOD_ID = "simpleweather";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    public static WeatherConfig CONFIG = new WeatherConfig();

    private SimpleWeather() {}

    /** False when the Simple QoL super-mod switched this sub-mod off ("Enable Simple Weather"). */
    public static boolean active() {
        return SubMods.enabled(MOD_ID);
    }

    public static WeatherConfig config() {
        return CONFIG;
    }

    static Path path() {
        return configDir.resolve(MOD_ID + ".json");
    }

    public static void load() {
        CONFIG = read(configDir);
    }

    /** Own file if present, else the one-time takeover from {@code simplequalityoflife.json} (then written as own file). */
    static WeatherConfig read(Path dir) {
        Path own = dir.resolve(MOD_ID + ".json");
        WeatherConfig config = null;
        try {
            if (Files.isRegularFile(own) && Files.size(own) <= 65536) {
                config = new Gson().fromJson(Files.readString(own), WeatherConfig.class);
            } else {
                config = fromQol(dir.resolve("simplequalityoflife.json"));
                if (config != null) write(own, config);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Weather settings, using defaults", e);
        }
        if (config == null) config = new WeatherConfig();
        config.normalize();
        return config;
    }

    /** The weather values of an old Simple QoL config, or null without such a file. */
    static WeatherConfig fromQol(Path qol) throws IOException {
        if (!Files.isRegularFile(qol) || Files.size(qol) > 1 << 20) return null;
        JsonObject root = JsonParser.parseString(Files.readString(qol)).getAsJsonObject();
        JsonObject old = root.has("qOL") && root.get("qOL").isJsonObject() ? root.getAsJsonObject("qOL") : null;
        if (old == null || !(old.has("disableWeather") || old.has("clientRainParticleDensity"))) return null;
        WeatherConfig config = new WeatherConfig();
        if (old.has("disableWeather")) config.disableWeather = old.get("disableWeather").getAsBoolean();
        if (old.has("clientRainParticleDensity")) config.clientRainParticleDensity = old.get("clientRainParticleDensity").getAsInt();
        return config;
    }

    public static void save() {
        CONFIG.normalize();
        try {
            write(path(), CONFIG);
        } catch (IOException e) {
            LOGGER.warn("Cannot save Simple Weather settings", e);
        }
    }

    private static void write(Path file, WeatherConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
