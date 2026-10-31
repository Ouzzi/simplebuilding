package com.simpleresonance.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpleresonance.SimpleResonance;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpleresonance-server.json} (server authority, every number clamped to a safe range); missing or
 * broken keys use the defaults and the file is rewritten complete. Defaults are the recommendations of
 * docs/ai/KONZEPT-A-WP22-2026-10-29.md (owner question Q1-Q10, docs/ai/PLAN-A-ZONES31-2026-10-30.md).
 */
public final class ResonanceConfig {
    public static final String FILE = "simpleresonance-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "center", "z0", "d1", "d2", "zmax", "overworld", "nether", "end",
            "netherFactor", "netherZ0", "endZ0", "endOuterBonus", "otherDimensions", "ownerRadius", "snapRatio", "softSnap", "ownerlessCap",
            "ignoreAfkMinutes", "lifeWeight", "tau", "deathKeep", "bmax", "ahead", "graceHours", "nearbyCap", "nearbyRadius", "noise",
            "killPoints", "killPointsPerHour", "bossCooldownHours", "trimBenefitBaseMultiplier");

    /** Zone centre keywords; anything else is read as {@code "x,z"}. */
    public static final String CENTER_SPAWN = "world_spawn", CENTER_ORIGIN = "0,0";

    public static boolean enabled = true;
    public static String center = CENTER_SPAWN;
    /** Zone curve Z(d) = z0 + (zmax - z0) x smoothstep((d - d1) / (d2 - d1)). */
    public static double z0 = 0.5, zmax = 5.0, d1 = 512, d2 = 6000;
    public static boolean overworld = true, nether = true, end = true;
    /** Nether blocks count this many times (Vanilla 8:1) and the Nether is never milder than netherZ0; End likewise. */
    public static double netherFactor = 8.0, netherZ0 = 2.0, endZ0 = 3.0;
    /** End: outer islands (more than 1000 blocks from 0,0) add this to the level. */
    public static double endOuterBonus = 1.0;
    /** Dimensions of other mods use the Overworld curve. */
    public static boolean otherDimensions = true;
    public static double ownerRadius = 128;
    /** Nearest player takes the spawn at 100 % when he is at most 1/snapRatio as far away as the second nearest (0 = off). */
    public static double snapRatio = 3.0;
    /** Soft instead of hard step at the snap line (owner question 4). */
    public static boolean softSnap = false;
    /** Level cap for spawns nobody owns (chunk generation far from players). */
    public static double ownerlessCap = 2.0;
    /** Players idle longer than this do not own spawns (0 = AFK players count). */
    public static int ignoreAfkMinutes = 5;
    /** Share of the long-term value in W; the rest is the resonance share F since the last death. */
    public static double lifeWeight = 0.7;
    /** W_long = 1 - exp(-V / tau); V in points (1 point per 30 active minutes). */
    public static double tau = 30;
    /** Share of V kept after a death. */
    public static double deathKeep = 0.75;
    /** A top player raises the zone by at most this many levels. */
    public static double bmax = 2.0;
    /** A spawn is at most this many levels above the owner's own level P. */
    public static double ahead = 1.0;
    /** Playing hours with level at most 1; weakly rising to 5 over the same time again. */
    public static double graceHours = 6.0;
    /** Other players near the spawn cap its level as well (a pro's mob must not hit a newcomer standing next to him). */
    public static boolean nearbyCap = true;
    public static double nearbyRadius = 40;
    /** Uniform noise of a spawn's level, plus/minus this much. */
    public static double noise = 0.4;
    /** Points per kill of a hostile mob, at most killPointsPerHour per played hour (farm guard: kills at most double the progress of the time itself, 2 points per active hour). */
    public static double killPoints = 0.1, killPointsPerHour = 2.0;
    /** The same boss gives points once per this many played hours. */
    public static double bossCooldownHours = 24.0;
    /** Base of the old trim resonance (imported from the SimpleBuilding config on first start). */
    public static double trimBenefitBaseMultiplier = 2.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ResonanceConfig() {}

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleResonance.LOG::warn);
        if (read != null) {
            json = read;
        } else if (dir != null) {
            // First start: take the trim base multiplier over from the SimpleBuilding config (stage 1 of the move, no data lost).
            JsonObject legacy = readLegacy(dir.resolve("simplebuilding.json"));
            if (legacy != null && legacy.has("trimBenefitBaseMultiplier")) json.add("trimBenefitBaseMultiplier", legacy.get("trimBenefitBaseMultiplier"));
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleResonance.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads another mod's file without touching it (no quarantine: it is not ours); {@code null} when missing or unreadable. */
    public static JsonObject readLegacy(Path file) {
        try {
            return Files.isRegularFile(file) ? GSON.fromJson(Files.readString(file), JsonObject.class) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        center = text(json, "center", CENTER_SPAWN);
        if (!validCenter(center)) center = CENTER_SPAWN;
        z0 = clamp(number(json, "z0", 0.5), 0, 5);
        zmax = clamp(number(json, "zmax", 5.0), z0, 5);
        d1 = clamp(number(json, "d1", 512), 0, 100000);
        d2 = clamp(number(json, "d2", 6000), d1 + 1, 200000);
        overworld = bool(json, "overworld", true);
        nether = bool(json, "nether", true);
        end = bool(json, "end", true);
        netherFactor = clamp(number(json, "netherFactor", 8.0), 1, 64);
        netherZ0 = clamp(number(json, "netherZ0", 2.0), 0, 5);
        endZ0 = clamp(number(json, "endZ0", 3.0), 0, 5);
        endOuterBonus = clamp(number(json, "endOuterBonus", 1.0), 0, 5);
        otherDimensions = bool(json, "otherDimensions", true);
        ownerRadius = clamp(number(json, "ownerRadius", 128), 16, 512);
        snapRatio = clamp(number(json, "snapRatio", 3.0), 0, 20);
        softSnap = bool(json, "softSnap", false);
        ownerlessCap = clamp(number(json, "ownerlessCap", 2.0), 0, 5);
        ignoreAfkMinutes = (int) clamp(number(json, "ignoreAfkMinutes", 5), 0, 1440);
        lifeWeight = clamp(number(json, "lifeWeight", 0.7), 0, 1);
        tau = clamp(number(json, "tau", 30), 1, 1000);
        deathKeep = clamp(number(json, "deathKeep", 0.75), 0, 1);
        bmax = clamp(number(json, "bmax", 2.0), 0, 5);
        ahead = clamp(number(json, "ahead", 1.0), 0, 5);
        graceHours = clamp(number(json, "graceHours", 6.0), 0, 1000);
        nearbyCap = bool(json, "nearbyCap", true);
        nearbyRadius = clamp(number(json, "nearbyRadius", 40), 4, 128);
        noise = clamp(number(json, "noise", 0.4), 0, 2);
        killPoints = clamp(number(json, "killPoints", 0.1), 0, 10);
        killPointsPerHour = clamp(number(json, "killPointsPerHour", 2.0), 0, 1000);
        bossCooldownHours = clamp(number(json, "bossCooldownHours", 24.0), 0, 10000);
        trimBenefitBaseMultiplier = clamp(number(json, "trimBenefitBaseMultiplier", 2.0), 0, 10);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("center", center);
        o.addProperty("z0", z0);
        o.addProperty("d1", d1);
        o.addProperty("d2", d2);
        o.addProperty("zmax", zmax);
        o.addProperty("overworld", overworld);
        o.addProperty("nether", nether);
        o.addProperty("end", end);
        o.addProperty("netherFactor", netherFactor);
        o.addProperty("netherZ0", netherZ0);
        o.addProperty("endZ0", endZ0);
        o.addProperty("endOuterBonus", endOuterBonus);
        o.addProperty("otherDimensions", otherDimensions);
        o.addProperty("ownerRadius", ownerRadius);
        o.addProperty("snapRatio", snapRatio);
        o.addProperty("softSnap", softSnap);
        o.addProperty("ownerlessCap", ownerlessCap);
        o.addProperty("ignoreAfkMinutes", ignoreAfkMinutes);
        o.addProperty("lifeWeight", lifeWeight);
        o.addProperty("tau", tau);
        o.addProperty("deathKeep", deathKeep);
        o.addProperty("bmax", bmax);
        o.addProperty("ahead", ahead);
        o.addProperty("graceHours", graceHours);
        o.addProperty("nearbyCap", nearbyCap);
        o.addProperty("nearbyRadius", nearbyRadius);
        o.addProperty("noise", noise);
        o.addProperty("killPoints", killPoints);
        o.addProperty("killPointsPerHour", killPointsPerHour);
        o.addProperty("bossCooldownHours", bossCooldownHours);
        o.addProperty("trimBenefitBaseMultiplier", trimBenefitBaseMultiplier);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    /** {@code world_spawn}, {@code 0,0} or {@code "x,z"} with two finite numbers. */
    public static boolean validCenter(String value) {
        if (CENTER_SPAWN.equals(value)) return true;
        return parseCenter(value) != null;
    }

    /** The fixed centre {x, z} of a coordinate value, or {@code null} for {@code world_spawn} and broken text. */
    public static double[] parseCenter(String value) {
        if (value == null || CENTER_SPAWN.equals(value)) return null;
        String[] parts = value.split(",");
        if (parts.length != 2) return null;
        try {
            double x = Double.parseDouble(parts[0].trim()), z = Double.parseDouble(parts[1].trim());
            return Double.isFinite(x) && Double.isFinite(z) ? new double[] {x, z} : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static String text(JsonObject json, String key, String fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() ? v.getAsString().trim() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        JsonElement v = json.get(key);
        try {
            double d = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : fallback;
            return Double.isFinite(d) ? d : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static {
        reset();
    }
}
