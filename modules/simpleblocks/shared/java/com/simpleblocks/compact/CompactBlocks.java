package com.simpleblocks.compact;

import com.simpleblocks.SimpleBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * WH26-10 (docs/ai/PLAN-A-COMPACT-2026-11-06.md): Compressed (stage 1 = 9 base blocks), Dense (stage 2 = 9 Compressed) and Collapsed (stage 3 = 9 Dense) versions of
 * common Vanilla building blocks, every step reversible at the crafting table (1 -> 9). Each copies the properties of its base
 * block (sound, tool, map colour), only the hardness is {@link #FACTOR} times that of the stage below: mining takes about 4x
 * (Compressed), 16x (Dense) and 64x (Collapsed) as long. Collapsed items slow their carrier ({@link CollapsedEffect}). They are plain blocks: Sand and Gravel do not fall. Vanilla impact (A12): low, nothing
 * of Vanilla changes; it is only a way to store and build with large amounts. Textures are Vanilla-derived at build time.
 */
public final class CompactBlocks {
    /** Hardness factor per stage. */
    public static final float FACTOR = 4f;
    /** Stage prefixes, in order. */
    public static final List<String> STAGES = List.of("compressed", "dense", "collapsed");
    /** The last stage: slows its carrier down ({@link CollapsedEffect}). */
    public static final String COLLAPSED_STAGE = "collapsed";

    /** Base block path -> (Vanilla base block, hardness, blast resistance); the order is the tab order and matches generate_data.COMPACT_BASES. */
    public record Base(String path, Block block, float hardness, float resistance) {}

    public static final List<Base> BASES = List.of(
            new Base("dirt", Blocks.DIRT, 0.5f, 0.5f), new Base("mud", Blocks.MUD, 0.5f, 0.5f),
            new Base("cobblestone", Blocks.COBBLESTONE, 2.0f, 6.0f), new Base("stone", Blocks.STONE, 1.5f, 6.0f),
            new Base("sand", Blocks.SAND, 0.5f, 0.5f), new Base("red_sand", Blocks.RED_SAND, 0.5f, 0.5f),
            new Base("gravel", Blocks.GRAVEL, 0.6f, 0.6f), new Base("clay", Blocks.CLAY, 0.6f, 0.6f),
            new Base("netherrack", Blocks.NETHERRACK, 0.4f, 0.4f), new Base("cobbled_deepslate", Blocks.COBBLED_DEEPSLATE, 3.5f, 6.0f),
            new Base("andesite", Blocks.ANDESITE, 1.5f, 6.0f), new Base("diorite", Blocks.DIORITE, 1.5f, 6.0f),
            new Base("granite", Blocks.GRANITE, 1.5f, 6.0f), new Base("tuff", Blocks.TUFF, 1.5f, 6.0f),
            new Base("sandstone", Blocks.SANDSTONE, 0.8f, 0.8f), new Base("red_sandstone", Blocks.RED_SANDSTONE, 0.8f, 0.8f),
            new Base("end_stone", Blocks.END_STONE, 3.0f, 9.0f), new Base("blackstone", Blocks.BLACKSTONE, 1.5f, 6.0f),
            new Base("soul_sand", Blocks.SOUL_SAND, 0.5f, 0.5f));

    /** Block path ("compact_dirt", "dense_dirt", ...) -> block, in tab order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** The Collapsed blocks (stage 3). */
    public static final java.util.Set<Block> COLLAPSED = new java.util.HashSet<>();

    static {
        for (Base base : BASES) {
            float hardness = base.hardness();
            for (String stage : STAGES) {
                hardness *= FACTOR;
                String name = stage + "_" + base.path();
                ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleBlocks.id(name));
                Block block = new Block(BlockBehaviour.Properties.ofFullCopy(base.block()).strength(hardness, base.resistance()).setId(key));
                BLOCKS.put(name, block);
                if (stage.equals(COLLAPSED_STAGE)) COLLAPSED.add(block);
            }
        }
    }

    /** Every block path in registration order. */
    public static List<String> names() {
        return List.copyOf(BLOCKS.keySet());
    }

    public static void registerBlocks() {
        BLOCKS.forEach((name, block) -> Registry.register(BuiltInRegistries.BLOCK, SimpleBlocks.id(name), block));
    }

    public static void registerItems() {
        BLOCKS.forEach((name, block) -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleBlocks.id(name));
            Item.Properties props = new Item.Properties().setId(key).useBlockDescriptionPrefix();
            if (COLLAPSED.contains(block)) {
                props.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                        net.minecraft.network.chat.Component.translatable("tooltip.simpleblocks.collapsed.1").withStyle(net.minecraft.ChatFormatting.GRAY),
                        net.minecraft.network.chat.Component.translatable("tooltip.simpleblocks.collapsed.2").withStyle(net.minecraft.ChatFormatting.DARK_RED))));
            }
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, props)));
        });
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
        return out;
    }

    private CompactBlocks() {}
}
