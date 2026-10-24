package com.simplelib.api.client.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Brightness of the mod inventory and container backgrounds (owner N49, 2026-10-12): {@link #LIGHT} is the look so
 * far (Vanilla's #C6C6C6 boxes), {@link #DARK} a dark grey, {@link #DAWN} in between. Applied centrally in
 * {@link UiPalette}'s colour accessors, so every box, slot and label drawn from a palette follows it; labels switch to
 * {@link UiPalette#LIGHT_LABEL} once the box gets dark. A local look only, stored in
 * {@code config/simplelib-client.json} (no client classes: the file is read in {@code SimpleLib.init}); the
 * simpleinterfaces config screen edits it.
 */
public enum UiBrightness {
    LIGHT(1.0), DAWN(0.75), DARK(0.42);

    public static final String FILE = "simplelib-client.json";
    private static volatile UiBrightness current = LIGHT;
    private static volatile Path configDir;

    /** Factor on the RGB of every box colour (rim excluded). */
    public final double factor;

    UiBrightness(double factor) {
        this.factor = factor;
    }

    public static UiBrightness current() {
        return current;
    }

    /** Sets the brightness (runtime only; {@link #save} writes it). */
    public static void set(UiBrightness brightness) {
        current = brightness == null ? LIGHT : brightness;
    }

    /** {@code "light"}, {@code "dawn"} or {@code "dark"}, case-insensitive; anything else is {@link #LIGHT}. */
    public static UiBrightness parse(String name) {
        if (name != null) {
            for (UiBrightness b : values()) {
                if (b.name().equalsIgnoreCase(name.trim())) {
                    return b;
                }
            }
        }
        return LIGHT;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** A box colour in this brightness. */
    public int tone(int color) {
        return this == LIGHT ? color : (color & 0xFF000000) | (UiPalette.scale(color, this.factor) & 0x00FFFFFF);
    }

    /** The label on {@code fill} (both as stored in the palette): light once the toned box is dark. */
    public int label(int fill, int label) {
        if (this == LIGHT || UiPalette.luminance(tone(fill)) >= 125 || UiPalette.luminance(label) >= 125) {
            return label;
        }
        return UiPalette.LIGHT_LABEL;
    }

    /** Reads {@code config/simplelib-client.json} ({@code {"inventoryBrightness": "dark"}}); missing or broken = light. */
    public static void load(Path dir) {
        configDir = dir;
        set(LIGHT);
        if (dir == null) {
            return;
        }
        try {
            Path file = dir.resolve(FILE);
            if (Files.isRegularFile(file) && Files.size(file) <= 4096) {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.has("inventoryBrightness")) {
                    set(parse(json.get("inventoryBrightness").getAsString()));
                }
            }
        } catch (Exception ignored) {
            set(LIGHT);
        }
    }

    /** CMD24: {@code /simpleclient simplelib set inventoryBrightness light|dawn|dark} (called by the client entry). */
    public static void registerConfigCommand() {
        com.simplelib.api.config.ConfigCommands.client(com.simplelib.SimpleLib.MOD_ID)
                .json(() -> {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("inventoryBrightness", current.id());
                    return json;
                }, json -> set(json.has("inventoryBrightness") ? parse(json.get("inventoryBrightness").getAsString()) : LIGHT))
                .choices("inventoryBrightness", "light", "dawn", "dark").save(UiBrightness::save).register();
    }

    /** Writes the current brightness next to the other configs (no-op before {@link #load}). */
    public static void save() {
        Path dir = configDir;
        if (dir == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("inventoryBrightness", current.id());
            Files.writeString(dir.resolve(FILE), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (Exception e) {
            com.simplelib.SimpleLib.LOG.warn("Cannot save the inventory brightness: {}", e.toString());
        }
    }
}
