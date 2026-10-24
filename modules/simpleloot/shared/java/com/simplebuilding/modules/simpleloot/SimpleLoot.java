package com.simplebuilding.modules.simpleloot;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.SubMods;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Loot: vault rewards again after a cooldown in days (formerly Simple QoL). A sub-mod of Simple QoL, also playable alone. The loader entry sets {@link #configDir} and calls
 * {@link #load()}; on the first start the old Simple QoL values of the same name are taken over once.
 */
public final class SimpleLoot {
    public static final String MOD_ID = "simpleloot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    public static LootConfig CONFIG = new LootConfig();
    /** Runs after a command or the config screen changed the values (loader entries sync them to players). */
    public static Runnable onChange = () -> {};

    private SimpleLoot() {}

    /** False when the Simple QoL super-mod switched this sub-mod off ("Enable Simple Loot"). */
    public static boolean active() {
        return SubMods.enabled(MOD_ID);
    }

    /** The server values, numbers always within their bounds. */
    public static LootConfig config() {
        CONFIG.normalizeNumbers();
        return CONFIG;
    }

    static Path path() {
        return configDir.resolve(MOD_ID + ".json");
    }

    public static void load() {
        CONFIG = read(configDir);
        // CMD24: /<modid> config for every option; save() normalises, writes and syncs the clients.
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).gson(LootConfig.class, () -> CONFIG, c -> { c.normalize(); CONFIG = c; },
                () -> { LootConfig d = new LootConfig(); d.normalize(); return d; }).save(() -> save()).register();
    }

    /** Own file if present, else the one-time takeover from {@code simplequalityoflife.json} (then written as own file). */
    static LootConfig read(Path dir) {
        Path own = dir.resolve(MOD_ID + ".json");
        LootConfig config = null;
        try {
            if (Files.isRegularFile(own) && Files.size(own) <= 65536) {
                config = new Gson().fromJson(Files.readString(own), LootConfig.class);
            } else {
                config = fromQol(dir.resolve("simplequalityoflife.json"));
                if (config != null) write(own, config);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Loot settings, using defaults", e);
        }
        if (config == null) config = new LootConfig();
        config.normalize();
        return config;
    }

    /** The values of an old Simple QoL config ({@code qOL.<field>} or a top-level field of the same name), or null. */
    static LootConfig fromQol(Path qol) throws IOException {
        if (!Files.isRegularFile(qol) || Files.size(qol) > 1 << 20) return null;
        JsonObject root = JsonParser.parseString(Files.readString(qol)).getAsJsonObject();
        JsonObject old = root.has("qOL") && root.get("qOL").isJsonObject() ? root.getAsJsonObject("qOL") : new JsonObject();
        JsonObject picked = new JsonObject();
        for (var field : LootConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (old.has(field.getName())) picked.add(field.getName(), old.get(field.getName()));
            else if (root.has(field.getName())) picked.add(field.getName(), root.get(field.getName()));
        }
        return picked.isEmpty() ? null : new Gson().fromJson(picked, LootConfig.class);
    }

    public static void save() {
        CONFIG.normalize();
        try {
            write(path(), CONFIG);
        } catch (IOException e) {
            LOGGER.warn("Cannot save Simple Loot settings", e);
        }
        onChange.run();
    }

    private static void write(Path file, LootConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        ConfigFiles.writeAtomic(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
