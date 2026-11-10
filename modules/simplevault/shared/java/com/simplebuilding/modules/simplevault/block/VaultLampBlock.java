package com.simplebuilding.modules.simplevault.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Vault Lamp (UNDER21): the lamps of the light riddle and its mural; lit only by the riddle (no redstone), unbreakable. */
public class VaultLampBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public VaultLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    public static int light(BlockState state) {
        return state.getValue(LIT) ? 15 : 0;
    }
}
