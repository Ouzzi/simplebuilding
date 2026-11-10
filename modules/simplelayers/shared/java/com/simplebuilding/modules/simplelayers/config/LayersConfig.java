package com.simplebuilding.modules.simplelayers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplelayers-server.json} (server authority, A9; every number clamped). Two groups like the two
 * layers: {@code clouds.*} (Cloud Sea) and {@code backrooms.*}. Missing or broken keys use the defaults and the file is
 * rewritten complete. Lang: {@code text.autoconfig.simplelayers.option.<group>.<key>} (+ {@code .@Tooltip}).
 */
public final class LayersConfig {
    public static final String FILE = "simplelayers-server.json";
    public static final List<String> KEYS = List.of("clouds.enabled", "clouds.entryY", "clouds.bottomlessPits", "clouds.fogHint",
            "backrooms.enabled", "backrooms.entranceRarity", "backrooms.voidToBackrooms", "backrooms.unbreakableWalls",
            "backrooms.supplies", "backrooms.listener", "backrooms.shaftRarity", "backrooms.ambientVolume");
    public static final int MIN_ENTRY_Y = 400, MAX_ENTRY_Y = 1000, DEFAULT_ENTRY_Y = 700;

    /** Rising above {@link #cloudEntryY} in the Overworld leads into the Cloud Sea. */
    public static boolean cloudsEnabled;
    public static int cloudEntryY;
    /** "Hard" mode: mist pits go right through the island (default: a hidden hollow with a cloud floor below). */
    public static boolean bottomlessPits;
    /** Mist blocks shed a rare falling haze particle so careful players spot the pit (A14). */
    public static boolean fogHint;
    public static boolean backroomsEnabled;
    /** One Overworld chunk in this many gets a Glitched Bedrock (4..10000). */
    public static int entranceRarity;
    /** Falling far below the Overworld floor lands in the Backrooms instead of the void (default off). */
    public static boolean voidToBackrooms;
    /**
     * Players in the Backrooms get the Backroom effect: survival players cannot break the walls, floors and ceilings, and no
     * piston moves them there - only Simple Machines' Netherite Piston breaks them (LAYERS13).
     */
    public static boolean unbreakableWalls;
    /** Wooden office furniture and cobwebs in some Backrooms cells: wood and string to survive and craft (LAYERS13). */
    public static boolean supplies;
    /** "The Listener": rare approaching footsteps behind the player, no damage (default off until the owner accepts it). */
    public static boolean listener;
    /** BACK18: one Overworld chunk in this many gets the fenced-off entrance shaft (0 = none; only on flat, dry ground). */
    public static int shaftRarity;
    /** BACK18: volume of the quiet random sounds inside the Backrooms in percent (0 = silent; the Ambient slider applies too). */
    public static int ambientVolume;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        apply(new JsonObject());
    }

    private LayersConfig() {}

    /** Overworld height the Cloud Sea gives back falling players (20 blocks below the entry: no bouncing). */
    public static int cloudReturnY() {
        return cloudEntryY - 20;
    }

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleLayers.LOG::warn);
        apply(read == null ? new JsonObject() : read);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleLayers.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonObject clouds = group(json, "clouds"), back = group(json, "backrooms");
        cloudsEnabled = bool(clouds, "enabled", true);
        cloudEntryY = integer(clouds, "entryY", DEFAULT_ENTRY_Y, MIN_ENTRY_Y, MAX_ENTRY_Y);
        bottomlessPits = bool(clouds, "bottomlessPits", false);
        fogHint = bool(clouds, "fogHint", true);
        backroomsEnabled = bool(back, "enabled", true);
        entranceRarity = integer(back, "entranceRarity", 48, 4, 10000);
        voidToBackrooms = bool(back, "voidToBackrooms", false);
        unbreakableWalls = bool(back, "unbreakableWalls", true);
        supplies = bool(back, "supplies", true);
        listener = bool(back, "listener", false);
        shaftRarity = integer(back, "shaftRarity", 200, 0, 100000);
        ambientVolume = integer(back, "ambientVolume", 50, 0, 100);
        com.simplebuilding.modules.simplelayers.world.gen.LobbyGen.supplies = supplies;
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject(), clouds = new JsonObject(), back = new JsonObject();
        clouds.addProperty("enabled", cloudsEnabled);
        clouds.addProperty("entryY", cloudEntryY);
        clouds.addProperty("bottomlessPits", bottomlessPits);
        clouds.addProperty("fogHint", fogHint);
        back.addProperty("enabled", backroomsEnabled);
        back.addProperty("entranceRarity", entranceRarity);
        back.addProperty("voidToBackrooms", voidToBackrooms);
        back.addProperty("unbreakableWalls", unbreakableWalls);
        back.addProperty("supplies", supplies);
        back.addProperty("listener", listener);
        back.addProperty("shaftRarity", shaftRarity);
        back.addProperty("ambientVolume", ambientVolume);
        o.add("clouds", clouds);
        o.add("backrooms", back);
        return o;
    }

    private static JsonObject group(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static boolean bool(JsonObject json, String key, boolean def) {
        try {
            JsonElement e = json.get(key);
            return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static int integer(JsonObject json, String key, int def, int min, int max) {
        try {
            JsonElement e = json.get(key);
            int v = e != null && e.isJsonPrimitive() ? e.getAsInt() : def;
            return Math.max(min, Math.min(max, v));
        } catch (RuntimeException e) {
            return def;
        }
    }
}
