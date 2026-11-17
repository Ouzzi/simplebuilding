package com.simpleores.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleores.SimpleOres;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client config {@code config/simpleores-client.json} (N78b, docs/ai/PLAN-N78B-2026-11-17.md): "3D grates". On (default) the metal grates and
 * the Vanilla copper grates show bars with depth and their inner sides; off they are flat cubes like Vanilla (6 quads instead of 60 to 324).
 * Read while the block models load ({@link com.simpleores.client.GrateModels}), so it works on every loader without a client entry point;
 * Fabric also offers it in {@code /simpleclient simpleores} and the config screen, which reload the resources after a change.
 */
public final class OresClientConfig {
    public static final String FILE = "simpleores-client.json";
    /** The only key of the file (check_data reads it). */
    public static final String KEY = "grates3d";

    public static volatile boolean grates3d = true;
    private static volatile boolean loaded;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OresClientConfig() {}

    /** The option, reading the file once when nobody loaded it yet (NeoForge/Forge: the first model load). */
    public static boolean grates3d() {
        if (!loaded) {
            synchronized (OresClientConfig.class) {
                if (!loaded) load(SimpleOres.configDir != null ? SimpleOres.configDir : Path.of("config"));
            }
        }
        return grates3d;
    }

    public static synchronized void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject json = new JsonObject();
        if (file != null) {
            JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleOres.LOG::warn);
            if (read != null) json = read;
        }
        apply(json);
        loaded = true;
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleOres.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement v = json.get(KEY);
        grates3d = v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean() || v.getAsBoolean();
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty(KEY, grates3d);
        return o;
    }
}
