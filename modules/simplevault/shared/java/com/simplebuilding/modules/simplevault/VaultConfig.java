package com.simplebuilding.modules.simplevault;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplevault-server.json} (server authority, A9; group {@code vault}, UNDER21; every number clamped).
 * Missing or broken keys use the defaults and the file is rewritten complete. Lang:
 * {@code text.autoconfig.simplevault.option.vault.<key>} (+ {@code .@Tooltip}).
 */
public final class VaultConfig {
    public static final String FILE = "simplevault-server.json";
    public static final List<String> KEYS = List.of("vault.enabled", "vault.gapRarity", "vault.closeSeconds", "vault.ambientVolume");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The Bedrock Vault: Overworld bedrock gaps lead into it (new chunks only). */
    public static boolean enabled = true;
    /** One new Overworld chunk in this many gets a bedrock gap with the vault gate (0 = none). */
    public static int gapRarity = 400;
    /** Seconds after the first entry until the gate seals behind the players (it opens again once nobody is inside). */
    public static int closeSeconds = 30;
    /** Volume of the quiet random sounds in the vault in percent (0 = silent). */
    public static int ambientVolume = 40;

    private VaultConfig() {}

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleVault.LOG::warn);
        applyRoot(read == null ? new JsonObject() : read);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toRootJson()));
            } catch (Exception e) {
                SimpleVault.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** The whole file: {@code {"vault": {...}}}. */
    public static void applyRoot(JsonObject json) {
        JsonElement e = json.get("vault");
        apply(e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject());
    }

    public static JsonObject toRootJson() {
        JsonObject o = new JsonObject();
        o.add("vault", toJson());
        return o;
    }

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
