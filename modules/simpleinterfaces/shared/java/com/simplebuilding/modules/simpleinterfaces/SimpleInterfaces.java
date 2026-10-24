package com.simplebuilding.modules.simpleinterfaces;

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
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Interfaces: server side of the linked containers and Easy Shulkers / Ender Chests (formerly Simple QoL); the local screen style keeps its own config/simpleinterfaces.json. A sub-mod of Simple QoL, also playable alone. The loader entry sets {@link #configDir} and calls
 * {@link #load()}; on the first start the old Simple QoL values of the same name are taken over once.
 */
public final class SimpleInterfaces {
    public static final String MOD_ID = "simpleinterfaces";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    /** Server values; {@code simpleinterfaces.json} stays the local screen style ({@link ContainersConfig}). */
    public static final String FILE = MOD_ID + "-server.json";
    public static LinkedConfig CONFIG = new LinkedConfig();
    /** Runs after a command or the config screen changed the values (loader entries sync them to players). */
    public static Runnable onChange = () -> {};

    private SimpleInterfaces() {}

    /** False when the Simple QoL super-mod switched this sub-mod off ("Enable Simple Interfaces"); same switch as {@link ContainersConfig#on()}. */
    public static boolean active() {
        return SubMods.enabled(MOD_ID);
    }

    /** The server values, numbers always within their bounds. */
    public static LinkedConfig config() {
        CONFIG.normalizeNumbers();
        return CONFIG;
    }

    /** Client side: the server values from the last {@code config_sync} payload, null without one. */
    private static volatile LinkedConfig synced;
    /** Client side: the server's super-mod switch from the same payload. */
    private static volatile boolean syncedActive = true;

    /** Like {@link #active()}, but a client follows the server's switch once it received one. */
    public static boolean active(Level level) {
        return level != null && level.isClientSide() && synced != null ? syncedActive : active();
    }

    /** Gameplay reads the server values; on the client the synchronized copy (prediction must match the server). */
    public static LinkedConfig configFor(Level level) {
        LinkedConfig copy = level != null && level.isClientSide() ? synced : null;
        return copy == null ? config() : copy;
    }

    /** Server values and switch as JSON for the {@code config_sync} payload. */
    public static String syncJson() {
        JsonObject root = new JsonObject();
        root.addProperty("active", active());
        root.add("config", new Gson().toJsonTree(config()));
        return root.toString();
    }

    /** Client: keeps a valid {@code config_sync} payload; invalid or oversized ones are ignored. */
    public static void receiveSync(String json) {
        if (json == null || json.length() > 32768) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            LinkedConfig config = new Gson().fromJson(root.get("config"), LinkedConfig.class);
            if (config != null) {
                config.normalize();
                syncedActive = !root.has("active") || root.get("active").getAsBoolean();
                synced = config;
            }
        } catch (RuntimeException ignored) {
            // keep the previous values
        }
    }

    /** Client: forget the server values when leaving a world. */
    public static void clearSync() {
        synced = null;
        syncedActive = true;
        com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.enderSnapshot = null;
    }

    public static LinkedConfig syncedConfig() {
        return synced;
    }

    static Path path() {
        return configDir.resolve(FILE);
    }

    public static void load() {
        CONFIG = read(configDir);
    }

    /** Own file if present, else the one-time takeover from {@code simplequalityoflife.json} (then written as own file). */
    static LinkedConfig read(Path dir) {
        Path own = dir.resolve(FILE);
        LinkedConfig config = null;
        try {
            if (Files.isRegularFile(own) && Files.size(own) <= 65536) {
                config = new Gson().fromJson(Files.readString(own), LinkedConfig.class);
            } else {
                config = fromQol(dir.resolve("simplequalityoflife.json"));
                if (config != null) write(own, config);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Interfaces settings, using defaults", e);
        }
        if (config == null) config = new LinkedConfig();
        config.normalize();
        return config;
    }

    /** The values of an old Simple QoL config ({@code qOL.<field>} or a top-level field of the same name), or null. */
    static LinkedConfig fromQol(Path qol) throws IOException {
        if (!Files.isRegularFile(qol) || Files.size(qol) > 1 << 20) return null;
        JsonObject root = JsonParser.parseString(Files.readString(qol)).getAsJsonObject();
        JsonObject old = root.has("qOL") && root.get("qOL").isJsonObject() ? root.getAsJsonObject("qOL") : new JsonObject();
        JsonObject picked = new JsonObject();
        for (var field : LinkedConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (old.has(field.getName())) picked.add(field.getName(), old.get(field.getName()));
            else if (root.has(field.getName())) picked.add(field.getName(), root.get(field.getName()));
        }
        return picked.isEmpty() ? null : new Gson().fromJson(picked, LinkedConfig.class);
    }

    public static void save() {
        CONFIG.normalize();
        try {
            write(path(), CONFIG);
        } catch (IOException e) {
            LOGGER.warn("Cannot save Simple Interfaces settings", e);
        }
        onChange.run();
    }

    private static void write(Path file, LinkedConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        ConfigFiles.writeAtomic(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
