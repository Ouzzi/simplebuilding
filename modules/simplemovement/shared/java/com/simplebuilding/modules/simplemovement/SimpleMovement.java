package com.simplebuilding.modules.simplemovement;

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
 * Simple Movement: crawling, autowalk, ladder speed and Frost Walker on powder snow (formerly Simple QoL). A sub-mod of Simple QoL, also playable alone. The loader entry sets {@link #configDir} and calls
 * {@link #load()}; on the first start the old Simple QoL values of the same name are taken over once.
 */
public final class SimpleMovement {
    public static final String MOD_ID = "simplemovement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    public static MovementConfig CONFIG = new MovementConfig();
    /** Runs after a command or the config screen changed the values (loader entries sync them to players). */
    public static Runnable onChange = () -> {};
    /** Server: sends a player's crawl pose to that player and everyone tracking them (set by the loader entry). */
    public static java.util.function.BiConsumer<net.minecraft.world.entity.player.Player, Boolean> crawlSync = (p, c) -> {};

    private SimpleMovement() {}

    /** False when the Simple QoL super-mod switched this sub-mod off ("Enable Simple Movement"). */
    public static boolean active() {
        return SubMods.enabled(MOD_ID);
    }

    /** The server values, numbers always within their bounds. */
    public static MovementConfig config() {
        CONFIG.normalizeNumbers();
        return CONFIG;
    }

    /** Client side: the server values from the last {@code config_sync} payload, null without one. */
    private static volatile MovementConfig synced;
    /** Client side: the server's super-mod switch from the same payload. */
    private static volatile boolean syncedActive = true;

    /** Like {@link #active()}, but a client follows the server's switch once it received one. */
    public static boolean active(Level level) {
        return level != null && level.isClientSide() && synced != null ? syncedActive : active();
    }

    /** Gameplay reads the server values; on the client the synchronized copy (prediction must match the server). */
    public static MovementConfig configFor(Level level) {
        MovementConfig copy = level != null && level.isClientSide() ? synced : null;
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
            MovementConfig config = new Gson().fromJson(root.get("config"), MovementConfig.class);
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
    }

    public static MovementConfig syncedConfig() {
        return synced;
    }

    static Path path() {
        return configDir.resolve(MOD_ID + ".json");
    }

    public static void load() {
        CONFIG = read(configDir);
    }

    /** Own file if present, else the one-time takeover from {@code simplequalityoflife.json} (then written as own file). */
    static MovementConfig read(Path dir) {
        Path own = dir.resolve(MOD_ID + ".json");
        MovementConfig config = null;
        try {
            if (Files.isRegularFile(own) && Files.size(own) <= 65536) {
                config = new Gson().fromJson(Files.readString(own), MovementConfig.class);
            } else {
                config = fromQol(dir.resolve("simplequalityoflife.json"));
                if (config != null) write(own, config);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Movement settings, using defaults", e);
        }
        if (config == null) config = new MovementConfig();
        config.normalize();
        return config;
    }

    /** The values of an old Simple QoL config ({@code qOL.<field>} or a top-level field of the same name), or null. */
    static MovementConfig fromQol(Path qol) throws IOException {
        if (!Files.isRegularFile(qol) || Files.size(qol) > 1 << 20) return null;
        JsonObject root = JsonParser.parseString(Files.readString(qol)).getAsJsonObject();
        JsonObject old = root.has("qOL") && root.get("qOL").isJsonObject() ? root.getAsJsonObject("qOL") : new JsonObject();
        JsonObject picked = new JsonObject();
        for (var field : MovementConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (old.has(field.getName())) picked.add(field.getName(), old.get(field.getName()));
            else if (root.has(field.getName())) picked.add(field.getName(), root.get(field.getName()));
        }
        return picked.isEmpty() ? null : new Gson().fromJson(picked, MovementConfig.class);
    }

    public static void save() {
        CONFIG.normalize();
        try {
            write(path(), CONFIG);
        } catch (IOException e) {
            LOGGER.warn("Cannot save Simple Movement settings", e);
        }
        onChange.run();
    }

    private static void write(Path file, MovementConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        ConfigFiles.writeAtomic(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
