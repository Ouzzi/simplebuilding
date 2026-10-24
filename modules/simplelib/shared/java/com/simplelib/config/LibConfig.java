package com.simplelib.config;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config {@code config/simplelib-server.json} (the warm food options). Every value is clamped to its hard bounds when
 * loaded (plan section 13), so an edited file can never exceed them; missing keys use the defaults
 * and the file is rewritten with the clamped values.
 */
public final class LibConfig {
    public static final int WARM_BASE_DEFAULT = 100, WARM_BASE_MIN = 20, WARM_BASE_MAX = 1200;
    public static final int WARM_DURATION_DEFAULT = 12_000, WARM_DURATION_MIN = 1200, WARM_DURATION_MAX = 48_000;
    public static final int WARM_BUNDLE_DEFAULT = 48_000, WARM_BUNDLE_MIN = 1200, WARM_BUNDLE_MAX = 96_000;
    public static final double EAT_BONUS_DEFAULT = 0.15, EAT_BONUS_MAX = 0.30;

    public static int warmBaseTicks = WARM_BASE_DEFAULT;
    public static int warmDurationTicks = WARM_DURATION_DEFAULT;
    public static int warmBundleDurationTicks = WARM_BUNDLE_DEFAULT;
    public static double eatSpeedBonus = EAT_BONUS_DEFAULT;
    /** Legendary moments (SP22-2, {@link com.simplelib.api.LegendaryMoments}): fanfare + particles; every listed id counts. */
    public static final int LEGENDARY_LIST_MAX = 256;
    public static boolean legendaryEffects = true;
    public static boolean legendaryAllChallenges = false;
    /** W24-3: Vanilla's challenge toast stays silent for a legendary advancement (the choir plays instead); client side. */
    public static boolean legendaryMuteToast = true;
    public static java.util.List<net.minecraft.resources.Identifier> legendaryAdvancements = java.util.List.of();
    public static java.util.List<net.minecraft.resources.Identifier> legendaryEnchantments = java.util.List.of();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE = "simplelib-server.json";
    /** Keys this class does not own (the crucible options before split stage 5): written back unchanged so Simple Crucibles can still take them over. */
    private static JsonObject kept = new JsonObject();
    private static Path file;

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleLib.LOG::warn);
        if (read != null) json = read;
        kept = json.deepCopy();
        apply(json);
        save();
    }

    /** Writes the current values (also from the config screen); keys of other owners stay. */
    public static void save() {
        if (file != null) {
            try {
                Files.createDirectories(file.getParent());
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleLib.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads and clamps every value. Public for the config bounds GameTest. */
    public static void apply(JsonObject json) {
        warmBaseTicks = clamp(getInt(json, "warmBaseTicks", WARM_BASE_DEFAULT), WARM_BASE_MIN, WARM_BASE_MAX);
        warmDurationTicks = clamp(getInt(json, "warmDurationTicks", WARM_DURATION_DEFAULT), WARM_DURATION_MIN, WARM_DURATION_MAX);
        warmBundleDurationTicks = clamp(getInt(json, "warmBundleDurationTicks", WARM_BUNDLE_DEFAULT), WARM_BUNDLE_MIN, WARM_BUNDLE_MAX);
        eatSpeedBonus = clamp(getDouble(json, "eatSpeedBonus", EAT_BONUS_DEFAULT), 0.0, EAT_BONUS_MAX);
        legendaryEffects = getBool(json, "legendaryEffects", true);
        legendaryAllChallenges = getBool(json, "legendaryAllChallenges", false);
        legendaryMuteToast = getBool(json, "legendaryMuteToast", true);
        legendaryAdvancements = getIds(json, "legendaryAdvancements");
        legendaryEnchantments = getIds(json, "legendaryEnchantments");
    }

    /** Valid ids only, no duplicates, at most {@value #LEGENDARY_LIST_MAX}. Public for the config screen. */
    public static java.util.List<net.minecraft.resources.Identifier> ids(Iterable<String> raw) {
        var out = new java.util.LinkedHashSet<net.minecraft.resources.Identifier>();
        for (String s : raw) {
            var id = s == null ? null : net.minecraft.resources.Identifier.tryParse(s.trim());
            if (id != null && !s.isBlank() && out.size() < LEGENDARY_LIST_MAX) out.add(id);
        }
        return java.util.List.copyOf(out);
    }

    private static java.util.List<net.minecraft.resources.Identifier> getIds(JsonObject o, String k) {
        var raw = new java.util.ArrayList<String>();
        try {
            if (o.has(k)) o.getAsJsonArray(k).forEach(e -> raw.add(e.getAsString()));
        } catch (Exception e) {
            return java.util.List.of();
        }
        return ids(raw);
    }

    private static com.google.gson.JsonArray toArray(java.util.List<net.minecraft.resources.Identifier> ids) {
        var a = new com.google.gson.JsonArray();
        ids.forEach(id -> a.add(id.toString()));
        return a;
    }

    public static JsonObject toJson() {
        JsonObject o = kept.deepCopy();
        o.addProperty("warmBaseTicks", warmBaseTicks);
        o.addProperty("warmDurationTicks", warmDurationTicks);
        o.addProperty("warmBundleDurationTicks", warmBundleDurationTicks);
        o.addProperty("eatSpeedBonus", eatSpeedBonus);
        o.addProperty("legendaryEffects", legendaryEffects);
        o.addProperty("legendaryAllChallenges", legendaryAllChallenges);
        o.addProperty("legendaryMuteToast", legendaryMuteToast);
        o.add("legendaryAdvancements", toArray(legendaryAdvancements));
        o.add("legendaryEnchantments", toArray(legendaryEnchantments));
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean getBool(JsonObject o, String k, boolean d) {
        try { return o.has(k) ? o.get(k).getAsBoolean() : d; } catch (Exception e) { return d; }
    }

    private static int getInt(JsonObject o, String k, int d) {
        try { return o.has(k) ? o.get(k).getAsInt() : d; } catch (Exception e) { return d; }
    }

    private static double getDouble(JsonObject o, String k, double d) {
        try {
            double v = o.has(k) ? o.get(k).getAsDouble() : d;
            return Double.isFinite(v) ? v : d;
        } catch (Exception e) { return d; }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private LibConfig() {}
}
