package com.simplestorage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplestorage.SimpleStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Config {@code config/simplestorage.json} (split stage 8c-1, docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md); missing or broken
 * keys use the defaults and the file is rewritten complete. On the first start it takes over the bundle option a world had
 * in SimpleBuilding's config ({@code tools.invertBundleInteractions} in {@code config/simplebuilding.json}), which stays
 * unchanged (framework {@code ConfigFiles.adoptLegacy}).
 */
public final class StorageConfig {
    public static final String FILE = "simplestorage.json";
    /** SimpleBuilding's config the option came from. */
    public static final String LEGACY_FILE = "simplebuilding.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("invertBundleInteractions");

    /** Swap the clicks of bundles and quivers: left click takes out, right click puts in (Vanilla the other way round). */
    public static boolean invertBundleInteractions = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        if (file != null) {
            com.simplebuilding.framework.api.ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), StorageConfig::fromSimpleBuilding, SimpleStorage.LOG::info);
        }
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleStorage.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleStorage.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** The option out of SimpleBuilding's config text, as this file's JSON; null when it has none. */
    static String fromSimpleBuilding(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject tools = parsed.getAsJsonObject().getAsJsonObject("tools");
        JsonElement value = tools == null ? null : tools.get("invertBundleInteractions");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) return null;
        JsonObject taken = new JsonObject();
        taken.add("invertBundleInteractions", value.deepCopy());
        return GSON.toJson(taken);
    }

    public static void apply(JsonObject json) {
        JsonElement v = json.get("invertBundleInteractions");
        invertBundleInteractions = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() && v.getAsBoolean();
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("invertBundleInteractions", invertBundleInteractions);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private StorageConfig() {}
}
