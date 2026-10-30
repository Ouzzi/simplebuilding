package com.simpleanimals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleanimals.SimpleAnimals;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleanimals-server.json} (server authority, every number clamped to a safe range);
 * missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class AnimalsConfig {
    public static final String FILE = "simpleanimals-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "spawnScale", "deer", "boar", "hedgehog", "crab", "zebra",
            "boarAggression", "boarCloseSeconds", "hedgehogHuntRadius", "hedgehogHunts", "zorse");

    public static final double SPAWN_DEFAULT = 1.0, SPAWN_MIN = 0.0, SPAWN_MAX = 1.0;
    public static final int CLOSE_DEFAULT = 6, CLOSE_MIN = 2, CLOSE_MAX = 30;
    public static final int HUNT_DEFAULT = 8, HUNT_MIN = 2, HUNT_MAX = 16;

    public static boolean enabled = true;
    /** Share of the natural spawn attempts that go through (1 = the spawn weights of the wiki, 0 = none). */
    public static double spawnScale = SPAWN_DEFAULT;
    public static boolean deer = true, boar = true, hedgehog = true, crab = true, zebra = true;
    /** Boars defend their young and react to shoving; off = only when hurt. */
    public static boolean boarAggression = true;
    /** Seconds a player may stay within three blocks of an adult boar before it rams. */
    public static int boarCloseSeconds = CLOSE_DEFAULT;
    /** The hedgehog hunts silverfish within this many blocks. */
    public static int hedgehogHuntRadius = HUNT_DEFAULT;
    public static boolean hedgehogHunts = true;
    /** Zebra x horse gives the infertile zorse; off = zebras and horses do not breed with each other. */
    public static boolean zorse = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleAnimals.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleAnimals.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        spawnScale = clamp(number(json, "spawnScale", SPAWN_DEFAULT), SPAWN_MIN, SPAWN_MAX);
        deer = bool(json, "deer", true);
        boar = bool(json, "boar", true);
        hedgehog = bool(json, "hedgehog", true);
        crab = bool(json, "crab", true);
        zebra = bool(json, "zebra", true);
        boarAggression = bool(json, "boarAggression", true);
        boarCloseSeconds = (int) clamp(number(json, "boarCloseSeconds", CLOSE_DEFAULT), CLOSE_MIN, CLOSE_MAX);
        hedgehogHuntRadius = (int) clamp(number(json, "hedgehogHuntRadius", HUNT_DEFAULT), HUNT_MIN, HUNT_MAX);
        hedgehogHunts = bool(json, "hedgehogHunts", true);
        zorse = bool(json, "zorse", true);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("spawnScale", spawnScale);
        o.addProperty("deer", deer);
        o.addProperty("boar", boar);
        o.addProperty("hedgehog", hedgehog);
        o.addProperty("crab", crab);
        o.addProperty("zebra", zebra);
        o.addProperty("boarAggression", boarAggression);
        o.addProperty("boarCloseSeconds", boarCloseSeconds);
        o.addProperty("hedgehogHuntRadius", hedgehogHuntRadius);
        o.addProperty("hedgehogHunts", hedgehogHunts);
        o.addProperty("zorse", zorse);
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

    private AnimalsConfig() {}
}
