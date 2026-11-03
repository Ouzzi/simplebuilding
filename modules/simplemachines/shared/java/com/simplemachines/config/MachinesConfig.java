package com.simplemachines.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplemachines.SimpleMachines;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplemachines-server.json}: {@code features.doublePiston} and {@code features.signalDampener}
 * (both default on, A12: nothing of Vanilla changes, the parts are only added) and, since stage 2, the piston upgrades' options
 * {@code pistons.*} (until 03.11. SimpleBuilding's {@code breakerPistonsLoseDurability}, {@code pistonsBreachEndPortalFrames},
 * {@code pistonsBreachModdedUnbreakables}, {@code pistonsFireBreakEvents}; a file without the section takes them over once from
 * {@code simplebuilding.json}) and, since stage 3, the hopper speeds {@code hoppers.*} (how many times faster than a Vanilla hopper,
 * 1 to {@value #MAX_HOPPER_SPEED}; until 03.11. SimpleBuilding's {@code server.machines.*HopperSpeed}, taken over once the same way).
 * Missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class MachinesConfig {
    public static final String FILE = "simplemachines-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("features.doublePiston", "features.signalDampener",
            "pistons.breakerPistonsLoseDurability", "pistons.breachEndPortalFrames", "pistons.breachModdedUnbreakables",
            "pistons.fireBreakEvents", "hoppers.reinforcedSpeed", "hoppers.netheriteSpeed", "hoppers.enderiteSpeed");
    /** SimpleBuilding's config, read once for the piston options (stage 2). */
    public static final String LEGACY_FILE = "simplebuilding.json";

    public static boolean doublePiston = true;
    public static boolean signalDampener = true;
    /** Netherite (226) and Enderite (281) Pistons lose 1 durability per block they break; off: never. */
    public static boolean breakerPistonsLoseDurability = true;
    /** The piston upgrades treat End portal frames as breachable (owner 2026-09-26: off). */
    public static boolean breachEndPortalFrames = false;
    /** The piston upgrades also breach unbreakable blocks of other mods (audit #24: off). */
    public static boolean breachModdedUnbreakables = false;
    /** Before a breaker destroys a block it fires the loader's block break event with a fake player (claims; audit N4). */
    public static boolean fireBreakEvents = true;

    /** Stage 3: speed of a hopper tier against a Vanilla hopper (8 = one item every tick, the fastest). */
    public static final int MAX_HOPPER_SPEED = 8;
    public static final int REINFORCED_HOPPER_DEFAULT = 2, NETHERITE_HOPPER_DEFAULT = 4, ENDERITE_HOPPER_DEFAULT = 8;
    public static int reinforcedHopperSpeed = REINFORCED_HOPPER_DEFAULT;
    public static int netheriteHopperSpeed = NETHERITE_HOPPER_DEFAULT;
    public static int enderiteHopperSpeed = ENDERITE_HOPPER_DEFAULT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleMachines.LOG::warn);
        if (read != null) json = read;
        if (dir != null && !json.has("pistons")) {
            JsonObject taken = pistonsFromSimpleBuilding(dir.resolve(LEGACY_FILE));
            if (taken != null) {
                json.add("pistons", taken);
                SimpleMachines.LOG.info("Took over the piston options from {} into {} (old file unchanged)", LEGACY_FILE, FILE);
            }
        }
        if (dir != null && !json.has("hoppers")) {
            JsonObject taken = hoppersFromSimpleBuilding(dir.resolve(LEGACY_FILE));
            if (taken != null) {
                json.add("hoppers", taken);
                SimpleMachines.LOG.info("Took over the hopper speeds from {} into {} (old file unchanged)", LEGACY_FILE, FILE);
            }
        }
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleMachines.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement features = json.get("features");
        JsonObject f = features != null && features.isJsonObject() ? features.getAsJsonObject() : new JsonObject();
        doublePiston = bool(f, "doublePiston", true);
        signalDampener = bool(f, "signalDampener", true);
        JsonElement pistons = json.get("pistons");
        JsonObject p = pistons != null && pistons.isJsonObject() ? pistons.getAsJsonObject() : new JsonObject();
        breakerPistonsLoseDurability = bool(p, "breakerPistonsLoseDurability", true);
        breachEndPortalFrames = bool(p, "breachEndPortalFrames", false);
        breachModdedUnbreakables = bool(p, "breachModdedUnbreakables", false);
        fireBreakEvents = bool(p, "fireBreakEvents", true);
        JsonElement hoppers = json.get("hoppers");
        JsonObject h = hoppers != null && hoppers.isJsonObject() ? hoppers.getAsJsonObject() : new JsonObject();
        reinforcedHopperSpeed = speed(h, "reinforcedSpeed", REINFORCED_HOPPER_DEFAULT);
        netheriteHopperSpeed = speed(h, "netheriteSpeed", NETHERITE_HOPPER_DEFAULT);
        enderiteHopperSpeed = speed(h, "enderiteSpeed", ENDERITE_HOPPER_DEFAULT);
    }

    /** How many times faster than a Vanilla hopper a tier moves items (also the items per tick of its cart). */
    public static int hopperSpeed(com.simplemachines.hopper.HopperTier tier) {
        return switch (tier) {
            case REINFORCED -> reinforcedHopperSpeed;
            case NETHERITE -> netheriteHopperSpeed;
            case ENDERITE -> enderiteHopperSpeed;
        };
    }

    /** Ticks between two transfers of a tier: Vanilla's 8 divided by the speed (default 4/2/1), at least 1. */
    public static int hopperCooldown(com.simplemachines.hopper.HopperTier tier) {
        return Math.max(1, Math.round(8.0f / hopperSpeed(tier)));
    }

    /** The three hopper speeds out of SimpleBuilding's config ({@code server.machines.*HopperSpeed}), or null without file or values. */
    public static JsonObject hoppersFromSimpleBuilding(Path legacy) {
        if (legacy == null || !Files.isRegularFile(legacy)) return null;
        try {
            JsonElement parsed = com.google.gson.JsonParser.parseString(Files.readString(legacy));
            if (!parsed.isJsonObject()) return null;
            JsonElement server = parsed.getAsJsonObject().get("server");
            JsonElement machines = server != null && server.isJsonObject() ? server.getAsJsonObject().get("machines") : null;
            if (machines == null || !machines.isJsonObject()) return null;
            JsonObject taken = new JsonObject();
            for (String tier : new String[]{"reinforced", "netherite", "enderite"}) {
                JsonElement v = machines.getAsJsonObject().get(tier + "HopperSpeed");
                if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) taken.add(tier + "Speed", v);
            }
            return taken.isEmpty() ? null : taken;
        } catch (Exception e) {
            SimpleMachines.LOG.warn("Could not read the hopper speeds from {}: {}", LEGACY_FILE, e.toString());
            return null;
        }
    }

    /** The four piston options out of SimpleBuilding's config file (top-level keys there), or null without file or values. */
    public static JsonObject pistonsFromSimpleBuilding(Path legacy) {
        if (legacy == null || !Files.isRegularFile(legacy)) return null;
        try {
            JsonElement parsed = com.google.gson.JsonParser.parseString(Files.readString(legacy));
            if (!parsed.isJsonObject()) return null;
            JsonObject root = parsed.getAsJsonObject();
            JsonObject taken = new JsonObject();
            for (String[] key : new String[][]{{"breakerPistonsLoseDurability", "breakerPistonsLoseDurability"},
                    {"pistonsBreachEndPortalFrames", "breachEndPortalFrames"}, {"pistonsBreachModdedUnbreakables", "breachModdedUnbreakables"},
                    {"pistonsFireBreakEvents", "fireBreakEvents"}}) {
                JsonElement v = root.get(key[0]);
                if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) taken.add(key[1], v);
            }
            return taken.isEmpty() ? null : taken;
        } catch (Exception e) {
            SimpleMachines.LOG.warn("Could not read the piston options from {}: {}", LEGACY_FILE, e.toString());
            return null;
        }
    }

    public static JsonObject toJson() {
        JsonObject features = new JsonObject();
        features.addProperty("doublePiston", doublePiston);
        features.addProperty("signalDampener", signalDampener);
        JsonObject pistons = new JsonObject();
        pistons.addProperty("breakerPistonsLoseDurability", breakerPistonsLoseDurability);
        pistons.addProperty("breachEndPortalFrames", breachEndPortalFrames);
        pistons.addProperty("breachModdedUnbreakables", breachModdedUnbreakables);
        pistons.addProperty("fireBreakEvents", fireBreakEvents);
        JsonObject hoppers = new JsonObject();
        hoppers.addProperty("reinforcedSpeed", reinforcedHopperSpeed);
        hoppers.addProperty("netheriteSpeed", netheriteHopperSpeed);
        hoppers.addProperty("enderiteSpeed", enderiteHopperSpeed);
        JsonObject o = new JsonObject();
        o.add("features", features);
        o.add("pistons", pistons);
        o.add("hoppers", hoppers);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    private static int speed(JsonObject json, String key, int fallback) {
        JsonElement v = json.get(key);
        int value = fallback;
        try {
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) value = v.getAsInt();
        } catch (RuntimeException e) {
            value = fallback;
        }
        return Math.max(1, Math.min(MAX_HOPPER_SPEED, value));
    }

    static {
        reset();
    }

    private MachinesConfig() {}
}
