package com.simplebuilding.modules.simplerespawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplerespawn.json}, read once at start. An unreadable file is quarantined by the
 * framework ({@code ConfigFiles}, {@code .broken-<time>}) and the defaults are written (same policy as the other modules). Every key has a bilingual
 * name and tooltip under {@code config.simplerespawn.<key>} (lang) and a wiki entry.
 */
public final class RespawnConfig {
    public static final String FILE = "simplerespawn.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** All keys in file order (lang/wiki checks read this list). */
    public static final List<String> KEYS = List.of("enabled", "onlyWithOtherPlayers", "bleedOutSeconds", "secondsLostPerDamage",
            "reviveSeconds", "reviveRange", "reviveHealth", "reviveFood", "reviveResistanceSeconds", "reviveWeaknessSeconds",
            "crawlSpeed", "mobsIgnoreDowned", "announce");

    public static boolean enabled = true;
    /** On: knocked down only while at least one other (non-spectator) player is online; off: always. */
    public static boolean onlyWithOtherPlayers = true;
    public static int bleedOutSeconds = 60;
    /** Seconds of the bleed-out time lost per point of damage taken while downed. */
    public static double secondsLostPerDamage = 1.0;
    public static int reviveSeconds = 5;
    public static double reviveRange = 3.0;
    /** Health after a revive (6 = 3 hearts). */
    public static int reviveHealth = 6;
    /** Food level after a revive (0 = empty bar, saturation is always 0). */
    public static int reviveFood = 0;
    public static int reviveResistanceSeconds = 3;
    public static int reviveWeaknessSeconds = 0;
    /** Factor on the (already slow) crawling speed while downed. */
    public static double crawlSpeed = 0.5;
    public static boolean mobsIgnoreDowned = true;
    /** Chat line to everyone when a player is downed or revived. */
    public static boolean announce = true;

    private RespawnConfig() {}

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file, text -> GSON.fromJson(text, JsonObject.class),
                SimpleRespawn.LOG::warn);
        JsonObject json = read == null ? new JsonObject() : read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleRespawn.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads and clamps every value; missing or broken keys keep their defaults. */
    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        onlyWithOtherPlayers = bool(json, "onlyWithOtherPlayers", true);
        bleedOutSeconds = clamp(integer(json, "bleedOutSeconds", 60), 5, 600);
        secondsLostPerDamage = clamp(number(json, "secondsLostPerDamage", 1.0), 0.0, 30.0);
        reviveSeconds = clamp(integer(json, "reviveSeconds", 5), 1, 30);
        reviveRange = clamp(number(json, "reviveRange", 3.0), 1.5, 8.0);
        reviveHealth = clamp(integer(json, "reviveHealth", 6), 1, 20);
        reviveFood = clamp(integer(json, "reviveFood", 0), 0, 20);
        reviveResistanceSeconds = clamp(integer(json, "reviveResistanceSeconds", 3), 0, 30);
        reviveWeaknessSeconds = clamp(integer(json, "reviveWeaknessSeconds", 0), 0, 120);
        crawlSpeed = clamp(number(json, "crawlSpeed", 0.5), 0.0, 1.0);
        mobsIgnoreDowned = bool(json, "mobsIgnoreDowned", true);
        announce = bool(json, "announce", true);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("onlyWithOtherPlayers", onlyWithOtherPlayers);
        o.addProperty("bleedOutSeconds", bleedOutSeconds);
        o.addProperty("secondsLostPerDamage", secondsLostPerDamage);
        o.addProperty("reviveSeconds", reviveSeconds);
        o.addProperty("reviveRange", reviveRange);
        o.addProperty("reviveHealth", reviveHealth);
        o.addProperty("reviveFood", reviveFood);
        o.addProperty("reviveResistanceSeconds", reviveResistanceSeconds);
        o.addProperty("reviveWeaknessSeconds", reviveWeaknessSeconds);
        o.addProperty("crawlSpeed", crawlSpeed);
        o.addProperty("mobsIgnoreDowned", mobsIgnoreDowned);
        o.addProperty("announce", announce);
        return o;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double clamp(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        try { return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback; } catch (Exception e) { return fallback; }
    }

    private static int integer(JsonObject json, String key, int fallback) {
        try { return json.has(key) ? json.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; }
    }

    private static double number(JsonObject json, String key, double fallback) {
        try { return json.has(key) ? json.get(key).getAsDouble() : fallback; } catch (Exception e) { return fallback; }
    }
}
