package com.simpledinos.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simpledinos.SimpleDinos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simpledinos-server.json} (server authority, every number clamped to a safe range); missing or
 * broken keys use the defaults and the file is rewritten complete. The defaults are the times of the concept (section 3).
 */
public final class DinosConfig {
    public static final String FILE = "simpledinos-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("enabled", "brushSeconds", "incubateSeconds", "hatchSeconds", "hatchWaitsForOwner",
            "growMinutes", "trexBiteDamage", "triceratopsShake", "shakeShare", "shakeAppleChance", "shakeTreeCooldownSeconds",
            "ramCooldownSeconds", "stegosaurusPlowWidth", "stegosaurusSow", "callRadius", "callSeconds", "callMaxAnimals",
            "callCooldownSeconds", "callIgnoreNamed", "raptorClimbSeconds", "raptorFeatherMinutes");

    public static final int BRUSH_DEFAULT = 45, BRUSH_MIN = 5, BRUSH_MAX = 600;
    public static final int INCUBATE_DEFAULT = 300, INCUBATE_MIN = 10, INCUBATE_MAX = 3600;
    public static final int HATCH_DEFAULT = 600, HATCH_MIN = 10, HATCH_MAX = 7200;
    public static final int GROW_DEFAULT = 40, GROW_MIN = 1, GROW_MAX = 600;
    public static final double BITE_DEFAULT = 10.0, BITE_MIN = 1.0, BITE_MAX = 40.0;
    public static final double SHARE_DEFAULT = 0.5, APPLE_DEFAULT = 0.35;
    public static final int TREE_REST_DEFAULT = 600, TREE_REST_MIN = 60, TREE_REST_MAX = 3600;
    public static final int RAM_COOLDOWN_DEFAULT = 3, RAM_COOLDOWN_MIN = 1, RAM_COOLDOWN_MAX = 30;
    public static final int PLOW_WIDTH_DEFAULT = 3, PLOW_WIDTH_MIN = 1, PLOW_WIDTH_MAX = 3;
    public static final int CALL_RADIUS_DEFAULT = 24, CALL_RADIUS_MIN = 8, CALL_RADIUS_MAX = 48;
    public static final int CALL_SECONDS_DEFAULT = 120, CALL_SECONDS_MIN = 30, CALL_SECONDS_MAX = 600;
    public static final int CALL_ANIMALS_DEFAULT = 24, CALL_ANIMALS_MIN = 1, CALL_ANIMALS_MAX = 64;
    public static final int CALL_COOLDOWN_DEFAULT = 20, CALL_COOLDOWN_MIN = 5, CALL_COOLDOWN_MAX = 300;
    public static final int CLIMB_DEFAULT = 5, CLIMB_MIN = 0, CLIMB_MAX = 10;
    public static final int FEATHER_DEFAULT = 10, FEATHER_MIN = 1, FEATHER_MAX = 60;

    public static boolean enabled = true;
    /** Seconds of brushing that free a fossil (45 = 75 % of the former minute, a pause costs twice its length). */
    public static int brushSeconds = BRUSH_DEFAULT;
    /** Seconds a player has to sit on the egg until it is brooded. */
    public static int incubateSeconds = INCUBATE_DEFAULT;
    /** Seconds from a brooded egg to the hatchling (10 minutes = half a Minecraft day). */
    public static int hatchSeconds = HATCH_DEFAULT;
    /**
     * A brooded egg hatches only when its owner (the longest sitter) is online, in the egg's dimension and within 16 blocks
     * (F12=B, owner 12.11.); off: the old rule, a stranger within 16 blocks gets the hatchling when the owner is offline.
     * Without a known owner the old rule applies either way.
     */
    public static boolean hatchWaitsForOwner = true;
    /** Minutes a hatchling needs to grow up (40 = two Minecraft days). */
    public static int growMinutes = GROW_DEFAULT;
    /** Bite damage of an adult T. rex (before difficulty). */
    public static double trexBiteDamage = BITE_DEFAULT;
    /** a-dinorest1: a Triceratops charge against a natural tree shakes it (fruit, saplings, sticks fall; the tree stays). */
    public static boolean triceratopsShake = true;
    /** Share of the tree's natural leaves whose Vanilla hand drops are rolled per shake (at most 40 rolls). */
    public static double shakeShare = SHARE_DEFAULT;
    /** Chance of the extra windfall roll (table simpledinos:shake/windfall, an apple) on oak and dark oak trees. */
    public static double shakeAppleChance = APPLE_DEFAULT;
    /** Seconds a shaken tree rests (it only knocks dully meanwhile). */
    public static int shakeTreeCooldownSeconds = TREE_REST_DEFAULT;
    /** Seconds between two Triceratops charges (shown in the jump bar). */
    public static int ramCooldownSeconds = RAM_COOLDOWN_DEFAULT;
    /** a-dinorest2: blocks the lowered Dino Plow tills side by side behind a Stegosaurus (1 to 3). */
    public static int stegosaurusPlowWidth = PLOW_WIDTH_DEFAULT;
    /** a-dinorest2: the lowered plow sows seeds from the rider's off hand on the empty fields of its strip. */
    public static boolean stegosaurusSow = true;
    /** a-dinorest3: blocks around a Parasaurolophus in which its crest call gathers farm animals. */
    public static int callRadius = CALL_RADIUS_DEFAULT;
    /** Seconds the called animals follow the Parasaurolophus. */
    public static int callSeconds = CALL_SECONDS_DEFAULT;
    /** Animals one call gathers at most (the nearest first). */
    public static int callMaxAnimals = CALL_ANIMALS_DEFAULT;
    /** Seconds between two calls (shown in the jump bar; the first 5 seconds a second press dismisses the herd). */
    public static int callCooldownSeconds = CALL_COOLDOWN_DEFAULT;
    /** Animals with a name tag ignore the call. */
    public static boolean callIgnoreNamed = true;
    /** b-utah48: seconds a ridden Utahraptor can climb a wall before it slides down (0 = it does not climb); 5 s = about 12 blocks (Vanilla climbs 2.35 blocks a second). */
    public static int raptorClimbSeconds = CLIMB_DEFAULT;
    /** b-utah48: minutes between two raptor feathers brushed from one Utahraptor. */
    public static int raptorFeatherMinutes = FEATHER_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static int brushTicks() {
        return brushSeconds * 20;
    }

    public static int incubateTicks() {
        return incubateSeconds * 20;
    }

    public static int hatchTicks() {
        return hatchSeconds * 20;
    }

    public static int growTicks() {
        return growMinutes * 1200;
    }

    public static int ramCooldownTicks() {
        return ramCooldownSeconds * 20;
    }

    public static int treeRestTicks() {
        return shakeTreeCooldownSeconds * 20;
    }

    public static int callTicks() {
        return callSeconds * 20;
    }

    public static int callCooldownTicks() {
        return callCooldownSeconds * 20;
    }

    public static int raptorClimbTicks() {
        return raptorClimbSeconds * 20;
    }

    public static int raptorFeatherTicks() {
        return raptorFeatherMinutes * 1200;
    }

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleDinos.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleDinos.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        enabled = bool(json, "enabled", true);
        brushSeconds = (int) clamp(number(json, "brushSeconds", BRUSH_DEFAULT), BRUSH_MIN, BRUSH_MAX);
        incubateSeconds = (int) clamp(number(json, "incubateSeconds", INCUBATE_DEFAULT), INCUBATE_MIN, INCUBATE_MAX);
        hatchSeconds = (int) clamp(number(json, "hatchSeconds", HATCH_DEFAULT), HATCH_MIN, HATCH_MAX);
        hatchWaitsForOwner = bool(json, "hatchWaitsForOwner", true);
        growMinutes = (int) clamp(number(json, "growMinutes", GROW_DEFAULT), GROW_MIN, GROW_MAX);
        trexBiteDamage = clamp(number(json, "trexBiteDamage", BITE_DEFAULT), BITE_MIN, BITE_MAX);
        triceratopsShake = bool(json, "triceratopsShake", true);
        shakeShare = clamp(number(json, "shakeShare", SHARE_DEFAULT), 0.0, 1.0);
        shakeAppleChance = clamp(number(json, "shakeAppleChance", APPLE_DEFAULT), 0.0, 1.0);
        shakeTreeCooldownSeconds = (int) clamp(number(json, "shakeTreeCooldownSeconds", TREE_REST_DEFAULT), TREE_REST_MIN, TREE_REST_MAX);
        ramCooldownSeconds = (int) clamp(number(json, "ramCooldownSeconds", RAM_COOLDOWN_DEFAULT), RAM_COOLDOWN_MIN, RAM_COOLDOWN_MAX);
        stegosaurusPlowWidth = (int) clamp(number(json, "stegosaurusPlowWidth", PLOW_WIDTH_DEFAULT), PLOW_WIDTH_MIN, PLOW_WIDTH_MAX);
        stegosaurusSow = bool(json, "stegosaurusSow", true);
        callRadius = (int) clamp(number(json, "callRadius", CALL_RADIUS_DEFAULT), CALL_RADIUS_MIN, CALL_RADIUS_MAX);
        callSeconds = (int) clamp(number(json, "callSeconds", CALL_SECONDS_DEFAULT), CALL_SECONDS_MIN, CALL_SECONDS_MAX);
        callMaxAnimals = (int) clamp(number(json, "callMaxAnimals", CALL_ANIMALS_DEFAULT), CALL_ANIMALS_MIN, CALL_ANIMALS_MAX);
        callCooldownSeconds = (int) clamp(number(json, "callCooldownSeconds", CALL_COOLDOWN_DEFAULT), CALL_COOLDOWN_MIN, CALL_COOLDOWN_MAX);
        callIgnoreNamed = bool(json, "callIgnoreNamed", true);
        raptorClimbSeconds = (int) clamp(number(json, "raptorClimbSeconds", CLIMB_DEFAULT), CLIMB_MIN, CLIMB_MAX);
        raptorFeatherMinutes = (int) clamp(number(json, "raptorFeatherMinutes", FEATHER_DEFAULT), FEATHER_MIN, FEATHER_MAX);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("brushSeconds", brushSeconds);
        o.addProperty("incubateSeconds", incubateSeconds);
        o.addProperty("hatchSeconds", hatchSeconds);
        o.addProperty("hatchWaitsForOwner", hatchWaitsForOwner);
        o.addProperty("growMinutes", growMinutes);
        o.addProperty("trexBiteDamage", trexBiteDamage);
        o.addProperty("triceratopsShake", triceratopsShake);
        o.addProperty("shakeShare", shakeShare);
        o.addProperty("shakeAppleChance", shakeAppleChance);
        o.addProperty("shakeTreeCooldownSeconds", shakeTreeCooldownSeconds);
        o.addProperty("ramCooldownSeconds", ramCooldownSeconds);
        o.addProperty("stegosaurusPlowWidth", stegosaurusPlowWidth);
        o.addProperty("stegosaurusSow", stegosaurusSow);
        o.addProperty("callRadius", callRadius);
        o.addProperty("callSeconds", callSeconds);
        o.addProperty("callMaxAnimals", callMaxAnimals);
        o.addProperty("callCooldownSeconds", callCooldownSeconds);
        o.addProperty("callIgnoreNamed", callIgnoreNamed);
        o.addProperty("raptorClimbSeconds", raptorClimbSeconds);
        o.addProperty("raptorFeatherMinutes", raptorFeatherMinutes);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
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

    private DinosConfig() {}
}
