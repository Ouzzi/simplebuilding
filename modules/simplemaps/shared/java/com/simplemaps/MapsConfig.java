package com.simplemaps;

import com.simplebuilding.framework.api.ConfigFiles;
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
    /** Picture maps (AUD-6, concept 19.10.): a client-converted picture becomes Vanilla maps; on by default (owner 24.10.). Side = longer edge in maps, permission = Vanilla level 0..4. */
    public static final int PICTURE_SIDE_DEFAULT = 3, PICTURE_SIDE_MIN = 1, PICTURE_SIDE_MAX = 4, PICTURE_PERMISSION_MAX = 4;
    public static boolean pictureMap = true;
    public static int pictureMapMaxSide = PICTURE_SIDE_DEFAULT, pictureMapPermission = 0;
    /** Web map export (stage 1, static files): off by default; see {@code docs/SIMPLEMAPS-WEBKARTE.md}. */
    public static final int WEB_CHUNKS_DEFAULT = 32, WEB_CHUNKS_MIN = 1, WEB_CHUNKS_MAX = 512;
    public static final int WEB_INTERVAL_DEFAULT = 0, WEB_INTERVAL_MAX = 1440;
    /** Mods that bring their own web map: one of them loaded switches the web export off (autoOffWith). */
    public static final java.util.List<String> WEB_AUTO_OFF_DEFAULT = java.util.List.of("bluemap", "dynmap", "squaremap", "pl3xmap", "journeymap");
    public static boolean webEnabled = false, webHideClaims = false;
    /** Shops (Simple Shops' map layer) in the web map's markers.json: off by default, owner names are in it (privacy). */
    public static boolean webShops = false;
    /**
     * Stage 3: waypoints players shared themselves ({@code /simplemaps web waypoints share}) in markers.json, off by default;
     * how protected chunks look with {@code webHideClaims}: {@code hide} (empty, default) or {@code pixelate} (one colour per chunk).
     */
    public static final String WEB_CLAIM_HIDE = "hide", WEB_CLAIM_PIXELATE = "pixelate";
    public static boolean webWaypoints = false;
    public static String webClaimStyle = WEB_CLAIM_HIDE;
    /** Chunks read per server tick while exporting; minutes between automatic exports (0 = only by command). */
    public static int webChunksPerTick = WEB_CHUNKS_DEFAULT, webIntervalMinutes = WEB_INTERVAL_DEFAULT;
    /** Empty = {@code <world>/simplemaps-web}; a relative path is relative to the world folder. */
    public static String webExportDir = "";
    public static java.util.List<String> webAutoOffWith = WEB_AUTO_OFF_DEFAULT;
    /**
     * Built-in web server (stage 2, read only, {@code docs/SIMPLEMAPS-WEBKARTE.md}): off by default and only with
     * {@code webEnabled}; binds to the local machine unless {@code webHttpBind} says otherwise; empty token = none.
     */
    public static final String WEB_HTTP_BIND_DEFAULT = "127.0.0.1";
    public static final int WEB_HTTP_PORT_DEFAULT = 8124, WEB_HTTP_PORT_MIN = 1, WEB_HTTP_PORT_MAX = 65535;
    public static final int WEB_HTTP_RATE_DEFAULT = 1200, WEB_HTTP_RATE_MIN = 60, WEB_HTTP_RATE_MAX = 20000;
    public static final int WEB_LIVE_DEFAULT = 2, WEB_LIVE_MIN = 1, WEB_LIVE_MAX = 60;
    public static boolean webHttpEnabled = false;
    public static String webHttpBind = WEB_HTTP_BIND_DEFAULT, webHttpToken = "";
    /** Port; requests per minute and client address; seconds between two live.json snapshots (players). */
    public static int webHttpPort = WEB_HTTP_PORT_DEFAULT, webHttpRequestsPerMinute = WEB_HTTP_RATE_DEFAULT, webLiveSeconds = WEB_LIVE_DEFAULT;
    /**
     * Whether the module is active. Standalone it always is; bundled in SimpleBuilding the host's config switch
     * "Enable Simple Maps" ({@code enableSimpleMaps} in {@code simplebuilding.json}, read once at load, restart to
     * change) can turn it off: the items stay registered (stable registries) but do nothing and are not offered.
     */
    public static boolean enabled = true;
    /**
     * Mega wayfinder map (owner B23-K2): which item unlocks a further dimension at the anvil ({@code dimension id -> item id}
     * or {@code "any"} = any block while standing in that dimension), the rule of every dimension not listed ({@code "any"}
     * or {@code ""} = none) and the anvil's level cost.
     */
    public static final int MEGA_LEVELS_DEFAULT = 5, MEGA_LEVELS_MIN = 1, MEGA_LEVELS_MAX = 39;
    public static java.util.Map<String, String> megaUnlockItems = java.util.Map.of();
    public static String megaUnlockDefault = MegaMaps.ANY;
    public static int megaUnlockLevels = MEGA_LEVELS_DEFAULT;
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
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
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
        pictureMap = getBool(json, "pictureMap", true);
        pictureMapMaxSide = clamp(getInt(json, "pictureMapMaxSide", PICTURE_SIDE_DEFAULT), PICTURE_SIDE_MIN, PICTURE_SIDE_MAX);
        pictureMapPermission = clamp(getInt(json, "pictureMapPermission", 0), 0, PICTURE_PERMISSION_MAX);
        webEnabled = getBool(json, "webEnabled", false);
        webHideClaims = getBool(json, "webHideClaims", false);
        webShops = getBool(json, "webShops", false);
        webWaypoints = getBool(json, "webWaypoints", false);
        webClaimStyle = WEB_CLAIM_PIXELATE.equals(getString(json, "webClaimStyle", WEB_CLAIM_HIDE).trim().toLowerCase(java.util.Locale.ROOT))
                ? WEB_CLAIM_PIXELATE : WEB_CLAIM_HIDE;
        webChunksPerTick = clamp(getInt(json, "webChunksPerTick", WEB_CHUNKS_DEFAULT), WEB_CHUNKS_MIN, WEB_CHUNKS_MAX);
        webIntervalMinutes = clamp(getInt(json, "webIntervalMinutes", WEB_INTERVAL_DEFAULT), 0, WEB_INTERVAL_MAX);
        webExportDir = getString(json, "webExportDir", "");
        webAutoOffWith = getIds(json, "webAutoOffWith", WEB_AUTO_OFF_DEFAULT);
        webHttpEnabled = getBool(json, "webHttpEnabled", false);
        String bind = getString(json, "webHttpBind", WEB_HTTP_BIND_DEFAULT).trim();
        webHttpBind = bind.isEmpty() ? WEB_HTTP_BIND_DEFAULT : bind;
        webHttpPort = clamp(getInt(json, "webHttpPort", WEB_HTTP_PORT_DEFAULT), WEB_HTTP_PORT_MIN, WEB_HTTP_PORT_MAX);
        webHttpToken = getString(json, "webHttpToken", "").trim();
        webHttpRequestsPerMinute = clamp(getInt(json, "webHttpRequestsPerMinute", WEB_HTTP_RATE_DEFAULT), WEB_HTTP_RATE_MIN, WEB_HTTP_RATE_MAX);
        webLiveSeconds = clamp(getInt(json, "webLiveSeconds", WEB_LIVE_DEFAULT), WEB_LIVE_MIN, WEB_LIVE_MAX);
        megaUnlockItems = getRules(json, "megaUnlockItems");
        String rule = getString(json, "megaUnlockDefault", MegaMaps.ANY);
        megaUnlockDefault = rule.isEmpty() || rule.equals(MegaMaps.ANY) ? rule : MegaMaps.ANY;
        megaUnlockLevels = clamp(getInt(json, "megaUnlockLevels", MEGA_LEVELS_DEFAULT), MEGA_LEVELS_MIN, MEGA_LEVELS_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("revealRadius", revealRadius);
        o.addProperty("maxTilesPerMap", maxTilesPerMap);
        o.addProperty("allowCopy", allowCopy);
        o.addProperty("allowExtend", allowExtend);
        o.addProperty("allowCombine", allowCombine);
        o.addProperty("layers", layers);
        o.addProperty("pictureMap", pictureMap);
        o.addProperty("pictureMapMaxSide", pictureMapMaxSide);
        o.addProperty("pictureMapPermission", pictureMapPermission);
        o.addProperty("webEnabled", webEnabled);
        o.addProperty("webExportDir", webExportDir);
        o.addProperty("webChunksPerTick", webChunksPerTick);
        o.addProperty("webIntervalMinutes", webIntervalMinutes);
        o.addProperty("webHideClaims", webHideClaims);
        o.addProperty("webShops", webShops);
        o.addProperty("webWaypoints", webWaypoints);
        o.addProperty("webClaimStyle", webClaimStyle);
        com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
        webAutoOffWith.forEach(ids::add);
        o.add("webAutoOffWith", ids);
        o.addProperty("webHttpEnabled", webHttpEnabled);
        o.addProperty("webHttpBind", webHttpBind);
        o.addProperty("webHttpPort", webHttpPort);
        o.addProperty("webHttpToken", webHttpToken);
        o.addProperty("webHttpRequestsPerMinute", webHttpRequestsPerMinute);
        o.addProperty("webLiveSeconds", webLiveSeconds);
        JsonObject rules = new JsonObject();
        megaUnlockItems.forEach(rules::addProperty);
        o.add("megaUnlockItems", rules);
        o.addProperty("megaUnlockDefault", megaUnlockDefault);
        o.addProperty("megaUnlockLevels", megaUnlockLevels);
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

    /** {@code dimension id -> item id | "any"}; invalid ids are dropped, at most {@link MegaMaps#MAX} rules. */
    private static java.util.Map<String, String> getRules(JsonObject json, String key) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        try {
            if (!json.has(key) || !json.get(key).isJsonObject()) return java.util.Map.of();
            for (var e : json.getAsJsonObject(key).entrySet()) {
                String value = e.getValue().isJsonPrimitive() ? e.getValue().getAsString().trim() : "";
                if (net.minecraft.resources.Identifier.tryParse(e.getKey()) == null || MegaMaps.builtIn(e.getKey()) || out.size() >= MegaMaps.MAX) continue;
                if (value.equals(MegaMaps.ANY) || !value.isEmpty() && net.minecraft.resources.Identifier.tryParse(value) != null) out.put(e.getKey(), value);
            }
        } catch (Exception e) {
            return java.util.Map.of();
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private MapsConfig() {}
}
