package com.simplebuilding.modules.simpleoptimization;

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
 * Simple Optimization: a super-mod without content of its own (docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md §5.2).
 * It requires and nests Simple Opt Render, Simple Opt Memory and Simple Opt Server; its only setting is one switch
 * "Enable Simple X" per sub-mod (mixin areas read the file directly, since they decide before onInitialize) in {@code config/simpleoptimization.json}, handed to the sub-mods through framework
 * {@link SubMods} (no class imports). Each sub-mod keeps its own config and stays playable alone.
 */
public final class SimpleOptimization {
    public static final String MOD_ID = "simpleoptimization", FILE = "simpleoptimization.json";
    /** Bundled sub-mods, in config order. */
    public static final List<String> SUB_MODS = List.of("simpleoptrender", "simpleoptmemory", "simpleoptserver");
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Persisted as {@code config/simpleoptimization.json}; every switch defaults on. */
    public static final class Config {
        public boolean enableSimpleOptRender = true;
        public boolean enableSimpleOptMemory = true;
        public boolean enableSimpleOptServer = true;

        public boolean enabled(String subMod) {
            return switch (subMod) {
                case "simpleoptrender" -> enableSimpleOptRender;
                case "simpleoptmemory" -> enableSimpleOptMemory;
                case "simpleoptserver" -> enableSimpleOptServer;
                default -> true;
            };
        }
    }

    private static Config config = new Config();

    private SimpleOptimization() {}

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
                Files.writeString(file, GSON.toJson(config));
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
