package com.simplefoundry.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The fire grate (heater): a hull block that glows while its foundry burns fuel. The desk sets {@link #LIT}; the grate has no logic of its own. */
public class GrateBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public GrateBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
