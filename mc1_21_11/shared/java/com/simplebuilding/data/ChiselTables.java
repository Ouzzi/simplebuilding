package com.simplebuilding.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The chisel and spatula transformation chains as datapack data:
 * {@code data/<namespace>/chisel_transformations/<name>.json}.
 *
 * <pre>{@code
 * {
 *   "tier": "stone",          // stone | iron (copper, iron) | diamond (gold, diamond) | netherite | enderite
 *   "table": "chisel",        // chisel (always) | touch (only with Constructor's Touch)
 *   "chains": [
 *     { "blocks": ["minecraft:smooth_sandstone", "minecraft:cut_sandstone", "minecraft:sandstone"] },
 *     { "cyclic": true, "blocks": ["minecraft:netherrack", "minecraft:nether_bricks"] }
 *   ],
 *   "remove": ["minecraft:stone"]   // optional: drop what this tier/table says for these blocks
 * }
 * }</pre>
 *
 * A chain moves each block one step forward with the chisel ({@code a -> b}) and one step back with
 * the spatula or a sneaking chisel ({@code b -> a}); a cyclic chain also links the last block to
 * the first. Each tier inherits every lower tier, and Constructor's Touch adds the {@code touch}
 * table on top of the {@code chisel} table - the rule the chisel always followed, now applied to
 * whatever the files say.
 *
 * <p>The mod ships one file per tier and table ({@code stone_chisel.json}, {@code stone_touch.json},
 * ...), generated from the built-in chains that {@code ChiselItem} registers ({@link #record}); a
 * datapack replaces one by using the same path, or adds entries in files of its own. Files in the
 * {@code simplebuilding} namespace are applied first, all others after them in id order, so an
 * addition always wins over the defaults. Blocks that do not exist (a mod that is not installed)
 * are skipped with a warning, together with the links that touch them.
 *
 * <p>Until the first datapack load (and on a client that has not been sent the server's tables)
 * the built-in chains apply.
 */
public final class ChiselTables {

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding/chisel_transformations");

    public static final String DIRECTORY = "chisel_transformations";
    /** In inheritance order: each tier contains all tiers before it. */
    public static final List<String> TIERS = List.of("stone", "iron", "diamond", "netherite", "enderite");
    public static final List<String> TABLES = List.of("chisel", "touch");

    /** One tier's effective maps: chisel forward/back, and with Constructor's Touch forward/back. */
    public record Tier(Map<Block, Block> forward, Map<Block, Block> backward,
                       Map<Block, Block> touchForward, Map<Block, Block> touchBackward) {
        static final Tier EMPTY = new Tier(Map.of(), Map.of(), Map.of(), Map.of());
    }

    /** One chain as registered by {@code ChiselItem} or read from a file. */
    public record Chain(String tier, String table, boolean cyclic, List<Block> blocks) {
    }

    private static final List<Chain> BUILT_IN = new ArrayList<>();
    private static volatile @Nullable Map<String, Tier> builtInTiers;
    private static volatile @Nullable Map<String, Tier> loaded;

    private ChiselTables() {
    }

    // =====================================================================================
    // Built-in defaults
    // =====================================================================================

    /** Called by {@code ChiselItem}'s static tables for every chain, in registration order. */
    public static synchronized void record(String tier, String table, boolean cyclic, Block... blocks) {
        BUILT_IN.add(new Chain(tier, table, cyclic, List.of(blocks)));
        builtInTiers = null;
    }

    /** The built-in chains in registration order (forces {@code ChiselItem}'s tables to load). */
    public static synchronized List<Chain> builtInChains() {
        com.simplebuilding.items.custom.ChiselItem.ensureTablesLoaded();
        return List.copyOf(BUILT_IN);
    }

    /** The effective maps the built-in chains alone give. */
    public static Map<String, Tier> builtInTiers() {
        Map<String, Tier> tiers = builtInTiers;
        if (tiers == null) {
            tiers = compose(builtInChains(), Map.of());
            builtInTiers = tiers;
        }
        return tiers;
    }

    // =====================================================================================
    // Effective tables
    // =====================================================================================

    /** The effective maps of {@code tier} (one of {@link #TIERS}); empty for an unknown tier. */
    public static Tier tier(String tier) {
        Map<String, Tier> tiers = loaded;
        if (tiers == null) {
            tiers = builtInTiers();
        }
        return tiers.getOrDefault(tier, Tier.EMPTY);
    }

    /** Whether datapack files are in effect (false: the built-in chains). */
    public static boolean isLoaded() {
        return loaded != null;
    }

    /** Back to the built-in chains. */
    public static void resetToBuiltIn() {
        loaded = null;
    }

    /** Replaces the effective tables with what {@code files} say (see the class comment for the order). */
    public static void apply(Map<Identifier, JsonElement> files) {
        List<Chain> chains = new ArrayList<>();
        Map<String, List<Block>> removals = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> file : ModDataTables.inApplyOrder(files)) {
            try {
                read(file.getKey(), file.getValue().getAsJsonObject(), chains, removals);
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping chisel transformation file {}: {}", file.getKey(), e.toString());
            }
        }
        loaded = compose(chains, removals);
    }

    private static void read(Identifier id, JsonObject json, List<Chain> chains, Map<String, List<Block>> removals) {
        String tier = json.get("tier").getAsString();
        String table = json.has("table") ? json.get("table").getAsString() : "chisel";
        if (!TIERS.contains(tier) || !TABLES.contains(table)) {
            throw new IllegalArgumentException("unknown tier/table " + tier + "/" + table);
        }
        if (json.has("chains")) {
            for (JsonElement element : json.getAsJsonArray("chains")) {
                JsonObject chain = element.getAsJsonObject();
                boolean cyclic = chain.has("cyclic") && chain.get("cyclic").getAsBoolean();
                List<Block> blocks = new ArrayList<>();
                for (JsonElement block : chain.getAsJsonArray("blocks")) {
                    blocks.add(block(id, block.getAsString()));
                }
                chains.add(new Chain(tier, table, cyclic, blocks));
            }
        }
        if (json.has("remove")) {
            List<Block> removed = removals.computeIfAbsent(tier + "/" + table, key -> new ArrayList<>());
            for (JsonElement block : json.getAsJsonArray("remove")) {
                Block resolved = block(id, block.getAsString());
                if (resolved != null) {
                    removed.add(resolved);
                }
            }
        }
    }

    private static @Nullable Block block(Identifier file, String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
            LOGGER.warn("{}: unknown block {}, links to it are skipped", file, id);
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(key);
    }

    /**
     * The effective maps of every tier from {@code chains} (applied in order, later links win) and
     * {@code removals} ({@code "tier/table" -> blocks}, applied after all chains).
     */
    public static Map<String, Tier> compose(List<Chain> chains, Map<String, List<Block>> removals) {
        Map<String, Map<Block, Block>> forward = new HashMap<>();
        Map<String, Map<Block, Block>> backward = new HashMap<>();
        for (Chain chain : chains) {
            String key = chain.tier() + "/" + chain.table();
            Map<Block, Block> fwd = forward.computeIfAbsent(key, k -> new HashMap<>());
            Map<Block, Block> bwd = backward.computeIfAbsent(key, k -> new HashMap<>());
            List<Block> blocks = chain.blocks();
            if (blocks.size() < 2) {
                continue;
            }
            int links = chain.cyclic() ? blocks.size() : blocks.size() - 1;
            for (int i = 0; i < links; i++) {
                Block current = blocks.get(i);
                Block next = blocks.get((i + 1) % blocks.size());
                if (current == null || next == null) {
                    continue;
                }
                fwd.put(current, next);
                bwd.put(next, current);
            }
        }
        for (Map.Entry<String, List<Block>> removal : removals.entrySet()) {
            for (Block block : removal.getValue()) {
                forward.getOrDefault(removal.getKey(), new HashMap<>()).remove(block);
                backward.getOrDefault(removal.getKey(), new HashMap<>()).remove(block);
            }
        }
        Map<String, Tier> result = new LinkedHashMap<>();
        Map<Block, Block> fwd = Map.of();
        Map<Block, Block> bwd = Map.of();
        Map<Block, Block> touchFwd = Map.of();
        Map<Block, Block> touchBwd = Map.of();
        for (String tier : TIERS) {
            Map<Block, Block> chiselFwd = forward.getOrDefault(tier + "/chisel", Map.of());
            Map<Block, Block> chiselBwd = backward.getOrDefault(tier + "/chisel", Map.of());
            Map<Block, Block> ownTouchFwd = forward.getOrDefault(tier + "/touch", Map.of());
            Map<Block, Block> ownTouchBwd = backward.getOrDefault(tier + "/touch", Map.of());
            fwd = merge(fwd, chiselFwd);
            bwd = merge(bwd, chiselBwd);
            touchFwd = merge(touchFwd, merge(chiselFwd, ownTouchFwd));
            touchBwd = merge(touchBwd, merge(chiselBwd, ownTouchBwd));
            result.put(tier, new Tier(fwd, bwd, touchFwd, touchBwd));
        }
        return Map.copyOf(result);
    }

    private static Map<Block, Block> merge(Map<Block, Block> base, Map<Block, Block> addition) {
        Map<Block, Block> result = new HashMap<>(base);
        result.putAll(addition);
        return Map.copyOf(result);
    }

    // =====================================================================================
    // Default files (datagen)
    // =====================================================================================

    /** The default file for {@code tier}/{@code table}: the built-in chains of that pair, in order. */
    public static JsonObject defaultFile(String tier, String table) {
        JsonObject json = new JsonObject();
        json.addProperty("tier", tier);
        json.addProperty("table", table);
        JsonArray chains = new JsonArray();
        for (Chain chain : builtInChains()) {
            if (!chain.tier().equals(tier) || !chain.table().equals(table)) {
                continue;
            }
            JsonObject entry = new JsonObject();
            if (chain.cyclic()) {
                entry.addProperty("cyclic", true);
            }
            JsonArray blocks = new JsonArray();
            for (Block block : chain.blocks()) {
                blocks.add(BuiltInRegistries.BLOCK.getKey(block).toString());
            }
            entry.add("blocks", blocks);
            chains.add(entry);
        }
        json.add("chains", chains);
        return json;
    }
}
