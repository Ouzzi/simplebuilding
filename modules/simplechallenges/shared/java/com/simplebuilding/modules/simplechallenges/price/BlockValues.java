package com.simplebuilding.modules.simplechallenges.price;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rarity value of a block (concept 5, {@code block_value.json}): block unlock prices a column by the sum of its blocks, so a
 * column with a diamond ore costs more than plain stone. The shipped table lives in
 * {@code data/simplechallenges/simplechallenges/block_value.json}; this class holds the parsed table and falls back to
 * the same few defaults when the file is missing.
 */
public final class BlockValues {
    private record Pattern(String contains, double value) {}

    public static final BlockValues DEFAULT = parse(null);
    private static volatile BlockValues current = DEFAULT;

    private final double fallback;
    private final Map<String, Double> values = new HashMap<>();
    private final List<Pattern> patterns = new ArrayList<>();
    /** How common a block is in the world and its crafting (Lucky Block odds, concept 5): 1 for everything not listed. */
    private final Map<String, Double> common = new HashMap<>();
    private final List<Pattern> commonPatterns = new ArrayList<>();

    private BlockValues(double fallback) {
        this.fallback = fallback;
    }

    public static BlockValues current() {
        return current;
    }

    public static void use(BlockValues v) {
        current = v == null ? DEFAULT : v;
    }

    /** Builds a table from the file's JSON ({@code default}, {@code values}, {@code patterns[{contains,value}]}); null gives the built-in minimum. */
    public static BlockValues parse(JsonObject json) {
        double fb = 0.05;
        BlockValues v;
        try {
            if (json != null && json.has("default")) fb = Math.max(0, json.get("default").getAsDouble());
            v = new BlockValues(fb);
            if (json != null && json.has("values") && json.get("values").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("values").entrySet()) v.values.put(e.getKey(), Math.max(0, e.getValue().getAsDouble()));
            }
            if (json != null && json.has("patterns") && json.get("patterns").isJsonArray()) {
                for (JsonElement p : json.getAsJsonArray("patterns")) {
                    JsonObject o = p.getAsJsonObject();
                    v.patterns.add(new Pattern(o.get("contains").getAsString(), Math.max(0, o.get("value").getAsDouble())));
                }
            }
            if (json != null && json.has("commonness") && json.get("commonness").isJsonObject()) {
                JsonObject c = json.getAsJsonObject("commonness");
                if (c.has("values") && c.get("values").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> e : c.getAsJsonObject("values").entrySet()) v.common.put(e.getKey(), Math.max(0, e.getValue().getAsDouble()));
                }
                if (c.has("patterns") && c.get("patterns").isJsonArray()) {
                    for (JsonElement p : c.getAsJsonArray("patterns")) {
                        JsonObject o = p.getAsJsonObject();
                        v.commonPatterns.add(new Pattern(o.get("contains").getAsString(), Math.max(0, o.get("value").getAsDouble())));
                    }
                }
            }
        } catch (RuntimeException e) {
            v = new BlockValues(0.05);
        }
        if (v.values.isEmpty() && v.patterns.isEmpty()) {
            v.values.put("minecraft:stone", 0.005);
            v.values.put("minecraft:diamond_ore", 30.0);
        }
        return v;
    }

    /** Value of one block (air and fluids are counted as 0 by the caller). Exact id first, then the longest matching pattern, else the default. */
    public double value(String blockId) {
        Double exact = values.get(blockId);
        if (exact != null) return exact;
        double best = fallback;
        int bestLen = -1;
        for (Pattern p : patterns) {
            if (blockId.contains(p.contains()) && p.contains().length() > bestLen) {
                best = p.value();
                bestLen = p.contains().length();
            }
        }
        return best;
    }

    /** Commonness of one block: exact id first, then the longest matching pattern, else 1. Lucky weight = commonness / (value + 1)^alpha. */
    public double commonness(String blockId) {
        Double exact = common.get(blockId);
        if (exact != null) return exact;
        double best = 1;
        int bestLen = -1;
        for (Pattern p : commonPatterns) {
            if (blockId.contains(p.contains()) && p.contains().length() > bestLen) {
                best = p.value();
                bestLen = p.contains().length();
            }
        }
        return best;
    }
}
