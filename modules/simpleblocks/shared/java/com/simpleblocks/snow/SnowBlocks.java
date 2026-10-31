package com.simpleblocks.snow;

import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * SN30 (b-snow32, docs/ai/PLAN-B-SNOW32-2026-10-31.md): the snowlogged cell ({@link SnowloggedBlock}, no item - it is made by
 * using a snow layer on a fence, flower or grass, see {@link Snowlogging}) and frost grass ({@link FrostGrassBlock}). Kept
 * apart from {@link com.simpleblocks.nature.NatureBlocks}, whose list is tied to the id moves from SimpleBuilding. The fields
 * are set when the loader registers them (null before, and on loaders that do not register them; the mixins check that).
 */
public final class SnowBlocks {
    public static final String SNOWLOGGED_NAME = "snowlogged";
    public static final String FROST_GRASS_NAME = "frost_grass";
    /** Blocks a snow layer can share its cell with (fences, small flowers, grass, ferns, bushes). */
    public static final TagKey<Block> SNOWLOGGABLE = TagKey.create(Registries.BLOCK, SimpleBlocks.id("snowloggable"));

    public static Block SNOWLOGGED;
    public static Block FROST_GRASS;
    public static Item FROST_GRASS_ITEM;
    public static BlockEntityType<SnowloggedBlockEntity> SNOWLOGGED_BE;

    private SnowBlocks() {}

    public static void registerBlocks() {
        ResourceKey<Block> cell = ResourceKey.create(Registries.BLOCK, SimpleBlocks.id(SNOWLOGGED_NAME));
        // Vanilla snow's properties without "replaceable" (a block placed on it must not wipe out the fence) and without
        // "requires the correct tool" (the held block drops with any tool); dynamic shape: snow plus the held block.
        SNOWLOGGED = Registry.register(BuiltInRegistries.BLOCK, cell, new SnowloggedBlock(BlockBehaviour.Properties.of().setId(cell)
                .mapColor(MapColor.SNOW).forceSolidOff().randomTicks().strength(0.1F).sound(SoundType.SNOW)
                .pushReaction(PushReaction.DESTROY).dynamicShape().noLootTable()));
        ResourceKey<Block> frost = ResourceKey.create(Registries.BLOCK, SimpleBlocks.id(FROST_GRASS_NAME));
        FROST_GRASS = Registry.register(BuiltInRegistries.BLOCK, frost,
                new FrostGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).setId(frost).mapColor(MapColor.ICE)));
    }

    public static void registerItems() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleBlocks.id(FROST_GRASS_NAME));
        FROST_GRASS_ITEM = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(FROST_GRASS, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    /** The loader builds the type (constructor differs between loaders), like Simple Octets' mixed cell. */
    public static void registerBlockEntity(BlockEntityType<SnowloggedBlockEntity> type) {
        SNOWLOGGED_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleBlocks.id(SNOWLOGGED_NAME), type);
    }
}
