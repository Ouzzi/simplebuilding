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
 * unchanged (framework {@code ConfigFiles.adoptLegacy}). Split stage 8b-3 (docs/ai/PLAN-B-S8B3-STORAGE-2026-10-21.md): the
 * rare finds' chances ({@code server.loot.*} there), server-local and clamped (server authority).
 */
public final class StorageConfig {
    public static final String FILE = "simplestorage.json";
    /** SimpleBuilding's config the option came from. */
    public static final String LEGACY_FILE = "simplebuilding.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("invertBundleInteractions", "betterChestPercent", "reinforcedShulkerPercent",
            "netheriteShulkerPercent", "enderiteShulkerPercent", "endermitesPerRareShulker");
    /** Highest percentages and escort a server may set (as in SimpleBuilding's {@code ServerTuning}). */
    public static final double MAX_BETTER_CHEST_PERCENT = 5.0, MAX_REINFORCED_SHULKER_PERCENT = 10.0, MAX_NETHERITE_SHULKER_PERCENT = 5.0,
            MAX_ENDERITE_SHULKER_PERCENT = 5.0;
    public static final int MAX_ENDERMITES_PER_RARE_SHULKER = 8;
    public static final double BETTER_CHEST_DEFAULT = 1.0, REINFORCED_SHULKER_DEFAULT = 2.0, NETHERITE_SHULKER_DEFAULT = 1.0,
            ENDERITE_SHULKER_DEFAULT = 0.5;
    public static final int ENDERMITES_DEFAULT = 4;

    /** Swap the clicks of bundles and quivers: left click takes out, right click puts in (Vanilla the other way round). */
    public static boolean invertBundleInteractions = false;
    /** Percent per loot chest of a new stronghold, bastion/nether fortress, end city/end ship that becomes a tiered chest (at most 5). */
    public static double betterChestPercent = BETTER_CHEST_DEFAULT;
    /** Percent per shulker of a new end city that becomes reinforced (at most 10), Netherite (at most 5), Enderite (at most 5). */
    public static double reinforcedShulkerPercent = REINFORCED_SHULKER_DEFAULT;
    public static double netheriteShulkerPercent = NETHERITE_SHULKER_DEFAULT;
    public static double enderiteShulkerPercent = ENDERITE_SHULKER_DEFAULT;
    /** Endermites per rare shulker, once, when a player comes near (at most 8; not persistent). */
    public static int endermitesPerRareShulker = ENDERMITES_DEFAULT;

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

    /** The options out of SimpleBuilding's config text, as this file's JSON; null when it has none. */
    public static String fromSimpleBuilding(String text) {
        JsonElement parsed = JsonParser.parseString(text);
        if (!parsed.isJsonObject()) return null;
        JsonObject taken = new JsonObject();
        JsonObject tools = parsed.getAsJsonObject().getAsJsonObject("tools");
        JsonElement value = tools == null ? null : tools.get("invertBundleInteractions");
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) taken.add("invertBundleInteractions", value.deepCopy());
        // Split stage 8b-3: the rare finds' values (server.loot.*, same names).
        JsonObject server = parsed.getAsJsonObject().getAsJsonObject("server");
        JsonObject loot = server == null ? null : server.getAsJsonObject("loot");
        if (loot != null) {
            for (String key : KEYS.subList(1, KEYS.size())) {
                JsonElement number = loot.get(key);
                if (number != null && number.isJsonPrimitive() && number.getAsJsonPrimitive().isNumber()) taken.add(key, number.deepCopy());
            }
        }
        return taken.isEmpty() ? null : GSON.toJson(taken);
    }

    public static void apply(JsonObject json) {
        JsonElement v = json.get("invertBundleInteractions");
        invertBundleInteractions = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() && v.getAsBoolean();
        betterChestPercent = percent(json, "betterChestPercent", BETTER_CHEST_DEFAULT, MAX_BETTER_CHEST_PERCENT);
        reinforcedShulkerPercent = percent(json, "reinforcedShulkerPercent", REINFORCED_SHULKER_DEFAULT, MAX_REINFORCED_SHULKER_PERCENT);
        netheriteShulkerPercent = percent(json, "netheriteShulkerPercent", NETHERITE_SHULKER_DEFAULT, MAX_NETHERITE_SHULKER_PERCENT);
        enderiteShulkerPercent = percent(json, "enderiteShulkerPercent", ENDERITE_SHULKER_DEFAULT, MAX_ENDERITE_SHULKER_PERCENT);
        JsonElement mites = json.get("endermitesPerRareShulker");
        int count = ENDERMITES_DEFAULT;
        try {
            if (mites != null && mites.isJsonPrimitive() && mites.getAsJsonPrimitive().isNumber()) count = mites.getAsInt();
        } catch (RuntimeException e) {
            count = ENDERMITES_DEFAULT;
        }
        endermitesPerRareShulker = Math.max(0, Math.min(MAX_ENDERMITES_PER_RARE_SHULKER, count));
    }

    /** A percentage between 0 and {@code max}; missing, not a number or NaN gives the default. */
    private static double percent(JsonObject json, String key, double fallback, double max) {
        JsonElement v = json.get(key);
        double value = fallback;
        try {
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) value = v.getAsDouble();
        } catch (RuntimeException e) {
            value = fallback;
        }
        if (!Double.isFinite(value)) value = fallback;
        return Math.max(0.0, Math.min(max, value));
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("invertBundleInteractions", invertBundleInteractions);
        o.addProperty("betterChestPercent", betterChestPercent);
        o.addProperty("reinforcedShulkerPercent", reinforcedShulkerPercent);
        o.addProperty("netheriteShulkerPercent", netheriteShulkerPercent);
        o.addProperty("enderiteShulkerPercent", enderiteShulkerPercent);
        o.addProperty("endermitesPerRareShulker", endermitesPerRareShulker);
        return o;
    }

    /** Chance (0..0.05) per structure loot chest to generate as a tiered chest; server only (world generation). */
    public static double betterChestChance() {
        return betterChestPercent / 100.0;
    }

    /** Chance (0..0.10) per end city shulker to become reinforced; server only. */
    public static double reinforcedShulkerChance() {
        return reinforcedShulkerPercent / 100.0;
    }

    /** Chance (0..0.05) per end city shulker to become Netherite; server only. */
    public static double netheriteShulkerChance() {
        return netheriteShulkerPercent / 100.0;
    }

    /** Chance (0..0.05) per end city shulker to become Enderite; server only. */
    public static double enderiteShulkerChance() {
        return enderiteShulkerPercent / 100.0;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private StorageConfig() {}
}
