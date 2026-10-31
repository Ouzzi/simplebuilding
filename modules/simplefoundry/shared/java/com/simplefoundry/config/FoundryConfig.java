package com.simplefoundry.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplefoundry.SimpleFoundry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplefoundry-server.json}: server authority, every number clamped to a hard limit; missing or broken keys
 * use the defaults and the file is rewritten complete.
 */
public final class FoundryConfig {
    public static final String FILE = "simplefoundry-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "maxChambers", "meltTicksPerUnit", "castTicks");
    /** Hard upper limits (A9): a server cannot raise them. */
    public static final int MAX_CHAMBERS_LIMIT = 512, MAX_AXIS = 32;
    public static final int CHAMBERS_DEFAULT = 256, MELT_DEFAULT = 4, CAST_DEFAULT = 100;

    public static boolean enabled = true;
    /** Most melting chambers one foundry may have. */
    public static int maxChambers = CHAMBERS_DEFAULT;
    /** Ticks to melt one unit (an ingot is nine units). */
    public static int meltTicksPerUnit = MELT_DEFAULT;
    /** Ticks between two pours, and the time an ingot needs to set in the tray. */
    public static int castTicks = CAST_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleFoundry.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleFoundry.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        maxChambers = clamp(integer(json, "maxChambers", CHAMBERS_DEFAULT), 1, MAX_CHAMBERS_LIMIT);
        meltTicksPerUnit = clamp(integer(json, "meltTicksPerUnit", MELT_DEFAULT), 1, 40);
        castTicks = clamp(integer(json, "castTicks", CAST_DEFAULT), 20, 1200);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("maxChambers", maxChambers);
        o.addProperty("meltTicksPerUnit", meltTicksPerUnit);
        o.addProperty("castTicks", castTicks);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsInt() : fallback;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static {
        reset();
    }

    private FoundryConfig() {}
}
