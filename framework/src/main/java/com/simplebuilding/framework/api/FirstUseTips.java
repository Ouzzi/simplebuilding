package com.simplebuilding.framework.api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Since 0.1.9. The shared switch for the first pickup tips of every Simple mod (discoverability level H,
 * docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md): the first time a player picks up a machine or tool whose use is not
 * obvious, the mod shows one short line in the action bar. Players who do not want them turn them off per mod.
 *
 * <p>One file for all mods, {@code config/simple-tips.properties}, one key per mod:
 * {@code <modid>.firstPickupTips=true|false}. A missing key reads as {@code true} and is written back, so the file
 * lists every installed Simple mod with tips after its first tip. Read on every tip that would show, so a change
 * applies without a restart. Same pattern as {@link CreativeTabSettings}.
 */
public final class FirstUseTips {
    public static final String FILE = "simple-tips.properties";
    public static final String SUFFIX = ".firstPickupTips";

    private FirstUseTips() {}

    /** Whether {@code modId} may show its first pickup tips; {@code true} unless switched off. */
    public static synchronized boolean firstPickupTips(Path configDir, String modId) {
        if (configDir == null) {
            return true;
        }
        Path file = configDir.resolve(FILE);
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                return true;
            }
        }
        String value = properties.getProperty(modId + SUFFIX);
        if (value == null) {
            properties.setProperty(modId + SUFFIX, "true");
            try {
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Simple mods: one short tip the first time you pick up a machine or tool "
                            + "(false = no tips from that mod)");
                }
            } catch (IOException ignored) {
                // read-only config directory: the default stays in effect
            }
            return true;
        }
        return !"false".equalsIgnoreCase(value.trim());
    }

    /** Writes the switch for {@code modId} (config screens and tests). */
    public static synchronized void set(Path configDir, String modId, boolean enabled) throws IOException {
        Path file = configDir.resolve(FILE);
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        properties.setProperty(modId + SUFFIX, Boolean.toString(enabled));
        Files.createDirectories(configDir);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        }
    }
}
