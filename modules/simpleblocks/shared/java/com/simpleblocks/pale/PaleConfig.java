package com.simpleblocks.pale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Group {@code pale} of {@code config/simpleblocks-server.json} (DW04B-2/3, docs/ai/PLAN-B-PALE01-2026-11-04.md), read and
 * written by {@link com.simpleblocks.snow.SnowConfig} (one file): {@code enabled} (default true) and the chance per random
 * tick of each chain (0-1, default Vanilla copper's 0.0569; 0 = never). Read on every tick, so a change works at once.
 */
public final class PaleConfig {
    /** Chain key -> config key, in wiki order. */
    public static final List<String> CHAINS = List.of("pumpkin", "carvedPumpkin", "jackOLantern", "melon", "hayBlock");
    public static final List<String> KEYS = List.of("pale.enabled", "pale.pumpkin", "pale.carvedPumpkin", "pale.jackOLantern", "pale.melon",
            "pale.hayBlock");
    public static final double DEFAULT_CHANCE = 0.0569;

    public static boolean enabled = true;
    public static final Map<String, Double> CHANCE = new LinkedHashMap<>();

    static {
        reset();
    }

    private PaleConfig() {}

    public static void reset() {
        apply(new JsonObject());
    }

    /** Chance per random tick of one chain (0 when the switch is off). */
    public static double chance(String chain) {
        return enabled ? CHANCE.getOrDefault(chain, DEFAULT_CHANCE) : 0;
    }

    public static void apply(JsonObject pale) {
        JsonElement on = pale.get("enabled");
        enabled = on == null || !on.isJsonPrimitive() || !on.getAsJsonPrimitive().isBoolean() || on.getAsBoolean();
        for (String key : CHAINS) {
            double v = DEFAULT_CHANCE;
            JsonElement e = pale.get(key);
            try {
                if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) v = e.getAsDouble();
            } catch (RuntimeException ignored) {
                v = DEFAULT_CHANCE;
            }
            CHANCE.put(key, Double.isFinite(v) ? Math.max(0, Math.min(1, v)) : DEFAULT_CHANCE);
        }
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        CHANCE.forEach(o::addProperty);
        return o;
    }
}
