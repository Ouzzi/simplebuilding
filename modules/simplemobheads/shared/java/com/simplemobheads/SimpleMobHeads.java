package com.simplemobheads;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplemobheads.config.MobHeadsConfig;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Mobheads (KONZEPT-SIMPLE-MOBHEADS-2026-10-13): every mob's head in one sub-mod. Required sub-mod of SimpleBuilding,
 * plays on its own with Vanilla + SimpleLib + framework. This class holds the id helpers, the config and the switches.
 */
public final class SimpleMobHeads {
    public static final String MOD_ID = "simplemobheads";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Mobheads");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Path configDir = Path.of("config");
    private static MobHeadsConfig config;

    private SimpleMobHeads() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Called first by every loader entry (mod constructor / initializer): config dir, id moves, config takeover. */
    public static void init(Path dir) {
        configDir = dir;
        HeadMoves.register();
        adoptFunConfig(dir);
        config();
        // CMD24: /simplemobheads config for every switch (the hint book is added when loot tables load).
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).gson(MobHeadsConfig.class, SimpleMobHeads::config, SimpleMobHeads::setConfig, () -> {
            MobHeadsConfig d = new MobHeadsConfig();
            d.normalize(abilityIds());
            return d;
        }).save(SimpleMobHeads::saveConfig).restart("hintBook").register();
        LOG.info("Simple Mobheads: {} heads", HeadTable.HEADS.size());
    }

    /** Off through a super mod's switch (framework SubMods) or the master switch: no drops, abilities, greetings. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && config().enabled;
    }

    /** Every ability id in table order. */
    public static Set<String> abilityIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (HeadDef head : HeadTable.HEADS) {
            if (!head.ability().isEmpty()) {
                ids.add(head.ability());
            }
        }
        return ids;
    }

    public static synchronized MobHeadsConfig config() {
        if (config == null) {
            Path file = configDir.resolve(MOD_ID + ".json");
            config = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, MobHeadsConfig.class), LOG::warn);
            boolean write = config == null || !Files.exists(file);
            if (config == null) {
                config = new MobHeadsConfig();
            }
            config.normalize(abilityIds());
            if (write) {
                saveConfig();
            }
        }
        return config;
    }

    public static synchronized void saveConfig() {
        config().normalize(abilityIds());
        try {
            Files.createDirectories(configDir);
            Files.writeString(configDir.resolve(MOD_ID + ".json"), GSON.toJson(config));
        } catch (java.io.IOException e) {
            LOG.warn("Simple Mobheads: could not write the config: {}", e.toString());
        }
    }

    /** Tests and the config screen: replace the loaded config (normalised) and save it. */
    public static synchronized void setConfig(MobHeadsConfig replacement) {
        config = replacement;
        saveConfig();
    }

    /** Tests: forget the loaded config so the next read comes from {@code dir}. */
    public static synchronized void reloadConfigFrom(Path dir) {
        configDir = dir;
        config = null;
    }

    public static Path configDir() {
        return configDir;
    }

    /**
     * One-time takeover from Simple Fun 1.3 ({@code config/simplefun.json}, object {@code fun}): the PvP head switch, the
     * greetings (off only when all four were off) and the four animal head abilities (off when their own switch or
     * {@code enableAnimalHeads} was off). Simple Fun 1.4 ignores those keys from now on.
     */
    public static boolean adoptFunConfig(Path dir) {
        return com.simplebuilding.framework.api.ConfigFiles.adoptLegacy(dir.resolve(MOD_ID + ".json"), dir.resolve("simplefun.json"), text -> {
            JsonObject fun = JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("fun");
            if (fun == null) {
                return null;
            }
            MobHeadsConfig adopted = new MobHeadsConfig();
            adopted.playerHeadDrops = bool(fun, "playerHeadDrops", true);
            adopted.greeting = bool(fun, "pigHeadGreeting", true) || bool(fun, "cowHeadGreeting", true)
                    || bool(fun, "chickenHeadGreeting", true) || bool(fun, "sheepHeadGreeting", true);
            boolean heads = bool(fun, "enableAnimalHeads", true);
            adopted.abilities.put("cow", heads && bool(fun, "cowHeadMilking", true));
            adopted.abilities.put("sheep", heads && bool(fun, "sheepHeadShearing", true));
            adopted.abilities.put("chicken", heads && bool(fun, "chickenHeadEggs", true));
            adopted.abilities.put("pig", heads && bool(fun, "pigHeadTruffles", true));
            adopted.normalize(abilityIds());
            return GSON.toJson(adopted);
        }, LOG::info);
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
    }
}
