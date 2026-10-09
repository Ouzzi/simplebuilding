package com.simplebuilding.framework.api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Since 0.1.5. The shared creative-tab switch of every Simple mod (owner N22): each mod keeps its own creative
 * tab, and additionally sorts its items into the fitting Vanilla tabs - unless the player turns that off for
 * this mod. Off means the Vanilla tabs (and so the search tab) stay exactly as Vanilla builds them.
 *
 * <p>One file for all mods, {@code config/simple-creative-tabs.properties}, one key per mod:
 * {@code <modid>.addItemsToVanillaTabs=true|false}. A missing key reads as {@code true} and is written back, so
 * the file lists every installed Simple mod after its first start. Creative tabs are built on the client when a
 * world is joined, so the value is read at that moment (no restart needed).
 */
public final class CreativeTabSettings {
    public static final String FILE = "simple-creative-tabs.properties";
    public static final String SUFFIX = ".addItemsToVanillaTabs";

    private CreativeTabSettings() {}

    /** Whether {@code modId} may add its items to Vanilla creative tabs; {@code true} unless switched off. */
    public static synchronized boolean addItemsToVanillaTabs(Path configDir, String modId) {
        String key = modId + SUFFIX;
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
        String value = properties.getProperty(key);
        if (value == null) {
            properties.setProperty(key, "true");
            try {
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Simple mods: add each mod's items to the Vanilla creative tabs too "
                            + "(false = Vanilla tabs stay unchanged, the mod's own tab remains)");
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
