package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The plain Light Panes (normal, transparent, the sixteen colors): a lamp that shines ({@link #LIT}) only while it gets a redstone
 * signal (dark when unpowered, N65). The look (frame, veil, color) is the model's; the behavior is the same for all of them.
 */
public class LightPaneBlock extends AbstractLightPaneBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LightPaneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.UP).setValue(WATERLOGGED, false)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    protected BlockState applySignal(BlockState state, Level level, BlockPos pos) {
        return state.setValue(LIT, level.hasNeighborSignal(pos));
    }

    public static int light(BlockState state) {
        return state.getValue(LIT) ? LIGHT : 0;
    }
}
