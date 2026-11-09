package com.simplebuilding.modules.simplemobmanners;

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
 * Simple Mob Manners (formerly Simple Creatures, id {@code simplecreatures}): muted mobs, named babies that stay young and
 * piglins that accept gold trims and tools (formerly Simple QoL). A sub-mod of Simple QoL, also playable alone. The loader
 * entry sets {@link #configDir} and calls {@link #load()}; on the first start an old {@code simplecreatures.json} or else
 * the old Simple QoL values of the same name are taken over once. Its only registry id is the new
 * {@code simplemobmanners:frog_bucket} (N62, see {@link FrogBucket}), so there is nothing to move through SimpleLib {@code IdMoves}.
 */
public final class SimpleMobManners {
    public static final String MOD_ID = "simplemobmanners";
    /** The id before the rename: its config file is taken over once, {@code /simplecreatures} stays as an alias. */
    public static final String LEGACY_ID = "simplecreatures";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    public static MobMannersConfig CONFIG = new MobMannersConfig();
    /**
     * PORT FLAG (W10, IG31-63). Vanilla 26.3 replaced the "baby name tag" idea by the golden dandelion (age lock, {@code
     * AgeableMob.canUseGoldenDandelion}), so on 26.3 and newer the name-suffix mechanic is OFF (false) and only the dandelion
     * works. The ports to 26.2 and 1.21.11 (no dandelion there) set this to {@code true}: the suffix keeps babies young as before.
     * There is no item to migrate; old configs keep {@code nametagBabySuffixes} and old named animals simply age normally.
     */
    public static final boolean BABY_NAMETAG_LEGACY = false;
    /** Runs after a command or the config screen changed the values (loader entries sync them to players). */
    public static Runnable onChange = () -> {};

    private SimpleMobManners() {}

    /** False when the Simple QoL super-mod switched this sub-mod off ("Enable Simple Mob Manners"). */
    public static boolean active() {
        return SubMods.enabled(MOD_ID);
    }

    /** The server values, numbers always within their bounds. */
    public static MobMannersConfig config() {
        CONFIG.normalizeNumbers();
        return CONFIG;
    }

    static Path path() {
        return configDir.resolve(MOD_ID + ".json");
    }

    public static void load() {
        CONFIG = read(configDir);
    }

    /**
     * Own file if present, else the one-time takeover from the old {@code simplecreatures.json} (renamed mod) or from
     * {@code simplequalityoflife.json} (then written as own file; the old files stay untouched).
     */
    static MobMannersConfig read(Path dir) {
        Path own = dir.resolve(MOD_ID + ".json");
        MobMannersConfig config = null;
        try {
            Path legacy = dir.resolve(LEGACY_ID + ".json");
            if (Files.isRegularFile(own) && Files.size(own) <= 65536) {
                config = new Gson().fromJson(Files.readString(own), MobMannersConfig.class);
            } else if (Files.isRegularFile(legacy) && Files.size(legacy) <= 65536) {
                config = new Gson().fromJson(Files.readString(legacy), MobMannersConfig.class);
                if (config != null) {
                    write(own, config);
                    LOGGER.info("Took over {} as {} (Simple Creatures is now Simple Mob Manners)", legacy.getFileName(), own.getFileName());
                }
            } else {
                config = fromQol(dir.resolve("simplequalityoflife.json"));
                if (config != null) write(own, config);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Simple Mob Manners settings, using defaults", e);
        }
        if (config == null) config = new MobMannersConfig();
        config.normalize();
        return config;
    }

    /** The values of an old Simple QoL config ({@code qOL.<field>} or a top-level field of the same name), or null. */
    static MobMannersConfig fromQol(Path qol) throws IOException {
        if (!Files.isRegularFile(qol) || Files.size(qol) > 1 << 20) return null;
        JsonObject root = JsonParser.parseString(Files.readString(qol)).getAsJsonObject();
        JsonObject old = root.has("qOL") && root.get("qOL").isJsonObject() ? root.getAsJsonObject("qOL") : new JsonObject();
        JsonObject picked = new JsonObject();
        for (var field : MobMannersConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (old.has(field.getName())) picked.add(field.getName(), old.get(field.getName()));
            else if (root.has(field.getName())) picked.add(field.getName(), root.get(field.getName()));
        }
        return picked.isEmpty() ? null : new Gson().fromJson(picked, MobMannersConfig.class);
    }

    public static void save() {
        CONFIG.normalize();
        try {
            write(path(), CONFIG);
        } catch (IOException e) {
            LOGGER.warn("Cannot save Simple Mob Manners settings", e);
        }
        onChange.run();
    }

    private static void write(Path file, MobMannersConfig config) throws IOException {
        Files.createDirectories(file.getParent());
        ConfigFiles.writeAtomic(file, new GsonBuilder().setPrettyPrinting().create().toJson(config));
    }
}
