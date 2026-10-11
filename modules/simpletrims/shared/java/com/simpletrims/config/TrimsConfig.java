package com.simpletrims.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simpletrims.SimpleTrims;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server config {@code config/simpletrims-server.json}; missing keys use the defaults, the file is rewritten. */
public final class TrimsConfig {
    public static final boolean ENABLED_DEFAULT = true;
    /** "Enable Simple Trims" (super/sub-mod switch; SimpleBuilding mirrors it as "Simple Trims aktivieren"). */
    public static boolean enabled = ENABLED_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE = "simpletrims-server.json";

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, JsonObject.class, GSON, SimpleTrims.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleTrims.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        try {
            var v = json.get("enabled");
            enabled = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : ENABLED_DEFAULT;
        } catch (Exception e) {
            enabled = ENABLED_DEFAULT;
        }
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private TrimsConfig() {}
}
