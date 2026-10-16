package com.simplelib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config {@code config/simplelib-server.json} (the warm food options). Every value is clamped to its hard bounds when
 * loaded (plan section 13), so an edited file can never exceed them; missing keys use the defaults
 * and the file is rewritten with the clamped values.
 */
public final class LibConfig {
    public static final int WARM_BASE_DEFAULT = 100, WARM_BASE_MIN = 20, WARM_BASE_MAX = 1200;
    public static final int WARM_DURATION_DEFAULT = 12_000, WARM_DURATION_MIN = 1200, WARM_DURATION_MAX = 48_000;
    public static final int WARM_BUNDLE_DEFAULT = 48_000, WARM_BUNDLE_MIN = 1200, WARM_BUNDLE_MAX = 96_000;
    public static final double EAT_BONUS_DEFAULT = 0.15, EAT_BONUS_MAX = 0.30;

    public static int warmBaseTicks = WARM_BASE_DEFAULT;
    public static int warmDurationTicks = WARM_DURATION_DEFAULT;
    public static int warmBundleDurationTicks = WARM_BUNDLE_DEFAULT;
    public static double eatSpeedBonus = EAT_BONUS_DEFAULT;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE = "simplelib-server.json";
    /** Keys this class does not own (the crucible options before split stage 5): written back unchanged so Simple Crucibles can still take them over. */
    private static JsonObject kept = new JsonObject();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleLib.LOG::warn);
        if (read != null) json = read;
        kept = json.deepCopy();
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleLib.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads and clamps every value. Public for the config bounds GameTest. */
    public static void apply(JsonObject json) {
        warmBaseTicks = clamp(getInt(json, "warmBaseTicks", WARM_BASE_DEFAULT), WARM_BASE_MIN, WARM_BASE_MAX);
        warmDurationTicks = clamp(getInt(json, "warmDurationTicks", WARM_DURATION_DEFAULT), WARM_DURATION_MIN, WARM_DURATION_MAX);
        warmBundleDurationTicks = clamp(getInt(json, "warmBundleDurationTicks", WARM_BUNDLE_DEFAULT), WARM_BUNDLE_MIN, WARM_BUNDLE_MAX);
        eatSpeedBonus = clamp(getDouble(json, "eatSpeedBonus", EAT_BONUS_DEFAULT), 0.0, EAT_BONUS_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = kept.deepCopy();
        o.addProperty("warmBaseTicks", warmBaseTicks);
        o.addProperty("warmDurationTicks", warmDurationTicks);
        o.addProperty("warmBundleDurationTicks", warmBundleDurationTicks);
        o.addProperty("eatSpeedBonus", eatSpeedBonus);
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

    private LibConfig() {}
}
