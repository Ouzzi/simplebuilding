package com.simplebuilding.modules.simpleearth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Server config {@code config/simpleearth-server.json} (server authority, A9): the floors and caps every client value is
 * clamped to, sampling budget and cache, seed protection (E8) and which dimensions get an orbit (E7). Missing or broken keys
 * use the defaults and the file is rewritten complete.
 */
public final class EarthServerConfig {
    public static final String FILE = "simpleearth-server.json";
    public static final List<String> KEYS = List.of("enabled", "dimensions", "minActivationY", "minSwitchGap", "maxOrbitSpeed",
            "minEdge", "maxEdge", "exploredOnly", "showOthers", "workerThreads", "tilesPerSecond", "memoryTiles", "diskCacheMiB",
            "slowFallSeconds");

    public static boolean enabled;
    public static final List<String> dimensions = new ArrayList<>();
    /** Lowest activation height a client may pick (also never below build height + 64). */
    public static int minActivationY;
    /** Render switch at least this far above the activation height. */
    public static int minSwitchGap;
    public static int maxOrbitSpeed;
    public static int minEdge;
    public static int maxEdge;
    /** E8: only show terrain where the world already has a region file (no free seed map). */
    public static boolean exploredOnly;
    /** E9: other players in orbit appear as dots on the earth. */
    public static boolean showOthers;
    public static int workerThreads;
    public static int tilesPerSecond;
    public static int memoryTiles;
    public static int diskCacheMiB;
    public static int slowFallSeconds;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        reset();
    }

    public static void reset() {
        apply(new JsonObject());
    }

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleEarth.LOG::warn);
        apply(read == null ? new JsonObject() : read);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleEarth.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        dimensions.clear();
        if (json.has("dimensions") && json.get("dimensions").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("dimensions")) if (e.isJsonPrimitive()) dimensions.add(e.getAsString());
        } else {
            dimensions.add("minecraft:overworld");
        }
        minActivationY = integer(json, "minActivationY", 1024, 384, 16384);
        minSwitchGap = integer(json, "minSwitchGap", 128, 32, 8192);
        maxOrbitSpeed = integer(json, "maxOrbitSpeed", 2048, 16, 8192);
        minEdge = integer(json, "minEdge", 8192, 1024, 262144);
        maxEdge = integer(json, "maxEdge", 262144, minEdge, 1048576);
        exploredOnly = bool(json, "exploredOnly", false);
        showOthers = bool(json, "showOthers", true);
        workerThreads = integer(json, "workerThreads", 2, 1, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
        tilesPerSecond = integer(json, "tilesPerSecond", 24, 1, 512);
        memoryTiles = integer(json, "memoryTiles", 1024, 16, 65536);
        diskCacheMiB = integer(json, "diskCacheMiB", 256, 0, 65536);
        slowFallSeconds = integer(json, "slowFallSeconds", 30, 0, 600);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        JsonArray dims = new JsonArray();
        dimensions.forEach(dims::add);
        o.add("dimensions", dims);
        o.addProperty("minActivationY", minActivationY);
        o.addProperty("minSwitchGap", minSwitchGap);
        o.addProperty("maxOrbitSpeed", maxOrbitSpeed);
        o.addProperty("minEdge", minEdge);
        o.addProperty("maxEdge", maxEdge);
        o.addProperty("exploredOnly", exploredOnly);
        o.addProperty("showOthers", showOthers);
        o.addProperty("workerThreads", workerThreads);
        o.addProperty("tilesPerSecond", tilesPerSecond);
        o.addProperty("memoryTiles", memoryTiles);
        o.addProperty("diskCacheMiB", diskCacheMiB);
        o.addProperty("slowFallSeconds", slowFallSeconds);
        return o;
    }

    /** Effective activation height for a client wish: never below the server floor nor below build height + 64. */
    public static int activationY(int wish, int maxBuildY) {
        return Math.max(Math.max(minActivationY, maxBuildY + 64), Math.min(wish, 16384));
    }

    public static int switchY(int wish, int activation) {
        return Math.max(activation + minSwitchGap, Math.min(wish, 30000));
    }

    public static int edge(int wish) {
        return Math.max(minEdge, Math.min(maxEdge, wish));
    }

    public static double speed(double wish) {
        return Math.max(1, Math.min(maxOrbitSpeed, Double.isFinite(wish) ? wish : 256));
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        try {
            return o.has(key) ? o.get(key).getAsBoolean() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static int integer(JsonObject o, String key, int def, int min, int max) {
        try {
            return o.has(key) ? Math.max(min, Math.min(max, o.get(key).getAsInt())) : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private EarthServerConfig() {}
}
