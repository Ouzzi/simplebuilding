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
 * {@code simplebuilding.json}). Missing or broken keys use the defaults and the file is rewritten complete.
 */
public final class MachinesConfig {
    public static final String FILE = "simplemachines-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("features.doublePiston", "features.signalDampener",
            "pistons.breakerPistonsLoseDurability", "pistons.breachEndPortalFrames", "pistons.breachModdedUnbreakables",
            "pistons.fireBreakEvents");
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
        JsonObject o = new JsonObject();
        o.add("features", features);
        o.add("pistons", pistons);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement v = json.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : fallback;
    }

    static {
        reset();
    }

    private MachinesConfig() {}
}
