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
    /**
     * Reveal radius in blocks around the holder. Default = Vanilla {@code MapItem.update} at scale 0 (1:1):
     * {@code radius = 128 / (1 << scale)}, halved under a ceiling (the item applies the halving).
     */
    public static final int REVEAL_RADIUS_DEFAULT = 128, REVEAL_RADIUS_MIN = 16, REVEAL_RADIUS_MAX = 128;
    /** Explored 128x128 tiles one map may hold (4096 tiles = 8192 x 8192 blocks; about 64 KiB per tile unpacked). */
    public static final int MAX_TILES_DEFAULT = 4096, MAX_TILES_MIN = 16, MAX_TILES_MAX = 16384;

    public static int revealRadius = REVEAL_RADIUS_DEFAULT;
    public static int maxTilesPerMap = MAX_TILES_DEFAULT;
    public static boolean allowCopy = true, allowExtend = true, allowCombine = true;
    /** Map layers (loaded chunks, shops, chunk loaders): the server answers layer requests only when true. */
    public static boolean layers = true;
    /** Web map export (stage 1, static files): off by default; see {@code docs/SIMPLEMAPS-WEBKARTE.md}. */
    public static final int WEB_CHUNKS_DEFAULT = 32, WEB_CHUNKS_MIN = 1, WEB_CHUNKS_MAX = 512;
    public static final int WEB_INTERVAL_DEFAULT = 0, WEB_INTERVAL_MAX = 1440;
    /** Mods that bring their own web map: one of them loaded switches the web export off (autoOffWith). */
    public static final java.util.List<String> WEB_AUTO_OFF_DEFAULT = java.util.List.of("bluemap", "dynmap", "squaremap", "pl3xmap", "journeymap");
    public static boolean webEnabled = false, webHideClaims = false;
    /** Chunks read per server tick while exporting; minutes between automatic exports (0 = only by command). */
    public static int webChunksPerTick = WEB_CHUNKS_DEFAULT, webIntervalMinutes = WEB_INTERVAL_DEFAULT;
    /** Empty = {@code <world>/simplemaps-web}; a relative path is relative to the world folder. */
    public static String webExportDir = "";
    public static java.util.List<String> webAutoOffWith = WEB_AUTO_OFF_DEFAULT;
    /**
     * Whether the module is active. Standalone it always is; bundled in SimpleBuilding the host's config switch
     * "Enable Simple Maps" ({@code enableSimpleMaps} in {@code simplebuilding.json}, read once at load, restart to
     * change) can turn it off: the items stay registered (stable registries) but do nothing and are not offered.
     */
    public static boolean enabled = true;
    public static final String HOST_FILE = "simplebuilding.json", HOST_KEY = "enableSimpleMaps";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configDir;

    /** Whether the items also go into the Vanilla creative tabs (shared per-mod switch, default on). */
    public static boolean vanillaTabs() {
        return com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(configDir, SimpleMaps.MOD_ID);
    }

    public static void load(Path dir) {
        configDir = dir;
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleMaps.LOG::warn);
        if (read != null) json = read;
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
        layers = getBool(json, "layers", true);
        webEnabled = getBool(json, "webEnabled", false);
        webHideClaims = getBool(json, "webHideClaims", false);
        webChunksPerTick = clamp(getInt(json, "webChunksPerTick", WEB_CHUNKS_DEFAULT), WEB_CHUNKS_MIN, WEB_CHUNKS_MAX);
        webIntervalMinutes = clamp(getInt(json, "webIntervalMinutes", WEB_INTERVAL_DEFAULT), 0, WEB_INTERVAL_MAX);
        webExportDir = getString(json, "webExportDir", "");
        webAutoOffWith = getIds(json, "webAutoOffWith", WEB_AUTO_OFF_DEFAULT);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("revealRadius", revealRadius);
        o.addProperty("maxTilesPerMap", maxTilesPerMap);
        o.addProperty("allowCopy", allowCopy);
        o.addProperty("allowExtend", allowExtend);
        o.addProperty("allowCombine", allowCombine);
        o.addProperty("layers", layers);
        o.addProperty("webEnabled", webEnabled);
        o.addProperty("webExportDir", webExportDir);
        o.addProperty("webChunksPerTick", webChunksPerTick);
        o.addProperty("webIntervalMinutes", webIntervalMinutes);
        o.addProperty("webHideClaims", webHideClaims);
        com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
        webAutoOffWith.forEach(ids::add);
        o.add("webAutoOffWith", ids);
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

    private static String getString(JsonObject json, String key, String fallback) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString().trim() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    /** Valid mod ids only (like Simple Tweaks' autoOffWith), at most 16; a missing or broken entry keeps the default. */
    private static java.util.List<String> getIds(JsonObject json, String key, java.util.List<String> fallback) {
        try {
            if (!json.has(key) || !json.get(key).isJsonArray()) return fallback;
            java.util.List<String> out = new java.util.ArrayList<>();
            for (var e : json.getAsJsonArray(key)) {
                String id = e.isJsonPrimitive() ? e.getAsString() : "";
                if (id.matches("[a-z][a-z0-9_.-]{0,63}") && !out.contains(id) && out.size() < 16) out.add(id);
            }
            return java.util.List.copyOf(out);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private MapsConfig() {}
}
