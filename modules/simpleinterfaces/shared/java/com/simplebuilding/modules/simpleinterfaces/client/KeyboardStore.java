package com.simplebuilding.modules.simpleinterfaces.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplelib.api.client.keys.KeyboardEdit;
import com.simplelib.api.client.keys.KeyboardLayout;
import com.simplelib.api.client.keys.KeyboardLayout.Cell;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * The chosen keyboard layout and the player's own layouts ({@code config/simpleinterfaces-keyboards.json}, b-keys34): client
 * side only, nothing reaches a server. {@code selected} is a built-in id (us, de, uk, fr) or the name of an own layout.
 * Own layouts are fixed key lists written with Minecraft key names.
 */
final class KeyboardStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<Cell>> CUSTOM = new LinkedHashMap<>();
    private static String selected = "us";
    private static boolean loaded;

    private KeyboardStore() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("simpleinterfaces-keyboards.json");
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        CUSTOM.clear();
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonObject root = GSON.fromJson(Files.readString(f), JsonObject.class);
            if (root == null) return;
            if (root.has("layouts") && root.get("layouts").isJsonObject()) {
                for (var e : root.getAsJsonObject("layouts").entrySet()) {
                    if (KeyboardEdit.validName(e.getKey())) KeyboardEdit.decode(e.getValue()).ifPresent(cells -> CUSTOM.put(e.getKey(), cells));
                }
            }
            if (root.has("selected")) selected = root.get("selected").getAsString();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("keyboard layouts: {}", e.toString());
        }
        if (!exists(selected)) selected = "us";
    }

    static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("selected", selected);
            JsonObject layouts = new JsonObject();
            CUSTOM.forEach((name, cells) -> layouts.add(name, KeyboardEdit.encode(cells)));
            root.add("layouts", layouts);
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(root));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("keyboard layouts not saved: {}", e.toString());
        }
    }

    static boolean exists(String id) {
        return KeyboardLayout.PRESETS.contains(id) || CUSTOM.containsKey(id);
    }

    static boolean isCustom(String id) {
        load();
        return CUSTOM.containsKey(id);
    }

    static String selected() {
        load();
        return selected;
    }

    static void select(String id) {
        load();
        if (exists(id)) selected = id;
        save();
    }

    /** All ids in cycling order: the built-in layouts, then the own ones. */
    static List<String> ids() {
        load();
        List<String> out = new ArrayList<>(KeyboardLayout.PRESETS);
        out.addAll(CUSTOM.keySet());
        return out;
    }

    /** Short caption for the layout button. */
    static String shortName(String id) {
        return KeyboardLayout.PRESETS.contains(id) ? id.toUpperCase(java.util.Locale.ROOT) : id;
    }

    /** The cells of layout {@code id}; built-in layouts follow {@code size}, own ones are fixed. */
    static List<Cell> cells(String id, KeyboardLayout.Size size) {
        load();
        List<Cell> own = CUSTOM.get(id);
        return own != null ? own : KeyboardLayout.cells(size, KeyboardLayout.PRESETS.contains(id) ? id : "us");
    }

    /** Stores an own layout (replacing one of the same name) and selects it. */
    static void put(String name, List<Cell> cells) {
        load();
        CUSTOM.put(name, List.copyOf(cells));
        selected = name;
        save();
    }

    static void delete(String name) {
        load();
        CUSTOM.remove(name);
        if (selected.equals(name)) selected = "us";
        save();
    }
}
