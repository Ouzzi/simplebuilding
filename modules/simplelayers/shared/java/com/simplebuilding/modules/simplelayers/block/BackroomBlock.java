package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Backrooms block (wallpaper, damp carpet, ceiling tile, neon light, EXIT sign, exit door). Outside the Backrooms an ordinary
 * decoration block; inside, survival players cannot break it (concept §2.2 "Unzerstörbarkeit der Wände", config
 * {@code backrooms.unbreakableWalls}) - otherwise one would dig straight through the maze.
 */
public class BackroomBlock extends Block {
    public BackroomBlock(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} may not break Backrooms blocks where he stands. */
    public static boolean protectedFor(Player player) {
        return LayersConfig.unbreakableWalls && !player.isCreative() && player.level().dimension() == SimpleLayers.BACKROOMS;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return protectedFor(player) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }
}
