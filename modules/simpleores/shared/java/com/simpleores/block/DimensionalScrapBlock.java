package com.simpleores.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dimensional Scrap (owner 2026-10-01, moved from SimpleBuilding in IG31-13): found in every dimension, a mysterious block
 * without a recipe. Only the tools in {@link #BREAKERS} make progress (with SimpleBuilding the Enderite pickaxe, with Simple
 * Hammers the Enderite sledgehammer; the tag names them with {@code required: false}); every other tool and the hand do not.
 * The hardness lets an unenchanted Enderite pickaxe (speed {@link #ENDERITE_SPEED}) need {@link #BREAK_SECONDS} seconds, as
 * long as obsidian by hand. On its own (no Enderite tool) the block stays unbreakable.
 */
public class DimensionalScrapBlock extends Block {
    /** Break time with an unenchanted Enderite pickaxe on the ground, in seconds. */
    public static final int BREAK_SECONDS = 250;
    /** Mining speed of the Enderite tool material (SimpleBuilding {@code ModToolMaterials.ENDERITE}). */
    public static final float ENDERITE_SPEED = 10.0F;
    /** Vanilla: progress per tick = tool speed / hardness / 30 with the right tool. */
    public static final float HARDNESS = BREAK_SECONDS * 20F * ENDERITE_SPEED / 30F;
    /** Tools that break the scrap. */
    public static final TagKey<Item> BREAKERS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simpleores", "breaks_dimensional_scrap"));

    public DimensionalScrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player.getMainHandItem().is(BREAKERS) ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }
}
