package com.simpleovens.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simpleovens.SimpleOvens;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleovens-server.json} (server authority, every number clamped); missing or broken keys
 * use the defaults and the file is rewritten complete. On the first start it takes the furnace speeds a world had in
 * SimpleBuilding's config ({@code server.machines.*FurnaceSpeed} in {@code config/simplebuilding.json}), which stays
 * unchanged (framework {@code ConfigFiles.adoptLegacy}).
 */
public final class OvensConfig {
    public static final String FILE = "simpleovens-server.json";
    /** SimpleBuilding's config the speeds came from (split plan stage 4). */
    public static final String LEGACY_FILE = "simplebuilding.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "reinforcedSpeed", "netheriteSpeed", "enderiteSpeed");
    /** Speed of a tier against the Vanilla machine of its family; the most a server may set. */
    public static final int MAX_SPEED = 8;
    public static final int REINFORCED_DEFAULT = 2, NETHERITE_DEFAULT = 4, ENDERITE_DEFAULT = 8;

    public static boolean enabled = true;
    public static int reinforcedSpeed = REINFORCED_DEFAULT;
    public static int netheriteSpeed = NETHERITE_DEFAULT;
    public static int enderiteSpeed = ENDERITE_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        if (file != null) {
            com.simplebuilding.framework.api.ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), OvensConfig::fromSimpleBuilding, SimpleOvens.LOG::info);
        }
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleOvens.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleOvens.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** The speeds out of SimpleBuilding's config text, as this file's JSON; null when it has none. */
    static String fromSimpleBuilding(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject server = parsed.getAsJsonObject().getAsJsonObject("server");
        JsonObject machines = server == null ? null : server.getAsJsonObject("machines");
        if (machines == null) return null;
        JsonObject taken = new JsonObject();
        copy(machines, "reinforcedFurnaceSpeed", taken, "reinforcedSpeed");
        copy(machines, "netheriteFurnaceSpeed", taken, "netheriteSpeed");
        copy(machines, "enderiteFurnaceSpeed", taken, "enderiteSpeed");
        return taken.isEmpty() ? null : GSON.toJson(taken);
    }

    private static void copy(JsonObject from, String key, JsonObject to, String as) {
        JsonElement value = from.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) to.add(as, value.deepCopy());
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        reinforcedSpeed = clamp(integer(json, "reinforcedSpeed", REINFORCED_DEFAULT));
        netheriteSpeed = clamp(integer(json, "netheriteSpeed", NETHERITE_DEFAULT));
        enderiteSpeed = clamp(integer(json, "enderiteSpeed", ENDERITE_DEFAULT));
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("reinforcedSpeed", reinforcedSpeed);
        o.addProperty("netheriteSpeed", netheriteSpeed);
        o.addProperty("enderiteSpeed", enderiteSpeed);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement v = json.get(key);
        try {
            return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(MAX_SPEED, value));
    }

    private OvensConfig() {}
}
