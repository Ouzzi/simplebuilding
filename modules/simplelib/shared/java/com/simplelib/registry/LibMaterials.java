package com.simplelib.registry;

import com.simplelib.SimpleLib;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Shared materials (split plan stage 3d, §2.5): one owner for the base materials several Simple mods use. SimpleLib
 * always registers them, so a world stays loadable whichever mod is missing; their ways into the world (recipes with the
 * condition {@code simplelib:material_requested}, the anvil crushing) only run when a loaded mod requested the group
 * ({@code SimpleLibApi.requestMaterial}). SimpleLib names none of its users.
 *
 * <p>Registration is idempotent: the loader entry and a partner's first {@code SimpleLibApi.materialItem} call both
 * reach {@link #ensureBlocks}/{@link #ensureItems}, so the order of the mods' registration events does not matter
 * (blocks in the block event, items in the item event).
 *
 * <p>IG31-13 (31.10.): Enderite left SimpleLib for Simple Ores ({@code simpleores:*}, ids move there). The name stays a
 * <em>provided</em> group: the mod that owns it calls {@link #provide}, and {@link #requested} (the condition
 * {@code simplelib:material_requested}) is true while a loaded mod provides it, so recipes of other mods can ask for it
 * without naming the owner. The six building cores ({@code cores}) moved here from SimpleBuilding: always registered,
 * a neutral {@link com.simplelib.item.CoreItem} whose right click a partner fills ({@code CoreHooks}).
 */
public final class LibMaterials {
    /** The six building cores in tier order (IG31-13). */
    public static final List<String> CORES = List.of("copper_core", "iron_core", "gold_core", "diamond_core", "netherite_core", "enderite_core");

    /** N48: the material pressure plates (block ids; the block items have the same ids). */
    public static final List<String> PLATES = List.of("diamond_pressure_plate", "netherite_pressure_plate", "enderite_pressure_plate", "emerald_pressure_plate");
    /** N48: the two Nether bookshelves of the group {@code nether_bookshelves}. */
    public static final List<String> NETHER_BOOKSHELVES = List.of("crimson_blazewood_bookshelf", "warped_blazewood_bookshelf");

    /** Group -> ids (items; the blocks have block items of the same id). */
    public static final Map<String, List<String>> GROUPS = Map.of(
            "cracked_diamond", List.of("diamond_pebble", "cracked_diamond", "cracked_diamond_block"),
            "netherite_nugget", List.of("netherite_nugget"),
            "upgrade_templates", List.of("basic_upgrade_template"),
            // WF25-10 (07.11.): the Strong Upgrade template ("chart") and its shard, formerly Simple Beacons'; used by Beacons and the wings.
            "strong_upgrade", List.of("strong_upgrade_template", "strong_upgrade_shard"),
            // IG31-13: the building cores (copper to enderite), formerly SimpleBuilding's; used by Beacons, Gadgets, the wands.
            "cores", CORES,
            // Module family stage 3 (rule 16): the building block several family mods share (Blocks, End, Enderite, Octets).
            "polished_end_stone", List.of("polished_end_stone", "polished_end_stone_stairs", "polished_end_stone_slab", "polished_end_stone_wall"),
            // N48 (14.11.): the pressure plates of materials, formerly Simple Pads' (diamond, netherite, enderite) and Simple Ores' (enderite); Emerald is new.
            "material_pressure_plates", PLATES,
            // N48 (14.11.): the plain Crimson/Warped blazewood bookshelves, building blocks for Simple Blocks and SimpleBuilding; Simple Enchanting
            // counts them as strong shelves. Always registered; the crafting recipe (without Simple Enchanting) only on request.
            "nether_bookshelves", NETHER_BOOKSHELVES);

    /** Groups another mod owns and registers itself; it announces them with {@link #provide}. */
    public static final Set<String> PROVIDED_GROUPS = Set.of("enderite");

    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> PROVIDED = ConcurrentHashMap.newKeySet();
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static BlockEntityType<com.simplelib.block.FilterPlateBlockEntity> filterPlate;

    private LibMaterials() {}

    public static void request(String group) {
        if (PROVIDED_GROUPS.contains(group)) return; // its owner registers it; requests change nothing
        if (!GROUPS.containsKey(group)) throw new IllegalArgumentException("unknown SimpleLib material group " + group + ", known: " + GROUPS.keySet());
        if (REQUESTED.add(group)) SimpleLib.LOG.info("SimpleLib material '{}' requested", group);
    }

    public static boolean requested(String group) {
        return REQUESTED.contains(group) || PROVIDED.contains(group);
    }

    /** The owner of a provided group ({@link #PROVIDED_GROUPS}) announces it, in its constructor / initializer. */
    public static void provide(String group) {
        if (!PROVIDED_GROUPS.contains(group)) throw new IllegalArgumentException("not a provided SimpleLib material group " + group + ", known: " + PROVIDED_GROUPS);
        if (PROVIDED.add(group)) SimpleLib.LOG.info("SimpleLib material '{}' provided", group);
    }

    /** Whether a loaded mod provides the group (Enderite: Simple Ores). */
    public static boolean provided(String group) {
        return PROVIDED.contains(group);
    }

    public static synchronized void ensureBlocks() {
        if (!BLOCKS.isEmpty()) return;
        block("cracked_diamond_block", BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).strength(7.0F, 14.0F));
        block("polished_end_stone", BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops()
                .strength(3.0F, 9.0F).sound(SoundType.STONE));
        // Stairs, slab and wall of it (octet rule, owner 17.10.): the properties copy the full block.
        plates();
        Block polished = BLOCKS.get("polished_end_stone");
        shape("polished_end_stone_stairs", polished, p -> new StairBlock(polished.defaultBlockState(), p));
        shape("polished_end_stone_slab", polished, SlabBlock::new);
        shape("polished_end_stone_wall", polished, p -> new WallBlock(p.forceSolidOn()));
        // N48: Nether bookshelves (a full copy of Vanilla's bookshelf with the Nether wood sound).
        for (String name : NETHER_BOOKSHELVES) shape(name, Blocks.BOOKSHELF, p -> new Block(p.sound(SoundType.NETHER_WOOD)));
    }

    /**
     * N48: the pressure plates, properties as Simple Pads had them (diamond, netherite, enderite: no collision, pistons cannot move
     * them); the block entity type of the two filter plates keeps the id of Simple Pads' (moved: {@code filter_pressure_plate_be}).
     */
    private static void plates() {
        block("diamond_pressure_plate", p -> new com.simplelib.block.DiamondPressurePlateBlock(BlockSetType.IRON,
                p.mapColor(MapColor.DIAMOND).noCollision().strength(0.5f).pushReaction(PushReaction.IMMOVEABLE)));
        block("netherite_pressure_plate", p -> new com.simplelib.block.FilterPressurePlateBlock(BlockSetType.IRON,
                p.mapColor(MapColor.COLOR_BLACK).noCollision().strength(4.0f).pushReaction(PushReaction.IMMOVEABLE), false));
        block("enderite_pressure_plate", p -> new com.simplelib.block.FilterPressurePlateBlock(BlockSetType.IRON,
                p.mapColor(MapColor.COLOR_PURPLE).noCollision().strength(5.0f).pushReaction(PushReaction.IMMOVEABLE), true));
        block("emerald_pressure_plate", p -> new com.simplelib.block.EmeraldPressurePlateBlock(
                p.mapColor(MapColor.EMERALD).noCollision().strength(0.5f).pushReaction(PushReaction.IMMOVEABLE)));
        filterPlate = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleLib.id("filter_pressure_plate_be"),
                new BlockEntityType<>(com.simplelib.block.FilterPlateBlockEntity::new, Set.of(BLOCKS.get("netherite_pressure_plate"), BLOCKS.get("enderite_pressure_plate"))));
    }

    /** The block entity type of the Netherite and Enderite plates (N48). */
    public static BlockEntityType<com.simplelib.block.FilterPlateBlockEntity> filterPlateType() {
        ensureBlocks();
        return filterPlate;
    }

    public static synchronized void ensureItems() {
        if (!ITEMS.isEmpty()) return;
        ensureBlocks();
        item("basic_upgrade_template", p -> upgradeTemplate("basic_upgrade_template", p));
        // WF25-10: mythic tier like before (RarityRules), the shard survives explosions like the Nether Star.
        item("strong_upgrade_template", LibMaterials::strongTemplate);
        item("strong_upgrade_shard", p -> new Item(p.rarity(Rarity.UNCOMMON).delayedComponent(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT,
                registries -> new net.minecraft.world.item.component.DamageResistant(registries.getOrThrow(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)))));
        item("diamond_pebble", Item::new);
        item("cracked_diamond", Item::new);
        item("cracked_diamond_block", p -> new BlockItem(BLOCKS.get("cracked_diamond_block"), p.useBlockDescriptionPrefix()));
        item("netherite_nugget", p -> new Item(p.fireResistant()));
        for (String name : PLATES) {
            item(name, p -> new BlockItem(BLOCKS.get(name), p.useBlockDescriptionPrefix()));
        }
        for (String name : List.of("polished_end_stone", "polished_end_stone_stairs", "polished_end_stone_slab", "polished_end_stone_wall")) {
            item(name, p -> new BlockItem(BLOCKS.get(name), p.useBlockDescriptionPrefix()));
        }
        for (String name : NETHER_BOOKSHELVES) item(name, p -> new BlockItem(BLOCKS.get(name), p.useBlockDescriptionPrefix()));
        // IG31-13: the building cores, numbers as SimpleBuilding had them (not stackable, rare; Netherite/Enderite fire resistant, Enderite epic).
        for (String name : CORES) {
            boolean fire = name.startsWith("netherite") || name.startsWith("enderite");
            Rarity rarity = name.startsWith("enderite") ? Rarity.EPIC : Rarity.RARE;
            item(name, p -> new com.simplelib.item.CoreItem((fire ? p.fireResistant() : p).stacksTo(1).rarity(rarity)));
        }
    }

    public static Item item(String id) {
        ensureItems();
        Item item = ITEMS.get(id);
        if (item == null) throw new IllegalArgumentException("not a SimpleLib material item: " + id);
        return item;
    }

    public static Block block(String id) {
        ensureBlocks();
        Block block = BLOCKS.get(id);
        if (block == null) throw new IllegalArgumentException("not a SimpleLib material block: " + id);
        return block;
    }

    /** Every material item id (tests, tab, info). */
    public static List<String> itemIds() {
        ensureItems();
        return List.copyOf(ITEMS.keySet());
    }

    private static void block(String name, BlockBehaviour.Properties properties) {
        Identifier id = SimpleLib.id(name);
        Block block = new Block(properties.setId(ResourceKey.create(Registries.BLOCK, id)));
        BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, id, block));
    }

    private static void block(String name, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = SimpleLib.id(name);
        BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)))));
    }

    private static void shape(String name, Block full, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = SimpleLib.id(name);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(full).setId(ResourceKey.create(Registries.BLOCK, id));
        BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(properties)));
    }

    private static void item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLib.id(name));
        ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, factory.apply(com.simplelib.api.RarityRules.apply(name, new Item.Properties()).setId(key))));
    }

    /** The Strong Upgrade (WF25-10): empty slot icon lists, texts in the SimpleLib lang (applies to beacons, conduits, wings). */
    private static SmithingTemplateItem strongTemplate(Item.Properties properties) {
        String key = "item." + SimpleLib.MOD_ID + ".strong_upgrade_template";
        return new SmithingTemplateItem(
                Component.translatable(key + ".applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".base_slot_description"),
                Component.translatable(key + ".additions_slot_description"),
                List.of(), List.of(), properties);
    }

    private static SmithingTemplateItem upgradeTemplate(String id, Item.Properties properties) {
        String key = "item." + SimpleLib.MOD_ID + "." + id;
        return new SmithingTemplateItem(
                Component.translatable(key + ".applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".base_slot_description"),
                Component.translatable(key + ".additions_slot_description"),
                List.of(Identifier.withDefaultNamespace("container/slot/pickaxe"), Identifier.withDefaultNamespace("container/slot/chestplate")),
                List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
                properties.stacksTo(64).rarity(Rarity.UNCOMMON));
    }
}
