package com.simplebuilding.modules.simpleaesthetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Aesthetics: a super-mod without content of its own (docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md, stage 1c).
 * It requires and nests Simple Visuals, Simple Sounds and Simple Models; its only setting is one switch
 * "Enable Simple X" per sub-mod in {@code config/simpleaesthetics.json}, handed to the sub-mods through framework
 * {@link SubMods} (no class imports). Each sub-mod keeps its own config and stays playable alone.
 */
public final class SimpleAesthetics {
    public static final String MOD_ID = "simpleaesthetics", FILE = "simpleaesthetics.json";
    /** Bundled sub-mods, in config order. */
    public static final List<String> SUB_MODS = List.of("simplevisuals", "simplesounds", "simplemodels");
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Persisted as {@code config/simpleaesthetics.json}; every switch defaults on. */
    public static final class Config {
        public boolean enableSimpleVisuals = true;
        public boolean enableSimpleSounds = true;
        public boolean enableSimpleModels = true;

        public boolean enabled(String subMod) {
            return switch (subMod) {
                case "simplevisuals" -> enableSimpleVisuals;
                case "simplesounds" -> enableSimpleSounds;
                case "simplemodels" -> enableSimpleModels;
                default -> true;
            };
        }
    }

    private static Config config = new Config();

    private SimpleAesthetics() {}

    public static Config config() {
        return config;
    }

    /** Reads (or creates) the config next to the others, rewrites it complete and applies the switches. */
    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        Config read = ConfigFiles.readOrQuarantine(file, text -> GSON.fromJson(text, Config.class), LOG::warn);
        config = read != null ? read : new Config();
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(config));
            } catch (Exception e) {
                LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
        apply(config);
    }

    /** Tests only: puts a previously loaded config back live. */
    static void restore(Config c) {
        config = c;
        apply(c);
    }

    /** Hands every "Enable Simple X" switch to its sub-mod through framework SubMods. */
    public static void apply(Config c) {
        for (String subMod : SUB_MODS) SubMods.set(subMod, c.enabled(subMod));
    }
}
