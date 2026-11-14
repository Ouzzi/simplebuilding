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
            "sageOre", "dimensionalScrap", "sageOreOtherDimensions", "roseGoldOreChance",
            "leadOre", "endLeadOre", "zincGeodes", "endZincGeodes", "nickelOre", "tinOre", "tinPlacers",
            "endOres", "astralitOre", "nihilitOre", "endSignals", "endPistons", "endRails", "endPistonCooldownTicks", "astralRailMaxSpeed",
            "astralRailBoost", "nihilRailBrake");

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
    /** N4 (b-ores13, 09.11.): one switch per deposit; each takes effect at the next world start in newly generated chunks. */
    public static boolean leadOre = true;
    public static boolean endLeadOre = true;
    public static boolean zincGeodes = true;
    public static boolean endZincGeodes = true;
    public static boolean nickelOre = true;
    /** Mountain tin: the stone veins, the nests in granite and the extra veins of the mountain biomes. */
    public static boolean tinOre = true;
    /** Tin-bearing gravel and sand in river and beach beds. */
    public static boolean tinPlacers = true;

    /**
     * N46/N47-M2 (b-circ46, 14.11.): the Astralit/Nihilit line moved here from SimpleBuilding with its switches (before
     * {@code server.oreGeneration.endOres/astralitOre/nihilitOre}, {@code server.features.endSignals/endPistons/endRails} and
     * {@code server.machines.*} of SimpleBuilding's server config; same defaults and bounds). End ores: new veins only.
     */
    public static boolean endOres = true;
    public static boolean astralitOre = true;
    public static boolean nihilitOre = true;
    /** Astral/Nihil/Bridge Redstone carry a signal; off: the line is dead, its pistons and rails stand still. */
    public static boolean endSignals = true;
    /** Astral/Nihil pistons move blocks (only with endSignals). */
    public static boolean endPistons = true;
    /** Astral/Nihil rails boost and brake (only with endSignals). */
    public static boolean endRails = true;
    /** Ticks between two moves of one End piston (4 to 100). */
    public static int endPistonCooldownTicks = 8;
    /** Top speed on Astral rails in blocks per second (8 to 20). */
    public static int astralRailMaxSpeed = 16;
    /** Boost of a powered Astral rail per tick (0.07 to 0.25). */
    public static double astralRailBoost = 0.12;
    /** Constant part of the Nihil brake per tick (0.02 to 0.4). */
    public static double nihilRailBrake = 0.08;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleOres.LOG::warn);
        if (read != null) json = read;
        else if (dir != null) adoptSimpleBuilding(dir, json);
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

    /** The old home of the Astralit/Nihilit line options (N47-M2): config/simplebuilding.json, section "server". */
    public static final String SB_FILE = "simplebuilding.json";
    /** SimpleBuilding section -> keys that moved here unchanged (same names, same ranges). */
    static final java.util.Map<String, List<String>> SB_KEYS = java.util.Map.of(
            "oreGeneration", List.of("endOres", "astralitOre", "nihilitOre"),
            "features", List.of("endSignals", "endPistons", "endRails"),
            "machines", List.of("endPistonCooldownTicks", "astralRailMaxSpeed", "astralRailBoost", "nihilRailBrake"));

    /**
     * First start without an own file (b-circ46, pattern simpletools qolMigration): the line's values a SimpleBuilding config
     * still holds are taken over once; the own file written afterwards is the marker, from then on it alone counts.
     */
    static void adoptSimpleBuilding(Path dir, JsonObject json) {
        Path old = dir.resolve(SB_FILE);
        if (!Files.isRegularFile(old)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(old), JsonObject.class);
            JsonObject server = root != null && root.get("server") instanceof JsonObject o ? o : null;
            if (server == null) return;
            SB_KEYS.forEach((section, keys) -> {
                if (!(server.get(section) instanceof JsonObject from)) return;
                for (String key : keys) if (from.has(key) && !json.has(key)) json.add(key, from.get(key));
            });
            SimpleOres.LOG.info("{}: Astralit/Nihilit line options taken over from {}", FILE, SB_FILE);
        } catch (Exception e) {
            SimpleOres.LOG.warn("Could not read {} for the line options: {}", SB_FILE, e.toString());
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
        leadOre = bool(json, "leadOre", true);
        endLeadOre = bool(json, "endLeadOre", true);
        zincGeodes = bool(json, "zincGeodes", true);
        endZincGeodes = bool(json, "endZincGeodes", true);
        nickelOre = bool(json, "nickelOre", true);
        tinOre = bool(json, "tinOre", true);
        tinPlacers = bool(json, "tinPlacers", true);
        endOres = bool(json, "endOres", true);
        astralitOre = bool(json, "astralitOre", true);
        nihilitOre = bool(json, "nihilitOre", true);
        endSignals = bool(json, "endSignals", true);
        endPistons = bool(json, "endPistons", true);
        endRails = bool(json, "endRails", true);
        endPistonCooldownTicks = (int) Math.round(number(json, "endPistonCooldownTicks", 8, 4, 100));
        astralRailMaxSpeed = (int) Math.round(number(json, "astralRailMaxSpeed", 16, 8, 20));
        astralRailBoost = number(json, "astralRailBoost", 0.12, 0.07, 0.25);
        nihilRailBrake = number(json, "nihilRailBrake", 0.08, 0.02, 0.4);
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
        o.addProperty("leadOre", leadOre);
        o.addProperty("endLeadOre", endLeadOre);
        o.addProperty("zincGeodes", zincGeodes);
        o.addProperty("endZincGeodes", endZincGeodes);
        o.addProperty("nickelOre", nickelOre);
        o.addProperty("tinOre", tinOre);
        o.addProperty("tinPlacers", tinPlacers);
        o.addProperty("endOres", endOres);
        o.addProperty("astralitOre", astralitOre);
        o.addProperty("nihilitOre", nihilitOre);
        o.addProperty("endSignals", endSignals);
        o.addProperty("endPistons", endPistons);
        o.addProperty("endRails", endRails);
        o.addProperty("endPistonCooldownTicks", endPistonCooldownTicks);
        o.addProperty("astralRailMaxSpeed", astralRailMaxSpeed);
        o.addProperty("astralRailBoost", astralRailBoost);
        o.addProperty("nihilRailBrake", nihilRailBrake);
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
