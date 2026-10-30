package com.simplebuilding.modules.simplechallenges.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads and validates a preset file. Problems come back as readable messages (shown in the selection screen later), never as
 * an exception. Schema (version 1): {@code name}, {@code icon}, {@code family}, {@code modes[]}; the mode {@code unlock} has
 * {@code cell}, {@code start} (rectangles {@code [x0,z0,x1,z1]} in cell units), {@code currency}, {@code difficulty},
 * {@code dimensions}, {@code pityMinutes}, {@code credits} {task, goal, challenge}.
 */
public final class PresetParser {
    public static final int SCHEMA = 1;
    /** Modes the concept lists that later stages build: accepted with a warning, not runnable yet. */
    private static final Set<String> PLANNED = Set.of("skyblock", "oneblock", "lucky", "skyline", "line", "randomdrops", "skygrid");

    public record Result(Preset preset, List<String> errors, List<String> warnings) {
        public boolean ok() {
            return preset != null && errors.isEmpty();
        }
    }

    private PresetParser() {}

    public static Result parse(String id, String text) {
        List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement e = JsonParser.parseString(text);
            if (!e.isJsonObject()) return fail(id, "preset is not a JSON object");
            root = e.getAsJsonObject();
        } catch (RuntimeException ex) {
            return fail(id, "invalid JSON: " + ex.getMessage());
        }
        if (id == null || !id.matches("[a-z0-9_/.-]{1,64}")) errors.add("invalid preset id '" + id + "'");
        int schema = intOf(root, "schemaVersion", -1);
        if (schema != SCHEMA) errors.add("schemaVersion must be " + SCHEMA + ", found " + schema);
        String name = str(root, "name", id);
        if (name.isBlank() || name.length() > 64) errors.add("name must be 1-64 characters");
        String icon = str(root, "icon", "minecraft:grass_block");
        if (!icon.contains(":")) errors.add("icon must be an item id like minecraft:grass_block");
        String family = str(root, "family", "unlock");
        UnlockMode unlock = null;
        JsonArray modes = root.has("modes") && root.get("modes").isJsonArray() ? root.getAsJsonArray("modes") : null;
        if (modes == null || modes.isEmpty()) {
            errors.add("modes must be a non-empty array");
        } else {
            int unlockModes = 0;
            for (JsonElement m : modes) {
                if (!m.isJsonObject()) {
                    errors.add("a mode must be an object");
                    continue;
                }
                JsonObject mode = m.getAsJsonObject();
                String type = str(mode, "type", "");
                if (type.equals("unlock")) {
                    // chunk unlock and block unlock are one setting of the same mode (concept 2): a second unlock mode is refused
                    if (++unlockModes > 1) {
                        errors.add("only one unlock mode per preset (chunk and block unlock are the same mode with another cell size)");
                        continue;
                    }
                    unlock = parseUnlock(mode, errors);
                } else if (PLANNED.contains(type)) {
                    warnings.add("mode '" + type + "' is not available in this version");
                } else {
                    errors.add("unknown mode type '" + type + "'");
                }
            }
        }
        if (!errors.isEmpty()) return new Result(null, errors, warnings);
        return new Result(new Preset(id, name, icon, family, unlock, List.copyOf(warnings), text), errors, warnings);
    }

    private static UnlockMode parseUnlock(JsonObject o, List<String> errors) {
        int cell = intOf(o, "cell", 16);
        if (!UnlockGrid.validCell(cell)) errors.add("cell must be one of 1, 2, 4, 8, 16, 32, 48, found " + cell);
        List<int[]> start = new ArrayList<>();
        long cells = 0;
        if (o.has("start") && o.get("start").isJsonArray()) {
            for (JsonElement r : o.getAsJsonArray("start")) {
                int[] rect = rect(r);
                if (rect == null) {
                    errors.add("start rectangles must be [x0, z0, x1, z1] with whole numbers");
                    continue;
                }
                cells += (long) (Math.abs(rect[2] - rect[0]) + 1) * (Math.abs(rect[3] - rect[1]) + 1);
                start.add(rect);
            }
        }
        if (start.isEmpty()) start.add(new int[]{0, 0, 0, 0});
        // the origin cell is always part of the start area (spawn is fixed at 0,0)
        if (cells > UnlockMode.MAX_START_CELLS) errors.add("start area has " + cells + " cells, at most " + UnlockMode.MAX_START_CELLS);
        Currency currency = Currency.parse(str(o, "currency", "items"));
        if (currency == null) errors.add("currency must be items, credits or xp");
        Difficulty difficulty = Difficulty.parse(str(o, "difficulty", "medium"));
        if (difficulty == null) errors.add("difficulty must be easy, medium or hard");
        List<String> dims = new ArrayList<>();
        if (o.has("dimensions") && o.get("dimensions").isJsonArray()) {
            for (JsonElement d : o.getAsJsonArray("dimensions")) {
                String s = d.isJsonPrimitive() ? d.getAsString() : "";
                if (!s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) errors.add("invalid dimension id '" + s + "'");
                else dims.add(s);
            }
        }
        if (dims.isEmpty()) dims.add("minecraft:overworld");
        int pity = intOf(o, "pityMinutes", 30);
        if (pity < 1 || pity > 1440) errors.add("pityMinutes must be 1-1440");
        JsonObject credits = o.has("credits") && o.get("credits").isJsonObject() ? o.getAsJsonObject("credits") : new JsonObject();
        int task = intOf(credits, "task", 0), goal = intOf(credits, "goal", 0), challenge = intOf(credits, "challenge", 0);
        if (task < 0 || goal < 0 || challenge < 0 || task > 100 || goal > 100 || challenge > 100) errors.add("credits per frame must be 0-100");
        if (currency == Currency.CREDITS && task + goal + challenge == 0) errors.add("currency credits needs credits per advancement (credits.task/goal/challenge)");
        if (!errors.isEmpty()) return null;
        return new UnlockMode(cell, List.copyOf(start), currency, difficulty, List.copyOf(dims), pity, task, goal, challenge);
    }

    private static int[] rect(JsonElement e) {
        if (!e.isJsonArray() || e.getAsJsonArray().size() != 4) return null;
        int[] r = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                double d = e.getAsJsonArray().get(i).getAsDouble();
                if (d != Math.rint(d) || Math.abs(d) > 1_000_000) return null;
                r[i] = (int) d;
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return r;
    }

    private static Result fail(String id, String message) {
        return new Result(null, List.of(message), List.of());
    }

    private static String str(JsonObject o, String key, String def) {
        try {
            return o.has(key) ? o.get(key).getAsString() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static int intOf(JsonObject o, String key, int def) {
        try {
            return o.has(key) ? o.get(key).getAsInt() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }
}
