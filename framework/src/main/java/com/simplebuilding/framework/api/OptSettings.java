package com.simplebuilding.framework.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Since 0.1.12. Config file of a Simple Optimization sub-mod ({@code config/<id>.json}, concept §5):
 * {@code {"areas": {"<area>": {"mode": "auto", ...}}, "conflictScan": true}}. Readable before any Minecraft class
 * loads (mixin plugins call it), only Gson and files. A missing area or key gets its default and the file is written
 * back complete; a broken file is quarantined ({@link ConfigFiles}) and defaults apply. Gson is on every Minecraft
 * classpath, so the framework only compiles against it.
 */
public final class OptSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final JsonObject root;

    private OptSettings(JsonObject root) {
        this.root = root;
    }

    /** {@code defaults}: area id → key → default value (Number, Boolean or String; "mode" defaults to "auto" unless given). */
    public static OptSettings load(Path dir, String file, Map<String, Map<String, Object>> defaults, Consumer<String> warn) {
        Path path = dir == null ? null : dir.resolve(file);
        JsonObject read = ConfigFiles.readOrQuarantine(path, text -> JsonParser.parseString(text).getAsJsonObject(), warn);
        OptSettings settings = of(read, defaults);
        if (path != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(path, GSON.toJson(settings.root));
            } catch (Exception e) {
                warn.accept("Could not write " + file + ": " + e);
            }
        }
        return settings;
    }

    /** The merge without files (tests, and the defaults when nothing could be read). */
    public static OptSettings of(JsonObject read, Map<String, Map<String, Object>> defaults) {
        JsonObject root = read == null ? new JsonObject() : read.deepCopy();
        JsonObject areas = root.has("areas") && root.get("areas").isJsonObject() ? root.getAsJsonObject("areas") : new JsonObject();
        root.add("areas", areas);
        defaults.forEach((area, keys) -> {
            JsonObject entry = areas.has(area) && areas.get(area).isJsonObject() ? areas.getAsJsonObject(area) : new JsonObject();
            areas.add(area, entry);
            keys.forEach((key, value) -> {
                if (!entry.has(key)) entry.add(key, GSON.toJsonTree(value));
            });
            // Since 0.1.13 a default "mode" in {@code defaults} wins (areas that change gameplay start "off").
            if (!entry.has("mode")) entry.addProperty("mode", "auto");
        });
        if (!root.has("conflictScan")) root.addProperty("conflictScan", true);
        return new OptSettings(root);
    }

    private JsonElement value(String area, String key) {
        JsonObject areas = root.getAsJsonObject("areas");
        if (!areas.has(area) || !areas.get(area).isJsonObject()) return null;
        JsonElement value = areas.getAsJsonObject(area).get(key);
        return value == null || value.isJsonNull() ? null : value;
    }

    public OptCompat.Mode mode(String area) {
        JsonElement value = value(area, "mode");
        return OptCompat.Mode.parse(value != null && value.isJsonPrimitive() ? value.getAsString() : null);
    }

    /** The value clamped to [min, max]; unreadable → {@code fallback}. */
    public int intValue(String area, String key, int fallback, int min, int max) {
        JsonElement value = value(area, key);
        try {
            int v = value == null ? fallback : value.getAsInt();
            return Math.max(min, Math.min(max, v));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public double doubleValue(String area, String key, double fallback, double min, double max) {
        JsonElement value = value(area, key);
        try {
            double v = value == null ? fallback : value.getAsDouble();
            return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public boolean boolValue(String area, String key, boolean fallback) {
        JsonElement value = value(area, key);
        try {
            return value == null ? fallback : value.getAsBoolean();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Since 0.1.13: the root key {@code conflictScan} (target check of concept §4.5), default on. */
    public boolean conflictScan() {
        try {
            return !root.has("conflictScan") || root.get("conflictScan").getAsBoolean();
        } catch (RuntimeException e) {
            return true;
        }
    }

    public String json() {
        return GSON.toJson(root);
    }

    /**
     * The super-mod's "Enable Simple X" switch, read from its file because mixin plugins decide before any
     * {@code onInitialize}: missing file or key → on (a sub-mod alone always plays).
     */
    public static boolean switchOn(Path dir, String superFile, String key, Consumer<String> warn) {
        Path path = dir == null ? null : dir.resolve(superFile);
        if (path == null || !Files.isRegularFile(path)) return true;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            return !o.has(key) || o.get(key).getAsBoolean();
        } catch (Exception e) {
            warn.accept("Unreadable " + superFile + " (" + e + "); sub-mod stays on");
            return true;
        }
    }

    /** Priority table (concept §5.1): {@code {"areas": {"<area>": {"winner": "theirs"|"ours", ...}}}}. Bad text → empty. */
    public static Map<String, OptCompat.Winner> priorities(String json) {
        Map<String, OptCompat.Winner> out = new HashMap<>();
        try {
            JsonObject areas = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("areas");
            for (var e : areas.entrySet()) {
                String w = e.getValue().getAsJsonObject().get("winner").getAsString();
                out.put(e.getKey(), "ours".equalsIgnoreCase(w) ? OptCompat.Winner.OURS : OptCompat.Winner.THEIRS);
            }
        } catch (RuntimeException e) {
            out.clear();
        }
        return out;
    }
}
