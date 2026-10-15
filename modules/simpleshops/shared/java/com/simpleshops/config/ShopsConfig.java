package com.simpleshops.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simpleshops.SimpleShops;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleshops-server.json} (server authority, every number clamped to a safe range);
 * missing or broken keys use the defaults and the file is rewritten complete. There is deliberately no switch that
 * lowers the protection of a placed shop (no blast resistance or owner rule options).
 */
public final class ShopsConfig {
    public static final String FILE = "simpleshops-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "maxShopsPerPlayer", "idleTimeoutSeconds", "collectServerShopIncome");

    public static final int MAX_SHOPS_DEFAULT = 16, MAX_SHOPS_MIN = 1, MAX_SHOPS_MAX = 64;
    public static final int IDLE_DEFAULT = 300, IDLE_MIN = 30, IDLE_MAX = 1800;

    public static boolean enabled = true;
    /** Sales blocks and display cases a player may have placed, each counted on its own. */
    public static int maxShopsPerPlayer = MAX_SHOPS_DEFAULT;
    /** Seconds without a click before the server closes a shop menu (keeps an AFK player from blocking a shop). */
    public static int idleTimeoutSeconds = IDLE_DEFAULT;

    /** Server shops: false = the price items vanish (sink), true = they are summed in the admin cash ({@code /servershop cash}). */
    public static boolean collectServerShopIncome = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleShops.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleShops.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        maxShopsPerPlayer = clamp(integer(json, "maxShopsPerPlayer", MAX_SHOPS_DEFAULT), MAX_SHOPS_MIN, MAX_SHOPS_MAX);
        idleTimeoutSeconds = clamp(integer(json, "idleTimeoutSeconds", IDLE_DEFAULT), IDLE_MIN, IDLE_MAX);
        collectServerShopIncome = bool(json, "collectServerShopIncome", false);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("maxShopsPerPlayer", maxShopsPerPlayer);
        o.addProperty("idleTimeoutSeconds", idleTimeoutSeconds);
        o.addProperty("collectServerShopIncome", collectServerShopIncome);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static {
        reset();
    }

    private ShopsConfig() {}
}
