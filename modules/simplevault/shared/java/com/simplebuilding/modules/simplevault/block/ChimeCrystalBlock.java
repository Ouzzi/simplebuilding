package com.simplebuilding.modules.simplevault.block;

import com.simplebuilding.modules.simplevault.VaultPuzzles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Chime Crystal (UNDER21): four tones of the sound riddle, each looking like its block in the frieze (amethyst, prismarine,
 * copper, quartz). Right-click strikes it ({@link VaultPuzzles#strike}); unbreakable, faint light.
 */
public class ChimeCrystalBlock extends Block {
    public static final IntegerProperty TONE = IntegerProperty.create("tone", 0, 3);

    public ChimeCrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TONE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TONE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) VaultPuzzles.strike(server, pos, state.getValue(TONE), player);
        return InteractionResult.SUCCESS;
    }
}
