package com.simpleredstone.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleredstone.SimpleRedstone;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleredstone-server.json}: {@code features.doublePiston} and {@code features.signalDampener}
 * (both default on, A12: nothing of Vanilla changes, the parts are only added). Missing or broken keys use the defaults and the
 * file is rewritten complete.
 */
public final class RedstoneConfig {
    public static final String FILE = "simpleredstone-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("features.doublePiston", "features.signalDampener");

    public static boolean doublePiston = true;
    public static boolean signalDampener = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleRedstone.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleRedstone.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement features = json.get("features");
        JsonObject f = features != null && features.isJsonObject() ? features.getAsJsonObject() : new JsonObject();
        doublePiston = bool(f, "doublePiston", true);
        signalDampener = bool(f, "signalDampener", true);
    }

    public static JsonObject toJson() {
        JsonObject features = new JsonObject();
        features.addProperty("doublePiston", doublePiston);
        features.addProperty("signalDampener", signalDampener);
        JsonObject o = new JsonObject();
        o.add("features", features);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    static {
        reset();
    }

    private RedstoneConfig() {}
}
