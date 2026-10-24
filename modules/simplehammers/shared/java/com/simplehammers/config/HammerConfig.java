package com.simplehammers.config;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplehammers.SimpleHammers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplehammers-server.json} (server authority, every number clamped); missing or broken keys
 * use the defaults and the file is rewritten complete. On the first start it takes the hammer values a world had in
 * SimpleBuilding's config ({@code server.tools.*} in {@code config/simplebuilding.json}), which stays unchanged
 * (framework {@code ConfigFiles.adoptLegacy}). Values are server-local like Simple Ovens': a client counts with its own
 * file, the server ends an upgrade early when its value is lower (the client lets go at the changed block).
 */
public final class HammerConfig {
    public static final String FILE = "simplehammers-server.json";
    /** SimpleBuilding's config the values came from (split plan stage 7). */
    public static final String LEGACY_FILE = "simplebuilding.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "upgradeSeconds", "reinforcedDamagePerHit",
            "netheriteDamagePerHit", "enderiteDamagePerHit", "diamondBlockPebbles");
    /** Most strikes (= seconds) of one upgrade a server may set. */
    public static final int MAX_UPGRADE_SECONDS = 30;
    /** Most durability per strike a server may set. */
    public static final int MAX_UPGRADE_DAMAGE = 64;
    public static final int UPGRADE_SECONDS_DEFAULT = 5;
    public static final int REINFORCED_DAMAGE_DEFAULT = 2, NETHERITE_DAMAGE_DEFAULT = 4, ENDERITE_DAMAGE_DEFAULT = 10;
    /** Pebbles of one diamond block: 9 diamonds = 81 pebbles. */
    public static final int DIAMOND_BLOCK_PEBBLES_DEFAULT = 81;

    public static boolean enabled = true;
    public static int upgradeSeconds = UPGRADE_SECONDS_DEFAULT;
    public static int reinforcedDamagePerHit = REINFORCED_DAMAGE_DEFAULT;
    public static int netheriteDamagePerHit = NETHERITE_DAMAGE_DEFAULT;
    public static int enderiteDamagePerHit = ENDERITE_DAMAGE_DEFAULT;
    public static int diamondBlockPebbles = DIAMOND_BLOCK_PEBBLES_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HammerConfig() {}

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        if (file != null) {
            com.simplebuilding.framework.api.ConfigFiles.adoptLegacy(file, dir.resolve(LEGACY_FILE), HammerConfig::fromSimpleBuilding, SimpleHammers.LOG::info);
        }
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleHammers.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleHammers.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** The hammer values out of SimpleBuilding's config text, as this file's JSON; null when it has none. */
    public static String fromSimpleBuilding(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject server = parsed.getAsJsonObject().getAsJsonObject("server");
        JsonObject tools = server == null ? null : server.getAsJsonObject("tools");
        if (tools == null) return null;
        JsonObject taken = new JsonObject();
        copy(tools, "sledgehammerUpgradeSeconds", taken, "upgradeSeconds");
        copy(tools, "reinforcedUpgradeDamagePerHit", taken, "reinforcedDamagePerHit");
        copy(tools, "netheriteUpgradeDamagePerHit", taken, "netheriteDamagePerHit");
        copy(tools, "enderiteUpgradeDamagePerHit", taken, "enderiteDamagePerHit");
        copy(tools, "diamondBlockPebbles", taken, "diamondBlockPebbles");
        return taken.isEmpty() ? null : GSON.toJson(taken);
    }

    private static void copy(JsonObject from, String key, JsonObject to, String as) {
        JsonElement value = from.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) to.add(as, value.deepCopy());
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        upgradeSeconds = clamp(integer(json, "upgradeSeconds", UPGRADE_SECONDS_DEFAULT), 1, MAX_UPGRADE_SECONDS);
        reinforcedDamagePerHit = clamp(integer(json, "reinforcedDamagePerHit", REINFORCED_DAMAGE_DEFAULT), 0, MAX_UPGRADE_DAMAGE);
        netheriteDamagePerHit = clamp(integer(json, "netheriteDamagePerHit", NETHERITE_DAMAGE_DEFAULT), 0, MAX_UPGRADE_DAMAGE);
        enderiteDamagePerHit = clamp(integer(json, "enderiteDamagePerHit", ENDERITE_DAMAGE_DEFAULT), 0, MAX_UPGRADE_DAMAGE);
        diamondBlockPebbles = clamp(integer(json, "diamondBlockPebbles", DIAMOND_BLOCK_PEBBLES_DEFAULT), 1, DIAMOND_BLOCK_PEBBLES_DEFAULT);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("upgradeSeconds", upgradeSeconds);
        o.addProperty("reinforcedDamagePerHit", reinforcedDamagePerHit);
        o.addProperty("netheriteDamagePerHit", netheriteDamagePerHit);
        o.addProperty("enderiteDamagePerHit", enderiteDamagePerHit);
        o.addProperty("diamondBlockPebbles", diamondBlockPebbles);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    /** Durability per strike of an upgrade into the reinforced, Netherite or Enderite tier. */
    public static int upgradeDamagePerHit(boolean toReinforced, boolean toEnderite) {
        return toReinforced ? reinforcedDamagePerHit : toEnderite ? enderiteDamagePerHit : netheriteDamagePerHit;
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
