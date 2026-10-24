package com.simplebuilding.modules.simpletools;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Tools: full-durability bonus, Thrift, anvil repair cost and furnace lava fill (formerly Simple QoL). A sub-mod of Simple QoL, also playable alone. The loader entry sets {@link #configDir} and calls
 * {@link #load()}; on the first start the old Simple QoL values of the same name are taken over once.
 */
public final class SimpleTools {
    public static final String MOD_ID = "simpletools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    public static ToolsConfig CONFIG = new ToolsConfig();
    /** Runs after a command or the config screen changed the values (loader entries sync them to players). */
    public static Runnable onChange = () -> {};

    private SimpleTools() {}

    /** False when the Simple QoL super-mod switched this sub-mod off ("Enable Simple Tools"). */
    public static boolean active() {
        return SubMods.enabled(MOD_ID);
    }

    /** The server values, numbers always within their bounds. */
    public static ToolsConfig config() {
        CONFIG.normalizeNumbers();
        return CONFIG;
    }

    /** Client side: the server values from the last {@code config_sync} payload, null without one. */
    private static volatile ToolsConfig synced;
    /** Client side: the server's super-mod switch from the same payload. */
    private static volatile boolean syncedActive = true;

    /** Like {@link #active()}, but a client follows the server's switch once it received one. */
    public static boolean active(Level level) {
        return level != null && level.isClientSide() && synced != null ? syncedActive : active();
    }

    /** Gameplay reads the server values; on the client the synchronized copy (prediction must match the server). */
    public static ToolsConfig configFor(Level level) {
        ToolsConfig copy = level != null && level.isClientSide() ? synced : null;
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
            ToolsConfig config = new Gson().fromJson(root.get("config"), ToolsConfig.class);
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

    public static ToolsConfig syncedConfig() {
        return synced;
    }

    static Path path() {
        return configDir.resolve(MOD_ID + ".json");
    }

    /**
     * Ids taken over from Simple Mobs (21.10.: the Biome Brush moved here) and the shared Netherite nugget its recipes use.
     * Called first in every loader entry, before registry events and data packs.
     */
    public static void registerMoves() {
        Identifier from = Identifier.fromNamespaceAndPath("simplemobs", "biome_brush");
        com.simplelib.api.IdMoves.move(Registries.ITEM, from, ToolsRegistry.BIOME_BRUSH_ID);
        com.simplelib.api.IdMoves.move(Registries.RECIPE, from, ToolsRegistry.BIOME_BRUSH_ID);
        com.simplelib.api.IdMoves.move(Registries.ADVANCEMENT, Identifier.fromNamespaceAndPath("simplemobs", "recipes/tools/biome_brush"),
                Identifier.fromNamespaceAndPath(MOD_ID, "recipes/tools/biome_brush"));
        com.simplelib.api.SimpleLibApi.requestMaterial("netherite_nugget");
    }

    public static void load() {
        CONFIG = read(configDir);
        // CMD24: /<modid> config for every option; save() normalises, writes and syncs the clients.
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).gson(ToolsConfig.class, () -> CONFIG, c -> { c.normalize(); CONFIG = c; },
                () -> { ToolsConfig d = new ToolsConfig(); d.normalize(); return d; }).save(() -> save()).register();
    }

    /** Own file if present, else the one-time takeover from {@code simplequalityoflife.json} (then written as own file). */
    static ToolsConfig read(Path dir) {
        Path own = dir.resolve(MOD_ID + ".json");
        ToolsConfig config = null;
        try {
            if (Files.isRegularFile(own) && Files.size(own) <= 65536) {
                config = new Gson().fromJson(Files.readString(own), ToolsConfig.class);
            } else {
                config = fromQol(dir.resolve("simplequalityoflife.json"));
                if (config != null) write(own, config);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Tools settings, using defaults", e);
        }
        if (config == null) config = new ToolsConfig();
        config.normalize();
        return config;
    }

    /** The values of an old Simple QoL config ({@code qOL.<field>} or a top-level field of the same name), or null. */
    static ToolsConfig fromQol(Path qol) throws IOException {
        if (!Files.isRegularFile(qol) || Files.size(qol) > 1 << 20) return null;
        JsonObject root = JsonParser.parseString(Files.readString(qol)).getAsJsonObject();
        JsonObject old = root.has("qOL") && root.get("qOL").isJsonObject() ? root.getAsJsonObject("qOL") : new JsonObject();
        JsonObject picked = new JsonObject();
        for (var field : ToolsConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (old.has(field.getName())) picked.add(field.getName(), old.get(field.getName()));
            else if (root.has(field.getName())) picked.add(field.getName(), root.get(field.getName()));
        }
        return picked.isEmpty() ? null : new Gson().fromJson(picked, ToolsConfig.class);
    }

    public static void save() {
        CONFIG.normalize();
        try {
            write(path(), CONFIG);
        } catch (IOException e) {
            LOGGER.warn("Cannot save Simple Tools settings", e);
        }
        onChange.run();
    }

    private static void write(Path file, ToolsConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        ConfigFiles.writeAtomic(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
