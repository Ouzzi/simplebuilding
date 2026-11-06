package com.simpleores.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleores.SimpleOres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleores-server.json}; missing or broken keys use the defaults and the file is rewritten
 * complete. The vein counts are data (worldgen JSON), not config. Every value is clamped to its bounds (server authority, A9).
 */
public final class OresConfig {
    public static final String FILE = "simpleores-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "soundproofing", "soundproofFactor", "soundproofThreshold", "soundproofRecords",
            "sageOre", "dimensionalScrap", "sageOreOtherDimensions", "roseGoldOreChance");

    /** Simple Arrows' option since 03.11. (simplearrows-server.json); an old file keeps it for that one takeover. */
    public static final String LEGACY_ARROW_KEY = "silverArrowUndeadBonus";

    public static boolean enabled = true;
    /** Lead walls muffle world sounds (concept ORES31 section 3). */
    public static boolean soundproofing = true;
    /** Volume factor per lead block on the way (0.2 to 0.9). */
    public static double soundproofFactor = 0.5;
    /** Below this volume a muffled sound is not sent at all (0.01 to 0.3). */
    public static double soundproofThreshold = 0.05;
    /** Records and music devices are muffled too. */
    public static boolean soundproofRecords = false;
    /** IG31-13: new veins of Sage Ore in the Overworld (moved from SimpleBuilding with its ore). */
    public static boolean sageOre = true;
    /** IG31-13: new Dimensional Scrap in all three dimensions (moved from SimpleBuilding). */
    public static boolean dimensionalScrap = true;

    /** 06.11.: Sage Ore also in the Nether and the End (drops sage crystal shards). */
    public static boolean sageOreOtherDimensions = true;
    /** 06.11.: percent chance per Nether gold ore block of a vein (below Y 31, at most one per vein) to be Nether Rose Gold Ore instead (0 to 10). */
    public static double roseGoldOreChance = 2.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleOres.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                JsonObject out = toJson();
                // 03.11.: the silver arrow's bonus moved to Simple Arrows, which takes it over from this file once; keep it until then.
                if (json.has(LEGACY_ARROW_KEY)) out.add(LEGACY_ARROW_KEY, json.get(LEGACY_ARROW_KEY));
                ConfigFiles.writeAtomic(file, GSON.toJson(out));
            } catch (Exception e) {
                SimpleOres.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        soundproofing = bool(json, "soundproofing", true);
        soundproofFactor = number(json, "soundproofFactor", 0.5, 0.2, 0.9);
        soundproofThreshold = number(json, "soundproofThreshold", 0.05, 0.01, 0.3);
        soundproofRecords = bool(json, "soundproofRecords", false);
        sageOre = bool(json, "sageOre", true);
        dimensionalScrap = bool(json, "dimensionalScrap", true);
        sageOreOtherDimensions = bool(json, "sageOreOtherDimensions", true);
        roseGoldOreChance = number(json, "roseGoldOreChance", 2.0, 0.0, 10.0);
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback, double min, double max) {
        JsonElement v = json.get(key);
        double value = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : fallback;
        return Double.isNaN(value) ? fallback : Math.max(min, Math.min(max, value));
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("soundproofing", soundproofing);
        o.addProperty("soundproofFactor", soundproofFactor);
        o.addProperty("soundproofThreshold", soundproofThreshold);
        o.addProperty("soundproofRecords", soundproofRecords);
        o.addProperty("sageOre", sageOre);
        o.addProperty("dimensionalScrap", dimensionalScrap);
        o.addProperty("sageOreOtherDimensions", sageOreOtherDimensions);
        o.addProperty("roseGoldOreChance", roseGoldOreChance);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    static {
        reset();
    }

    private OresConfig() {}
}
