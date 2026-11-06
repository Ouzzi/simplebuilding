package com.simplebuilding.modules.simpleenddragonfight;

import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server config {@code config/simpleenddragonfight.json}, read once at start. An unreadable file is renamed to
 * {@code .broken-<time>} and the defaults are written (same policy as the other modules). Every key has a bilingual
 * name and tooltip under {@code config.simpleenddragonfight.<key>} (lang) and a wiki entry.
 */
public final class DragonFightConfig {
    public static final String FILE = "simpleenddragonfight.json";
    private static final Logger LOG = LoggerFactory.getLogger(DragonFightRegistry.ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** All keys in file order (lang/wiki checks read this list). */
    public static final List<String> KEYS = List.of("enabled", "raiseBaseHealth", "maxPlayers", "countCreative", "leaveGraceSeconds",
            "islandRadius", "speedMultiplier", "speedPerPlayer", "speedCap", "crystalHealScaling", "mitesEnabled", "waveIntervalSeconds",
            "miteBase", "miteExtraPerTwoPlayers", "maxAliveMites", "animationEnabled", "animationTicks", "maxQueued", "exposeSpawnEgg", "summonScaling", "summonMaxMultiplier", "summonSpeedCap",
            "rewardEnabled", "rewardFromDragon", "rewardItem", "rewardJewelFromDragon", "rewardJewelItem", "tintFromDragon", "summonAttackCap", "tintJewelFromDragon", "xpPerFight", "xpMax", "miteExtraPerSummons");

    public static boolean enabled = true;
    /** On: 300 health per counted player; off: the Vanilla base (200) per player. */
    public static boolean raiseBaseHealth = true;
    /** 0 = every player counts. */
    public static int maxPlayers = 0;
    public static boolean countCreative = false;
    public static int leaveGraceSeconds = 60;
    public static int islandRadius = 160;
    public static double speedMultiplier = 1.20;
    public static double speedPerPlayer = 0.015;
    public static double speedCap = 1.35;
    public static boolean crystalHealScaling = true;
    public static boolean mitesEnabled = true;
    public static int waveIntervalSeconds = 30;
    public static int miteBase = 2;
    public static int miteExtraPerTwoPlayers = 1;
    public static int maxAliveMites = 16;
    public static boolean animationEnabled = true;
    public static int animationTicks = 60;
    public static int maxQueued = 16;
    public static boolean exposeSpawnEgg = false;
    /** Each further summoning: +15 % health, damage and speed (0 = off). */
    public static double summonScaling = 0.15;
    /** Highest summoning multiplier. */
    public static double summonMaxMultiplier = 3.0;
    /** Highest total flight speed with summonings (the player speed cap stays below it). */
    public static double summonSpeedCap = 2.0;
    /** WI26-6: reward item for every participant from the n-th defeated dragon on, once per player. */
    public static boolean rewardEnabled = true;
    public static int rewardFromDragon = 4;
    public static String rewardItem = "simplemobs:ender_heart";
    /** WI26-6: second reward (default Ender Gem) from the n-th defeated dragon on; 0 = off. */
    public static int rewardJewelFromDragon = 10;
    public static String rewardJewelItem = "simplemobs:ender_gem";
    /** WI26-6: the n-th fight and later: dragon in chorus colour (client tint); 0 = off. */
    public static int tintFromDragon = 4;
    /** WI26-6: the n-th fight and later: soul blue and chorus violet together (Ender Gem fight); 0 = off. */
    public static int tintJewelFromDragon = 10;
    /** Damage the dragon deals never rises above this factor through summonings (health and speed follow summonMaxMultiplier). */
    public static double summonAttackCap = 2.0;

    /** Extra XP per further defeated dragon: fight n (n >= 2) drops 500 + xpPerFight * (n - 1) in total; 0 = Vanilla (500). */
    public static int xpPerFight = 250;
    /** Highest total XP of one dragon fight from the 2nd on (the 1st keeps the Vanilla 12000). */
    public static int xpMax = 5000;
    /** One more Dragonmite per wave for every n further summonings; 0 = off. */
    public static int miteExtraPerSummons = 3;

    private DragonFightConfig() {}

    public static void load(Path dir) {
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject json = new JsonObject();
        if (file != null && Files.isRegularFile(file)) {
            try {
                JsonObject read = GSON.fromJson(Files.readString(file), JsonObject.class);
                if (read == null) throw new IllegalStateException("file is empty");
                json = read;
            } catch (Exception e) {
                Path broken = file.resolveSibling(FILE + ".broken-" + System.currentTimeMillis());
                try { Files.move(file, broken); } catch (Exception ignored) { }
                LOG.warn("Unreadable {}, using defaults ({}); old file kept as {}", FILE, e.toString(), broken.getFileName());
            }
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    /** Reads and clamps every value; missing or broken keys keep their defaults. */
    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        raiseBaseHealth = bool(json, "raiseBaseHealth", true);
        maxPlayers = clamp(integer(json, "maxPlayers", 0), 0, 1000);
        countCreative = bool(json, "countCreative", false);
        leaveGraceSeconds = clamp(integer(json, "leaveGraceSeconds", 60), 0, 3600);
        islandRadius = clamp(integer(json, "islandRadius", 160), 16, 1000);
        speedMultiplier = clamp(number(json, "speedMultiplier", 1.20), 0.5, 3.0);
        speedPerPlayer = clamp(number(json, "speedPerPlayer", 0.015), 0.0, 0.5);
        speedCap = clamp(number(json, "speedCap", 1.35), 0.5, 3.0);
        crystalHealScaling = bool(json, "crystalHealScaling", true);
        mitesEnabled = bool(json, "mitesEnabled", true);
        waveIntervalSeconds = clamp(integer(json, "waveIntervalSeconds", 30), 5, 600);
        miteBase = clamp(integer(json, "miteBase", 2), 0, 16);
        miteExtraPerTwoPlayers = clamp(integer(json, "miteExtraPerTwoPlayers", 1), 0, 8);
        maxAliveMites = clamp(integer(json, "maxAliveMites", 16), 0, 64);
        animationEnabled = bool(json, "animationEnabled", true);
        animationTicks = clamp(integer(json, "animationTicks", 60), 20, 200);
        maxQueued = clamp(integer(json, "maxQueued", 16), 1, 64);
        exposeSpawnEgg = bool(json, "exposeSpawnEgg", false);
        summonScaling = clamp(number(json, "summonScaling", 0.15), 0.0, 1.0);
        summonMaxMultiplier = clamp(number(json, "summonMaxMultiplier", 3.0), 1.0, 10.0);
        summonSpeedCap = clamp(number(json, "summonSpeedCap", 2.0), 0.5, 3.0);
        rewardEnabled = bool(json, "rewardEnabled", true);
        rewardFromDragon = clamp(integer(json, "rewardFromDragon", 4), 1, 1000);
        rewardItem = text(json, "rewardItem", "simplemobs:ender_heart");
        rewardJewelFromDragon = clamp(integer(json, "rewardJewelFromDragon", 10), 0, 1000);
        rewardJewelItem = text(json, "rewardJewelItem", "simplemobs:ender_gem");
        tintFromDragon = clamp(integer(json, "tintFromDragon", 4), 0, 1000);
        tintJewelFromDragon = clamp(integer(json, "tintJewelFromDragon", 10), 0, 1000);
        summonAttackCap = clamp(number(json, "summonAttackCap", 2.0), 1.0, 10.0);
        xpPerFight = clamp(integer(json, "xpPerFight", 250), 0, 5000);
        xpMax = clamp(integer(json, "xpMax", 5000), 500, 100000);
        miteExtraPerSummons = clamp(integer(json, "miteExtraPerSummons", 3), 0, 50);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("raiseBaseHealth", raiseBaseHealth);
        o.addProperty("maxPlayers", maxPlayers);
        o.addProperty("countCreative", countCreative);
        o.addProperty("leaveGraceSeconds", leaveGraceSeconds);
        o.addProperty("islandRadius", islandRadius);
        o.addProperty("speedMultiplier", speedMultiplier);
        o.addProperty("speedPerPlayer", speedPerPlayer);
        o.addProperty("speedCap", speedCap);
        o.addProperty("crystalHealScaling", crystalHealScaling);
        o.addProperty("mitesEnabled", mitesEnabled);
        o.addProperty("waveIntervalSeconds", waveIntervalSeconds);
        o.addProperty("miteBase", miteBase);
        o.addProperty("miteExtraPerTwoPlayers", miteExtraPerTwoPlayers);
        o.addProperty("maxAliveMites", maxAliveMites);
        o.addProperty("animationEnabled", animationEnabled);
        o.addProperty("animationTicks", animationTicks);
        o.addProperty("maxQueued", maxQueued);
        o.addProperty("exposeSpawnEgg", exposeSpawnEgg);
        o.addProperty("summonScaling", summonScaling);
        o.addProperty("summonMaxMultiplier", summonMaxMultiplier);
        o.addProperty("summonSpeedCap", summonSpeedCap);
        o.addProperty("rewardEnabled", rewardEnabled);
        o.addProperty("rewardFromDragon", rewardFromDragon);
        o.addProperty("rewardItem", rewardItem);
        o.addProperty("rewardJewelFromDragon", rewardJewelFromDragon);
        o.addProperty("rewardJewelItem", rewardJewelItem);
        o.addProperty("tintFromDragon", tintFromDragon);
        o.addProperty("tintJewelFromDragon", tintJewelFromDragon);
        o.addProperty("summonAttackCap", summonAttackCap);
        o.addProperty("xpPerFight", xpPerFight);
        o.addProperty("xpMax", xpMax);
        o.addProperty("miteExtraPerSummons", miteExtraPerSummons);
        return o;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double clamp(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        try { return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback; } catch (Exception e) { return fallback; }
    }

    private static String text(JsonObject json, String key, String fallback) {
        try { return json.has(key) && json.get(key).isJsonPrimitive() && !json.get(key).getAsString().isBlank() ? json.get(key).getAsString().trim() : fallback; } catch (Exception e) { return fallback; }
    }

    private static int integer(JsonObject json, String key, int fallback) {
        try { return json.has(key) ? json.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; }
    }

    private static double number(JsonObject json, String key, double fallback) {
        try { return json.has(key) ? json.get(key).getAsDouble() : fallback; } catch (Exception e) { return fallback; }
    }
}
