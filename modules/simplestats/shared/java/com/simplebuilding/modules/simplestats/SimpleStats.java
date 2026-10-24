package com.simplebuilding.modules.simplestats;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simplebuilding.modules.simplestats.data.StatData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple Stats: every tool, weapon and piece of armor keeps its own statistics. Loader entries set {@link #configDir} and call {@link #load()}. */
public final class SimpleStats {
    public static final String MOD_ID = "simplestats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    public static StatsConfig CONFIG = new StatsConfig();

    private SimpleStats() {}

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    public static StatsConfig config() { return CONFIG; }

    public static StatData.Budget budget() { return new StatData.Budget(CONFIG.maxBytes, StatData.Budget.DEFAULT.maxEntriesPerList()); }

    public static void load() {
        CONFIG = read(configDir.resolve(MOD_ID + ".json"));
    }

    static StatsConfig read(Path file) {
        StatsConfig config = null;
        try {
            if (Files.isRegularFile(file) && Files.size(file) <= 65536) config = new Gson().fromJson(Files.readString(file), StatsConfig.class);
            else write(file, new StatsConfig());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Stats settings, using defaults", e);
        }
        if (config == null) config = new StatsConfig();
        config.normalize();
        return config;
    }

    private static void write(Path file, StatsConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        ConfigFiles.writeAtomic(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
