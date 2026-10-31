package com.simpleblocks.snow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleblocks.SimpleBlocks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleblocks-server.json}, group {@code snow} (b-snow32, docs/ai/PLAN-B-SNOW32-2026-10-31.md):
 * {@code snowlogging} and {@code frostGrass} (both default on) and the chances of frost grass, 1 in N per random tick. Missing
 * or broken keys use the defaults and the file is rewritten complete; numbers are clamped to 1-100.
 */
public final class SnowConfig {
    public static final String FILE = "simpleblocks-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("snow.snowlogging", "snow.frostGrass", "snow.freezeOneIn", "snow.thawOneIn");

    public static final int FREEZE_DEFAULT = 3, THAW_DEFAULT = 6, ONE_IN_MIN = 1, ONE_IN_MAX = 100;

    /** Snow layers placed on a fence, flower or grass share the block with it. */
    public static boolean snowlogging = true;
    /** Grass blocks under snow freeze to frost grass (cold biome or ice below). */
    public static boolean frostGrass = true;
    /** A grass block under snow freezes with a chance of 1 in this per random tick (about 68 s per tick at randomTickSpeed 3). */
    public static int freezeOneIn = FREEZE_DEFAULT;
    /** Frost grass without snow thaws back to grass with a chance of 1 in this per random tick. */
    public static int thawOneIn = THAW_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleBlocks.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleBlocks.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement group = json.get("snow");
        JsonObject s = group != null && group.isJsonObject() ? group.getAsJsonObject() : new JsonObject();
        snowlogging = bool(s, "snowlogging", true);
        frostGrass = bool(s, "frostGrass", true);
        freezeOneIn = number(s, "freezeOneIn", FREEZE_DEFAULT);
        thawOneIn = number(s, "thawOneIn", THAW_DEFAULT);
    }

    public static JsonObject toJson() {
        JsonObject s = new JsonObject();
        s.addProperty("snowlogging", snowlogging);
        s.addProperty("frostGrass", frostGrass);
        s.addProperty("freezeOneIn", freezeOneIn);
        s.addProperty("thawOneIn", thawOneIn);
        JsonObject o = new JsonObject();
        o.add("snow", s);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static int number(JsonObject json, String key, int fallback) {
        JsonElement v = json.get(key);
        double d = fallback;
        try {
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) d = v.getAsDouble();
        } catch (RuntimeException e) {
            d = fallback;
        }
        if (!Double.isFinite(d)) d = fallback;
        return (int) Math.max(ONE_IN_MIN, Math.min(ONE_IN_MAX, Math.round(d)));
    }

    static {
        reset();
    }

    private SnowConfig() {}
}
