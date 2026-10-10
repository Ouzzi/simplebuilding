package com.simplebuilding.modules.simpleinterfaces.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One-time takeover from the old mod id {@code simplecontainers} (renamed 2026-10-10): copies
 * {@code config/simplecontainers.json} when the new file is missing and renames the key binding in
 * {@code options.txt}. Runs on client init, before Minecraft reads the options; never overwrites new settings.
 */
public final class LegacyMigration {
    static final String OLD_KEY = "key_key.simplecontainers.toggle_style:";
    static final String NEW_KEY = "key_key.simpleinterfaces.toggle_style:";

    private LegacyMigration() {}

    public static void run(Path gameDir) {
        try {
            Path oldConfig = gameDir.resolve("config/simplecontainers.json");
            Path newConfig = gameDir.resolve("config/simpleinterfaces.json");
            if (Files.isRegularFile(oldConfig) && !Files.exists(newConfig)) Files.copy(oldConfig, newConfig);
            Path options = gameDir.resolve("options.txt");
            if (Files.isRegularFile(options) && Files.size(options) <= 1 << 20) {
                String text = Files.readString(options);
                String migrated = options(text);
                if (!migrated.equals(text)) Files.writeString(options, migrated);
            }
        } catch (IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("Cannot take over simplecontainers settings", e);
        }
    }

    /** The options text with the old key line renamed, unless a new key line already exists. */
    public static String options(String text) {
        if (text.contains(NEW_KEY) || !text.contains(OLD_KEY)) return text;
        return text.replace(OLD_KEY, NEW_KEY);
    }
}
