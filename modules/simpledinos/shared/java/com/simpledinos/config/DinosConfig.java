package com.simpledinos.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpledinos.SimpleDinos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpledinos-server.json} (server authority, every number clamped to a safe range); missing or
 * broken keys use the defaults and the file is rewritten complete. The defaults are the times of the concept (section 3).
 */
public final class DinosConfig {
    public static final String FILE = "simpledinos-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "brushSeconds", "incubateSeconds", "hatchSeconds", "growMinutes",
            "trexBiteDamage");

    public static final int BRUSH_DEFAULT = 60, BRUSH_MIN = 5, BRUSH_MAX = 600;
    public static final int INCUBATE_DEFAULT = 300, INCUBATE_MIN = 10, INCUBATE_MAX = 3600;
    public static final int HATCH_DEFAULT = 600, HATCH_MIN = 10, HATCH_MAX = 7200;
    public static final int GROW_DEFAULT = 40, GROW_MIN = 1, GROW_MAX = 600;
    public static final double BITE_DEFAULT = 10.0, BITE_MIN = 1.0, BITE_MAX = 40.0;

    public static boolean enabled = true;
    /** Seconds of brushing that free a fossil (60 = about one minute, a pause costs twice its length). */
    public static int brushSeconds = BRUSH_DEFAULT;
    /** Seconds a player has to sit on the egg until it is brooded. */
    public static int incubateSeconds = INCUBATE_DEFAULT;
    /** Seconds from a brooded egg to the hatchling (10 minutes = half a Minecraft day). */
    public static int hatchSeconds = HATCH_DEFAULT;
    /** Minutes a hatchling needs to grow up (40 = two Minecraft days). */
    public static int growMinutes = GROW_DEFAULT;
    /** Bite damage of an adult T. rex (before difficulty). */
    public static double trexBiteDamage = BITE_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static int brushTicks() {
        return brushSeconds * 20;
    }

    public static int incubateTicks() {
        return incubateSeconds * 20;
    }

    public static int hatchTicks() {
        return hatchSeconds * 20;
    }

    public static int growTicks() {
        return growMinutes * 1200;
    }

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleDinos.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleDinos.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        brushSeconds = (int) clamp(number(json, "brushSeconds", BRUSH_DEFAULT), BRUSH_MIN, BRUSH_MAX);
        incubateSeconds = (int) clamp(number(json, "incubateSeconds", INCUBATE_DEFAULT), INCUBATE_MIN, INCUBATE_MAX);
        hatchSeconds = (int) clamp(number(json, "hatchSeconds", HATCH_DEFAULT), HATCH_MIN, HATCH_MAX);
        growMinutes = (int) clamp(number(json, "growMinutes", GROW_DEFAULT), GROW_MIN, GROW_MAX);
        trexBiteDamage = clamp(number(json, "trexBiteDamage", BITE_DEFAULT), BITE_MIN, BITE_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("brushSeconds", brushSeconds);
        o.addProperty("incubateSeconds", incubateSeconds);
        o.addProperty("hatchSeconds", hatchSeconds);
        o.addProperty("growMinutes", growMinutes);
        o.addProperty("trexBiteDamage", trexBiteDamage);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        JsonElement v = json.get(key);
        try {
            double d = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : fallback;
            return Double.isFinite(d) ? d : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static {
        reset();
    }

    private DinosConfig() {}
}
