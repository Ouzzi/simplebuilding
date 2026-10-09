package com.simplebuilding.woodwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crawling into a lying hollow log: a sneaking player who stands in front of (or in) a horizontal tube and faces
 * along its axis takes the swimming/crawling pose, so the 0.6-block box fits through the opening. Once inside,
 * Vanilla keeps the pose (standing and crouching no longer fit). Runs on both sides from
 * {@code HollowLogCrawlMixin} (Player#updatePlayerPose).
 */
public final class HollowLogCrawl {
    private HollowLogCrawl() {
    }

    public static boolean shouldCrawl(Player player) {
        if (!player.isShiftKeyDown() || player.isSpectator() || player.isPassenger() || player.getAbilities().flying
                || player.isFallFlying()) {
            return false;
        }
        Level level = player.level();
        Direction facing = player.getDirection();
        BlockPos feet = player.blockPosition();
        return alignedTube(level.getBlockState(feet), facing) || alignedTube(level.getBlockState(feet.relative(facing)), facing);
    }

    private static boolean alignedTube(BlockState state, Direction facing) {
        return state.getBlock() instanceof HollowLogBlock && state.getValue(RotatedPillarBlock.AXIS) == facing.getAxis();
    }
}
