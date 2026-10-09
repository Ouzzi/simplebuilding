package com.simplelib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config {@code config/simplelib-server.json}. Every value is clamped to its hard bounds when
 * loaded (plan section 13), so an edited file can never exceed them; missing keys use the defaults
 * and the file is rewritten with the clamped values.
 */
public final class LibConfig {
    public static final int MAX_SPEED = 16;
    public static final int REINFORCED_SPEED_DEFAULT = 2, NETHERITE_SPEED_DEFAULT = 4, ENDERITE_SPEED_DEFAULT = 8;
    public static final double FACTOR_MEDIUM_DEFAULT = 0.5, FACTOR_HIGH_DEFAULT = 0.75, FACTOR_EXTREME_DEFAULT = 1.0;
    public static final double FACTOR_MIN = 0.1, FACTOR_MAX = 1.0;
    public static final double TWO_BELOW_PENALTY_DEFAULT = 0.10, TWO_BELOW_PENALTY_MAX = 0.5;
    public static final int AFTERGLOW_MAX_SECONDS = 60;
    public static final int RECHECK_DEFAULT = 20, RECHECK_MIN = 5, RECHECK_MAX = 200;
    public static final int WARM_BASE_DEFAULT = 100, WARM_BASE_MIN = 20, WARM_BASE_MAX = 1200;
    public static final int WARM_DURATION_DEFAULT = 12_000, WARM_DURATION_MIN = 1200, WARM_DURATION_MAX = 48_000;
    public static final int WARM_BUNDLE_DEFAULT = 48_000, WARM_BUNDLE_MIN = 1200, WARM_BUNDLE_MAX = 96_000;
    public static final double EAT_BONUS_DEFAULT = 0.15, EAT_BONUS_MAX = 0.30;

