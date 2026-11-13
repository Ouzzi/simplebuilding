package com.simpleoctets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleoctets-server.json} (PLAN40 simpleoctets-4, A12): the two places where Simple Octets changes a
 * Vanilla input can be switched off. {@code features.melonSlicePlacing}: sneak + right-click with a melon slice on a block places a
 * melon octet instead of eating it. {@code features.cornerBreaking}: sneaking with a sledgehammer breaks only the looked-at corner of a
 * full block (the mixin on every block then returns at once). Both default on (built and accepted features). Missing or broken keys
 * use the defaults and the file is rewritten complete. Server side; a client only predicts and the server's world wins.
 */
public final class OctetsConfig {
    public static final String FILE = "simpleoctets-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("features.melonSlicePlacing", "features.cornerBreaking");

    /** Sneak + right-click with a melon slice places a melon octet (off: the slice is only eaten, the tooltip hint is gone). */
    public static boolean melonSlicePlacing = true;
    /** Sneaking with a sledgehammer breaks one octet off a full block (off: the hammer breaks the whole block as usual). */
    public static boolean cornerBreaking = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleOctets.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleOctets.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement group = json == null ? null : json.get("features");
        JsonObject f = group != null && group.isJsonObject() ? group.getAsJsonObject() : new JsonObject();
        melonSlicePlacing = bool(f, "melonSlicePlacing", true);
        cornerBreaking = bool(f, "cornerBreaking", true);
    }

    public static JsonObject toJson() {
        JsonObject f = new JsonObject();
        f.addProperty("melonSlicePlacing", melonSlicePlacing);
        f.addProperty("cornerBreaking", cornerBreaking);
        JsonObject o = new JsonObject();
        o.add("features", f);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private OctetsConfig() {}
}
