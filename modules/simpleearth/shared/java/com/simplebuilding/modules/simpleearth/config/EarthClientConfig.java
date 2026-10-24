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
 * Client config {@code config/simpleearth-client.json}: the chosen version (standard V6) and one value set per version
 * (schema {@link EarthSettings}). The menu edits it live; every value is clamped to its range.
 */
public final class EarthClientConfig {
    public static final String FILE = "simpleearth-client.json";
    public enum DevButton { AUTO, ON, OFF }

    public static int version = EarthSettings.DEFAULT_VERSION;
    /** Dev button in the pause menu: AUTO = only in a development environment. */
    public static DevButton devButton = DevButton.AUTO;
    private static final Map<Integer, Map<String, Double>> VALUES = new LinkedHashMap<>();
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
        JsonObject versions = json.has("versions") && json.get("versions").isJsonObject() ? json.getAsJsonObject("versions") : new JsonObject();
        VALUES.clear();
        for (int ver = 1; ver <= EarthSettings.VERSIONS; ver++) {
            JsonObject own = versions.has("V" + ver) && versions.get("V" + ver).isJsonObject() ? versions.getAsJsonObject("V" + ver) : new JsonObject();
            Map<String, Double> map = new LinkedHashMap<>();
            for (EarthSettings.Setting s : EarthSettings.forVersion(ver)) {
                double value = s.def();
                JsonElement e = own.get(s.key());
                try {
                    if (e != null) value = e.getAsDouble();
                } catch (RuntimeException ignored) {
                }
                map.put(s.key(), s.clamp(value));
            }
            VALUES.put(ver, map);
        }
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("version", version);
        o.addProperty("devButton", devButton.name().toLowerCase(java.util.Locale.ROOT));
        JsonObject versions = new JsonObject();
        VALUES.forEach((ver, map) -> {
            JsonObject own = new JsonObject();
            map.forEach((k, val) -> {
                EarthSettings.Setting s = EarthSettings.find(ver, k);
                if (s != null && s.kind() == EarthSettings.Kind.INT) own.addProperty(k, (long) Math.round(val));
                else own.addProperty(k, val);
            });
            versions.add("V" + ver, own);
        });
        o.add("versions", versions);
        return o;
    }

    public static double get(int ver, String key) {
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
        VALUES.computeIfAbsent(ver, k -> new LinkedHashMap<>()).put(key, c);
        return c;
    }

    public static void resetVersion(int ver) {
        for (EarthSettings.Setting s : EarthSettings.forVersion(ver)) set(ver, s.key(), s.def());
    }

    /** The values the view maths of the chosen version reads. */
    public static ViewParams params(double edge) {
        ViewParams p = new ViewParams();
        p.edge = edge;
        VALUES.getOrDefault(version, Map.of()).forEach(p::set);
        return p;
    }

    private EarthClientConfig() {}
}
