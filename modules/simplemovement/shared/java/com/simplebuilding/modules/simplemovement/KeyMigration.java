package com.simplebuilding.modules.simplemovement;

import com.simplebuilding.framework.api.ConfigFiles;
import java.io.IOException;
import java.nio.file.Path;

/** One-time takeover of the Simple QoL key bindings in {@code options.txt} (no client classes, so tests run on servers). */
public final class KeyMigration {
    static final String[][] KEYS = {
            {"key_key.simplequalityoflife.autowalk:", "key_key.simplemovement.autowalk:"},
            {"key_key.simplequalityoflife.crawl:", "key_key.simplemovement.crawl:"}};

    private KeyMigration() {}

    /** Runs before Minecraft reads the options; never overwrites a new key line. */
    public static void run(Path gameDir) {
        try {
            ConfigFiles.migrateOptions(gameDir, KeyMigration::options);
        } catch (IOException | RuntimeException e) {
            SimpleMovement.LOGGER.warn("Cannot take over the Simple QoL key bindings", e);
        }
    }

    /** The options text with the old key lines renamed, each unless its new line already exists. */
    public static String options(String text) {
        for (var pair : KEYS) text = ConfigFiles.renameOptionKey(text, pair[0], pair[1]);
        return text;
    }
}
