package com.simplebuilding.doors;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.jspecify.annotations.Nullable;

/**
 * Registration of Simple Doors (WP-17 / BS28-2). Oak came first (owner choice ABN29-3) and keeps its fields in
 * {@code ModBlocks}/{@code ModItems} (and its retired half doors); N57-1 (owner 14.11.: "for every door variant the bigger doors")
 * adds the four big formats ({@link #WIDE}) for every other door: the Vanilla woods, iron (redstone only, like Vanilla), the four
 * copper stages (they do not age - like waxed copper) and the doors of Simple Ores, Simple End and Simple Enchanting. All live in
 * SimpleBuilding's namespace (the big doors are SimpleBuilding's feature); the module variants copy their base door's Vanilla
 * counterpart (metal: copper door, End wood: oak, blazewood: crimson/warped) and only get recipes while their mod is loaded. Models,
 * recipes, loot and textures: tools/textures/doors_2026_10_29.py ({@code VARIANTS} there lists the same ids in the same order).
 */
public final class WideDoors {
    /** The wood whose doors came first. */
    public static final String OAK = "oak";
    /** The big formats every variant has (the half doors are oak only, IG31-9). */
    public static final List<DoorFormat> WIDE = List.of(DoorFormat.FULL_2X2, DoorFormat.FULL_2X3, DoorFormat.WIDE_1_5X2, DoorFormat.WIDE_1_5X3);

    /**
     * One door variant besides oak: block id prefix, the base door (any namespace), whether it is wood (axe; else pickaxe), the
     * Vanilla door whose properties and block set it copies, and the mod its base door belongs to (null: Vanilla).
     */
    public record Variant(String id, Identifier door, boolean wood, Block like, @Nullable String mod) {
        public BlockSetType type() {
            return like instanceof DoorBlock door ? door.type() : BlockSetType.OAK;
        }
    }

    public static final List<Variant> VARIANTS = variants();
    /** Variant id (oak included) -> format -> block / item, in registration order. */
    public static final Map<String, Map<DoorFormat, Block>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Map<DoorFormat, Item>> ITEMS = new LinkedHashMap<>();

    private WideDoors() {}

    private static List<Variant> variants() {
        List<Variant> out = new ArrayList<>();
        for (String wood : List.of("spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")) {
            out.add(vanilla(wood, true));
        }
        for (String metal : List.of("iron", "copper", "exposed_copper", "weathered_copper", "oxidized_copper")) {
            out.add(vanilla(metal, false));
        }
        Block copper = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("copper_door"));
        for (String metal : List.of("brass", "bronze", "lead", "nickel", "silver", "stainless_steel", "tin", "zinc")) {
            out.add(new Variant(metal, Identifier.fromNamespaceAndPath("simpleores", metal + "_door"), false, copper, "simpleores"));
        }
        for (String wood : List.of("ashwood", "duskwood")) {
            out.add(new Variant(wood, Identifier.fromNamespaceAndPath("simpleend", wood + "_door"), true, Blocks.OAK_DOOR, "simpleend"));
        }
        out.add(new Variant("crimson_blazewood", Identifier.fromNamespaceAndPath("simpleenchanting", "crimson_blazewood_door"), true, Blocks.CRIMSON_DOOR, "simpleenchanting"));
        out.add(new Variant("warped_blazewood", Identifier.fromNamespaceAndPath("simpleenchanting", "warped_blazewood_door"), true, Blocks.WARPED_DOOR, "simpleenchanting"));
        return List.copyOf(out);
    }

    private static Variant vanilla(String id, boolean wood) {
        Identifier door = Identifier.withDefaultNamespace(id + "_door");
        return new Variant(id, door, wood, BuiltInRegistries.BLOCK.getValue(door), null);
    }

    /** The oak door block of a format; immovable for pistons (a door torn in two would be worse than a door that stays). */
    public static Block registerOak(DoorFormat format) {
        Block block = register(OAK, format, Blocks.OAK_DOOR, BlockSetType.OAK);
        BLOCKS.computeIfAbsent(OAK, k -> new LinkedHashMap<>()).put(format, block);
        return block;
    }

    private static Block register(String wood, DoorFormat format, Block like, BlockSetType type) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, format.id(wood));
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(like).setId(key).pushReaction(McVersion.PUSH_BLOCKED);
        return Registry.register(BuiltInRegistries.BLOCK, id, new WideDoorBlock(format, type, properties));
    }

    public static Item registerItem(Block block, DoorFormat format) {
        return registerItem(block, format, OAK);
    }

    private static Item registerItem(Block block, DoorFormat format, String wood) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, format.id(wood));
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = Registry.register(BuiltInRegistries.ITEM, id, new WideDoorItem(block, new Item.Properties().setId(key)));
        ITEMS.computeIfAbsent(wood, k -> new LinkedHashMap<>()).put(format, item);
        return item;
    }

    /** N57-1: the blocks of every variant besides oak (called once from ModBlocks, after the oak doors). */
    public static void registerBlocks() {
        for (Variant v : VARIANTS) {
            Map<DoorFormat, Block> formats = BLOCKS.computeIfAbsent(v.id(), k -> new LinkedHashMap<>());
            for (DoorFormat format : WIDE) formats.put(format, register(v.id(), format, v.like(), v.type()));
        }
    }

    /** N57-1: the items of every variant besides oak (called once from ModItems, after the oak doors). */
    public static void registerItems() {
        for (Variant v : VARIANTS) {
            for (DoorFormat format : WIDE) registerItem(BLOCKS.get(v.id()).get(format), format, v.id());
        }
    }

    /** The four big door items of a variant (oak included), in format order. */
    public static Item[] items(String wood) {
        Map<DoorFormat, Item> formats = ITEMS.get(wood);
        return WIDE.stream().map(formats::get).toArray(Item[]::new);
    }
}
