package com.simplemaps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config {@code config/simplemaps-server.json} (Feature 8). Every value is clamped to the hard bounds when
 * loaded, so an edited file can never exceed them; the file is rewritten with the clamped values.
 */
public final class MapsConfig {
    public static final String FILE = "simplemaps-server.json";
    /** Reveal radius in blocks around the holder (Vanilla 1:1 map: 128, halved under a ceiling). */
    public static final int REVEAL_RADIUS_DEFAULT = 96, REVEAL_RADIUS_MIN = 16, REVEAL_RADIUS_MAX = 128;
    /** Explored 128x128 tiles one map may hold (4096 tiles = 8192 x 8192 blocks; about 64 KiB per tile unpacked). */
    public static final int MAX_TILES_DEFAULT = 4096, MAX_TILES_MIN = 16, MAX_TILES_MAX = 16384;

    public static int revealRadius = REVEAL_RADIUS_DEFAULT;
    public static int maxTilesPerMap = MAX_TILES_DEFAULT;
    public static boolean allowCopy = true, allowExtend = true, allowCombine = true;
    /**
     * Whether the module is active. Standalone it always is; bundled in SimpleBuilding the host's config switch
     * "Enable Simple Maps" ({@code enableSimpleMaps} in {@code simplebuilding.json}, read once at load, restart to
     * change) can turn it off: the items stay registered (stable registries) but do nothing and are not offered.
     */
    public static boolean enabled = true;
    public static final String HOST_FILE = "simplebuilding.json", HOST_KEY = "enableSimpleMaps";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        try {
            if (file != null && Files.isRegularFile(file)) {
                json = GSON.fromJson(Files.readString(file), JsonObject.class);
                if (json == null) json = new JsonObject();
            }
        } catch (Exception e) {
            SimpleMaps.LOG.warn("Unreadable {}, using defaults: {}", FILE, e.toString());
            json = new JsonObject();
        }
        apply(json);
        enabled = hostAllows(dir);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleMaps.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** False only when a SimpleBuilding config next to ours explicitly says {@code enableSimpleMaps: false}. */
    public static boolean hostAllows(Path dir) {
        try {
            Path host = dir == null ? null : dir.resolve(HOST_FILE);
            if (host == null || !Files.isRegularFile(host)) return true;
            JsonObject json = GSON.fromJson(Files.readString(host), JsonObject.class);
            return json == null || !json.has(HOST_KEY) || !json.get(HOST_KEY).isJsonPrimitive() || json.get(HOST_KEY).getAsBoolean();
        } catch (Exception e) {
            return true;
        }
    }

    /** Reads and clamps every value. Public for the config bounds GameTest. */
    public static void apply(JsonObject json) {
        revealRadius = clamp(getInt(json, "revealRadius", REVEAL_RADIUS_DEFAULT), REVEAL_RADIUS_MIN, REVEAL_RADIUS_MAX);
        maxTilesPerMap = clamp(getInt(json, "maxTilesPerMap", MAX_TILES_DEFAULT), MAX_TILES_MIN, MAX_TILES_MAX);
        allowCopy = getBool(json, "allowCopy", true);
        allowExtend = getBool(json, "allowExtend", true);
        allowCombine = getBool(json, "allowCombine", true);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("revealRadius", revealRadius);
        o.addProperty("maxTilesPerMap", maxTilesPerMap);
        o.addProperty("allowCopy", allowCopy);
        o.addProperty("allowExtend", allowExtend);
        o.addProperty("allowCombine", allowCombine);
        return o;
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean getBool(JsonObject json, String key, boolean fallback) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private MapsConfig() {}
}
