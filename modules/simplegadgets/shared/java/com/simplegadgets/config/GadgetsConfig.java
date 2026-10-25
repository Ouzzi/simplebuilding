package com.simplegadgets.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplegadgets.SimpleGadgets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplegadgets-server.json} (server authority, every number clamped); missing or broken keys use
 * the defaults and the file is rewritten complete. On the first start it takes the values a world had in SimpleBuilding's config
 * ({@code tools.rotator*}, {@code server.charges.*}, {@code server.laser.*}, {@code tweaks.laserPointer.*} in
 * {@code config/simplebuilding.json}), which stays unchanged (framework {@code ConfigFiles.adoptLegacy}). Values are
 * server-local like Simple Hammers': a client counts with its own file (the rod's colour and size are the client's own anyway);
 * the maximum charges are read when the items register (restart needed, as before).
 */
public final class GadgetsConfig {
    public static final String FILE = "simplegadgets-server.json";
    /** SimpleBuilding's config the values came from (OW24-2 G2). */
    public static final String LEGACY_FILE = "simplebuilding.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("rotatorMaxCharge", "rotatorChargePerTurn", "rotatorSwapEnabled",
            "rotatorSwapWindupTicks", "rotatorSwapMaxHardness", "rotatorSwapWhitelist", "rotatorSwapBlacklist",
            "rodEnabled", "rodMaxCharge", "rodRange", "rodChargePerSecond", "rodEffectCost", "rodColor", "rodScale",
            "rodIgniteFlammables", "rodIgniteTnt", "rodIgniteEntities", "rodScanEntities", "rodScanPlayers", "rodScanIntervalTicks");

    public static final int ROTATOR_MIN_CHARGE = 64, ROTATOR_MAX_CHARGE = 4096, ROTATOR_CHARGE_DEFAULT = 1024;
    public static final int ROTATOR_TURN_COST_DEFAULT = 1, ROTATOR_TURN_COST_MAX = 4096;
    public static final int ROTATOR_WINDUP_DEFAULT = 20, ROTATOR_WINDUP_MAX = 200;
    public static final double ROTATOR_HARDNESS_DEFAULT = 6.0, ROTATOR_HARDNESS_MAX = 50.0;

    public static final int ROD_MIN_CHARGE = 64, ROD_MAX_CHARGE = 2560, ROD_CHARGE_DEFAULT = 640;
    public static final int ROD_RANGE_MAX = 1024, ROD_RANGE_DEFAULT = 512;
    public static final int ROD_COST_MAX = 2560, ROD_BEAM_COST_DEFAULT = 4, ROD_EFFECT_COST_DEFAULT = 5;
    /** Light amethyst, the dot's default colour. */
    public static final int ROD_COLOR_DEFAULT = 0xB38EF3;
    /** The dot's colour until 2026-10-07; lifted to {@link #ROD_COLOR_DEFAULT} like SimpleBuilding's config did. */
    public static final int ROD_LEGACY_COLOR = 0xFF0000;
    public static final float ROD_SCALE_DEFAULT = 0.25f;
    public static final int ROD_SCAN_MIN = 20, ROD_SCAN_MAX = 100;

    public static int rotatorMaxCharge = ROTATOR_CHARGE_DEFAULT;
    public static int rotatorChargePerTurn = ROTATOR_TURN_COST_DEFAULT;
    public static boolean rotatorSwapEnabled = true;
    public static int rotatorSwapWindupTicks = ROTATOR_WINDUP_DEFAULT;
    public static double rotatorSwapMaxHardness = ROTATOR_HARDNESS_DEFAULT;
    public static String rotatorSwapWhitelist = "";
    public static String rotatorSwapBlacklist = "";

    public static boolean rodEnabled = true;
    public static int rodMaxCharge = ROD_CHARGE_DEFAULT;
    public static int rodRange = ROD_RANGE_DEFAULT;
    public static int rodChargePerSecond = ROD_BEAM_COST_DEFAULT;
    public static int rodEffectCost = ROD_EFFECT_COST_DEFAULT;
    public static int rodColor = ROD_COLOR_DEFAULT;
    public static float rodScale = ROD_SCALE_DEFAULT;
    public static boolean rodIgniteFlammables = true;
    public static boolean rodIgniteTnt = true;
    public static boolean rodIgniteEntities = true;
    public static boolean rodScanEntities = true;
    public static boolean rodScanPlayers = false;
    public static int rodScanIntervalTicks = ROD_SCAN_MAX;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GadgetsConfig() {}

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        if (file != null) {
            ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), GadgetsConfig::fromSimpleBuilding, SimpleGadgets.LOG::info);
        }
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleGadgets.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleGadgets.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** The gadget values out of SimpleBuilding's config text, as this file's JSON; null when it has none. */
    public static String fromSimpleBuilding(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject root = parsed.getAsJsonObject();
        JsonObject tools = root.getAsJsonObject("tools");
        JsonObject server = root.getAsJsonObject("server");
        JsonObject charges = server == null ? null : server.getAsJsonObject("charges");
        JsonObject laser = server == null ? null : server.getAsJsonObject("laser");
        JsonObject pointer = root.getAsJsonObject("tweaks") == null ? null : root.getAsJsonObject("tweaks").getAsJsonObject("laserPointer");
        JsonObject taken = new JsonObject();
        if (tools != null) {
            for (String key : List.of("rotatorChargePerTurn", "rotatorSwapEnabled", "rotatorSwapWindupTicks", "rotatorSwapMaxHardness",
                    "rotatorSwapWhitelist", "rotatorSwapBlacklist")) {
                copy(tools, key, taken, key);
            }
        }
        if (charges != null) {
            copy(charges, "rotatorMaxCharge", taken, "rotatorMaxCharge");
            copy(charges, "lensMaxCharge", taken, "rodMaxCharge");
        }
        if (laser != null) {
            copy(laser, "igniteFlammables", taken, "rodIgniteFlammables");
            copy(laser, "igniteTnt", taken, "rodIgniteTnt");
            copy(laser, "igniteEntities", taken, "rodIgniteEntities");
            copy(laser, "scanEntities", taken, "rodScanEntities");
            copy(laser, "scanPlayers", taken, "rodScanPlayers");
            copy(laser, "scanIntervalTicks", taken, "rodScanIntervalTicks");
        }
        if (pointer != null) {
            copy(pointer, "enable", taken, "rodEnabled");
            copy(pointer, "range", taken, "rodRange");
            copy(pointer, "chargePerSecond", taken, "rodChargePerSecond");
            copy(pointer, "effectCost", taken, "rodEffectCost");
            copy(pointer, "color", taken, "rodColor");
            copy(pointer, "scale", taken, "rodScale");
        }
        return taken.isEmpty() ? null : GSON.toJson(taken);
    }

    private static void copy(JsonObject from, String key, JsonObject to, String as) {
        JsonElement value = from.get(key);
        if (value != null && value.isJsonPrimitive()) to.add(as, value.deepCopy());
    }

    public static void apply(JsonObject json) {
        rotatorMaxCharge = clamp(integer(json, "rotatorMaxCharge", ROTATOR_CHARGE_DEFAULT), ROTATOR_MIN_CHARGE, ROTATOR_MAX_CHARGE);
        rotatorChargePerTurn = clamp(integer(json, "rotatorChargePerTurn", ROTATOR_TURN_COST_DEFAULT), 0, ROTATOR_TURN_COST_MAX);
        rotatorSwapEnabled = bool(json, "rotatorSwapEnabled", true);
        rotatorSwapWindupTicks = clamp(integer(json, "rotatorSwapWindupTicks", ROTATOR_WINDUP_DEFAULT), 0, ROTATOR_WINDUP_MAX);
        rotatorSwapMaxHardness = clamp(real(json, "rotatorSwapMaxHardness", ROTATOR_HARDNESS_DEFAULT), 0, ROTATOR_HARDNESS_MAX);
        rotatorSwapWhitelist = string(json, "rotatorSwapWhitelist");
        rotatorSwapBlacklist = string(json, "rotatorSwapBlacklist");

        rodEnabled = bool(json, "rodEnabled", true);
        rodMaxCharge = clamp(integer(json, "rodMaxCharge", ROD_CHARGE_DEFAULT), ROD_MIN_CHARGE, ROD_MAX_CHARGE);
        rodRange = clamp(integer(json, "rodRange", ROD_RANGE_DEFAULT), 1, ROD_RANGE_MAX);
        rodChargePerSecond = clamp(integer(json, "rodChargePerSecond", ROD_BEAM_COST_DEFAULT), 0, ROD_COST_MAX);
        rodEffectCost = clamp(integer(json, "rodEffectCost", ROD_EFFECT_COST_DEFAULT), 0, ROD_COST_MAX);
        int color = integer(json, "rodColor", ROD_COLOR_DEFAULT) & 0xFFFFFF;
        rodColor = color == ROD_LEGACY_COLOR ? ROD_COLOR_DEFAULT : color;
        rodScale = (float) clamp(real(json, "rodScale", ROD_SCALE_DEFAULT), 0.05, 1.0);
        rodIgniteFlammables = bool(json, "rodIgniteFlammables", true);
        rodIgniteTnt = bool(json, "rodIgniteTnt", true);
        rodIgniteEntities = bool(json, "rodIgniteEntities", true);
        rodScanEntities = bool(json, "rodScanEntities", true);
        rodScanPlayers = bool(json, "rodScanPlayers", false);
        rodScanIntervalTicks = clamp(integer(json, "rodScanIntervalTicks", ROD_SCAN_MAX), ROD_SCAN_MIN, ROD_SCAN_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("rotatorMaxCharge", rotatorMaxCharge);
        o.addProperty("rotatorChargePerTurn", rotatorChargePerTurn);
        o.addProperty("rotatorSwapEnabled", rotatorSwapEnabled);
        o.addProperty("rotatorSwapWindupTicks", rotatorSwapWindupTicks);
        o.addProperty("rotatorSwapMaxHardness", rotatorSwapMaxHardness);
        o.addProperty("rotatorSwapWhitelist", rotatorSwapWhitelist);
        o.addProperty("rotatorSwapBlacklist", rotatorSwapBlacklist);
        o.addProperty("rodEnabled", rodEnabled);
        o.addProperty("rodMaxCharge", rodMaxCharge);
        o.addProperty("rodRange", rodRange);
        o.addProperty("rodChargePerSecond", rodChargePerSecond);
        o.addProperty("rodEffectCost", rodEffectCost);
        o.addProperty("rodColor", rodColor);
        o.addProperty("rodScale", rodScale);
        o.addProperty("rodIgniteFlammables", rodIgniteFlammables);
        o.addProperty("rodIgniteTnt", rodIgniteTnt);
        o.addProperty("rodIgniteEntities", rodIgniteEntities);
        o.addProperty("rodScanEntities", rodScanEntities);
        o.addProperty("rodScanPlayers", rodScanPlayers);
        o.addProperty("rodScanIntervalTicks", rodScanIntervalTicks);
        return o;
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

    private static double real(JsonObject json, String key, double fallback) {
        JsonElement v = json.get(key);
        try {
            double value = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : fallback;
            return Double.isFinite(value) ? value : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() ? v.getAsString() : "";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
