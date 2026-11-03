package com.simpleenchanting.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleenchanting.SimpleEnchanting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleenchanting-server.json}: {@code features.endgameLevels} (default on): the Astral Enchanter at
 * tier 60 lifts Protection and Feather Falling to V (until 03.11. SimpleBuilding's {@code server.features.endgameEnchantLevels}; a
 * missing file takes that value over once from {@code simplebuilding.json}). Missing or broken keys use the defaults and the file
 * is rewritten complete.
 */
public final class EnchantingConfig {
    public static final String FILE = "simpleenchanting-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("features.endgameLevels");
    /** SimpleBuilding's config, read once for the endgame switch. */
    public static final String LEGACY_FILE = "simplebuilding.json";

    /** Endgame levels at the Astral Enchanter's top tier (Protection V, Feather Falling V; A12: only this table, the anvil never). */
    public static boolean endgameLevels = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        boolean existed = file != null && Files.isRegularFile(file);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleEnchanting.LOG::warn);
        if (read != null) json = read;
        if (dir != null && !existed) {
            Boolean taken = endgameFromSimpleBuilding(dir.resolve(LEGACY_FILE));
            if (taken != null) {
                JsonObject features = new JsonObject();
                features.addProperty("endgameLevels", taken);
                json.add("features", features);
                SimpleEnchanting.LOG.info("Took over the endgame switch from {} into {} (old file unchanged)", LEGACY_FILE, FILE);
            }
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleEnchanting.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement features = json.get("features");
        JsonObject f = features != null && features.isJsonObject() ? features.getAsJsonObject() : new JsonObject();
        endgameLevels = bool(f, "endgameLevels", true);
    }

    /** {@code server.features.endgameEnchantLevels} out of SimpleBuilding's config file, or null without file or value. */
    public static Boolean endgameFromSimpleBuilding(Path legacy) {
        if (legacy == null || !Files.isRegularFile(legacy)) return null;
        try {
            JsonElement parsed = com.google.gson.JsonParser.parseString(Files.readString(legacy));
            if (!parsed.isJsonObject()) return null;
            JsonElement server = parsed.getAsJsonObject().get("server");
            if (server == null || !server.isJsonObject()) return null;
            JsonElement features = server.getAsJsonObject().get("features");
            if (features == null || !features.isJsonObject()) return null;
            JsonElement v = features.getAsJsonObject().get("endgameEnchantLevels");
            return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : null;
        } catch (Exception e) {
            SimpleEnchanting.LOG.warn("Could not read the endgame switch from {}: {}", LEGACY_FILE, e.toString());
            return null;
        }
    }

    public static JsonObject toJson() {
        JsonObject features = new JsonObject();
        features.addProperty("endgameLevels", endgameLevels);
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

    private EnchantingConfig() {}
}
