package com.simplewanderers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplewanderers.SimpleWanderers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplewanderers-server.json} (server authority, every number clamped to a safe range);
 * missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class WandererConfig {
    public static final String FILE = "simplewanderers-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "respawnDays", "rerollDays", "restockDays", "villageRadius",
            "homeRadius", "horse", "raidBlocksSpawn", "biomeAssortment");

    public static final double RESPAWN_DEFAULT = 7.0, RESPAWN_MIN = 0.0, RESPAWN_MAX = 60.0;
    public static final double REROLL_DEFAULT = 3.0, REROLL_MIN = 1.0, REROLL_MAX = 14.0;
    public static final double RESTOCK_DEFAULT = 1.5, RESTOCK_MIN = 0.5;
    public static final int VILLAGE_DEFAULT = 48, VILLAGE_MIN = 16, VILLAGE_MAX = 64;
    public static final int HOME_DEFAULT = 24, HOME_MIN = 8, HOME_MAX = 48;

    public static boolean enabled = true;
    /** Days until a killed wanderer (or a lost horse) is replaced, only counted while a player is at the village; 0 = never. */
    public static double respawnDays = RESPAWN_DEFAULT;
    /** Days between a fresh set of offers (restock is part of it). */
    public static double rerollDays = REROLL_DEFAULT;
    /** Days between two restocks; at most the reroll period, never below half a day. */
    public static double restockDays = RESTOCK_DEFAULT;
    /** Radius around a player in which a village bell counts as a village visit. */
    public static int villageRadius = VILLAGE_DEFAULT;
    /** How far the wanderer strolls from the bell. */
    public static int homeRadius = HOME_DEFAULT;
    /** The wanderer brings a leashed horse. */
    public static boolean horse = true;
    /** No new wanderer while a raid is active at the village. */
    public static boolean raidBlocksSpawn = true;
    /** The village biome adds two offers from its own pool. */
    public static boolean biomeAssortment = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static long respawnTicks() {
        return (long) (respawnDays * SimpleWanderers.DAY);
    }

    public static long rerollTicks() {
        return Math.max(1L, (long) (rerollDays * SimpleWanderers.DAY));
    }

    public static long restockTicks() {
        return Math.max(1L, (long) (restockDays * SimpleWanderers.DAY));
    }

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleWanderers.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleWanderers.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        respawnDays = clamp(number(json, "respawnDays", RESPAWN_DEFAULT), RESPAWN_MIN, RESPAWN_MAX);
        rerollDays = clamp(number(json, "rerollDays", REROLL_DEFAULT), REROLL_MIN, REROLL_MAX);
        restockDays = clamp(number(json, "restockDays", RESTOCK_DEFAULT), RESTOCK_MIN, rerollDays);
        villageRadius = (int) clamp(number(json, "villageRadius", VILLAGE_DEFAULT), VILLAGE_MIN, VILLAGE_MAX);
        homeRadius = (int) clamp(number(json, "homeRadius", HOME_DEFAULT), HOME_MIN, HOME_MAX);
        horse = bool(json, "horse", true);
        raidBlocksSpawn = bool(json, "raidBlocksSpawn", true);
        biomeAssortment = bool(json, "biomeAssortment", true);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("respawnDays", respawnDays);
        o.addProperty("rerollDays", rerollDays);
        o.addProperty("restockDays", restockDays);
        o.addProperty("villageRadius", villageRadius);
        o.addProperty("homeRadius", homeRadius);
        o.addProperty("horse", horse);
        o.addProperty("raidBlocksSpawn", raidBlocksSpawn);
        o.addProperty("biomeAssortment", biomeAssortment);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        JsonElement v = json.get(key);
        try {
            double d = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : fallback;
            return Double.isFinite(d) ? d : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static {
        reset();
    }

    private WandererConfig() {}
}
