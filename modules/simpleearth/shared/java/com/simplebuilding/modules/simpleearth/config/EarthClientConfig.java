package com.simplebuilding.modules.simpleearth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.view.ViewParams;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client config {@code config/simpleearth-client.json}: the chosen version (standard V6, E3), the display settings of all
 * versions ({@code render}, {@link EarthSettings#RENDER}: detail level, render distance, budgets, E9/E10) and one value set
 * per version (schema {@link EarthSettings}). The menu edits it live; every value is clamped to its range.
 */
public final class EarthClientConfig {
    public static final String FILE = "simpleearth-client.json";
    public enum DevButton { AUTO, ON, OFF }

    public static int version = EarthSettings.DEFAULT_VERSION;
    /** Dev button in the pause menu: AUTO = only in a development environment. */
    public static DevButton devButton = DevButton.AUTO;
    private static final Map<Integer, Map<String, Double>> VALUES = new LinkedHashMap<>();
    private static final Map<String, Double> RENDER = new LinkedHashMap<>();
    /**
     * E29-1 (owner 26.10.): heights, size and look ({@link EarthSettings#SHARED}) are ONE set for all versions - a version
     * switch keeps them (they used to be a copy per version and seemed to reset). Only the versions' own style values stay per version.
     */
    private static final Map<String, Double> SHARED = new LinkedHashMap<>();

    static boolean shared(String key) {
        for (EarthSettings.Setting s : EarthSettings.SHARED) if (s.key().equals(key)) return true;
        return false;
    }
    private static Path file;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        apply(new JsonObject());
    }

    public static void load(Path dir) {
        file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleEarth.LOG::warn);
        apply(read == null ? new JsonObject() : read);
        save();
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
        } catch (Exception e) {
            SimpleEarth.LOG.warn("Could not write {}: {}", FILE, e.toString());
        }
    }

    public static void apply(JsonObject json) {
        int v = 0;
        try {
            v = json.has("version") ? json.get("version").getAsInt() : EarthSettings.DEFAULT_VERSION;
        } catch (RuntimeException ignored) {
        }
        version = v >= 1 && v <= EarthSettings.VERSIONS ? v : EarthSettings.DEFAULT_VERSION;
        try {
            devButton = json.has("devButton") ? DevButton.valueOf(json.get("devButton").getAsString().toUpperCase(java.util.Locale.ROOT)) : DevButton.AUTO;
        } catch (RuntimeException e) {
            devButton = DevButton.AUTO;
        }
        JsonObject render = json.has("render") && json.get("render").isJsonObject() ? json.getAsJsonObject("render") : new JsonObject();
        RENDER.clear();
        for (EarthSettings.Setting s : EarthSettings.RENDER) RENDER.put(s.key(), read(s, render.get(s.key())));
        JsonObject versions = json.has("versions") && json.get("versions").isJsonObject() ? json.getAsJsonObject("versions") : new JsonObject();
        // the shared set; older files kept it per version - take the chosen version's copy then
        JsonObject sharedJson = json.has("shared") && json.get("shared").isJsonObject() ? json.getAsJsonObject("shared")
                : versions.has("V" + version) && versions.get("V" + version).isJsonObject() ? versions.getAsJsonObject("V" + version) : new JsonObject();
        SHARED.clear();
        for (EarthSettings.Setting s : EarthSettings.SHARED) SHARED.put(s.key(), read(s, sharedJson.get(s.key())));
        VALUES.clear();
        for (int ver = 1; ver <= EarthSettings.VERSIONS; ver++) {
            JsonObject own = versions.has("V" + ver) && versions.get("V" + ver).isJsonObject() ? versions.getAsJsonObject("V" + ver) : new JsonObject();
            Map<String, Double> map = new LinkedHashMap<>();
            for (EarthSettings.Setting s : EarthSettings.forVersion(ver)) if (!shared(s.key())) map.put(s.key(), read(s, own.get(s.key())));
            VALUES.put(ver, map);
        }
    }

    /** A stored value (number, or true/false for a toggle), clamped; the default when missing or broken. */
    private static double read(EarthSettings.Setting s, JsonElement e) {
        double value = s.def();
        try {
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) value = e.getAsBoolean() ? 1 : 0;
            else if (e != null) value = e.getAsDouble();
        } catch (RuntimeException ignored) {
        }
        return s.clamp(value);
    }

    private static void write(JsonObject out, EarthSettings.Setting s, String k, double val) {
        if (s != null && s.kind() == EarthSettings.Kind.TOGGLE) out.addProperty(k, val >= 0.5);
        else if (s != null && (s.kind() == EarthSettings.Kind.INT || s.kind() == EarthSettings.Kind.LOG)) out.addProperty(k, (long) Math.round(val));
        else out.addProperty(k, val);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("version", version);
        o.addProperty("devButton", devButton.name().toLowerCase(java.util.Locale.ROOT));
        JsonObject render = new JsonObject();
        RENDER.forEach((k, val) -> write(render, EarthSettings.findRender(k), k, val));
        o.add("render", render);
        JsonObject sharedJson = new JsonObject();
        SHARED.forEach((k, val) -> write(sharedJson, EarthSettings.find(1, k), k, val));
        o.add("shared", sharedJson);
        JsonObject versions = new JsonObject();
        VALUES.forEach((ver, map) -> {
            JsonObject own = new JsonObject();
            map.forEach((k, val) -> write(own, EarthSettings.find(ver, k), k, val));
            versions.add("V" + ver, own);
        });
        o.add("versions", versions);
        return o;
    }

    public static double get(int ver, String key) {
        if (shared(key)) {
            Double v = SHARED.get(key);
            if (v != null) return v;
        }
        Map<String, Double> map = VALUES.get(ver);
        Double v = map == null ? null : map.get(key);
        if (v != null) return v;
        EarthSettings.Setting s = EarthSettings.find(ver, key);
        return s == null ? 0 : s.def();
    }

    public static double get(String key) {
        return get(version, key);
    }

    /** Sets a value of a version (clamped); returns the stored value. */
    public static double set(int ver, String key, double value) {
        EarthSettings.Setting s = EarthSettings.find(ver, key);
        if (s == null) return 0;
        double c = s.clamp(value);
        if (shared(key)) SHARED.put(key, c);
        else VALUES.computeIfAbsent(ver, k -> new LinkedHashMap<>()).put(key, c);
        return c;
    }

    /** A display value of all versions ({@link EarthSettings#RENDER}). */
    public static double render(String key) {
        Double v = RENDER.get(key);
        if (v != null) return v;
        EarthSettings.Setting s = EarthSettings.findRender(key);
        return s == null ? 0 : s.def();
    }

    public static boolean renderOn(String key) {
        return render(key) >= 0.5;
    }

    public static double setRender(String key, double value) {
        EarthSettings.Setting s = EarthSettings.findRender(key);
        if (s == null) return 0;
        double c = s.clamp(value);
        RENDER.put(key, c);
        return c;
    }

    public static void resetRender() {
        for (EarthSettings.Setting s : EarthSettings.RENDER) setRender(s.key(), s.def());
    }

    public static void resetVersion(int ver) {
        for (EarthSettings.Setting s : EarthSettings.forVersion(ver)) set(ver, s.key(), s.def());
    }

    /** The values the view maths of the chosen version reads. */
    public static ViewParams params(double edge) {
        ViewParams p = new ViewParams();
        p.edge = edge;
        SHARED.forEach(p::set);
        VALUES.getOrDefault(version, Map.of()).forEach(p::set);
        return p;
    }

    private EarthClientConfig() {}
}