    public static int reinforcedSpeed = REINFORCED_SPEED_DEFAULT;
    public static int netheriteSpeed = NETHERITE_SPEED_DEFAULT;
    public static int enderiteSpeed = ENDERITE_SPEED_DEFAULT;
    public static double factorMedium = FACTOR_MEDIUM_DEFAULT;
    public static double factorHigh = FACTOR_HIGH_DEFAULT;
    public static double factorExtreme = FACTOR_EXTREME_DEFAULT;
    public static double twoBelowPenalty = TWO_BELOW_PENALTY_DEFAULT;
    /** Afterglow seconds per tier: iron, reinforced, netherite, enderite (owner F29: 2/4/8/16). */
    public static final int[] AFTERGLOW_DEFAULT = {2, 4, 8, 16};
    public static int[] afterglowSeconds = AFTERGLOW_DEFAULT.clone();
    public static boolean netherBonus = true;
    public static int heatRecheckTicks = RECHECK_DEFAULT;
    public static int warmBaseTicks = WARM_BASE_DEFAULT;
    public static int warmDurationTicks = WARM_DURATION_DEFAULT;
    public static int warmBundleDurationTicks = WARM_BUNDLE_DEFAULT;
    public static double eatSpeedBonus = EAT_BONUS_DEFAULT;
    public static final int VILLAGE_WEIGHT_DEFAULT = 3, VILLAGE_WEIGHT_MAX = 10;
    /** Weight of the field kitchen in each village houses pool (0 = off; owner 45: about every third village). */
    public static int villageKitchenWeight = VILLAGE_WEIGHT_DEFAULT;
    public static final double BURN_DAMAGE_DEFAULT = 1.0, BURN_DAMAGE_MAX = 4.0;
    /** Damage per hit while standing in a crucible at high heat or more, like magma (owner N11 P6; 0 = off). */
    public static double crucibleBurnDamage = BURN_DAMAGE_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE = "simplelib-server.json";

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        try {
            if (file != null && Files.isRegularFile(file)) {
                json = GSON.fromJson(Files.readString(file), JsonObject.class);
                if (json == null) json = new JsonObject();
            }
        } catch (Exception e) {
            SimpleLib.LOG.warn("Unreadable {}, using defaults: {}", FILE, e.toString());
            json = new JsonObject();
        }
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
        reinforcedSpeed = clamp(getInt(json, "reinforcedSpeed", REINFORCED_SPEED_DEFAULT), 1, MAX_SPEED);
        netheriteSpeed = clamp(getInt(json, "netheriteSpeed", NETHERITE_SPEED_DEFAULT), 1, MAX_SPEED);
        enderiteSpeed = clamp(getInt(json, "enderiteSpeed", ENDERITE_SPEED_DEFAULT), 1, MAX_SPEED);
        factorMedium = clamp(getDouble(json, "factorMedium", FACTOR_MEDIUM_DEFAULT), FACTOR_MIN, FACTOR_MAX);
        factorHigh = clamp(getDouble(json, "factorHigh", FACTOR_HIGH_DEFAULT), FACTOR_MIN, FACTOR_MAX);
        factorExtreme = clamp(getDouble(json, "factorExtreme", FACTOR_EXTREME_DEFAULT), FACTOR_MIN, FACTOR_MAX);
        twoBelowPenalty = clamp(getDouble(json, "twoBelowPenalty", TWO_BELOW_PENALTY_DEFAULT), 0.0, TWO_BELOW_PENALTY_MAX);
        String[] names = {"afterglowIron", "afterglowReinforced", "afterglowNetherite", "afterglowEnderite"};
        for (int i = 0; i < names.length; i++) {
            afterglowSeconds[i] = clamp(getInt(json, names[i], AFTERGLOW_DEFAULT[i]), 0, AFTERGLOW_MAX_SECONDS);
        }
        netherBonus = !json.has("netherBonus") || !json.get("netherBonus").isJsonPrimitive() || json.get("netherBonus").getAsBoolean();
        heatRecheckTicks = clamp(getInt(json, "heatRecheckTicks", RECHECK_DEFAULT), RECHECK_MIN, RECHECK_MAX);
        warmBaseTicks = clamp(getInt(json, "warmBaseTicks", WARM_BASE_DEFAULT), WARM_BASE_MIN, WARM_BASE_MAX);
        warmDurationTicks = clamp(getInt(json, "warmDurationTicks", WARM_DURATION_DEFAULT), WARM_DURATION_MIN, WARM_DURATION_MAX);
        warmBundleDurationTicks = clamp(getInt(json, "warmBundleDurationTicks", WARM_BUNDLE_DEFAULT), WARM_BUNDLE_MIN, WARM_BUNDLE_MAX);
        eatSpeedBonus = clamp(getDouble(json, "eatSpeedBonus", EAT_BONUS_DEFAULT), 0.0, EAT_BONUS_MAX);
        villageKitchenWeight = clamp(getInt(json, "villageKitchenWeight", VILLAGE_WEIGHT_DEFAULT), 0, VILLAGE_WEIGHT_MAX);
        crucibleBurnDamage = clamp(getDouble(json, "crucibleBurnDamage", BURN_DAMAGE_DEFAULT), 0.0, BURN_DAMAGE_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("reinforcedSpeed", reinforcedSpeed);
        o.addProperty("netheriteSpeed", netheriteSpeed);
        o.addProperty("enderiteSpeed", enderiteSpeed);
        o.addProperty("factorMedium", factorMedium);
        o.addProperty("factorHigh", factorHigh);
        o.addProperty("factorExtreme", factorExtreme);
        o.addProperty("twoBelowPenalty", twoBelowPenalty);
        o.addProperty("afterglowIron", afterglowSeconds[0]);
        o.addProperty("afterglowReinforced", afterglowSeconds[1]);
        o.addProperty("afterglowNetherite", afterglowSeconds[2]);
        o.addProperty("afterglowEnderite", afterglowSeconds[3]);
        o.addProperty("netherBonus", netherBonus);
        o.addProperty("heatRecheckTicks", heatRecheckTicks);
        o.addProperty("warmBaseTicks", warmBaseTicks);
        o.addProperty("warmDurationTicks", warmDurationTicks);
        o.addProperty("warmBundleDurationTicks", warmBundleDurationTicks);
        o.addProperty("eatSpeedBonus", eatSpeedBonus);
        o.addProperty("villageKitchenWeight", villageKitchenWeight);
        o.addProperty("crucibleBurnDamage", crucibleBurnDamage);
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
