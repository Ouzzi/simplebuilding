package com.simplebuilding.modules.simplemobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server config {@code config/simplemobs-server.json}. Read once at start; an unreadable file is renamed to
 * {@code .broken} and the defaults are written instead (same policy as the other modules, without the framework jar).
 */
public final class MobsConfig {
    public static final String FILE = "simplemobs-server.json";
    private static final Logger LOG = LoggerFactory.getLogger("simplemobs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Hollow Howler: the summoning ritual on/off (the mob itself and its spawn egg stay registered). */
    public static boolean howlerEnabled = true;
    /** Highest light (block + sky, like monster spawns) at the skull for the ritual to start and keep running. */
    public static int howlerMaxLight = HowlerLogic.MAX_LIGHT;
    public static boolean howlerSculkTrail = true;
    public static int howlerSculkTrailMax = HowlerLogic.TRAIL_MAX;
    public static boolean howlerTransformSculk = true;
    public static boolean howlerHunger = true;
    public static int howlerDespawnMinutes = 10;
    public static List<String> howlerAllowDimensions = new ArrayList<>(List.of("minecraft:overworld"));
    /** Shellker: four guards at every newly placed End gateway. */
    public static boolean shellkerGatewayGuards = true;
    /** Soulfire Blaze: natural spawning in Soul Sand Valleys and its chance per 10 s roll. */
    public static boolean soulfireBlazeSpawns = true;
    public static float soulfireBlazeChance = EndNetherLogic.SOUL_CHANCE;
    /** Dread: the summoning figure on/off (the mob and its spawn egg stay registered). */
    public static boolean dreadSummonEnabled = true;
    /** Hollow Watcher: natural spawning on the outer End islands and its chance per 30 s roll. */
    public static boolean hollowWatcherSpawns = true;
    public static float hollowWatcherChance = FearLogic.WATCHER_CHANCE;
    /** Hollow Watcher gaze zones: half angles in degrees at FOV 70 (scaled with the player's FOV): stands still / slow retreat / direct retreat. */
    public static float watcherGazeStare = FearLogic.GAZE_STARE_DEG, watcherGazeSlow = FearLogic.GAZE_SLOW_DEG, watcherGazeEdge = FearLogic.GAZE_EDGE_DEG;
    /** Blighted/Abyssal Wither: the helmet on the middle skull on/off (off: the figure summons the Vanilla Wither). */
    public static boolean witherSummonEnabled = true;
    /** Blighted Wither phase 3 wilts plants and natural leaves (also needs the mobGriefing game rule). */
    public static boolean witherWilt = true;
    /** Biome Brush on/off, only for operators, and its radius in 4-block biome cells (0 = one cell, max 2). */
    public static boolean biomeBrushEnabled = true;
    public static boolean biomeBrushOpsOnly = false;
    public static int biomeBrushRadius = 1;
    /** Trick Mirror: off by default (owner N61 E11); recipe and drop are removed, the item only works and shows in the creative tab when on. */
    public static boolean trickMirrorEnabled = false;

    private MobsConfig() {}

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject json = new JsonObject();
        if (file != null && Files.isRegularFile(file)) {
            try {
                JsonObject read = GSON.fromJson(Files.readString(file), JsonObject.class);
                if (read == null) throw new IllegalStateException("file is empty");
                json = read;
            } catch (Exception e) {
                Path broken = file.resolveSibling(FILE + ".broken-" + System.currentTimeMillis());
                try { Files.move(file, broken); } catch (Exception ignored) { }
                LOG.warn("Unreadable {}, using defaults ({}); old file kept as {}", FILE, e.toString(), broken.getFileName());
            }
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads and clamps every value; missing or broken keys keep their defaults. */
    public static void apply(JsonObject json) {
        howlerEnabled = bool(json, "howlerEnabled", true);
        howlerMaxLight = Math.max(0, Math.min(15, integer(json, "howlerMaxLight", HowlerLogic.MAX_LIGHT)));
        howlerSculkTrail = bool(json, "howlerSculkTrail", true);
        howlerSculkTrailMax = Math.max(0, Math.min(200, integer(json, "howlerSculkTrailMax", HowlerLogic.TRAIL_MAX)));
        howlerTransformSculk = bool(json, "howlerTransformSculk", true);
        howlerHunger = bool(json, "howlerHunger", true);
        howlerDespawnMinutes = Math.max(1, Math.min(1440, integer(json, "howlerDespawnMinutes", 10)));
        List<String> dims = new ArrayList<>();
        try {
            if (json.has("howlerAllowDimensions")) for (JsonElement e : json.getAsJsonArray("howlerAllowDimensions")) dims.add(e.getAsString());
            else dims.add("minecraft:overworld");
        } catch (Exception e) {
            dims = new ArrayList<>(List.of("minecraft:overworld"));
        }
        howlerAllowDimensions = dims;
        shellkerGatewayGuards = bool(json, "shellkerGatewayGuards", true);
        soulfireBlazeSpawns = bool(json, "soulfireBlazeSpawns", true);
        float chance;
        try { chance = json.has("soulfireBlazeChance") ? json.get("soulfireBlazeChance").getAsFloat() : EndNetherLogic.SOUL_CHANCE; } catch (Exception e) { chance = EndNetherLogic.SOUL_CHANCE; }
        soulfireBlazeChance = Math.max(0.0f, Math.min(1.0f, chance));
        dreadSummonEnabled = bool(json, "dreadSummonEnabled", true);
        hollowWatcherSpawns = bool(json, "hollowWatcherSpawns", true);
        float watcher;
        try { watcher = json.has("hollowWatcherChance") ? json.get("hollowWatcherChance").getAsFloat() : FearLogic.WATCHER_CHANCE; } catch (Exception e) { watcher = FearLogic.WATCHER_CHANCE; }
        hollowWatcherChance = Math.max(0.0f, Math.min(1.0f, watcher));
        float stare = num(json, "watcherGazeStare", FearLogic.GAZE_STARE_DEG), slow = num(json, "watcherGazeSlow", FearLogic.GAZE_SLOW_DEG), edge = num(json, "watcherGazeEdge", FearLogic.GAZE_EDGE_DEG);
        watcherGazeStare = Math.max(1.0f, Math.min(60.0f, stare));
        watcherGazeSlow = Math.max(watcherGazeStare, Math.min(80.0f, slow));
        watcherGazeEdge = Math.max(watcherGazeSlow, Math.min(120.0f, edge));
        witherSummonEnabled = bool(json, "witherSummonEnabled", true);
        witherWilt = bool(json, "witherWilt", true);
        biomeBrushEnabled = bool(json, "biomeBrushEnabled", true);
        biomeBrushOpsOnly = bool(json, "biomeBrushOpsOnly", false);
        biomeBrushRadius = Math.max(0, Math.min(2, integer(json, "biomeBrushRadius", 1)));
        trickMirrorEnabled = bool(json, "trickMirrorEnabled", false);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("howlerEnabled", howlerEnabled);
        o.addProperty("howlerMaxLight", howlerMaxLight);
        o.addProperty("howlerSculkTrail", howlerSculkTrail);
        o.addProperty("howlerSculkTrailMax", howlerSculkTrailMax);
        o.addProperty("howlerTransformSculk", howlerTransformSculk);
        o.addProperty("howlerHunger", howlerHunger);
        o.addProperty("howlerDespawnMinutes", howlerDespawnMinutes);
        JsonArray dims = new JsonArray();
        howlerAllowDimensions.forEach(dims::add);
        o.add("howlerAllowDimensions", dims);
        o.addProperty("shellkerGatewayGuards", shellkerGatewayGuards);
        o.addProperty("soulfireBlazeSpawns", soulfireBlazeSpawns);
        o.addProperty("soulfireBlazeChance", soulfireBlazeChance);
        o.addProperty("dreadSummonEnabled", dreadSummonEnabled);
        o.addProperty("hollowWatcherSpawns", hollowWatcherSpawns);
        o.addProperty("hollowWatcherChance", hollowWatcherChance);
        o.addProperty("watcherGazeStare", watcherGazeStare);
        o.addProperty("watcherGazeSlow", watcherGazeSlow);
        o.addProperty("watcherGazeEdge", watcherGazeEdge);
        o.addProperty("witherSummonEnabled", witherSummonEnabled);
        o.addProperty("witherWilt", witherWilt);
        o.addProperty("biomeBrushEnabled", biomeBrushEnabled);
        o.addProperty("biomeBrushOpsOnly", biomeBrushOpsOnly);
        o.addProperty("biomeBrushRadius", biomeBrushRadius);
        o.addProperty("trickMirrorEnabled", trickMirrorEnabled);
        return o;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        try { return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback; } catch (Exception e) { return fallback; }
    }

    private static float num(JsonObject json, String key, float fallback) {
        try { return json.has(key) ? json.get(key).getAsFloat() : fallback; } catch (Exception e) { return fallback; }
    }

    private static int integer(JsonObject json, String key, int fallback) {
        try { return json.has(key) ? json.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; }
    }
}
