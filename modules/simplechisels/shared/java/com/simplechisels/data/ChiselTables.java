package com.simplechisels.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplechisels.SimpleChisels;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The chisel transformation chains as datapack data, {@code data/<namespace>/chisel_transformations/<name>.json} (format
 * unchanged since SimpleBuilding, so its files and those of Simple End and Simple Blocks keep working):
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
 * A chain moves each block one step forward ({@code a -> b}) and one step back with sneaking ({@code b -> a}); a cyclic
 * chain also links the last block to the first. Each tier inherits every lower tier, and Constructor's Touch adds the
 * {@code touch} table on top of the {@code chisel} table. Files in the {@code simplechisels} namespace (the Vanilla chains)
 * are applied first, all others after them in id order, so an addition always wins over the defaults. Blocks that do not
 * exist (a mod that is not installed) are skipped with a warning, together with the links that touch them.
 *
 * <p>The server reads every file on each datapack load ({@link #reloadListener}) and sends them to each client
 * ({@link #syncFiles}); until then the built-in Vanilla chains of {@link BuiltInChains} apply.
 */
public final class ChiselTables {
    public static final String DIRECTORY = "chisel_transformations";
    /** In inheritance order: each tier contains all tiers before it. */
    public static final List<String> TIERS = List.of("stone", "iron", "diamond", "netherite", "enderite");
    public static final List<String> TABLES = List.of("chisel", "touch");
    /** Bound for all files together: a custom payload may have at most 1 MiB. */
    public static final int MAX_PACKET_BYTES = 900_000;

    /** One tier's effective maps: chisel forward/back, and with Constructor's Touch forward/back. */
    public record Tier(Map<Block, Block> forward, Map<Block, Block> backward,
                       Map<Block, Block> touchForward, Map<Block, Block> touchBackward) {
        static final Tier EMPTY = new Tier(Map.of(), Map.of(), Map.of(), Map.of());
    }

    /** One chain as built in or read from a file. */
    public record Chain(String tier, String table, boolean cyclic, List<Block> blocks) {
    }

    private static volatile @Nullable Map<String, Tier> builtInTiers;
    private static volatile @Nullable Map<String, Tier> loaded;
    private static volatile Map<String, String> lastFiles = Map.of();

    private ChiselTables() {
    }

    /** The effective maps the built-in chains alone give. */
    public static Map<String, Tier> builtInTiers() {
        Map<String, Tier> tiers = builtInTiers;
        if (tiers == null) {
            tiers = compose(BuiltInChains.all(), Map.of());
            builtInTiers = tiers;
        }
        return tiers;
    }

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

    /** Files of this mod first, then all others, each group in id order. */
    static List<Map.Entry<Identifier, JsonElement>> inApplyOrder(Map<Identifier, JsonElement> files) {
        List<Map.Entry<Identifier, JsonElement>> entries = new ArrayList<>(files.entrySet());
        entries.sort(Comparator.<Map.Entry<Identifier, JsonElement>, Boolean>comparing(
                        e -> !SimpleChisels.MOD_ID.equals(e.getKey().getNamespace()))
                .thenComparing(e -> e.getKey().toString()));
        return entries;
    }

    /** Replaces the effective tables with what {@code files} say (see the class comment for the order). */
    public static void apply(Map<Identifier, JsonElement> files) {
        List<Chain> chains = new ArrayList<>();
        Map<String, List<Block>> removals = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> file : inApplyOrder(files)) {
            try {
                read(file.getKey(), file.getValue().getAsJsonObject(), chains, removals);
            } catch (RuntimeException e) {
                SimpleChisels.LOG.warn("Skipping chisel transformation file {}: {}", file.getKey(), e.toString());
            }
        }
        loaded = compose(chains, removals);
    }

    /** Applies raw files (id -> JSON text): the server after a load, the client on sync. */
    public static void applyText(Map<String, String> files) {
        Map<Identifier, JsonElement> parsed = new HashMap<>();
        files.forEach((id, json) -> {
            Identifier key = Identifier.tryParse(id);
            if (key == null) return;
            try {
                parsed.put(key, JsonParser.parseString(json));
            } catch (RuntimeException e) {
                SimpleChisels.LOG.warn("Could not parse chisel transformation file {}: {}", id, e.toString());
            }
        });
        apply(parsed);
    }

    /** The server reload listener ({@code data/<namespace>/chisel_transformations/*.json}), registered by each loader. */
    public static SimplePreparableReloadListener<Map<String, String>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Map<String, String> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<String, String> files = new TreeMap<>();
                FileToIdConverter lister = FileToIdConverter.json(DIRECTORY);
                lister.listMatchingResources(manager).forEach((path, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        files.put(lister.fileToId(path).toString(), JsonParser.parseReader(reader).toString());
                    } catch (Exception e) {
                        SimpleChisels.LOG.warn("Could not read chisel transformation file {}: {}", path, e.toString());
                    }
                });
                return files;
            }

            @Override
            protected void apply(Map<String, String> files, ResourceManager manager, ProfilerFiller profiler) {
                lastFiles = Map.copyOf(files);
                applyText(files);
            }
        };
    }

    /** The files of the last server load (file id -> JSON text), trimmed to one packet, as the client sync sends them. */
    public static Map<String, String> syncFiles() {
        Map<String, String> files = new HashMap<>(lastFiles);
        while (bytes(files) > MAX_PACKET_BYTES && !files.isEmpty()) {
            String id = files.entrySet().stream().max(Comparator.comparingInt(e -> e.getValue().length())).get().getKey();
            SimpleChisels.LOG.warn("Chisel transformation file {} is too large for the sync packet and is not sent to clients", id);
            files.remove(id);
        }
        return files;
    }

    /** Applies the files of the last server load again (undoes a test's own {@link #apply}). */
    public static void reapplyLastLoad() {
        if (lastFiles.isEmpty()) resetToBuiltIn();
        else applyText(lastFiles);
    }

    /** Client side of the sync: the same tables the server uses (click prediction, hand hint). */
    public static void receive(Map<String, String> files) {
        if (!files.isEmpty()) applyText(files);
    }

    private static int bytes(Map<String, String> files) {
        int total = 0;
        for (var entry : files.entrySet()) {
            total += 6 + java.nio.charset.StandardCharsets.UTF_8.encode(entry.getKey()).remaining()
                    + java.nio.charset.StandardCharsets.UTF_8.encode(entry.getValue()).remaining();
        }
        return total;
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
            SimpleChisels.LOG.warn("{}: unknown block {}, links to it are skipped", file, id);
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(key);
    }

    /**
     * The effective maps of every tier from {@code chains} (applied in order, later links win) and {@code removals}
     * ({@code "tier/table" -> blocks}, applied after all chains).
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
}
