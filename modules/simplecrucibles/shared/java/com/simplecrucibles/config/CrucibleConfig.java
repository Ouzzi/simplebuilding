package com.simplecrucibles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplecrucibles.SimpleCrucibles;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config {@code config/simplecrucibles-server.json}. Every value is clamped to its hard bounds when loaded,
 * so an edited file can never exceed them; missing keys use the defaults and the file is rewritten with the clamped
 * values. Split stage 5: the options used to live in {@code simplelib-server.json}; the first start without an own
 * file takes them over from there once (SimpleLib keeps unknown keys, so they are still present). Split stage 5b: the
 * group {@code soulLava} was SimpleBuilding's {@code server.soulLava} in {@code simplebuilding.json}; while the own file
 * has no such group it is taken over from there once ({@link #takeOverSoulLava}).
 */
public final class CrucibleConfig {
    public static final int MAX_SPEED = 16;
    public static final int REINFORCED_SPEED_DEFAULT = 2, NETHERITE_SPEED_DEFAULT = 4, ENDERITE_SPEED_DEFAULT = 8;
    public static final double FACTOR_MEDIUM_DEFAULT = 0.5, FACTOR_HIGH_DEFAULT = 0.75, FACTOR_EXTREME_DEFAULT = 1.0;
    public static final double FACTOR_MIN = 0.1, FACTOR_MAX = 1.0;
    public static final double TWO_BELOW_PENALTY_DEFAULT = 0.10, TWO_BELOW_PENALTY_MAX = 0.5;
    public static final int AFTERGLOW_MAX_SECONDS = 60;
    public static final int RECHECK_DEFAULT = 20, RECHECK_MIN = 5, RECHECK_MAX = 200;
    public static final int VILLAGE_WEIGHT_DEFAULT = 3, VILLAGE_WEIGHT_MAX = 10;
    public static final double BURN_DAMAGE_DEFAULT = 1.0, BURN_DAMAGE_MAX = 4.0;
    /** Afterglow seconds per tier: iron, reinforced, netherite, enderite (owner F29: 2/4/8/16). */
    public static final int[] AFTERGLOW_DEFAULT = {2, 4, 8, 16};

    public static int reinforcedSpeed = REINFORCED_SPEED_DEFAULT;
    public static int netheriteSpeed = NETHERITE_SPEED_DEFAULT;
    public static int enderiteSpeed = ENDERITE_SPEED_DEFAULT;
    public static double factorMedium = FACTOR_MEDIUM_DEFAULT;
    public static double factorHigh = FACTOR_HIGH_DEFAULT;
    public static double factorExtreme = FACTOR_EXTREME_DEFAULT;
    public static double twoBelowPenalty = TWO_BELOW_PENALTY_DEFAULT;
    public static int[] afterglowSeconds = AFTERGLOW_DEFAULT.clone();
    public static boolean netherBonus = true;
    public static int heatRecheckTicks = RECHECK_DEFAULT;
    /** Weight of the field kitchen in each village houses pool (0 = off; owner 45: about every third village). */
    public static int villageKitchenWeight = VILLAGE_WEIGHT_DEFAULT;
    /** Damage per hit while standing in a crucible at high heat or more, like magma (owner N11 P6; 0 = off). */
    public static double crucibleBurnDamage = BURN_DAMAGE_DEFAULT;

    /** Soul lava (split stage 5b, was SimpleBuilding's server.soulLava): flow, burning, soul burn, world generation, bucket fuel. */
    public static final class SoulLavaConfig {
        public int flowOverworld = 2;
        public int flowNether = 5;
        public int tickDelayOverworld = 45;
        public int tickDelayNether = 20;
        public int burnSeconds = 30;
        public int soulBurnSeconds = 120;
        public int soulBurnIntervalTicks = 60;
        public double soulBurnChance = 0.5;
        /** Registration-time value: restart and matching client/server files required. */
        public int fuelMultiplier = 10;
        public double springChance = 0.005;
        public double fortressChance = 0.1;

        /** Reads and clamps every value (hard bounds as in SimpleBuilding before 5b). */
        void apply(JsonObject o) {
            SoulLavaConfig d = new SoulLavaConfig();
            flowOverworld = clamp(getInt(o, "flowOverworld", d.flowOverworld), 1, 4);
            flowNether = clamp(getInt(o, "flowNether", d.flowNether), 1, 7);
            tickDelayOverworld = clamp(getInt(o, "tickDelayOverworld", d.tickDelayOverworld), 20, 200);
            tickDelayNether = clamp(getInt(o, "tickDelayNether", d.tickDelayNether), 10, 200);
            burnSeconds = clamp(getInt(o, "burnSeconds", d.burnSeconds), 5, 60);
            soulBurnSeconds = clamp(getInt(o, "soulBurnSeconds", d.soulBurnSeconds), 5, 300);
            soulBurnIntervalTicks = clamp(getInt(o, "soulBurnIntervalTicks", d.soulBurnIntervalTicks), 20, 200);
            soulBurnChance = clamp(getDouble(o, "soulBurnChance", d.soulBurnChance), 0.0, 1.0);
            fuelMultiplier = clamp(getInt(o, "fuelMultiplier", d.fuelMultiplier), 1, 20);
            springChance = clamp(getDouble(o, "springChance", d.springChance), 0.0, 0.05);
            fortressChance = clamp(getDouble(o, "fortressChance", d.fortressChance), 0.0, 0.5);
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("flowOverworld", flowOverworld);
            o.addProperty("flowNether", flowNether);
            o.addProperty("tickDelayOverworld", tickDelayOverworld);
            o.addProperty("tickDelayNether", tickDelayNether);
            o.addProperty("burnSeconds", burnSeconds);
            o.addProperty("soulBurnSeconds", soulBurnSeconds);
            o.addProperty("soulBurnIntervalTicks", soulBurnIntervalTicks);
            o.addProperty("soulBurnChance", soulBurnChance);
            o.addProperty("fuelMultiplier", fuelMultiplier);
            o.addProperty("springChance", springChance);
            o.addProperty("fortressChance", fortressChance);
            return o;
        }
    }

    public static final SoulLavaConfig soulLava = new SoulLavaConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE = "simplecrucibles-server.json";
    /** SimpleLib's file the options came from before split stage 5. */
    public static final String OLD_FILE = "simplelib-server.json";
    /** SimpleBuilding's config, whose {@code server.soulLava} group came here with split stage 5b. */
    public static final String SB_FILE = "simplebuilding.json";

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        if (file != null) {
            com.simplebuilding.framework.api.ConfigFiles.adoptLegacy(file, dir.resolve(OLD_FILE),
                    text -> { JsonObject taken = takeOver(GSON.fromJson(text, JsonObject.class)); return taken.isEmpty() ? null : GSON.toJson(taken); },
                    SimpleCrucibles.LOG::info);
        }
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleCrucibles.LOG::warn);
        if (read != null) json = read;
        if (dir != null && !json.has("soulLava")) {
            JsonObject taken = readSoulLava(dir.resolve(SB_FILE));
            if (taken != null) {
                json.add("soulLava", taken);
                SimpleCrucibles.LOG.info("Took over the soul lava options from {} (split stage 5b)", SB_FILE);
            }
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleCrucibles.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** {@code server.soulLava} of SimpleBuilding's config file, or null (no file, no group, unreadable). */
    private static JsonObject readSoulLava(Path sbFile) {
        try {
            if (!Files.isRegularFile(sbFile)) return null;
            return takeOverSoulLava(GSON.fromJson(Files.readString(sbFile), JsonObject.class));
        } catch (Exception e) {
            SimpleCrucibles.LOG.warn("Could not read {}: {}", SB_FILE, e.toString());
            return null;
        }
    }

    /** The soul lava group of SimpleBuilding's config ({@code server.soulLava}), or null. Public for the migration test. */
    public static JsonObject takeOverSoulLava(JsonObject sbConfig) {
        if (sbConfig == null || !sbConfig.has("server") || !sbConfig.get("server").isJsonObject()) return null;
        JsonObject server = sbConfig.getAsJsonObject("server");
        if (!server.has("soulLava") || !server.get("soulLava").isJsonObject()) return null;
        JsonObject out = new JsonObject();
        JsonObject old = server.getAsJsonObject("soulLava");
        for (String key : soulLava.toJson().keySet()) {
            if (old.has(key)) out.add(key, old.get(key).deepCopy());
        }
        return out.isEmpty() ? null : out;
    }

    /** The crucible options of SimpleLib's old file (the migration of split stage 5). Public for the migration test. */
    public static JsonObject takeOver(JsonObject old) {
        JsonObject out = new JsonObject();
        if (old == null) return out;
        for (String key : toJson().keySet()) {
            if (old.has(key)) out.add(key, old.get(key));
        }
        return out;
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
        villageKitchenWeight = clamp(getInt(json, "villageKitchenWeight", VILLAGE_WEIGHT_DEFAULT), 0, VILLAGE_WEIGHT_MAX);
        crucibleBurnDamage = clamp(getDouble(json, "crucibleBurnDamage", BURN_DAMAGE_DEFAULT), 0.0, BURN_DAMAGE_MAX);
        soulLava.apply(json.has("soulLava") && json.get("soulLava").isJsonObject() ? json.getAsJsonObject("soulLava") : new JsonObject());
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
        o.addProperty("villageKitchenWeight", villageKitchenWeight);
        o.addProperty("crucibleBurnDamage", crucibleBurnDamage);
        o.add("soulLava", soulLava.toJson());
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

    private CrucibleConfig() {}
}
