package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.ConfigFiles;
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
    /** Hollow Watcher forest rule (N66): seconds in a Watcher core biome without a break, then this chance per 30 s roll. */
    public static int hollowWatcherForestSeconds = FearLogic.FOREST_SECONDS;
    public static float hollowWatcherForestChance = FearLogic.FOREST_CHANCE;
    /** Hollow Watcher gaze zones: half angles in degrees at FOV 70 (scaled with the player's FOV): stands still / slow retreat / direct retreat. */
    /** Hollow Watcher: direct-look half angle (aggressive), follow/teleport range in blocks, teleport interval in seconds. */
    public static float watcherGazeDirect = FearLogic.GAZE_DIRECT_DEG, watcherRange = FearLogic.RANGE_DEFAULT, watcherTeleportSeconds = FearLogic.TELEPORT_SECONDS_DEFAULT;
    public static float watcherGazeStare = FearLogic.GAZE_STARE_DEG, watcherGazeSlow = FearLogic.GAZE_SLOW_DEG, watcherGazeEdge = FearLogic.GAZE_EDGE_DEG;
    /** Blighted/Abyssal Wither: the helmet on the middle skull on/off (off: the figure summons the Vanilla Wither). */
    public static boolean witherSummonEnabled = true;
    /** Blighted Wither phase 3 wilts plants and natural leaves (also needs the mobGriefing game rule). */
    public static boolean witherWilt = true;
    /** Trick Mirror: off by default (owner N61 E11); recipe and drop are removed, the item only works and shows in the creative tab when on. */
    public static boolean trickMirrorEnabled = false;

    /** Hollow Howler rage pulse (like the Warden): Darkness for hunted players nearby, on a heartbeat, only in the rage phase. */
    public static boolean howlerRagePulse = true;
    public static int howlerPulseInterval = HowlerLogic.PULSE_INTERVAL_TICKS, howlerPulseTicks = HowlerLogic.PULSE_DARKNESS_TICKS, howlerPulseRadius = HowlerLogic.PULSE_RADIUS;
    /** Miniboss stats of the Hollow Howler (Nachtrag 68; Warden: 500 health, 0 armor, 30 attack): applied when it spawns. Size in percent. */
    public static int howlerHealth = (int) HowlerLogic.MAX_HEALTH, howlerAttack = (int) HowlerLogic.ATTACK, howlerScalePercent = (int) Math.round(HowlerLogic.SCALE * 100);
    /** Phase 2+ sculk jaws under the hunted players: at most {@code howlerJawMax} alive at once, one new every {@code howlerJawInterval} ticks. */
    public static int howlerJawMax = HowlerLogic.JAW_MAX, howlerJawInterval = HowlerLogic.JAW_INTERVAL_TICKS;
    /** Boss bars of the bosses and minibosses (phase notches, colors); the bar shows for players within this many blocks. */
    public static boolean minibossBars = true;
    public static int bossBarRange = 48;
    /** Client config {@code simplemobs-client.json}: "Reduce motion" turns off the camera shake of the Howler's shockwave and shriek. */
    public static final String CLIENT_FILE = "simplemobs-client.json";
    public static boolean reduceMotion = false;

    private MobsConfig() {}

    /** Reads (and writes if missing) the client config; unreadable files fall back to the defaults. */
    public static void loadClient(Path dir) {
        Path file = dir == null ? null : dir.resolve(CLIENT_FILE);
        try {
            if (file != null && Files.isRegularFile(file)) {
                JsonObject read = GSON.fromJson(Files.readString(file), JsonObject.class);
                if (read != null) reduceMotion = bool(read, "reduceMotion", false);
            } else if (file != null) {
                Files.createDirectories(dir);
                JsonObject o = new JsonObject();
                o.addProperty("reduceMotion", false);
                ConfigFiles.writeAtomic(file, GSON.toJson(o));
            }
        } catch (Exception e) {
            LOG.warn("Could not read {}: {}", CLIENT_FILE, e.toString());
        }
    }

    private static Path configDir;

    /** Whether the items also go into the Vanilla creative tabs (shared per-mod switch, default on). */
    public static boolean vanillaTabs() {
        return com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(configDir, MobsRegistry.ID);
    }

    public static void load(Path dir) {
        configDir = dir;
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
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
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
        howlerRagePulse = bool(json, "howlerRagePulse", true);
        howlerPulseInterval = Math.max(20, Math.min(1200, integer(json, "howlerPulseInterval", HowlerLogic.PULSE_INTERVAL_TICKS)));
        howlerPulseTicks = Math.max(20, Math.min(1200, integer(json, "howlerPulseTicks", HowlerLogic.PULSE_DARKNESS_TICKS)));
        howlerPulseRadius = Math.max(4, Math.min(64, integer(json, "howlerPulseRadius", HowlerLogic.PULSE_RADIUS)));
        howlerHealth = Math.max(20, Math.min(10000, integer(json, "howlerHealth", (int) HowlerLogic.MAX_HEALTH)));
        howlerAttack = Math.max(1, Math.min(500, integer(json, "howlerAttack", (int) HowlerLogic.ATTACK)));
        howlerScalePercent = Math.max(50, Math.min(300, integer(json, "howlerScalePercent", (int) Math.round(HowlerLogic.SCALE * 100))));
        howlerJawMax = Math.max(1, Math.min(30, integer(json, "howlerJawMax", HowlerLogic.JAW_MAX)));
        howlerJawInterval = Math.max(20, Math.min(1200, integer(json, "howlerJawInterval", HowlerLogic.JAW_INTERVAL_TICKS)));
        minibossBars = bool(json, "minibossBars", true);
        bossBarRange = Math.max(8, Math.min(256, integer(json, "bossBarRange", 48)));
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
        hollowWatcherForestSeconds = Math.max(10, Math.min(3600, integer(json, "hollowWatcherForestSeconds", FearLogic.FOREST_SECONDS)));
        hollowWatcherForestChance = Math.max(0.0f, Math.min(1.0f, num(json, "hollowWatcherForestChance", FearLogic.FOREST_CHANCE)));
        float stare = num(json, "watcherGazeStare", FearLogic.GAZE_STARE_DEG), slow = num(json, "watcherGazeSlow", FearLogic.GAZE_SLOW_DEG), edge = num(json, "watcherGazeEdge", FearLogic.GAZE_EDGE_DEG);
        watcherGazeStare = Math.max(1.0f, Math.min(60.0f, stare));
        watcherGazeDirect = Math.max(0.5f, Math.min(watcherGazeStare, num(json, "watcherGazeDirect", FearLogic.GAZE_DIRECT_DEG)));
        watcherRange = Math.max(8.0f, Math.min(128.0f, num(json, "watcherRange", FearLogic.RANGE_DEFAULT)));
        watcherTeleportSeconds = Math.max(1.0f, Math.min(120.0f, num(json, "watcherTeleportSeconds", FearLogic.TELEPORT_SECONDS_DEFAULT)));
        watcherGazeSlow = Math.max(watcherGazeStare, Math.min(80.0f, slow));
        watcherGazeEdge = Math.max(watcherGazeSlow, Math.min(120.0f, edge));
        witherSummonEnabled = bool(json, "witherSummonEnabled", true);
        witherWilt = bool(json, "witherWilt", true);
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
        o.addProperty("howlerRagePulse", howlerRagePulse);
        o.addProperty("howlerPulseInterval", howlerPulseInterval);
        o.addProperty("howlerPulseTicks", howlerPulseTicks);
        o.addProperty("howlerPulseRadius", howlerPulseRadius);
        o.addProperty("howlerHealth", howlerHealth);
        o.addProperty("howlerAttack", howlerAttack);
        o.addProperty("howlerScalePercent", howlerScalePercent);
        o.addProperty("howlerJawMax", howlerJawMax);
        o.addProperty("howlerJawInterval", howlerJawInterval);
        o.addProperty("minibossBars", minibossBars);
        o.addProperty("bossBarRange", bossBarRange);
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
        o.addProperty("hollowWatcherForestSeconds", hollowWatcherForestSeconds);
        o.addProperty("hollowWatcherForestChance", hollowWatcherForestChance);
        o.addProperty("watcherGazeStare", watcherGazeStare);
        o.addProperty("watcherGazeDirect", watcherGazeDirect);
        o.addProperty("watcherRange", watcherRange);
        o.addProperty("watcherTeleportSeconds", watcherTeleportSeconds);
        o.addProperty("watcherGazeSlow", watcherGazeSlow);
        o.addProperty("watcherGazeEdge", watcherGazeEdge);
        o.addProperty("witherSummonEnabled", witherSummonEnabled);
        o.addProperty("witherWilt", witherWilt);
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
