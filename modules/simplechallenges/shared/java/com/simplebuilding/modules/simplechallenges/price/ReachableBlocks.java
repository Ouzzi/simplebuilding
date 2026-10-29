package com.simplebuilding.modules.simplechallenges.price;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Which blocks give a usable item to bare hands: the item a player gets from this block without a tool (or null). */
public final class ReachableBlocks {
    private ReachableBlocks() {}

    public static String itemFor(BlockState state) {
        if (state.isAir()) return null;
        if (state.is(BlockTags.LOGS)) return BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()).toString();
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) || state.is(Blocks.ROOTED_DIRT)) return "minecraft:dirt";
        if (state.is(Blocks.COARSE_DIRT)) return "minecraft:coarse_dirt";
        if (state.is(Blocks.SAND)) return "minecraft:sand";
        if (state.is(Blocks.RED_SAND)) return "minecraft:red_sand";
        if (state.is(Blocks.GRAVEL)) return "minecraft:gravel";
        if (state.is(Blocks.MUD)) return "minecraft:mud";
        if (state.is(Blocks.CACTUS)) return "minecraft:cactus";
        if (state.is(Blocks.MOSS_BLOCK)) return "minecraft:moss_block";
        return null;
    }
}
