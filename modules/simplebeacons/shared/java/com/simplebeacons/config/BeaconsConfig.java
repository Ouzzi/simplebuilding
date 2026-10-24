package com.simplebeacons.config;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebeacons.SimpleBeacons;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server config {@code config/simplebeacons-server.json} (server authority, every number clamped to a safe range);
 * missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class BeaconsConfig {
    public static final String FILE = "simplebeacons-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "strongBeaconMaxLevels", "strongBeaconRangeBase",
            "strongBeaconRangePerLevel", "strongBeaconEffects", "strongBeaconMaterialRange", "vanillaBeaconMaterialRange",
            "strongConduitMaxRange", "upgradeStrikes");
    /** The ten extra effects, by their Vanilla id path (switch {@code strongBeaconEffects.<id>}); SP22-10 added the last four. */
    public static final List<String> EFFECT_IDS = List.of("night_vision", "water_breathing", "slow_falling",
            "fire_resistance", "dolphins_grace", "absorption", "saturation", "health_boost", "invisibility", "hero_of_the_village");

    public static final int MAX_LEVELS_DEFAULT = 6, MAX_LEVELS_MIN = 4, MAX_LEVELS_MAX = 6;
    public static final int RANGE_BASE_DEFAULT = 10, RANGE_BASE_MAX = 50;
    public static final int RANGE_PER_LEVEL_DEFAULT = 10, RANGE_PER_LEVEL_MIN = 1, RANGE_PER_LEVEL_MAX = 20;
    public static final int CONDUIT_RANGE_DEFAULT = 216, CONDUIT_RANGE_MIN = 96, CONDUIT_RANGE_MAX = 256;
    public static final int STRIKES_DEFAULT = 8, STRIKES_MIN = 1, STRIKES_MAX = 32;

    public static boolean enabled = true;
    public static int strongBeaconMaxLevels = MAX_LEVELS_DEFAULT;
    public static int strongBeaconRangeBase = RANGE_BASE_DEFAULT;
    public static int strongBeaconRangePerLevel = RANGE_PER_LEVEL_DEFAULT;
    public static final Map<String, Boolean> strongBeaconEffects = new LinkedHashMap<>();
    /** SP22-10: range factor from the pyramid's material (Netherite x2 ... Iron x1) for the Strong and the Vanilla beacon. */
    public static boolean strongBeaconMaterialRange = true;
    public static boolean vanillaBeaconMaterialRange = true;
    public static int strongConduitMaxRange = CONDUIT_RANGE_DEFAULT;
    public static int upgradeStrikes = STRIKES_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        reset();
    }

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleBeacons.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleBeacons.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        strongBeaconMaxLevels = clamp(integer(json, "strongBeaconMaxLevels", MAX_LEVELS_DEFAULT), MAX_LEVELS_MIN, MAX_LEVELS_MAX);
        strongBeaconRangeBase = clamp(integer(json, "strongBeaconRangeBase", RANGE_BASE_DEFAULT), 0, RANGE_BASE_MAX);
        strongBeaconRangePerLevel = clamp(integer(json, "strongBeaconRangePerLevel", RANGE_PER_LEVEL_DEFAULT), RANGE_PER_LEVEL_MIN, RANGE_PER_LEVEL_MAX);
        JsonObject effects = json.has("strongBeaconEffects") && json.get("strongBeaconEffects").isJsonObject()
                ? json.getAsJsonObject("strongBeaconEffects") : new JsonObject();
        strongBeaconEffects.clear();
        for (String id : EFFECT_IDS) strongBeaconEffects.put(id, bool(effects, id, true));
        strongBeaconMaterialRange = bool(json, "strongBeaconMaterialRange", true);
        vanillaBeaconMaterialRange = bool(json, "vanillaBeaconMaterialRange", true);
        strongConduitMaxRange = clamp(integer(json, "strongConduitMaxRange", CONDUIT_RANGE_DEFAULT), CONDUIT_RANGE_MIN, CONDUIT_RANGE_MAX);
        upgradeStrikes = clamp(integer(json, "upgradeStrikes", STRIKES_DEFAULT), STRIKES_MIN, STRIKES_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("strongBeaconMaxLevels", strongBeaconMaxLevels);
        o.addProperty("strongBeaconRangeBase", strongBeaconRangeBase);
        o.addProperty("strongBeaconRangePerLevel", strongBeaconRangePerLevel);
        JsonObject effects = new JsonObject();
        strongBeaconEffects.forEach(effects::addProperty);
        o.add("strongBeaconEffects", effects);
        o.addProperty("strongBeaconMaterialRange", strongBeaconMaterialRange);
        o.addProperty("vanillaBeaconMaterialRange", vanillaBeaconMaterialRange);
        o.addProperty("strongConduitMaxRange", strongConduitMaxRange);
        o.addProperty("upgradeStrikes", upgradeStrikes);
        return o;
    }

    /** Whether the extra effect {@code id} (a Vanilla effect path) may be chosen. */
    public static boolean effectOn(String id) {
        return strongBeaconEffects.getOrDefault(id, true);
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement v = json.get(key);
        try {
            return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private BeaconsConfig() {}
}
