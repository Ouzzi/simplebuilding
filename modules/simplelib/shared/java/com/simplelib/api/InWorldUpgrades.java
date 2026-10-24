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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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
 * <p>Loading: SimpleLib reads the files itself on every datapack load ({@link #reloadListener}, registered by each
 * loader) and sends them to every client on join and {@code /reload} ({@link #syncFiles}, applied there with
 * {@link #receive}). A partner that reads them with its own rules (SimpleBuilding: also its legacy folder
 * {@code sledgehammer_upgrades}, its own client sync) calls {@link #loadedByPartner} in its constructor; SimpleLib then
 * neither loads nor syncs, and the partner calls {@link #apply}.
 *
 * <p>The Vanilla axe way (principle 5a): while the axe ways are on (no hammer mod), {@link #axeWants} /
 * {@link #axeStrike} run an entry with an axe in the main hand - {@link #AXE_STRIKES} strikes with
 * {@link InWorldStrikes}' cracks and growing preview, the material from the off hand on the last one.
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

    /** Strikes of one axe-way upgrade (times the entry's duration factor), like the hammer's default. */
    public static final int AXE_STRIKES = 5;
    /** {@link InWorldStrikes} kind of the axe way. */
    public static final String AXE_KIND = "simplelib:in_world_upgrade";

    private static final Set<String> DEFAULTS = new CopyOnWriteArraySet<>();
    private static volatile boolean partnerLoads;
    /** The files of the last server load (file id -> JSON text), as the client sync sends them. */
    private static volatile Map<String, String> lastFiles = Map.of();
    private static final List<Registered> HANDLERS = new CopyOnWriteArrayList<>();
    private static volatile @Nullable Map<Block, Entry> loaded;
    private static final List<Runnable> APPLY_LISTENERS = new CopyOnWriteArrayList<>();

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

    /** A partner loads and syncs the files itself (see the class comment); SimpleLib's own listener and sync stay idle. */
    public static void loadedByPartner() {
        partnerLoads = true;
    }

    public static boolean isLoadedByPartner() {
        return partnerLoads;
    }

    /** SimpleLib's own server reload listener ({@code data/<namespace>/in_world_upgrades/*.json}); idle under a partner. */
    public static SimplePreparableReloadListener<Map<String, String>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Map<String, String> prepare(ResourceManager manager, ProfilerFiller profiler) {
                if (partnerLoads) return Map.of();
                Map<String, String> files = new java.util.TreeMap<>();
                net.minecraft.resources.FileToIdConverter lister = net.minecraft.resources.FileToIdConverter.json(DIRECTORY);
                lister.listMatchingResources(manager).forEach((path, resource) -> {
                    try (java.io.Reader reader = resource.openAsReader()) {
                        files.put(lister.fileToId(path).toString(), com.google.gson.JsonParser.parseReader(reader).toString());
                    } catch (Exception e) {
                        SimpleLib.LOG.warn("Could not read in-world upgrade file {}: {}", path, e.toString());
                    }
                });
                return files;
            }

            @Override
            protected void apply(Map<String, String> files, ResourceManager manager, ProfilerFiller profiler) {
                if (partnerLoads) return;
                lastFiles = Map.copyOf(files);
                applyText(files);
            }
        };
    }

    /** The files to send to a client after a datapack (re)load; empty while a partner syncs them itself. */
    public static Map<String, String> syncFiles() {
        return partnerLoads ? Map.of() : lastFiles;
    }

    /** Client side of the sync: the same table the server uses (prediction, displays). */
    public static void receive(Map<String, String> files) {
        if (partnerLoads || files.isEmpty()) return;
        applyText(files);
    }

    private static void applyText(Map<String, String> files) {
        Map<Identifier, JsonElement> parsed = new java.util.HashMap<>();
        files.forEach((id, json) -> {
            Identifier key = Identifier.tryParse(id);
            if (key == null) return;
            try {
                parsed.put(key, com.google.gson.JsonParser.parseString(json));
            } catch (RuntimeException e) {
                SimpleLib.LOG.warn("Could not parse in-world upgrade file {}: {}", id, e.toString());
            }
        });
        apply(parsed);
    }

    /**
     * Whether a click takes the Vanilla axe way of an upgrade entry: main hand, the axe ways on, an axe that fits the
     * entry, the entry's material in the off hand.
     */
    public static boolean axeWants(BlockState state, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SimpleLibApi.axeWaysEnabled()) return false;
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(ItemTags.AXES)) return false;
        Entry entry = entryFor(state.getBlock());
        return entry != null && toolFits(entry, tool) && axeGradeFits(entry.minTool(), tool) && player.getOffhandItem().is(entry.material());
    }

    /**
     * The entry's {@code min_tool} read for axes (the hammer's grade names): {@code "diamond"} wants a diamond or
     * netherite axe, {@code "netherite"} a netherite axe; any other name takes every axe.
     */
    public static boolean axeGradeFits(String minTool, ItemStack axe) {
        return switch (minTool) {
            case "diamond" -> axe.is(net.minecraft.world.item.Items.DIAMOND_AXE) || axe.is(net.minecraft.world.item.Items.NETHERITE_AXE);
            case "netherite" -> axe.is(net.minecraft.world.item.Items.NETHERITE_AXE);
            default -> true;
        };
    }

    /**
     * One strike of the axe way at {@code pos} (call after {@link #axeWants}): strikes too close together do not
     * count, every counted one wears the axe by the entry's {@code damage_per_hit}, cracks the block and grows the
     * preview of the next tier; the last uses up the material and replaces the block ({@link #replace}). Returns
     * false when the strike was refused (no entry, no right to build, not enough material).
     */
    public static boolean axeStrike(ServerLevel level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        Entry entry = entryFor(state.getBlock());
        if (entry == null || !player.mayBuild() || !level.mayInteract(player, pos)) return false;
        ItemStack material = player.getOffhandItem();
        boolean pays = !player.hasInfiniteMaterials();
        if (pays && (!material.is(entry.material()) || material.getCount() < entry.materialCount())) return false;
        int total = AXE_STRIKES * entry.durationFactor();
        int done = InWorldStrikes.count(level, pos, AXE_KIND, state, total, level.getGameTime());
        if (done == 0) return true;
        InWorldStrikes.feedback(level, pos, state, done, total);
        InWorldStrikes.preview(level, pos, entry.to().withPropertiesOf(state), done, total);
        if (pays && entry.damagePerHit() > 0) player.getMainHandItem().hurtAndBreak(entry.damagePerHit(), player, EquipmentSlot.MAINHAND);
        if (done >= total) {
            InWorldStrikes.clearPreview(level, pos);
            if (pays) material.shrink(entry.materialCount());
            replace(level, pos, entry.to());
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    /** Replaces the effective table with what {@code files} (file id -> JSON) say. */
    public static void apply(Map<Identifier, JsonElement> files) {
        loaded = parse(files);
        for (Runnable listener : APPLY_LISTENERS) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                SimpleLib.LOG.warn("In-world upgrade listener failed: {}", e.toString());
            }
        }
    }

    /** Runs after every {@link #apply} (JEI24: the recipe viewer adds the table once it arrives after JEI started). */
    public static void onApply(Runnable listener) {
        APPLY_LISTENERS.add(listener);
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
