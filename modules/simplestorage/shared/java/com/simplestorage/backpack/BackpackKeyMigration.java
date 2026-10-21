package com.simplestorage.backpack;

import com.simplestorage.SimpleStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One-time takeover of SimpleBuilding's backpack key binding in {@code options.txt} (split stage 8c-2: the key is
 * {@code key.simplestorage.open_backpack} now); like Simple Movement's {@code KeyMigration}, no client classes.
 */
public final class BackpackKeyMigration {
    static final String OLD = "key_key.simplebuilding.open_backpack:";
    static final String NEW = "key_key.simplestorage.open_backpack:";

    private BackpackKeyMigration() {}

    /** Runs before Minecraft reads the options; never overwrites a new key line. */
    public static void run(Path gameDir) {
        try {
            Path options = gameDir.resolve("options.txt");
            if (!Files.isRegularFile(options) || Files.size(options) > 1 << 20) return;
            String text = Files.readString(options);
            String migrated = options(text);
            if (!migrated.equals(text)) Files.writeString(options, migrated);
        } catch (IOException | RuntimeException e) {
            SimpleStorage.LOG.warn("Cannot take over SimpleBuilding's backpack key binding", e);
        }
    }

    /** The options text with the old key line renamed, unless the new line already exists. */
    public static String options(String text) {
        return text.contains(OLD) && !text.contains(NEW) ? text.replace(OLD, NEW) : text;
    }
}
