package com.simplebuilding.blocks.custom;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Stufe aus einem Naturblock (Besitzer N31): zwei Stufen derselben Art ergeben wieder den vollen Vanilla-Block (Erde,
 * Grasblock, Sand, Kies) statt einer Doppelstufe. Beim Setzen der zweiten Stufe liefert {@link #getStateForPlacement}
 * direkt den vollen Block; {@link FallingSlabBlock} macht beim Landen dasselbe.
 */
public class MergingSlabBlock extends SlabBlock {
    private final Block fullBlock;

    public MergingSlabBlock(Properties settings, Block fullBlock) {
        super(settings);
        this.fullBlock = fullBlock;
    }

    /** Der volle Block, zu dem zwei Stufen werden. */
    public BlockState fullState() {
        return fullBlock.defaultBlockState();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.is(this) && state.getValue(TYPE) == SlabType.DOUBLE ? fullState() : state;
    }

}
