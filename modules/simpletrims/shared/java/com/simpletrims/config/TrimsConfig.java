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
        try {
            if (file != null && Files.isRegularFile(file)) {
                json = GSON.fromJson(Files.readString(file), JsonObject.class);
                if (json == null) json = new JsonObject();
            }
        } catch (Exception e) {
            SimpleTrims.LOG.warn("Unreadable {}, using defaults: {}", FILE, e.toString());
            json = new JsonObject();
        }
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
            enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : ENABLED_DEFAULT;
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
