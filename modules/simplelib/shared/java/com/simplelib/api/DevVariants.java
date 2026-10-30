package com.simplelib.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dev variants (ABN30-9, docs/ai/PLAN-A-DEVUI-2026-10-30.md): one place where modules register look/behaviour variants
 * (a scope frame "A"/"B", icon sets ...) that the owner switches in game from the dev panel next to the inventory
 * ({@code com.simplelib.api.client.ui.DevVariantsPanel}) and names afterwards. A module only registers; it never builds UI.
 * The choice applies at once (read {@link #get} where the variant is drawn) and is stored in
 * {@code config/simplelib-devvariants.json}. No client classes here, so the logic runs in any test.
 */
public final class DevVariants {
    public static final String FILE = "simplelib-devvariants.json";
    private static final Map<String, Variant> VARIANTS = new LinkedHashMap<>();
    private static final Map<String, String> CHOICES = new LinkedHashMap<>();
    private static final List<Consumer<Variant>> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Path dir;
    private static volatile Boolean override;
    private static volatile boolean devEnvironment;

    private DevVariants() {}

    /** One selectable option; {@code id} is stored ("a"), {@code label} shown ("A - 2 px Rahmen"). */
    public record Option(String id, String label) {}

    /** @param id namespaced and unique ("simpleinterfaces:scope_frame"); {@code mod} and {@code name} are shown exactly as the owner names them. */
    public record Variant(String id, String mod, String name, List<Option> options, String defaultOption) {
        public Variant {
            options = List.copyOf(options);
            if (options.isEmpty() || options.stream().noneMatch(o -> o.id().equals(defaultOption))) {
                throw new IllegalArgumentException("default option missing: " + id);
            }
        }

        boolean has(String option) {
            return options.stream().anyMatch(o -> o.id().equals(option));
        }
    }

    public static synchronized Variant register(String id, String mod, String name, String defaultOption, Option... options) {
        Variant v = new Variant(id, mod, name, List.of(options), defaultOption);
        VARIANTS.put(id, v);
        return v;
    }

    public static synchronized List<Variant> all() {
        return List.copyOf(VARIANTS.values());
    }

    /** The chosen option id; an unknown or stale stored choice falls back to the default. Unregistered id: {@code ""}. */
    public static synchronized String get(String id) {
        Variant v = VARIANTS.get(id);
        if (v == null) return "";
        String c = CHOICES.get(id);
        return c != null && v.has(c) ? c : v.defaultOption();
    }

    public static boolean is(String id, String option) {
        return get(id).equals(option);
    }

    /** Takes effect at once; false for an unknown variant/option. Saved. */
    public static boolean select(String id, String option) {
        Variant v;
        synchronized (DevVariants.class) {
            v = VARIANTS.get(id);
            if (v == null || !v.has(option)) return false;
            CHOICES.put(id, option);
        }
        save();
        LISTENERS.forEach(l -> l.accept(v));
        return true;
    }

    /** Called after every {@link #select} (a module that caches the look can refresh it). */
    public static void onChange(Consumer<Variant> listener) {
        LISTENERS.add(listener);
    }

    /** Client entry: a development environment shows the panel by default, a release does not (see {@link #enabled}). */
    public static void setDevEnvironment(boolean value) {
        devEnvironment = value;
    }

    /** Config {@code devVariants}: explicit true/false wins; absent = on in a development environment, off in a release. */
    public static boolean enabled() {
        Boolean o = override;
        return o != null ? o : devEnvironment;
    }

    public static void setEnabled(Boolean value) {
        override = value;
        save();
    }

    /** Reads the file; missing or broken = defaults. Choices of variants that register later are kept. */
    public static synchronized void load(Path configDir) {
        dir = configDir;
        CHOICES.clear();
        override = null;
        if (configDir == null) return;
        try {
            Path file = configDir.resolve(FILE);
            if (Files.isRegularFile(file) && Files.size(file) <= 65536) {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.has("devVariants") && json.get("devVariants").isJsonPrimitive()) override = json.get("devVariants").getAsBoolean();
                if (json.has("choices")) json.getAsJsonObject("choices").entrySet().forEach(e -> CHOICES.put(e.getKey(), e.getValue().getAsString()));
            }
        } catch (Exception ignored) {
            CHOICES.clear();
            override = null;
        }
    }

    public static synchronized void save() {
        if (dir == null) return;
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            if (override != null) json.addProperty("devVariants", override);
            com.google.gson.JsonObject choices = new com.google.gson.JsonObject();
            new ArrayList<>(CHOICES.entrySet()).forEach(e -> choices.addProperty(e.getKey(), e.getValue()));
            json.add("choices", choices);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(FILE), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (Exception e) {
            com.simplelib.SimpleLib.LOG.warn("Cannot save the dev variants: {}", e.toString());
        }
    }

    /** Test helper: forget all variants and choices (does not touch the file). */
    public static synchronized void clearForTest() {
        VARIANTS.clear();
        CHOICES.clear();
        override = null;
        dir = null;
    }
}
