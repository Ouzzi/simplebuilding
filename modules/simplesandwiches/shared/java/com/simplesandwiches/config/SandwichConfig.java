package com.simplesandwiches.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplesandwiches.Sandwiches;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config {@code config/simplesandwiches-server.json}. Every value is clamped to the hard
 * bounds below when loaded, so an edited file can never exceed them. Missing keys use the defaults;
 * the file is rewritten with the clamped values.
 */
public final class SandwichConfig {
    public static final int MAX_INGREDIENTS_DEFAULT = 5, MAX_INGREDIENTS_MIN = 1, MAX_INGREDIENTS_MAX = 5;
    public static final double BUTTER_BONUS_DEFAULT = 0.10, BUTTER_BONUS_MIN = 0.0, BUTTER_BONUS_MAX = 0.25;
    public static final int EFFECT_CAP_TICKS_DEFAULT = 12_000, EFFECT_CAP_TICKS_MIN = 20, EFFECT_CAP_TICKS_MAX = 12_000;
    public static final double EFFECT_MULTIPLIER_CAP_DEFAULT = 2.0, EFFECT_MULTIPLIER_CAP_MIN = 1.0, EFFECT_MULTIPLIER_CAP_MAX = 3.0;
    public static final int BUTTER_TICKS_DEFAULT = 3_000, BUTTER_TICKS_MIN = 200, BUTTER_TICKS_MAX = 24_000;
    public static final int CHEESE_TICKS_DEFAULT = 9_000, CHEESE_TICKS_MIN = 600, CHEESE_TICKS_MAX = 72_000;
    public static final int HARVEST_WINDOW_DEFAULT = 1_800, HARVEST_WINDOW_MIN = 200, HARVEST_WINDOW_MAX = 12_000;
    public static final boolean BUNDLE_EATING_DEFAULT = true;
    public static final boolean KNIFE_CUTS_CAKE_DEFAULT = true;

    public static int maxIngredients = MAX_INGREDIENTS_DEFAULT;
    public static double butterBonus = BUTTER_BONUS_DEFAULT;
    public static int effectDurationCapTicks = EFFECT_CAP_TICKS_DEFAULT;
    public static double effectDurationMultiplierCap = EFFECT_MULTIPLIER_CAP_DEFAULT;
    public static int butterTicks = BUTTER_TICKS_DEFAULT;
    public static int cheeseTicks = CHEESE_TICKS_DEFAULT;
    public static int cheeseHarvestWindowTicks = HARVEST_WINDOW_DEFAULT;
    public static boolean bundleEating = BUNDLE_EATING_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE = "simplesandwiches-server.json";

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), Sandwiches.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                Sandwiches.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads and clamps every value. Public for the config bounds GameTest. */
    public static void apply(JsonObject json) {
        maxIngredients = clamp(getInt(json, "maxIngredients", MAX_INGREDIENTS_DEFAULT), MAX_INGREDIENTS_MIN, MAX_INGREDIENTS_MAX);
        butterBonus = clamp(getDouble(json, "butterBonus", BUTTER_BONUS_DEFAULT), BUTTER_BONUS_MIN, BUTTER_BONUS_MAX);
        effectDurationCapTicks = clamp(getInt(json, "effectDurationCapTicks", EFFECT_CAP_TICKS_DEFAULT), EFFECT_CAP_TICKS_MIN, EFFECT_CAP_TICKS_MAX);
        effectDurationMultiplierCap = clamp(getDouble(json, "effectDurationMultiplierCap", EFFECT_MULTIPLIER_CAP_DEFAULT), EFFECT_MULTIPLIER_CAP_MIN, EFFECT_MULTIPLIER_CAP_MAX);
        butterTicks = clamp(getInt(json, "butterTicks", BUTTER_TICKS_DEFAULT), BUTTER_TICKS_MIN, BUTTER_TICKS_MAX);
        cheeseTicks = clamp(getInt(json, "cheeseTicks", CHEESE_TICKS_DEFAULT), CHEESE_TICKS_MIN, CHEESE_TICKS_MAX);
        cheeseHarvestWindowTicks = clamp(getInt(json, "cheeseHarvestWindowTicks", HARVEST_WINDOW_DEFAULT), HARVEST_WINDOW_MIN, HARVEST_WINDOW_MAX);
        bundleEating = json.has("bundleEating") && json.get("bundleEating").isJsonPrimitive()
                ? json.get("bundleEating").getAsBoolean() : BUNDLE_EATING_DEFAULT;
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("maxIngredients", maxIngredients);
        o.addProperty("butterBonus", butterBonus);
        o.addProperty("effectDurationCapTicks", effectDurationCapTicks);
        o.addProperty("effectDurationMultiplierCap", effectDurationMultiplierCap);
        o.addProperty("butterTicks", butterTicks);
        o.addProperty("cheeseTicks", cheeseTicks);
        o.addProperty("cheeseHarvestWindowTicks", cheeseHarvestWindowTicks);
        o.addProperty("bundleEating", bundleEating);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static int getInt(JsonObject o, String k, int d) {
        try { return o.has(k) ? o.get(k).getAsInt() : d; } catch (Exception e) { return d; }
    }

    private static double getDouble(JsonObject o, String k, double d) {
        try {
            double v = o.has(k) ? o.get(k).getAsDouble() : d;
            return Double.isFinite(v) ? v : d;
        } catch (Exception e) { return d; }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private SandwichConfig() {}
}
