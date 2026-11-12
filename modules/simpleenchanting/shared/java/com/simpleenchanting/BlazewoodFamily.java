package com.simpleenchanting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * The blazewood family (PLAN40 simpleenchanting-1, docs/ai/PLAN-A-W1C-2026-11-12.md): stairs, slab, fence, fence gate, door,
 * trapdoor, pressure plate and button of the crimson and warped blazewood planks, like Vanilla's crimson/warped ones. Always this
 * module's namespace, with and without SimpleBuilding (which owns the planks): no SimpleBuilding datagen, recipes take the planks
 * by the item tag {@code simpleenchanting:<wood>_blazewood_planks} (both ids). Properties are full copies of Vanilla's crimson/warped
 * blocks (Nether wood: no fire, no fuel), block set and wood type are Vanilla's {@code CRIMSON}/{@code WARPED} (sounds, door and gate
 * behaviour, no new types, no mixins). The stairs stand on Vanilla's planks of their wood (load order free). Protected Vanilla
 * constructors are reached through empty subclasses, as Simple End's woods do.
 */
public final class BlazewoodFamily {
    /** The two woods, in tab order. */
    public static final List<String> WOODS = List.of("crimson", "warped");
    /** The shapes after the planks, in Vanilla's creative order of a wood row. */
    public static final List<String> SHAPES = List.of("stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "pressure_plate", "button");
    /** id path -> block, in registration order (wood by wood, shapes in {@link #SHAPES} order). */
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    private BlazewoodFamily() {}

    /** {@code crimson_blazewood_stairs} and so on. */
    public static String path(String wood, String shape) {
        return wood + "_blazewood_" + shape;
    }

    /** Every family id path, wood by wood (independent of what is registered: tests, station, tab). */
    public static List<String> paths() {
        List<String> out = new ArrayList<>();
        for (String wood : WOODS) {
            for (String shape : SHAPES) out.add(path(wood, shape));
        }
        return out;
    }

    /** The family paths of one wood, in tab order. */
    public static List<String> paths(String wood) {
        return SHAPES.stream().map(shape -> path(wood, shape)).toList();
    }

    public static Map<String, Block> blocks() {
        return java.util.Collections.unmodifiableMap(BLOCKS);
    }

    public static Map<String, Item> items() {
        return java.util.Collections.unmodifiableMap(ITEMS);
    }

    static void registerBlocks() {
        if (!BLOCKS.isEmpty()) return;
        for (String wood : WOODS) {
            boolean crimson = "crimson".equals(wood);
            BlockSetType set = crimson ? BlockSetType.CRIMSON : BlockSetType.WARPED;
            WoodType type = crimson ? WoodType.CRIMSON : WoodType.WARPED;
            Block planks = crimson ? Blocks.CRIMSON_PLANKS : Blocks.WARPED_PLANKS;
            block(wood, "stairs", crimson ? Blocks.CRIMSON_STAIRS : Blocks.WARPED_STAIRS, p -> new StairBlock(planks.defaultBlockState(), p) {});
            block(wood, "slab", crimson ? Blocks.CRIMSON_SLAB : Blocks.WARPED_SLAB, SlabBlock::new);
            block(wood, "fence", crimson ? Blocks.CRIMSON_FENCE : Blocks.WARPED_FENCE, FenceBlock::new);
            block(wood, "fence_gate", crimson ? Blocks.CRIMSON_FENCE_GATE : Blocks.WARPED_FENCE_GATE, p -> new FenceGateBlock(type, p));
            block(wood, "door", crimson ? Blocks.CRIMSON_DOOR : Blocks.WARPED_DOOR, p -> new DoorBlock(set, p) {});
            block(wood, "trapdoor", crimson ? Blocks.CRIMSON_TRAPDOOR : Blocks.WARPED_TRAPDOOR, p -> new TrapDoorBlock(set, p) {});
            block(wood, "pressure_plate", crimson ? Blocks.CRIMSON_PRESSURE_PLATE : Blocks.WARPED_PRESSURE_PLATE, p -> new PressurePlateBlock(set, p) {});
            // Vanilla's wooden buttons stay pressed 30 ticks.
            block(wood, "button", crimson ? Blocks.CRIMSON_BUTTON : Blocks.WARPED_BUTTON, p -> new ButtonBlock(set, 30, p) {});
        }
    }

    static void registerItems() {
        if (!ITEMS.isEmpty()) return;
        BLOCKS.forEach((path, block) -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleEnchanting.id(path));
            Item.Properties props = new Item.Properties().setId(key);
            // Like Vanilla's crimson/warped items: no cookingFuel (Nether wood does not burn); the door places both halves.
            Item item = path.endsWith("_door") ? new DoubleHighBlockItem(block, props) : new BlockItem(block, props);
            ITEMS.put(path, Registry.register(BuiltInRegistries.ITEM, key, item));
        });
    }

    private static void block(String wood, String shape, Block vanilla, Function<BlockBehaviour.Properties, Block> factory) {
        String path = path(wood, shape);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleEnchanting.id(path));
        BLOCKS.put(path, Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(vanilla).setId(key))));
    }
}
