package com.simplebuilding.modules.simplevault.block;

import com.simplebuilding.modules.simplevault.VaultPuzzles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Path Tile (UNDER21): floor of the path riddle; stepping on it counts ({@link VaultPuzzles#step}), a right step lights it. */
public class PathTileBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public PathTileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    public static int light(BlockState state) {
        return state.getValue(LIT) ? 7 : 0;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player player && !player.isSpectator()) VaultPuzzles.step(server, pos, player);
        super.stepOn(level, pos, state, entity);
    }
}
