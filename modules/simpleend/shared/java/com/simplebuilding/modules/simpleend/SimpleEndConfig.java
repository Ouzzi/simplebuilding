package com.simplebuilding.modules.simpleend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server config {@code config/simpleend.json}, read once at start. An unreadable file is renamed to
 * {@code .broken-<time>} and the defaults are written (same policy as the other modules). Every key has a bilingual
 * name and tooltip under {@code config.simpleend.<key>} (lang) and a wiki entry.
 */
public final class SimpleEndConfig {
    public static final String FILE = "simpleend.json";
    private static final Logger LOG = LoggerFactory.getLogger(SimpleEnd.ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** All keys in file order (lang/wiki checks read this list). */
    public static final List<String> KEYS = List.of("enabled", "enableSimpleEndDragonFight", "biomes", "biomeChance",
            "budsGrowAlone", "voidSafeItems", "longerDespawn", "forestFogWall", "forestFogStrength");

    public static boolean enabled = true;
    /** Second-level super-mod switch: the nested sub-mod Simple End Dragon Fight. */
    public static boolean enableSimpleEndDragonFight = true;
    /** Ash Valley and Dawnlands replace patches of the outer End highlands in new chunks. */
    public static boolean biomes = true;
    /** Share of 72x72-chunk cells on the outer islands that get one biome patch (0..1). */
    public static double biomeChance = 0.4;
    /** Buds grow by themselves in light >= 9 (random ticks); bone meal always works. */
    public static boolean budsGrowAlone = true;
    /** Dropped Simple End wood items float back up when they fall below the world. */
    public static boolean voidSafeItems = true;
    /** Dropped Simple End wood items stay 10 instead of 5 minutes. */
    public static boolean longerDespawn = true;

    /** Client: from outside the forest biomes show as a wall of fog (N1 B), and how far the fog is pulled (0..1). */
    public static boolean forestFogWall = true;
    public static double forestFogStrength = 0.85;

    private SimpleEndConfig() {}

    public static void load(Path dir) {
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
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        enableSimpleEndDragonFight = bool(json, "enableSimpleEndDragonFight", true);
        biomes = bool(json, "biomes", true);
        biomeChance = clamp(number(json, "biomeChance", 0.4), 0.0, 1.0);
        budsGrowAlone = bool(json, "budsGrowAlone", true);
        voidSafeItems = bool(json, "voidSafeItems", true);
        longerDespawn = bool(json, "longerDespawn", true);
        forestFogWall = bool(json, "forestFogWall", true);
        forestFogStrength = clamp(number(json, "forestFogStrength", 0.85), 0.0, 1.0);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("enableSimpleEndDragonFight", enableSimpleEndDragonFight);
        o.addProperty("biomes", biomes);
        o.addProperty("biomeChance", biomeChance);
        o.addProperty("budsGrowAlone", budsGrowAlone);
        o.addProperty("voidSafeItems", voidSafeItems);
        o.addProperty("longerDespawn", longerDespawn);
        o.addProperty("forestFogWall", forestFogWall);
        o.addProperty("forestFogStrength", forestFogStrength);
        return o;
    }

    private static double clamp(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        try { return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback; } catch (Exception e) { return fallback; }
    }

    private static double number(JsonObject json, String key, double fallback) {
        try { return json.has(key) ? json.get(key).getAsDouble() : fallback; } catch (Exception e) { return fallback; }
    }
}
