package com.simplechisels.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplechisels.ChiselTier;
import com.simplechisels.SimpleChisels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Server config {@code config/simplechisels-server.json} (server authority, every number clamped): {@code enabled} and
 * {@code cooldownTicks.<tier>} (ticks between two steps before Fast Chiseling, 2..200). Missing or broken keys use the
 * defaults and the file is rewritten complete. On the first start it takes the cooldowns a world had in SimpleBuilding's
 * config ({@code server.tools.<tier>ChiselCooldownTicks} in {@code config/simplebuilding.json}), which stays unchanged.
 * The cooldown is set on the server and synced by Vanilla, so the value needs no client sync.
 */
public final class ChiselConfig {
    public static final String FILE = "simplechisels-server.json";
    /** SimpleBuilding's config the values came from (WE25-7). */
    public static final String LEGACY_FILE = "simplebuilding.json";
    public static final int MIN_COOLDOWN = 2;
    public static final int MAX_COOLDOWN = 200;
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS;

    static {
        List<String> keys = new ArrayList<>(List.of("enabled"));
        for (ChiselTier tier : ChiselTier.values()) keys.add("cooldownTicks." + tier.name);
        KEYS = List.copyOf(keys);
    }

    public static boolean enabled = true;
    private static final Map<ChiselTier, Integer> COOLDOWNS = new EnumMap<>(ChiselTier.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ChiselConfig() {}

    /** The cooldown of {@code tier} in ticks (before Fast Chiseling). */
    public static int cooldownTicks(ChiselTier tier) {
        return COOLDOWNS.getOrDefault(tier, tier.cooldownTicks);
    }

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        if (file != null) {
            ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), ChiselConfig::fromSimpleBuilding, SimpleChisels.LOG::info);
        }
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleChisels.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleChisels.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** The chisel cooldowns out of SimpleBuilding's config text, as this file's JSON; null when it has none. */
    public static String fromSimpleBuilding(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject server = parsed.getAsJsonObject().getAsJsonObject("server");
        JsonObject tools = server == null ? null : server.getAsJsonObject("tools");
        if (tools == null) return null;
        JsonObject cooldowns = new JsonObject();
        for (ChiselTier tier : ChiselTier.values()) {
            JsonElement value = tools.get(tier.name + "ChiselCooldownTicks");
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) cooldowns.add(tier.name, value.deepCopy());
        }
        if (cooldowns.isEmpty()) return null;
        JsonObject taken = new JsonObject();
        taken.add("cooldownTicks", cooldowns);
        return GSON.toJson(taken);
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        JsonObject cooldowns = json.get("cooldownTicks") instanceof JsonObject o ? o : new JsonObject();
        for (ChiselTier tier : ChiselTier.values()) {
            COOLDOWNS.put(tier, clamp(integer(cooldowns, tier.name, tier.cooldownTicks), MIN_COOLDOWN, MAX_COOLDOWN));
        }
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        JsonObject cooldowns = new JsonObject();
        for (ChiselTier tier : ChiselTier.values()) cooldowns.addProperty(tier.name, cooldownTicks(tier));
        o.add("cooldownTicks", cooldowns);
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
}
