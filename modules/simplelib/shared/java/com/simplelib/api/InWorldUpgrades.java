package com.simplelib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The data type {@code simplelib:in_world_upgrades} (plan PLAN-MODULAUFTEILUNG §1.4/§2.6): upgrading a block in the
 * world into the next tier with a tool in the main hand and the material in the off hand. Every mod ships its entries
 * as {@code data/<namespace>/in_world_upgrades/<name>.json}:
 *
 * <pre>{@code
 * {
 *   "upgrades": [
 *     {
 *       "from": "mymod:reinforced_oven",
 *       "to": "mymod:netherite_oven",
 *       "material": "minecraft:netherite_ingot",  // off hand, used up on the last strike
 *       "material_count": 1,                       // optional, per block (default 1)
 *       "duration_factor": 1,                      // optional, times the strikes (default 1)
 *       "tool": "#simplelib:upgrade_tools",        // optional item tag of the tool (this is the default)
 *       "min_tool": "diamond",                     // optional, the tool owner's grade name (old name: min_hammer)
 *       "damage_per_hit": 4,                       // optional tool durability per strike (default 1)
 *       "tier": "netherite"                        // optional free word (advancements, file grouping)
 *     }
 *   ],
 *   "remove": ["mymod:reinforced_piston"]          // optional: no upgrade from these blocks
 * }
 * }</pre>
 *
 * Files of the namespaces given to {@link #registerDefaults} apply first, then all others, each group in id order;
 * a later entry for the same {@code from} block wins and {@code remove} takes entries out. Unknown blocks or items skip
 * the entry, a broken file is skipped from the broken point on.
 *
 * <p>The tool: {@code #simplelib:upgrade_tools} holds {@code #simplelib:hammers}, which a hammer mod fills; while the
 * Vanilla axe ways are on ({@link SimpleLibApi#axeWaysEnabled}) any axe also counts (principle 5a). Moving the block
 * entity contents is up to the handler registered for the target block ({@link #registerHandler}); without one the
 * block keeps its properties and its block entity.
 *
 * <p>Loading: whoever reads the datapack (today SimpleBuilding's data tables, which also sync them to clients) calls
 * {@link #apply}; the table here is then the effective one on that side.
 */
public final class InWorldUpgrades {
    public static final String DIRECTORY = "in_world_upgrades";
    /** Tools that run in-world upgrades: {@code #simplelib:hammers} (plus axes while the axe ways are on). */
    public static final TagKey<Item> UPGRADE_TOOLS = TagKey.create(Registries.ITEM, SimpleLib.id("upgrade_tools"));
    /** Hammers of any mod (empty in SimpleLib; a hammer mod adds its own). */
    public static final TagKey<Item> HAMMERS = TagKey.create(Registries.ITEM, SimpleLib.id("hammers"));

    /** One upgrade step as the files say it. {@code minTool} and {@code tier} mean what the tool's owner defines. */
    public record Entry(Block from, Block to, Item material, int materialCount, int durationFactor, TagKey<Item> tool,
                        String minTool, int damagePerHit, String tier) {
        public Entry {
            materialCount = Math.max(1, materialCount);
            durationFactor = Math.max(1, durationFactor);
            damagePerHit = Math.max(0, damagePerHit);
        }
    }

    /** Moves a block to its next tier with everything it holds; returns how many blocks changed (a double chest: 2). */
    @FunctionalInterface
    public interface Handler {
        int replace(ServerLevel level, BlockPos pos, Block to);
    }

    private record Registered(Predicate<Block> targets, Handler handler) {}

    private static final Set<String> DEFAULTS = new CopyOnWriteArraySet<>();
    private static final List<Registered> HANDLERS = new CopyOnWriteArrayList<>();
    private static volatile @Nullable Map<Block, Entry> loaded;

    /** Files of {@code namespace} apply before all others (a mod's own shipped defaults). */
    public static void registerDefaults(String namespace) {
        DEFAULTS.add(namespace);
    }

    /** The handler for upgrades whose target block matches {@code targets}; later registrations are asked first. */
    public static void registerHandler(Predicate<Block> targets, Handler handler) {
        HANDLERS.add(0, new Registered(targets, handler));
    }

    /** As {@link #registerHandler(Predicate, Handler)}, for the blocks of a block tag. */
    public static void registerHandler(TagKey<Block> targets, Handler handler) {
        registerHandler(block -> block.defaultBlockState().is(targets), handler);
    }

    /**
     * Turns {@code from} into {@code to} at {@code pos}: the registered handler moves the contents, otherwise the
     * block keeps its properties and its block entity. Returns how many blocks changed.
     */
    public static int replace(ServerLevel level, BlockPos pos, Block to) {
        for (Registered registered : HANDLERS) {
            if (registered.targets().test(to)) return registered.handler().replace(level, pos, to);
        }
        BlockState old = level.getBlockState(pos);
        level.setBlock(pos, to.withPropertiesOf(old), Block.UPDATE_ALL);
        return 1;
    }

    /** Whether a handler is registered for upgrades into {@code to}. */
    public static boolean hasHandler(Block to) {
        for (Registered registered : HANDLERS) {
            if (registered.targets().test(to)) return true;
        }
        return false;
    }

    /** Replaces the effective table with what {@code files} (file id -> JSON) say. */
    public static void apply(Map<Identifier, JsonElement> files) {
        loaded = parse(files);
    }

    /** The table {@code files} describe, without touching the effective one. */
    public static Map<Block, Entry> parse(Map<Identifier, JsonElement> files) {
        Map<Block, Entry> table = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> file : inApplyOrder(files)) {
            try {
                JsonObject json = file.getValue().getAsJsonObject();
                if (json.has("upgrades")) {
                    for (JsonElement element : json.getAsJsonArray("upgrades")) {
                        Entry entry = entry(file.getKey(), element.getAsJsonObject());
                        if (entry != null) table.put(entry.from(), entry);
                    }
                }
                if (json.has("remove")) {
                    for (JsonElement element : json.getAsJsonArray("remove")) {
                        Block block = block(file.getKey(), element.getAsString());
                        if (block != null) table.remove(block);
                    }
                }
            } catch (RuntimeException e) {
                SimpleLib.LOG.warn("Skipping in-world upgrade file {}: {}", file.getKey(), e.toString());
            }
        }
        return Map.copyOf(table);
    }

    /** The effective table (empty before the first {@link #apply}). */
    public static Map<Block, Entry> table() {
        Map<Block, Entry> table = loaded;
        return table != null ? table : Map.of();
    }

    public static boolean isLoaded() {
        return loaded != null;
    }

    /** The upgrade that starts at {@code from}, or null. */
    public static @Nullable Entry entryFor(Block from) {
        return table().get(from);
    }

    /** Whether {@code tool} may run {@code entry}: in its tool tag; for the default tag also an axe while the axe ways are on. */
    public static boolean toolFits(Entry entry, ItemStack tool) {
        if (tool.is(entry.tool())) return true;
        return entry.tool().equals(UPGRADE_TOOLS) && isUpgradeTool(tool);
    }

    /** {@code #simplelib:upgrade_tools}, or an axe while the Vanilla axe ways are on (principle 5a). */
    public static boolean isUpgradeTool(ItemStack tool) {
        return tool.is(UPGRADE_TOOLS) || (SimpleLibApi.axeWaysEnabled() && tool.is(ItemTags.AXES));
    }

    private static List<Map.Entry<Identifier, JsonElement>> inApplyOrder(Map<Identifier, JsonElement> files) {
        List<Map.Entry<Identifier, JsonElement>> entries = new ArrayList<>(files.entrySet());
        entries.sort(Comparator.<Map.Entry<Identifier, JsonElement>, Boolean>comparing(
                        e -> !DEFAULTS.contains(e.getKey().getNamespace()))
                .thenComparing(e -> e.getKey().toString()));
        return entries;
    }

    private static @Nullable Entry entry(Identifier file, JsonObject json) {
        Block from = block(file, json.get("from").getAsString());
        Block to = block(file, json.get("to").getAsString());
        Identifier materialId = Identifier.tryParse(json.get("material").getAsString());
        if (from == null || to == null || materialId == null || !BuiltInRegistries.ITEM.containsKey(materialId)) {
            return null;
        }
        TagKey<Item> tool = UPGRADE_TOOLS;
        if (json.has("tool")) {
            String text = json.get("tool").getAsString();
            Identifier tag = Identifier.tryParse(text.startsWith("#") ? text.substring(1) : text);
            if (tag == null) throw new IllegalArgumentException("bad tool tag " + text);
            tool = TagKey.create(Registries.ITEM, tag);
        }
        String minTool = json.has("min_tool") ? json.get("min_tool").getAsString()
                : json.has("min_hammer") ? json.get("min_hammer").getAsString() : "any";
        return new Entry(from, to, BuiltInRegistries.ITEM.getValue(materialId),
                json.has("material_count") ? json.get("material_count").getAsInt() : 1,
                json.has("duration_factor") ? json.get("duration_factor").getAsInt() : 1,
                tool, minTool,
                json.has("damage_per_hit") ? json.get("damage_per_hit").getAsInt() : 1,
                json.has("tier") ? json.get("tier").getAsString() : "");
    }

    private static @Nullable Block block(Identifier file, String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
            SimpleLib.LOG.warn("{}: unknown block {}, in-world upgrade entry skipped", file, id);
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(key);
    }

    private InWorldUpgrades() {}
}
