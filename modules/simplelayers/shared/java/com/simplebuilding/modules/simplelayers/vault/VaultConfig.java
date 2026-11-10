package com.simplebuilding.modules.simplelayers.vault;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Bedrock Vault part of {@code config/simplelayers-server.json} (group {@code vault}, UNDER21): read and written by
 * {@link com.simplebuilding.modules.simplelayers.config.LayersConfig} together with the other groups. Every number clamped.
 */
public final class VaultConfig {
    /** The Bedrock Vault: Overworld bedrock gaps lead into it (new chunks only). */
    public static boolean enabled = true;
    /** One new Overworld chunk in this many gets a bedrock gap with the vault gate (0 = none). */
    public static int gapRarity = 400;
    /** Seconds after the first entry until the gate seals behind the players (it opens again once nobody is inside). */
    public static int closeSeconds = 30;
    /** Volume of the quiet random sounds in the vault in percent (0 = silent). */
    public static int ambientVolume = 40;

    private VaultConfig() {}

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        gapRarity = integer(json, "gapRarity", 400, 0, 100000);
        closeSeconds = integer(json, "closeSeconds", 30, 5, 600);
        ambientVolume = integer(json, "ambientVolume", 40, 0, 100);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("gapRarity", gapRarity);
        o.addProperty("closeSeconds", closeSeconds);
        o.addProperty("ambientVolume", ambientVolume);
        return o;
    }

    private static boolean bool(JsonObject json, String key, boolean def) {
        try {
            JsonElement e = json.get(key);
            return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static int integer(JsonObject json, String key, int def, int min, int max) {
        try {
            JsonElement e = json.get(key);
            int v = e != null && e.isJsonPrimitive() ? e.getAsInt() : def;
            return Math.max(min, Math.min(max, v));
        } catch (RuntimeException e) {
            return def;
        }
    }
}
